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

public class StatusMessage {

    private Status status;
    private String identity;

    public StatusMessage(){
        this.status = new Status(200,"OK");
        this.identity = "0";
    }

    public StatusMessage(Status status, String identity){
        this.status = status;
        this.identity = identity;
    }

    public Status getStatus() {
        return status;
    }

    public String getIdentity() {
        return identity;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public final static class Status {

        public static final int NO_STATUS = -1;
        private int statusCode = 200;
        private String reasonPhrase = "OK";

        public Status(){
        }

        public Status(int statusCode, String reasonPhrase) {
            this.statusCode = statusCode;
            this.reasonPhrase = reasonPhrase;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public void setReasonPhrase(String reasonPhrase) {
            this.reasonPhrase = reasonPhrase;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getReasonPhrase() {
            return reasonPhrase;
        }
    }

    public final static class Builder {

        private Status status;
        private String identity;

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder identity(String identity) {
            this.identity = identity;
            return this;
        }

        public StatusMessage build(){
            // Jackson bark
            StatusMessage s = new StatusMessage();
            s.setIdentity(identity);
            s.setStatus(status);
            return s;
        }

    }

}
