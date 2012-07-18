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

import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.List;

/**
 * The Hanshake object representing a SwaggerSocket's initial handshake between the client and server.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class Handshake extends ProtocolBase {

    private String protocolName = "SwaggerSocket";
    private String protocolVersion = "1.0";
    private String dataFormat = "application/json";

    public Handshake() {
    }

    private Handshake(Builder b) {
        protocolName = b.protocolName;
        protocolVersion = b.protocolVersion;
        dataFormat = b.dataFormat;
        headers = b.headers;
        queryString = b.queryString;
        path = b.path;
        uuid = b.uuid;
        method = b.method;
        dataFormat = b.dataFormat;
        messageBody = b.body;
    }

    public String getProtocolName() {
        return protocolName;
    }

    public void setProtocolName(String protocolName) {
        this.protocolName = protocolName;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public final static class Builder {
        private String protocolName = "SwaggerSocket";
        private String protocolVersion = "1.0";
        private String dataFormat = "application/json";
        private List<Header> headers;
        private List<QueryString> queryString;
        private String path;
        private String uuid;
        private String method;
        private Object body;

        public Builder format(String dataFormat) {
            this.dataFormat = dataFormat;
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

        public Builder protocolName(String protocolName) {
            this.protocolName = protocolName;
            return this;
        }

        public Builder protocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public final Builder body(Object messageBody) {
            this.body = messageBody;
            return this;
        }

        public Handshake build() {
            return new Handshake(this);
        }

    }
}
