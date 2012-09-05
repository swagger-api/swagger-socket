package com.wordnik.swaggersocket.server;

import com.wordnik.swaggersocket.protocol.Header;
import com.wordnik.swaggersocket.protocol.Request;
import com.wordnik.swaggersocket.protocol.Response;
import com.wordnik.swaggersocket.protocol.ResponseMessage;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.cpr.FrameworkConfig;
import org.atmosphere.websocket.WebSocketResponseFilter;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.wordnik.swaggersocket.server.LongPollingBroadcastFilter.END_MESSAGE;


final class SwaggerSocketResponseFilter implements WebSocketResponseFilter {
    private static final Logger logger = LoggerFactory.getLogger(SwaggerSocketResponseFilter.class);
    private final ObjectMapper mapper;

    public SwaggerSocketResponseFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String filter(AtmosphereResponse r, String message) {

        // This is the handshake, nothing to do.
        if (r.request() == null || r.request().getAttribute("swaggersocket.handshake") != null) {
            return message;
        }

        if (invalidState((Request) r.request().getAttribute(String.valueOf(r.request().hashCode())))) {
            logger.error("Response's body not allowed on handshake {}", message);
            return null;
        }

        try {
            ResponseMessage m = wrapMessage(r, message);
            if (m != null) {
                return mapper.writeValueAsString(m);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] filter(AtmosphereResponse r, byte[] message) {
        return filter(r, message, 0, message.length);
    }

    @Override
    public byte[] filter(AtmosphereResponse r, byte[] message, int offset, int length) {

        // This is the handshake, nothing to do.
        if (r.request() == null || r.request().getAttribute("swaggersocket.handshake") != null) {
            return message;
        }


        if (invalidState((Request) r.request().getAttribute(String.valueOf(r.request().hashCode())))) {
            logger.error("Response's body not allowed on handshake {}", message);
            return null;
        }

        try {
            AtmosphereResource ar = (AtmosphereResource) r.request().getAttribute(FrameworkConfig.ATMOSPHERE_RESOURCE);
            if (ar.transport().equals(AtmosphereResource.TRANSPORT.LONG_POLLING)) {
                // We need to make sure we have the entire message
                String m = "";
                try {
                    m = new String(message, offset, length, r.getCharacterEncoding());
                } catch (UnsupportedEncodingException e) {
                    logger.trace("", e);
                }

                if (ar.isSuspended()) {
                    if (!m.endsWith(END_MESSAGE)) {
                        StringBuffer cumulatedMessage = (StringBuffer) ar.getRequest().getAttribute("swaggersocket.message");
                        if (cumulatedMessage == null) {
                            ar.getRequest().setAttribute("swaggersocket.message", new StringBuffer(m));
                        } else {
                            cumulatedMessage.append(m);
                        }
                        return null;
                    } else {
                        m = m.substring(0, m.indexOf(END_MESSAGE));
                        StringBuffer cumulatedMessage = (StringBuffer) ar.getRequest().getAttribute("swaggersocket.message");
                        ResponseMessage rm;
                        if (cumulatedMessage == null) {
                            rm = wrapMessage(r, m);
                        } else {
                            cumulatedMessage.append(m);
                            rm = wrapMessage(r, cumulatedMessage.toString());
                        }
                        ar.getRequest().removeAttribute("swaggersocket.message");
                        return mapper.writeValueAsBytes(rm);
                    }
                } else {
                    ResponseMessage rm = wrapMessage(r, m);
                    if (rm != null) {
                        return mapper.writeValueAsBytes(rm);
                    } else {
                        return null;
                    }
                }
            }

            ResponseMessage m = wrapMessage(r, new String(message, offset, length, r.request().getCharacterEncoding()));
            if (m != null) {
                return mapper.writeValueAsBytes(m);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean invalidState(Request swaggerSocketRequest) {
        return swaggerSocketRequest == null ? true : false;
    }

    protected final static ResponseMessage wrapMessage(AtmosphereResponse res, String message) {
        Response.Builder builder = new Response.Builder();

        builder.body(message)
                .status(res.getStatus(), res.getStatusMessage());

        Map<String, String> headers = res.headers();
        for (String s : headers.keySet()) {
            builder.header(new Header(s, headers.get(s)));
        }

        Request swaggerSocketRequest =
                Request.class.cast(res.request().getAttribute(String.valueOf(res.request().hashCode())));

        builder.uuid(swaggerSocketRequest.getUuid()).method(swaggerSocketRequest.getMethod())
                .path(swaggerSocketRequest.getPath());
        String identity = (String) res.request().getAttribute("swaggersocket.identity");

        AtomicInteger expectedResponseCount = (AtomicInteger) res.request().getAttribute("ResponseCountNumber");
        ResponseMessage m = null;
        if (expectedResponseCount != null) {
            m = (ResponseMessage) res.request().getAttribute(ResponseMessage.class.getName());
            if (m != null) {
                m.response(builder.build());
            } else {
                m = new ResponseMessage(identity, builder.build());
            }

            if (expectedResponseCount.decrementAndGet() <= 0) {
                return m;
            } else {
                res.request().setAttribute(ResponseMessage.class.getName(), m);
                return null;
            }
        }

        if (m == null) {
            m = new ResponseMessage(identity, builder.build());
        }
        return m;
    }
}
