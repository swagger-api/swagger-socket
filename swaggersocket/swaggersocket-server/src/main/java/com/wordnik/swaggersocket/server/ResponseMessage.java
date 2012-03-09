package com.wordnik.swaggersocket.server;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Add Builder
 */
public class ResponseMessage {
    private String identity;
    private List<Response> responses;

    public ResponseMessage() {}

    public ResponseMessage(String identity, Response response) {
        this.identity = identity;
        responses = new ArrayList<Response>();
        responses.add(response);
    }

    public List<Response> getResponses() {
        return responses;
    }

    public void setResponses(List<Response> responses) {
        this.responses = responses;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }
}
