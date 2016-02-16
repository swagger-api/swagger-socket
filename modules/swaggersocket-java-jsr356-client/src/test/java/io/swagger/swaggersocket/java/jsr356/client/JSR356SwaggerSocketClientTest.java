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

import io.swagger.swaggersocket.java.jsr356.client.exception.JSR356SwaggerSocketException;
import io.swagger.swaggersocket.java.jsr356.client.impl.JSR356SwaggerSocketClientImpl;
import io.swagger.swaggersocket.protocol.Request;
import io.swagger.swaggersocket.protocol.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JSR356SwaggerSocketClientTest extends EmbeddedTomcatTestBase {

    private static JSR356SwaggerSocketClient jsr356SwaggerSocketClient;

    @BeforeClass
    public static void setUp(){
        jsr356SwaggerSocketClient = new JSR356SwaggerSocketClientImpl();
        jsr356SwaggerSocketClient.open(String.format("ws://localhost:%d/test", port));
    }

    @Test
    public void testJSR356SwaggerSocketClientWithEchoService(){
        final Response response = jsr356SwaggerSocketClient.send(new Request.Builder()
                .path("/echo")
                .method("POST")
                .body("echo this...")
                .build());

        assertEquals("Echo Text Doesn't Match!", "echo this...", response.getMessageBody());
    }

    @Test
    public void testJSR356SwaggerSocketClientWithBatchCallToEchoService(){
        final List<Request> requests = new ArrayList<Request>();

        final Request request1 = new Request.Builder()
                .path("/echo")
                .method("POST")
                .body("echo this... 1")
                .build();

        final Request request2 = new Request.Builder()
                .path("/echo")
                .method("POST")
                .body("echo this... 2")
                .build();

        final Request request3 = new Request.Builder()
                .path("/echo")
                .method("POST")
                .body("echo this... 3")
                .build();

        requests.add(request1);
        requests.add(request2);
        requests.add(request3);

        final List<Response> responses = jsr356SwaggerSocketClient.send(requests);

        assertEquals("Echo Text Doesn't Match!", "echo this... 1", responses.get(0).getMessageBody());
        assertEquals("Echo Text Doesn't Match!", "echo this... 2", responses.get(1).getMessageBody());
        assertEquals("Echo Text Doesn't Match!", "echo this... 3", responses.get(2).getMessageBody());
    }

    @Test
    public void testJSR356SwaggerSocketClientWithTestJsonObjectAndAutoDeserialize(){
        final TestJsonObject requestJsonObject = new TestJsonObject();
        requestJsonObject.setTest("test json object");

        final TestJsonObject responseJsonObject = jsr356SwaggerSocketClient.send(new Request.Builder()
                .path("/testJsonObject")
                .method("POST")
                .body(requestJsonObject)
                .build(), TestJsonObject.class);

        assertEquals("Test Json Object Text Doesn't Match!", requestJsonObject.getTest(), responseJsonObject.getTest());
    }

    @Test
    public void testJSR356SwaggerSocketClientWithBatchCallOfTestJsonObjectAndAutoDeserialize(){
        final TestJsonObject requestJsonObject1 = new TestJsonObject();
        requestJsonObject1.setTest("test json object 1");

        final TestJsonObject requestJsonObject2 = new TestJsonObject();
        requestJsonObject2.setTest("test json object 2");

        final TestJsonObject requestJsonObject3 = new TestJsonObject();
        requestJsonObject3.setTest("test json object 3");

        final List<Request> requests = new ArrayList<Request>();
        requests.add(new Request.Builder()
                .path("/testJsonObject")
                .method("POST")
                .body(requestJsonObject1)
                .build());
        requests.add(new Request.Builder()
                .path("/testJsonObject")
                .method("POST")
                .body(requestJsonObject2)
                .build());
        requests.add(new Request.Builder()
                .path("/testJsonObject")
                .method("POST")
                .body(requestJsonObject3)
                .build());

        final List<TestJsonObject> responseJsonObjects = jsr356SwaggerSocketClient.send(requests, TestJsonObject.class);

        assertEquals("Test Json Object 1 Text Doesn't Match!", requestJsonObject1.getTest(), responseJsonObjects.get(0).getTest());
        assertEquals("Test Json Object 2 Text Doesn't Match!", requestJsonObject2.getTest(), responseJsonObjects.get(1).getTest());
        assertEquals("Test Json Object 3 Text Doesn't Match!", requestJsonObject3.getTest(), responseJsonObjects.get(2).getTest());
    }

    @Test(expected=JSR356SwaggerSocketException.class)
    public void testClientConnectingWithInvalidProtocolThrowsException(){
        final JSR356SwaggerSocketClientImpl jsr356SwaggerSocketClient = new JSR356SwaggerSocketClientImpl();
        jsr356SwaggerSocketClient.open(String.format("abcd://localhost:%d/test", port));
    }

    @Test
    public void testCanCleanlyCloseAndReopenConnection(){
        final Response response = jsr356SwaggerSocketClient.send(new Request.Builder()
                .path("/echo")
                .method("POST")
                .body("echo this...")
                .build());

        assertEquals("Echo Text Doesn't Match!", "echo this...", response.getMessageBody());

        assertTrue("Not Connected, But Should Be!", jsr356SwaggerSocketClient.isConnected());

        jsr356SwaggerSocketClient.close();

        assertFalse("Connected, But Shouldn't Be!", jsr356SwaggerSocketClient.isConnected());

        jsr356SwaggerSocketClient.open(String.format("ws://localhost:%d/test", port));

        assertTrue("Not Connected, But Should Be!", jsr356SwaggerSocketClient.isConnected());

        final Response response2 = jsr356SwaggerSocketClient.send(new Request.Builder()
                .path("/echo")
                .method("POST")
                .body("echo this...")
                .build());

        assertEquals("Echo Text Doesn't Match!", "echo this...", response2.getMessageBody());
    }


    @AfterClass
    public static void tearDown(){
        jsr356SwaggerSocketClient.close();
    }
}
