/**
 *  Copyright 2012 Wordnik, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.wordnik.swaggersocket.server;

import com.wordnik.swaggersocket.protocol.CloseMessage;
import com.wordnik.swaggersocket.protocol.HandshakeMessage;
import com.wordnik.swaggersocket.protocol.Header;
import com.wordnik.swaggersocket.protocol.Heartbeat;
import com.wordnik.swaggersocket.protocol.Message;
import com.wordnik.swaggersocket.protocol.ProtocolBase;
import com.wordnik.swaggersocket.protocol.QueryString;
import com.wordnik.swaggersocket.protocol.Request;
import com.wordnik.swaggersocket.protocol.Response;
import com.wordnik.swaggersocket.protocol.ResponseMessage;
import com.wordnik.swaggersocket.protocol.StatusMessage;
import org.atmosphere.client.TrackMessageSizeFilter;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.service.AtmosphereInterceptorService;
import org.atmosphere.cpr.Action;
import org.atmosphere.cpr.AsyncIOInterceptor;
import org.atmosphere.cpr.AsyncIOInterceptorAdapter;
import org.atmosphere.cpr.AsyncIOWriter;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereInterceptorAdapter;
import org.atmosphere.cpr.AtmosphereInterceptorWriter;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.DefaultBroadcaster;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.atmosphere.cpr.FrameworkConfig.INJECTED_ATMOSPHERE_RESOURCE;

@AtmosphereInterceptorService
public class SwaggerSocketProtocolInterceptor extends AtmosphereInterceptorAdapter {

    private final static String SWAGGER_SOCKET_DISPATCHED = "request.dispatched";
    private final static String IDENTITY = "swaggersocket.identity";
    private final static String RESPONSE_COUNTER = "-ResponseCountNumber";
    private final static String SUSPENDED_RESPONSE = "-PendingResource";


    private static final Logger logger = LoggerFactory.getLogger(SwaggerSocketProtocolInterceptor.class);
    private final ObjectMapper mapper;
    private boolean delegateHandshake = false;
    private final AsyncIOInterceptor interceptor = new Interceptor();
    private final ThreadLocal<Request> ssRequest = new ThreadLocal<Request>();
    private final ThreadLocal<String> transactionIdentity = new ThreadLocal<String>();
    private final Broadcaster heartbeat;

    public SwaggerSocketProtocolInterceptor() {
        this.mapper = new ObjectMapper();
        this.heartbeat = BroadcasterFactory.getDefault().get(DefaultBroadcaster.class, "/swaggersocket.heatbeat");
    }

    @Override
    public void configure(AtmosphereConfig config) {
    }

    @Override
    public Action inspect(final AtmosphereResource r) {

        final AtmosphereRequest request = r.getRequest();
        r.addEventListener(new AtmosphereResourceEventListenerAdapter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void onSuspend(AtmosphereResourceEvent event) {
                AsyncIOWriter writer = event.getResource().getResponse().getAsyncIOWriter();
                if (writer == null) {
                    writer = new AtmosphereInterceptorWriter();
                    r.getResponse().asyncIOWriter(writer);
                }

                if (AtmosphereInterceptorWriter.class.isAssignableFrom(writer.getClass())) {
                    AtmosphereInterceptorWriter.class.cast(writer).interceptor(interceptor);
                }
            }
        });

        boolean ok = false;
        if (request.getHeader("SwaggerSocket") != null) {
            ok = true;
        }

        if (ok && request.getAttribute(SWAGGER_SOCKET_DISPATCHED) == null) {

            AtmosphereResponse response = r.getResponse();
            response.setContentType("application/json");

            logger.debug("Method {} Transport {}", request.getMethod(), r.transport());
            // Suspend to keep the connection OPEN.
            if (request.getMethod() == "GET" && r.transport().equals(AtmosphereResource.TRANSPORT.LONG_POLLING)) {
                r.resumeOnBroadcast(true).suspend();

                BlockingQueue<AtmosphereResource> queue = (BlockingQueue<AtmosphereResource>)
                        getContextValue(request, SUSPENDED_RESPONSE);
                if (queue == null) {
                    queue = new LinkedBlockingQueue<AtmosphereResource>();
                    request.getSession().setAttribute(SUSPENDED_RESPONSE, queue);
                }
                queue.offer(r);

                String identity = (String) getContextValue(request, IDENTITY);
                schedule(r, identity);

                return Action.SUSPEND;
            }

            AtmosphereFramework framework = r.getAtmosphereConfig().framework();
            StringBuilder d = new StringBuilder();
            try {
                InputStreamReader isr = new InputStreamReader(request.getInputStream());
                BufferedReader bufReader = new BufferedReader(isr);
                char[] charBuffer = new char[8192];

                for (int readCount = bufReader.read(charBuffer); readCount > -1; readCount = bufReader.read(charBuffer)) {
                    d.append(charBuffer, 0, readCount);
                }

                String data = d.toString();

                if (data.length() == 0) {
                    return Action.CANCELLED;
                }

                String message = data.substring(0, 20).replaceAll(" ", "");
                logger.debug(data);
                if (message.startsWith("{\"handshake\"")) {
                    // This will fail if the message is not well formed.
                    HandshakeMessage handshakeMessage = mapper.readValue(data, HandshakeMessage.class);

                    // If we missed the CloseReason for whatever reason (IE is a good candidate), make sure we swap the previous session anyway.
                    String identity = (String) getContextValue(request, IDENTITY);
                    if (identity == null) {
                        identity = UUID.randomUUID().toString();
                    } else {
                        logger.debug("Client disconnected {}, cleaning session {}", identity);
                        try {
                            Enumeration<String> e = request.getSession().getAttributeNames();
                            while (e.hasMoreElements()) {
                                request.getSession().removeAttribute(e.nextElement());
                            }
                        } catch (Exception ex) {
                            logger.warn("", ex);
                        }
                    }
                    addContextValue(request, IDENTITY, identity);

                    StatusMessage statusMessage = new StatusMessage.Builder().status(new StatusMessage.Status(200, "OK"))
                            .identity(identity).build();
                    response.setContentType("application/json");
                    response.getOutputStream().write(mapper.writeValueAsBytes(statusMessage));

                    if (r.transport() == AtmosphereResource.TRANSPORT.WEBSOCKET) {
                        schedule(r, identity);
                    }

                    if (!delegateHandshake) {
                        return Action.CANCELLED;
                    }
                } else if (message.startsWith("{\"close\"")) {
                    CloseMessage c = mapper.readValue(data, CloseMessage.class);

                    logger.debug("Client disconnected {} with reason {}", c.getClose().getIdentity(), c.getClose().getReason());
                    try {
                        request.getSession().invalidate();
                    } catch (Exception ex) {
                        logger.warn("", ex);
                    }
                    return Action.CANCELLED;
                } else {
                    Message swaggerSocketMessage = mapper.readValue(data, Message.class);
                    swaggerSocketMessage.transactionID(UUID.randomUUID().toString());

                    String identity = (String) getContextValue(request, IDENTITY);

                    if (!swaggerSocketMessage.getIdentity().equals(identity)) {
                        StatusMessage statusMessage = new StatusMessage.Builder().status(new StatusMessage.Status(503, "Not Allowed"))
                                .identity(swaggerSocketMessage.getIdentity()).build();
                        response.getOutputStream().write(mapper.writeValueAsBytes(statusMessage));
                        return Action.CANCELLED;
                    }

                    transactionIdentity.set(swaggerSocketMessage.transactionID());

                    List<Request> requests = swaggerSocketMessage.getRequests();
                    addContextValue(request, swaggerSocketMessage.transactionID() + RESPONSE_COUNTER, new AtomicInteger(requests.size()));

                    AtmosphereRequest ar;
                    for (Request req : requests) {
                        ar = toAtmosphereRequest(request, req);
                        try {
                            ar.setAttribute(SWAGGER_SOCKET_DISPATCHED, "true");

                            // This is a new request, we must clean the Websocket AtmosphereResource.
                            request.removeAttribute(INJECTED_ATMOSPHERE_RESOURCE);
                            response.request(ar);
                            attachWriter(r);
                            ssRequest.set(req);
                            request.setAttribute("swaggerSocketRequest", req);

                            Action action = framework.doCometSupport(ar, response);
                            if (action.type() == Action.TYPE.SUSPEND) {
                                ar.destroyable(false);
                                response.destroyable(false);
                            }
                        } catch (ServletException e) {
                            logger.warn("", e);
                            return Action.CANCELLED;
                        }
                    }
                }
                return Action.CANCELLED;
            } catch (IOException e) {
                logger.warn("", e);
                return Action.CONTINUE;
            }

        } else {
            request.setAttribute(SWAGGER_SOCKET_DISPATCHED, null);
            if (!ok) {
                request.setAttribute(TrackMessageSizeInterceptor.SKIP_INTERCEPTOR, "true");
            }
        }
        return Action.CONTINUE;
    }

    private final void attachWriter(final AtmosphereResource r) {
        final AtmosphereRequest request = r.getRequest();

        AtmosphereResponse res = r.getResponse();
        AsyncIOWriter writer = res.getAsyncIOWriter();

        BlockingQueue<AtmosphereResource> queue = (BlockingQueue<AtmosphereResource>)
                getContextValue(request, SUSPENDED_RESPONSE);
        if (queue == null) {
            queue = new LinkedBlockingQueue<AtmosphereResource>();
            request.getSession().setAttribute(SUSPENDED_RESPONSE, queue);
        }

        if (AtmosphereInterceptorWriter.class.isAssignableFrom(writer.getClass())) {
            // WebSocket already had one.
            if (r.transport() != AtmosphereResource.TRANSPORT.WEBSOCKET) {
                writer = new AtmosphereInterceptorWriter() {

                    @Override
                    protected void writeReady(AtmosphereResponse response, byte[] data) throws IOException {

                        // We are buffering response.
                        if (data == null) return;

                        BlockingQueue<AtmosphereResource> queue =
                                (BlockingQueue<AtmosphereResource>) getContextValue(request, SUSPENDED_RESPONSE);
                        if (queue != null) {
                            AtmosphereResource resource;
                            try {
                                // TODO: Should this be configurable
                                // We stay suspended for 60 seconds
                                resource = queue.poll(60, TimeUnit.SECONDS);
                            } catch (InterruptedException e) {
                                logger.trace("", e);
                                return;
                            }

                            if (resource == null) {
                                logger.debug("No resource was suspended, resuming the second connection.");
                            } else {

                                logger.trace("Resuming {}", resource.uuid());

                                try {
                                    OutputStream o = resource.getResponse().getResponse().getOutputStream();
                                    o.write(data);
                                    o.flush();

                                    resource.resume();
                                } catch (IOException ex) {
                                    logger.warn("", ex);
                                }
                            }
                        } else {
                            logger.error("Queue was null");
                        }
                    }

                    /**
                     * Add an {@link AsyncIOInterceptor} that will be invoked in the order it was added.
                     *
                     * @param filter {@link AsyncIOInterceptor
                     * @return this
                     */
                    public AtmosphereInterceptorWriter interceptor(AsyncIOInterceptor filter) {
                        if (!filters.contains(filter)) {
                            filters.addLast(filter);
                        }
                        return this;
                    }
                };
                res.asyncIOWriter(writer);
            }
            AtmosphereInterceptorWriter.class.cast(writer).interceptor(interceptor);
        }
    }

    protected void schedule(AtmosphereResource r, String uuid) {
        heartbeat.addAtmosphereResource(r).scheduleFixedBroadcast("heartbeat-" + uuid, 60, 60, TimeUnit.SECONDS);
    }

    protected final static AtmosphereRequest toAtmosphereRequest(AtmosphereRequest r, ProtocolBase request) {
        AtmosphereRequest.Builder b = new AtmosphereRequest.Builder();

        if (request.getHeaders() != null) {
            for (Header h : request.getHeaders()) {
                r.header(h.getName(), h.getValue());
            }
        }

        Map<String, String[]> queryStrings = new HashMap<String, String[]>();
        if (request.getQueryString() != null) {
            for (QueryString h : request.getQueryString()) {
                String[] s = queryStrings.get(h.getName());
                if (s != null) {
                    String[] s1 = new String[s.length];
                    System.arraycopy(s, 0, s1, 0, s.length);
                    s1[s.length] = h.getValue();
                    queryStrings.put(h.getName(), s1);
                } else {
                    queryStrings.put(h.getName(), new String[]{h.getValue()});
                }
            }
        }

        String p = request.getPath().replaceAll("\\s+", "%20").trim();
        String requestURL = r.getRequestURL() + p;
        if (r.getRequestURL().toString().endsWith("/") && p.startsWith("/")) {
            requestURL = r.getRequestURL().toString() + p.substring(1);
        }

        String requestURI = r.getRequestURI() + p;
        if (r.getRequestURI().endsWith("/") && p.startsWith("/")) {
            requestURI = r.getRequestURI() + p.substring(1);
        }

        if (!p.startsWith("/")) {
            p = "/" + p;
        }

        b.pathInfo(p)
                .contentType(request.getDataFormat())
                .method(request.getMethod())
                .queryStrings(queryStrings)
                .requestURI(requestURI)
                .requestURL(requestURL)
                .request(r)
                .body(request.getMessageBody().toString());

        return b.build();
    }

    private final void addContextValue(AtmosphereRequest request, String name, Object value) {
        if (request.resource().transport().equals(AtmosphereResource.TRANSPORT.WEBSOCKET)) {
            request.setAttribute(name, value);
        } else {
            request.getSession().setAttribute(name, value);
        }
    }

    private final Object getContextValue(AtmosphereRequest request, String name) {
        if (request.resource().transport().equals(AtmosphereResource.TRANSPORT.WEBSOCKET)) {
            return request.getAttribute(name);
        } else {
            return request.getSession().getAttribute(name);
        }
    }

    private final class Interceptor extends AsyncIOInterceptorAdapter {

        @Override
        public byte[] transformPayload(AtmosphereResponse response, byte[] responseDraft, byte[] data) throws IOException {
            Object rm = wrapMessage(response, new String(responseDraft, response.getCharacterEncoding()));
            if (rm != null) {
                return mapper.writeValueAsBytes(rm);
            } else {
                return null;
            }
        }

        @Override
        public byte[] error(AtmosphereResponse response, int statusCode, String reasonPhrase) {
            Request swaggerSocketRequest = lookupRequest(response.request());

            if (swaggerSocketRequest == null) {
                logger.debug("Handshake mapping (could be expected) {} : {}", response.getStatus(), response.getStatusMessage());
                return new byte[0];
            }

            logger.debug("Unexpected status code {} : {}", response.getStatus(), response.getStatusMessage());
            StatusMessage statusMessage = new StatusMessage.Builder()
                    .status(new StatusMessage.Status(response.getStatus(),
                            response.getStatusMessage()))
                    .identity(swaggerSocketRequest.getUuid()).build();
            try {
                return mapper.writeValueAsBytes(statusMessage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected Request lookupRequest(AtmosphereRequest request) {
        Request swaggerSocketRequest = ssRequest.get();
        if (swaggerSocketRequest == null) {
            swaggerSocketRequest = (Request) request.getAttribute("swaggerSocketRequest");
        }
        return swaggerSocketRequest;
    }

    protected final Object wrapMessage(AtmosphereResponse res, String message) {

        if (message != null && message.startsWith("heartbeat-")) {
            String identity = (String) getContextValue(res.request(), IDENTITY);
            return new Heartbeat(String.valueOf(System.nanoTime()), identity);
        } else {
            Request swaggerSocketRequest = lookupRequest(res.request());
            Response.Builder builder = new Response.Builder();

            builder.body(message)
                    .status(res.getStatus(), res.getStatusMessage());

            Map<String, String> headers = res.headers();
            for (String s : headers.keySet()) {
                builder.header(new Header(s, headers.get(s)));
            }

            builder.uuid(swaggerSocketRequest.getUuid()).method(swaggerSocketRequest.getMethod())
                    .path(swaggerSocketRequest.getPath());
            String identity = (String) getContextValue(res.request(), IDENTITY);

            AtomicInteger expectedResponseCount = (AtomicInteger) getContextValue(res.request(), transactionIdentity.get() + RESPONSE_COUNTER);
            ResponseMessage m = null;
            if (expectedResponseCount != null && res.resource().transport() != AtmosphereResource.TRANSPORT.WEBSOCKET) {
                m = (ResponseMessage) getContextValue(res.request(), transactionIdentity.get() + ResponseMessage.class.getName());
                if (m != null) {
                    m.response(builder.build());
                } else {
                    m = new ResponseMessage(identity, builder.build());
                }

                if (expectedResponseCount.decrementAndGet() <= 0) {
                    return m;
                } else {
                    addContextValue(res.request(), transactionIdentity.get() + ResponseMessage.class.getName(), m);
                    return null;
                }
            }

            if (m == null) {
                m = new ResponseMessage(identity, builder.build());
            }
            return m;
        }
    }

}
