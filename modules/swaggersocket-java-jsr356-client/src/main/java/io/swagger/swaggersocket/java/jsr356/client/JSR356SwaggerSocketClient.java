/**
 *  Copyright 2016 SmartBear Software
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
package io.swagger.swaggersocket.java.jsr356.client;

import io.swagger.swaggersocket.protocol.Request;
import io.swagger.swaggersocket.protocol.Response;

import java.util.List;
import java.util.concurrent.Future;

public interface JSR356SwaggerSocketClient {

    void open(String url);

    void open(Request request);

    boolean isConnected();

    Response send(Request request);

    List<Response> send(List<Request> requests);

    <T> List<T> send(List<Request> requests, Class<T> resultClass);

    <T> T send(Request request, Class<T> resultClass);

    Future<Response> sendAsync(Request request);

    List<Future<Response>> sendAsync(List<Request> requests);

    <T> List<Future<T>> sendAsync(List<Request> requests, Class<T> resultClass);

    <T> Future<T> sendAsync(Request request, Class<T> resultClass);

    void close();

}
