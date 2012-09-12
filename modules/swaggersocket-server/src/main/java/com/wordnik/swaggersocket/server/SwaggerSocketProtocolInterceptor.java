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

import com.wordnik.swaggersocket.protocol.HandshakeMessage;
import com.wordnik.swaggersocket.protocol.Header;
import com.wordnik.swaggersocket.protocol.Message;
import com.wordnik.swaggersocket.protocol.ProtocolBase;
import com.wordnik.swaggersocket.protocol.QueryString;
import com.wordnik.swaggersocket.protocol.Request;
import com.wordnik.swaggersocket.protocol.Response;
import com.wordnik.swaggersocket.protocol.ResponseMessage;
import com.wordnik.swaggersocket.protocol.StatusMessage;
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
import org.atmosphere.cpr.AtmosphereResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.atmosphere.cpr.FrameworkConfig.INJECTED_ATMOSPHERE_RESOURCE;

@AtmosphereInterceptorService
public class SwaggerSocketProtocolInterceptor extends AtmosphereInterceptorAdapter {

    private final static String SWAGGER_SOCKET_DISPATCHED = "request.dispatched";

    private static final Logger logger = LoggerFactory.getLogger(SwaggerSocketProtocolInterceptor.class);
    private final ObjectMapper mapper;
    private boolean delegateHandshake = false;
    private final AsyncIOInterceptor interceptor = new Interceptor();
    private final ThreadLocal<Request> ssRequest = new ThreadLocal<Request>();

    public SwaggerSocketProtocolInterceptor() {
        this.mapper = new ObjectMapper();
    }

    @Override
    public void configure(AtmosphereConfig config) {
    }

