package com.wordnik.swaggersocket.server;

import java.util.Collections;
import java.util.List;

public class RequestMessage {

    private String identity;
    private Request[] requests;

    public RequestMessage(){}

    public RequestMessage(Builder b) {
        identity = b.identity;
        requests = b.requests;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public Request[] getRequests() {
        return requests;
    }

    public void setRequests(Request[] requests) {
        this.requests = requests;
    }

    public final static class Builder {

        private String identity = "0";
        private Request[] requests = new Request[0];

        public Builder identity(String identity) {
            this.identity = identity;
            return this;
        }

        public Builder requests(Request[] requests) {
            this.requests = requests;
            return this;
        }

        public RequestMessage build() {
            return new RequestMessage(this);
        }


    }



}
