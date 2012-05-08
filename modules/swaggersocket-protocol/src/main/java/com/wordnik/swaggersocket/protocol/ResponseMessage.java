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

    public ResponseMessage response(Response response) {
        responses.add(response);
        return this;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }
}
