/**
 *  Copyright 2015 Reverb Technologies, Inc.
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
package com.wordnik.swaggersocket.samples;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.ning.http.util.Base64;
import com.sun.jersey.spi.resource.Singleton;

import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterLifeCyclePolicy;
import org.atmosphere.jersey.SuspendResponse;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Path("/search/{tagid}")
@Produces("text/html;charset=ISO-8859-1")
@Singleton
public class TwitterFeed {

    private static final Logger logger = LoggerFactory.getLogger(TwitterFeed.class);

    private final AsyncHttpClient asyncClient = new AsyncHttpClient();
    private final ConcurrentHashMap<String, Future<?>> futures = new ConcurrentHashMap<String, Future<?>>();
    private final CountDownLatch suspendLatch = new CountDownLatch(1);

    @Context
    private ServletConfig sc;

    private boolean initialized;
    private String authorizationValue;
    private Exception authorizationException;

    @GET
    public SuspendResponse<String> search(final @PathParam("tagid") Broadcaster feed,
                                          final @PathParam("tagid") String tagid) {
        if (tagid.isEmpty()) {
            throw new WebApplicationException();
        }
        if (!initialized) {
            initialized = true;
            String key = sc.getInitParameter("com.twitter.consumer.key");
            String secret = sc.getInitParameter("com.twitter.consumer.secret");
            try {
                Future<Response> f = asyncClient.preparePost("https://api.twitter.com/oauth2/token")
                    .setHeader("Authorization", "Basic " + Base64.encode((key + ":" + secret).getBytes()))
                    .setHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8").setBody("grant_type=client_credentials").execute();
                JSONObject jtoken = new JSONObject(f.get().getResponseBody());
                authorizationValue = "Bearer " + jtoken.getString("access_token");
            } catch (Exception e) {
                logger.error("Unable to obtain a valid bearer token", e);
                authorizationException = e;
            }
        }
        // we avoid repeatedly invoking the above authorization tkoen retrieval call
        if (authorizationValue == null) {
        	throw new WebApplicationException(authorizationException);
        }
        if (feed.getAtmosphereResources().size() == 0) {

            final Future<?> future = feed.scheduleFixedBroadcast(new Callable<String>() {

                private final AtomicReference<String> refreshUrl = new AtomicReference<String>("");

                public String call() throws Exception {
                    String query = null;
                    if (!refreshUrl.get().isEmpty()) {
                        query = refreshUrl.get();
                    } else {
                        query = "?q=" + tagid;
                    }

                    //TODO add next_results handling 
                    asyncClient.prepareGet("https://api.twitter.com/1.1/search/tweets.json" + query)
                        .setHeader("Authorization",  authorizationValue)
                        .execute(
                            new AsyncCompletionHandler<Object>() {

                                @Override
                                public Object onCompleted(Response response) throws Exception {
                                    String s = response.getResponseBody();
                                    if (response.getStatusCode() != 200) {
                                        feed.resumeAll();
                                        feed.destroy();
                                        logger.info("Twitter Search API unavailable\n{}", s);
                                        return null;
                                    }
                                    JSONObject json = new JSONObject(s);
                                    JSONObject searchMetadata = json.getJSONObject("search_metadata");
                                    refreshUrl.set(searchMetadata.getString("refresh_url"));
                                    if (json.getJSONArray("statuses").length() > 0) {
                                        // Wait for the connection to be suspended.
                                        suspendLatch.await();
                                        feed.broadcast(s).get();
                                    }
                                    return null;
                                }

                            });
                    return null;
                }
                // rate-limit allows up to 450 searches/900 seconds
            }, 4, TimeUnit.SECONDS);

            futures.put(tagid, future);
        }

        return new SuspendResponse.SuspendResponseBuilder<String>().broadcaster(feed).outputComments(true)
                .addListener(new EventsLogger() {

                    @Override
                    public void onSuspend(final AtmosphereResourceEvent event) {
                        super.onSuspend(event);
                        event.getResource().getBroadcaster().setBroadcasterLifeCyclePolicy(BroadcasterLifeCyclePolicy.EMPTY_DESTROY);

                        // OK, we can start polling Twitter!
                        suspendLatch.countDown();
                    }
                }).build();
    }

    @GET
    @Path("/stop")
    public String stopSearch(final @PathParam("tagid") Broadcaster feed,
                             final @PathParam("tagid") String tagid) {
        feed.resumeAll();
        if (futures.get(tagid) != null) {
            futures.get(tagid).cancel(true);
        }
        logger.info("Stopping real time update for {}", tagid);
        return "";
    }
}
