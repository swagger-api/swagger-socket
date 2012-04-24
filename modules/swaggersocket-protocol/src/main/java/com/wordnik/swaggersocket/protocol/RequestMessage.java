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
package com.wordnik.swaggersocket.protocol;

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
