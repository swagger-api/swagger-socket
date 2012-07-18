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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Request extends ProtocolBase {

    private Object attachment;

    public Request() {
    }

    private Request(Builder b) {
        headers = b.headers;
        queryString = b.queryString;
        path = b.path;
        uuid = b.uuid;
        method = b.method;
        dataFormat = b.dataFormat;
        messageBody = b.messageBody;
        attachment = b.attachment;
    }

    public Object attachment() {
        return attachment;
    }

    public Request attach(Object attachment) {
        this.attachment = attachment;
        return this;
    }

    public final static class Builder {
        private String dataFormat = "application/json";
        private List<Header> headers = Collections.<Header>emptyList();
        private List<QueryString> queryString = Collections.<QueryString>emptyList();
        private String path = "/";
        private String uuid = UUID.randomUUID().toString();
        private String method = "POST";
        private Object messageBody = "";
        private Object attachment;

        public Builder format(String dataFormat) {
            this.dataFormat = dataFormat;
            return this;
        }

        public Builder body(Object messageBody) {
            this.messageBody = messageBody;
            return this;
        }

        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder headers(List<Header> headers) {
            this.headers = headers;
            return this;
        }

        public Builder queryString(List<QueryString> queryString) {
            this.queryString = queryString;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder attach(Object attachment) {
            this.attachment = attachment;
            return this;
        }

        public Request build() {
            return new Request(this);
        }

    }
}
