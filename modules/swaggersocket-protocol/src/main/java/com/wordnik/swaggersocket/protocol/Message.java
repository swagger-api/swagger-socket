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

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.List;

/**
 * A Java object representing the SwaggerSocket protocol implementation.
 */
@JsonIgnoreProperties({"transactionID"})
public class Message {

    private String identity;
    private List<Request> requests;
    private String transactionID;

    public Message() {
    }

    public List<Request> getRequests() {
        return requests;
    }

    public void setRequests(List<Request> requests) {
        this.requests = requests;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public Message transactionID(String transactionID) {
        this.transactionID = transactionID;
        return this;
    }

    public String transactionID() {
        return transactionID;
    }

}
