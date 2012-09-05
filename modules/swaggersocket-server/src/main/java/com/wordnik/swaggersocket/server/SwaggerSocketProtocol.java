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

import com.wordnik.swaggersocket.protocol.Handshake;
import com.wordnik.swaggersocket.protocol.HandshakeMessage;
import com.wordnik.swaggersocket.protocol.Header;
import com.wordnik.swaggersocket.protocol.Message;
import com.wordnik.swaggersocket.protocol.ProtocolBase;
import com.wordnik.swaggersocket.protocol.QueryString;
import com.wordnik.swaggersocket.protocol.Request;
import com.wordnik.swaggersocket.protocol.StatusMessage;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.websocket.WebSocket;
import org.atmosphere.websocket.WebSocketProcessor;
import org.atmosphere.websocket.WebSocketProtocol;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The SwaggerSocket Protocol implementation.
 */
public class SwaggerSocketProtocol implements WebSocketProtocol {

    private final static String DELEGATE_HANDSHAKE = SwaggerSocketProtocol.class.getName() + ".delegateHandshake";

    private static final Logger logger = LoggerFactory.getLogger(SwaggerSocketProtocol.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private boolean delegateHandshake = false;
    private final SwaggerSocketResponseFilter serializer;

    public SwaggerSocketProtocol() {
        serializer = new SwaggerSocketResponseFilter(mapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(AtmosphereConfig config) {
        String s = config.getInitParameter(DELEGATE_HANDSHAKE);
        if (s != null) {
            delegateHandshake = Boolean.parseBoolean(s);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen(WebSocket webSocket) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose(WebSocket webSocket) {
        AtmosphereResource resource = webSocket.resource();
        if (resource != null) {
            resource.getBroadcaster().removeAtmosphereResource(resource);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onError(WebSocket webSocket, WebSocketProcessor.WebSocketException t) {
        if (t.response() != null) {

            Request swaggerSocketRequest =
                    Request.class.cast(t.response().request().getAttribute(SwaggerSocketResponseFilter.SWAGGERSOCKET_REQUEST));

            if (swaggerSocketRequest == null) {
                logger.debug("Handshake mapping (could be expected) {} : {}", t.response().getStatus(), t.response().getStatusMessage());
                return;
            }

            logger.debug("Unexpected status code {} : {}", t.response().getStatus(), t.response().getStatusMessage());
            StatusMessage statusMessage = new StatusMessage.Builder()
                    .status(new StatusMessage.Status(t.response().getStatus(),
                            t.response().getStatusMessage()))
                    .identity(swaggerSocketRequest.getUuid()).build();
            try {
                byte[] b = mapper.writeValueAsBytes(statusMessage);
                webSocket.write(t.response(), b, 0, b.length);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AtmosphereRequest> onMessage(WebSocket webSocket, String data) {
        AtmosphereResource resource = webSocket.resource();

        AtomicBoolean handshakeDone = (AtomicBoolean) resource.getRequest().getAttribute("swaggersocket.handshakeDone");

        try {
            logger.debug(data);
            List<AtmosphereRequest> list = new ArrayList<AtmosphereRequest>();

            if (handshakeDone == null || !handshakeDone.get()) {
                HandshakeMessage handshakeMessage = mapper.readValue(data, HandshakeMessage.class);

                // We have got a valid Handshake message, so we are ready to serve resource.
                handshakeDone = new AtomicBoolean(true);
                Handshake handshake = handshakeMessage.getHandshake();
                String identity = UUID.randomUUID().toString();

                resource.getRequest().setAttribute("swaggersocket.handshakeDone", handshakeDone);
                resource.getRequest().setAttribute("swaggersocket.identity", identity);

                StatusMessage statusMessage = new StatusMessage.Builder().status(new StatusMessage.Status(200, "OK"))
                        .identity(identity).build();
                webSocket.write(resource.getResponse(), mapper.writeValueAsBytes(statusMessage));
                webSocket.webSocketResponseFilter(serializer);

                if (!delegateHandshake) {
                    return null;
                } else {
                    list.add(toAtmosphereRequest(resource.getRequest(), handshake, false));
                }
            } else {
                Message swaggerSocketMessage = mapper.readValue(data, Message.class);

                String identity = (String) resource.getRequest().getAttribute("swaggersocket.identity");

                if (!swaggerSocketMessage.getIdentity().equals(identity)) {
                    StatusMessage statusMessage = new StatusMessage.Builder().status(new StatusMessage.Status(503, "Not Allowed"))
                            .identity(identity).build();
                    webSocket.write(resource.getResponse(), mapper.writeValueAsBytes(statusMessage));
                    return null;
                }

                List<Request> requests = swaggerSocketMessage.getRequests();
                for (Request r : requests) {
                    list.add(toAtmosphereRequest(resource.getRequest(), r, requests.size() > 1));
                }
            }
            return list;
        } catch (IOException e) {
            logger.error("Invalid SwaggerSocket Message {}. Message will be ignored", data);
            logger.debug("parseMessage", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AtmosphereRequest> onMessage(WebSocket webSocket, byte[] data, int offset, int length) {
        // TODO: Not good performance wise.
        return onMessage(webSocket, new String(data, offset, length));
    }

    protected final static AtmosphereRequest toAtmosphereRequest(AtmosphereRequest r, ProtocolBase request, boolean dispatchAsync) {
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
                .dispatchRequestAsynchronously(dispatchAsync)
                .destroyable(true)
                .body(request.getMessageBody().toString());

        AtmosphereRequest ar = b.build();
        ar.setAttribute(SwaggerSocketResponseFilter.SWAGGERSOCKET_REQUEST, request);
        return ar;
    }

}