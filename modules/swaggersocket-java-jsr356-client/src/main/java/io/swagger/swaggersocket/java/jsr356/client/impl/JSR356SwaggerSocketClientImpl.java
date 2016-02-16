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
package io.swagger.swaggersocket.java.jsr356.client.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.swaggersocket.java.jsr356.client.JSR356SwaggerSocketClient;
import io.swagger.swaggersocket.java.jsr356.client.exception.JSR356SwaggerSocketException;
import io.swagger.swaggersocket.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

@ClientEndpoint
public class JSR356SwaggerSocketClientImpl implements JSR356SwaggerSocketClient {

    private static final String DELIMITER_PATTERN = "^\\d+<->";
    private static final Logger LOG = LoggerFactory.getLogger(JSR356SwaggerSocketClientImpl.class);

    private final Map<String, FutureCountDownLatch> messages;
    private final ObjectMapper objectMapper;
    private final ReentrantLock reentrantLock;
    private final WebSocketContainer webSocketContainer;

    private CountDownLatch connectionOpenLatch = null;

    private Handshake handshake = null;
    private String identity = null;
    private Session session = null;

    private boolean isConnected;

    public JSR356SwaggerSocketClientImpl() {
        messages = new ConcurrentHashMap<String, FutureCountDownLatch>();
        objectMapper = new ObjectMapper();
        objectMapper.getDeserializationConfig().without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.getSerializationConfig().without(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.getSerializationConfig().withSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, false);
        reentrantLock = new ReentrantLock();
        webSocketContainer = ContainerProvider.getWebSocketContainer();
    }

    @OnOpen
    public void onOpen(final Session session) {
        LOG.debug("JSR356 Swagger Socket Session: Connection Established, Performing Handshake...");
        this.session = session;
        performHandshake();
        LOG.debug("JSR356 Swagger Socket Session: OPEN");
    }

    @OnMessage
    public void onMessage(final String message) throws IOException {
        LOG.debug("JSR356 Swagger Socket Message: {}", message);

        final String parsedMessage = message.replaceFirst(DELIMITER_PATTERN, "");

        if (isConnected) {
            if(parsedMessage.startsWith("{\"heartbeat\"")){
                return;
            }
            else if (parsedMessage.startsWith("{\"status\"")) {
                handleStatus(parsedMessage);
            }
            else {
                handleResponses(parsedMessage);
            }
        } else {
            handleHandshake(parsedMessage);
        }
    }

    @OnClose
    public void onClose(final Session session) {
        LOG.debug("JSR356 Swagger Socket: Close Event Received");

        if(isConnected) {
            try {
                session.close();
            } catch (final IOException e) {
                LOG.error("Error Closing JSR356 Swagger Socket Session!", e);
            } finally {
                this.session = null;
                isConnected = false;
                identity = null;
            }
        }

        LOG.debug("JSR356 Swagger Socket Session CLOSED");
    }

    @OnError
    public void onError(final Throwable throwable) {
        LOG.error("JSR356 Swagger Socket ERROR: {}", throwable);
    }

    @Override
    public void open(final String url) {
        open(new Request.Builder().path(url).build());
    }