    @Override
    public Action inspect(AtmosphereResource r) {
        final AtmosphereRequest request = r.getRequest();

        if (request.getHeader("SwaggerSocket") != null
                && request.getAttribute(SWAGGER_SOCKET_DISPATCHED) == null) {

            AtmosphereResponse response = r.getResponse();
            response.setContentType("application/json");

            // Suspend to keep the connection OPEN.
            if (request.getMethod() == "GET" && r.transport().equals(AtmosphereResource.TRANSPORT.LONG_POLLING)) {
                BlockingQueue<AtmosphereResource> queue = (BlockingQueue<AtmosphereResource>) request.getSession().getAttribute("PendingResource");
                if (queue == null) {
                    queue = new LinkedBlockingQueue<AtmosphereResource>();
                    request.getSession().setAttribute("PendingResource", queue);
                }
                queue.offer(r);
                r.suspend();
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

                String handshakeTx = data.substring(0, 20);
                logger.debug(data);
                if (handshakeTx.replaceAll(" ", "").startsWith("{\"handshake\"")) {
                    // TODO: We only support one application.

                    request.getSession().invalidate();

                    // This will fail if the message is not well formed.
                    HandshakeMessage handshakeMessage = mapper.readValue(data, HandshakeMessage.class);

                    String identity = UUID.randomUUID().toString();

                    request.getSession().setAttribute("swaggersocket.handshaking", identity);

                    StatusMessage statusMessage = new StatusMessage.Builder().status(new StatusMessage.Status(200, "OK"))
                            .identity(identity).build();
                    response.getOutputStream().write(mapper.writeValueAsBytes(statusMessage));
                    request.getSession().setAttribute("swaggersocket.identity", identity);

                    if (!delegateHandshake) {
                        return Action.CANCELLED;
                    }
                } else {
                    Message swaggerSocketMessage = mapper.readValue(data, Message.class);

                    String identity = (String) request.getSession().getAttribute("swaggersocket.identity");

                    if (!swaggerSocketMessage.getIdentity().equals(identity)) {
                        StatusMessage statusMessage = new StatusMessage.Builder().status(new StatusMessage.Status(503, "Not Allowed"))
                                .identity(identity).build();
                        response.getOutputStream().write(mapper.writeValueAsBytes(statusMessage));
                        return Action.CANCELLED;
                    }

                    List<Request> requests = swaggerSocketMessage.getRequests();
                    AtmosphereRequest ar;
                    request.getSession().setAttribute("ResponseCountNumber", new AtomicInteger(requests.size()));
                    for (Request req : requests) {
                        ar = toAtmosphereRequest(request, req);
                        try {
                            ar.setAttribute(SWAGGER_SOCKET_DISPATCHED, "true");

                            // This is a new request, we must clean the Websocket AtmosphereResource.
                            request.removeAttribute(INJECTED_ATMOSPHERE_RESOURCE);
                            response.request(ar);
                            attachWriter(r);
                            ssRequest.set(req);

                            framework.doCometSupport(ar, response);
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

        }
        return Action.CONTINUE;
    }

    private final void attachWriter(AtmosphereResource r) {
        final AtmosphereRequest request = r.getRequest();

        AtmosphereResponse res = r.getResponse();
        if (res.getAsyncIOWriter() == null) {
            res.asyncIOWriter(new AtmosphereInterceptorWriter() {

                @Override
                protected void writeReady(AtmosphereResponse r, byte[] data) throws IOException {

                    // We are buffering response.
                    if (data == null) return;

                    BlockingQueue<AtmosphereResource> queue =
                            (BlockingQueue<AtmosphereResource>) request.getSession().getAttribute("PendingResource");
                    if (queue != null) {
                        AtmosphereResource resource;
                        try {
                            resource = queue.take();
                        } catch (InterruptedException e) {
                            throw new IOException(e);
                        }

                        OutputStream o = resource.getResponse().getResponse().getOutputStream();
                        o.write(data);
                        o.flush();
                        resource.resume();
                        request.getSession().setAttribute("PendingResource", null);
                    } else {
                        r.write(data);
                    }
                    r.flushBuffer();
                }
            });
        }

        AsyncIOWriter writer = res.getAsyncIOWriter();
        if (AtmosphereInterceptorWriter.class.isAssignableFrom(writer.getClass())) {
            AtmosphereInterceptorWriter.class.cast(writer).interceptor(interceptor);
        }
    }

    @Override
    public void postInspect(AtmosphereResource r) {
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


    private final class Interceptor extends AsyncIOInterceptorAdapter {

        @Override
        public byte[] transformPayload(AtmosphereResponse response, byte[] responseDraft, byte[] data) throws IOException {
            ResponseMessage rm = wrapMessage(response, new String(responseDraft, response.getCharacterEncoding()));
            if (rm != null) {
                return mapper.writeValueAsBytes(rm);
            } else {
                return null;
            }
        }

        @Override
        public byte[] error(AtmosphereResponse response, int statusCode, String reasonPhrase) {
            Request swaggerSocketRequest = ssRequest.get();

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

    protected final ResponseMessage wrapMessage(AtmosphereResponse res, String message) {
        Response.Builder builder = new Response.Builder();

        builder.body(message)
                .status(res.getStatus(), res.getStatusMessage());

        Map<String, String> headers = res.headers();
        for (String s : headers.keySet()) {
            builder.header(new Header(s, headers.get(s)));
        }

        Request swaggerSocketRequest = ssRequest.get();

        builder.uuid(swaggerSocketRequest.getUuid()).method(swaggerSocketRequest.getMethod())
                .path(swaggerSocketRequest.getPath());
        String identity = (String) res.request().getSession().getAttribute("swaggersocket.identity");

        AtomicInteger expectedResponseCount = (AtomicInteger) res.request().getSession().getAttribute("ResponseCountNumber");
        ResponseMessage m = null;
        if (expectedResponseCount != null && res.resource().transport() != AtmosphereResource.TRANSPORT.WEBSOCKET) {
            m = (ResponseMessage) res.request().getSession().getAttribute(ResponseMessage.class.getName());
            if (m != null) {
                m.response(builder.build());
            } else {
                m = new ResponseMessage(identity, builder.build());
            }

            if (expectedResponseCount.decrementAndGet() <= 0) {
                return m;
            } else {
                res.request().getSession().setAttribute(ResponseMessage.class.getName(), m);
                return null;
            }
        }

        if (m == null) {
            m = new ResponseMessage(identity, builder.build());
        }
        return m;
    }
}