    @Override
    public void open(final Request request) {
        try {
            reentrantLock.lock();

            if (session != null) {
                throw new JSR356SwaggerSocketException("Swagger Socket Connection Is Already Open!");
            }

            handshake = new Handshake.Builder()
                    .queryString(request.getQueryString())
                    .headers(request.getHeaders())
                    .format(request.getDataFormat())
                    .method(request.getMethod())
                    .path(request.getPath())
                    .body(request.getMessageBody())
                    .build();

            connectionOpenLatch = new CountDownLatch(1);

            final String swaggerSocketUrl = request.getPath() + "?SwaggerSocket=1.0";
            session = webSocketContainer.connectToServer(this, URI.create(swaggerSocketUrl));

            try {
                connectionOpenLatch.await(30, TimeUnit.SECONDS);
            }
            catch (final InterruptedException e) {
                if(session != null){
                    close();
                }

                throw new JSR356SwaggerSocketException("Timed Out Waiting for Connection!");
            }

        } catch (final DeploymentException e) {
            throw new JSR356SwaggerSocketException("An Unexpected Error Occurred Establishing the Swagger Socket Connection", e);
        } catch (final IOException e) {
            throw new JSR356SwaggerSocketException("An I/O Error Occurred With the Provided URL!", e);
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public Response send(final Request request){
        final List<Request> requests = new ArrayList<Request>();
        requests.add(request);

        final List<Response> responses = send(requests);
        return  responses.get(0);
    }

    @Override
    public List<Response> send(final List<Request> requests){
        final List<FutureCountDownLatch<Response>> resultList = new ArrayList<FutureCountDownLatch<Response>>();

        try {
            reentrantLock.lock();

            final Request[] requestsArray = new Request[requests.size()];
            requests.toArray(requestsArray);

            final RequestMessage requestMessage = new RequestMessage.Builder()
                    .requests(requestsArray)
                    .identity(identity)
                    .build();

            for(final Request thisRequest : requestMessage.getRequests()){
                final String uuid = UUID.randomUUID().toString();
                thisRequest.setUuid(uuid);
                final FutureCountDownLatch<Response> result = new FutureCountDownLatch<Response>();
                messages.put(uuid, result);
                resultList.add(result);
            }

            final String requestJson = objectMapper.writeValueAsString(requestMessage);
            session.getBasicRemote().sendText(requestJson);
        } catch (final IOException e) {
            throw new JSR356SwaggerSocketException("Error Sending Swagger Socket Request(s)", e);
        } finally {
            reentrantLock.unlock();
        }

        try {
            final List<Response> responses = new ArrayList<Response>();

            for(final FutureCountDownLatch<Response> resultLatches : resultList){
	            final Response response = resultLatches.get();
                responses.add(response);
	            messages.remove(response.getUuid());
            }

            return responses;
        }
        catch(final Exception e){
            throw new JSR356SwaggerSocketException("Error Receiving Swagger Socket Response(s)", e);
        }
    }

    @Override
    public <T> List<T> send(final List<Request> requests, final Class<T> resultClass) {

        for(final Request request: requests){
            final Object messageBody = request.getMessageBody();

            if(!(messageBody instanceof String)) {
                try {
                    request.setMessageBody(objectMapper.writeValueAsString(messageBody));
                } catch (JsonProcessingException e) {
                    throw new JSR356SwaggerSocketException("Error Serializing Swagger Socket Request(s)", e);
                }
            }
        }

        final List<Response> responses = send(requests);

        final List<T> resultValues = new ArrayList<T>();

        for(final Response response : responses) {
            final String responseBody = (String) response.getMessageBody();
            final T resultValue;

            try {
                resultValue = objectMapper.readValue(responseBody, resultClass);
            } catch (IOException e) {
                throw new JSR356SwaggerSocketException("Error Deserializing Swagger Socket Response(s)", e);
            }

            resultValues.add(resultValue);
        }

        return resultValues;
    }

    @Override
    public <T> T send(final Request request, final Class<T> resultClass) {
        final List<Request> requests = new ArrayList<Request>();
        requests.add(request);
        return send(requests, resultClass).get(0);
    }

    @Override
    public void close() {
        final Close close = new Close("Closed", identity);
        final CloseMessage closeMessage = new CloseMessage();
        closeMessage.setClose(close);

        try {
            reentrantLock.lock();

            if(isConnected) {
                final String closeJson = objectMapper.writeValueAsString(closeMessage);
                session.getBasicRemote().sendText(closeJson);
                onClose(session);
            }
            else {
                throw new JSR356SwaggerSocketException("Error Closing Swagger Socket Connection: Connection is Not Open!");
            }

        } catch (final Exception e) {
            throw new JSR356SwaggerSocketException("Error Closing Swagger Socket", e);
        } finally {
            reentrantLock.unlock();
        }
    }

    private void handleHandshake(final String handshake) throws IOException {
        final StatusMessage statusMessage = objectMapper.readValue(handshake, StatusMessage.class);

        if(statusMessage == null || statusMessage.getStatus().getStatusCode() >= 400){
            throw new JSR356SwaggerSocketException("Error Processing Swagger Socket Handshake");
        }
        else {
            identity = statusMessage.getIdentity();
            isConnected = true;
            connectionOpenLatch.countDown();
        }
    }

    private void handleResponses(final String responses) throws IOException {
        final ResponseMessage responseMessage = objectMapper.readValue(responses, ResponseMessage.class);
        final List<Response> responseMessageList = responseMessage.getResponses();

        for(int i = 0; i < responseMessageList.size(); i++){
            final Response thisResponse = responseMessageList.get(i);
            final FutureCountDownLatch<Response> responseLatch = messages.get(thisResponse.getUuid());
            responseLatch.set(thisResponse);
        }
    }

    private void handleStatus(final String status) throws IOException {
        LOG.error("JSR356 Swagger Socket Status ERROR: {}", status);
    }

    private void performHandshake() {
        final HandshakeMessage handshakeMessage = new HandshakeMessage();
        handshakeMessage.setHandshake(handshake);

        try {
            final String handshakeJson = objectMapper.writeValueAsString(handshakeMessage);
            writeMessage(handshakeJson);
        } catch (final JsonProcessingException e) {
            close();
            throw new JSR356SwaggerSocketException("Error Performing Handhsake!", e);
        }
    }

    private boolean writeMessage(final String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (final IOException e) {
            throw new JSR356SwaggerSocketException("Error Writing Swagger Socket Message", e);
        }

        return true;
    }

    private class FutureCountDownLatch<T> implements Future<T> {

        private CountDownLatch countDownLatch = new CountDownLatch(1);
        private T result = null;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            countDownLatch.countDown();
            return true;
        }

        @Override
        public boolean isCancelled() {
            return countDownLatch.getCount() == 0 && result == null;
        }

        @Override
        public boolean isDone() {
            return result != null;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            countDownLatch.await();
            return result;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            countDownLatch.await(timeout, unit);
            return result;
        }

        public void set(final T result) {
            this.result = result;
            countDownLatch.countDown();
        }

    }

}
