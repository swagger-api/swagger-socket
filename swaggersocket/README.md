## SwaggerSocket: A REST over WebSocket Protocol

### Getting started using the bi-directional samples
The quickest way to see how the protocol works is to try the samples:

    % git clone git@github.com:wordnik/swagger-sockets.git
    % cd swagger-socket
    % mvn -N; mvn
    % cd samples/swaggersocket-wordnik OR samples/swaggersocket-twitter
    % mvn jetty:run

### SwaggerSocket Protocol Server Implementation
The server side implementation of the SwaggerSocket Protocol is done via The Atmosphere Framework's WebSocketProtocol Extension Point API. The current implementation is named SwaggerSocketProtocol
and can be either extended or replaced by properly defining the extension point in web.xml as:

        <init-param>
            <param-name>org.atmosphere.websocket.WebSocketProtocol</param-name>
            <param-value>com.wordnik.swaggersocket.server.SwaggerSocketProtocol</param-value>
        </init-param>

### Swagger Socket JavaScript API
The Swagger Socket Client is defined as

    var swaggerSocket = new jQuery.swaggersocket.SwaggerSocket();

#### Listener are per Request. The response will be delivered as they come and can be correlated with their associated request using response.getRequest().

    var ss = new jQuery.swaggersocket.SwaggerSocketListener();
    ss.onOpen = function(response) {
        val request = response.getRequest();
    };
    ss.onClose = function(Response) {}; // Called when the Websocket gets closed
    ss.onError = function(errorMessage, Response) {}; // When an error occurs
    ss.onResponse = function(Response) {}; // When a response is ready
    ss.onResponses = function (Response) {}; // A List of all the ready responses

#### Opening a connection

     var request = new jQuery.swaggersocket.Request()
           .path(document.location.toString())
           .listener(ss);
     swaggerSocket.open(request);

#### Sending requests -- You can send an array of Request or single request.

    var requests = new Array();
    requests[0] = new jQuery.swaggersocket.Request()
            .path("path1")
            .method("POST")
            .data("FOO")
            .listener(ss);
    requests[1] = new jQuery.swaggersocket.Request()
            .path("/path2")
            .method("POST")
            .data("BAR")
            .listener(ss);

    swaggerSocket.send(requests);

### Swagger Socket Scala API
The SwaggerSocket protocol can also be used using the Scala language. The SwaggerSocket Scala library is using Asynchronous I/O so all the API call are non blocking. First, you need to hanshake using

    val ss = SwaggerSocket().open(new Request.Builder().path(getTargetUrl + "/").build())

Then you are ready to start sending requests. As simple as:

    // send (Request, SwaggerSoketListener )
    // or send (Array[Request], SwaggerSoketListener)
    ss.send(new Request.Builder()
      .path("/b")
      .method("POST")
      .body("Yo!")
      .format("JSON")
      .build(), new SwaggerSocketListener() {

      	override def error(e: SwaggerSocketException) {
           // Invoked when an exception occurs
	    }
     	override def message(r: Request, s: Response) {
           // Response is delievered here
        }
      })
Once completed, just need to close

    ss.close

## SwaggerSocket Protocol Details

The SwaggerSocket protocol is a JSON based protocol that runs preferably on top of the Websocket protocol, but can also be implemented using HTTP's long-polling Comet technique.
The protocol workflow consist of:

The client send an handshake request using a unique 'identity' to the server. The identity will be used by the server to track client's interaction.

    1. If the server isn't accepting the handshake, an error message with be sent back to the client with the
    reason explaining the handshake failure.
    2. If accepted, a success message will be returned.

On a successful handshake response, the client is now allowed to send requests by passing back it's identity data augmented with N requests.

The server will asynchronously process all the requests and return the response as soon as they are available.
There is no guarantee the response will be returned in the same order as the requests were received.
The connection between the client and the server can be, at any time, closed. In that scenario the client might re-handshake using the same 'identity',
and resent its original requests list or a subset of it. The server may decides to reprocess all requests, get the available cached responses, etc.
The client is guarantee to receive a response to its requests.
The client may decides to drop its requests. In that case, the server must not cache previous responses and discard them automatically.

### SwaggerSocket Protocol Workflow
Initially, the client sent the following handshake request. The client identity is represented by the 'identity' field. The identity field must be 0 and will be uniquely assigned by the server if the handshake succeed. The client must reuse that identity for all subsequent requests to the server.

The client is allowed to handshake and invoke a resource root path at the same time, but it is not allowed to receive any response's body.. If the handshake succeed, the request will be delivered to the resource which maps to the 'path' value, with the headers, query string and body.

    {
        "handshake" : {
            "protocolVersion" : "1.0",
            "protocolName" : "SwaggerSocket",
            "dataFormat" : "JSON",
            "identity" : 0,
            "path" : "/any_url",
            "method" : "GET",
            "headers" : [
                {
                    "name" : "name",
                    "value" : "value"
                }
            ],
            "queryStrings" : [
                {
                    "name" : "name",
                    "value" : "value"
                }
            ],
     }
    }
If the server accepts the handshake, the following message will be returned to the client using the same identity value

    {
        "identity" : "some_unique_uuid_generated_by_the_server",
        "status" : {
            "code" : "value",
            "reasonPhrase" : "reason"
        }
    }
If the server doesn't accepts the handshake, the following message will be returned (status code will be higher than 400)

    {
        "identity" : "some_unique_uuid_generated_by_the_server",
        "status" : {
            "code" : "value",
            "reasonPhrase" : "reason"
        }
    }
On a successful handshake, the client can now start sending requests using it's identity, augmented with an array of requests. Requests will be rooted/mapped to their associate resource using the 'path' field.

    {
        "identity" : some_unique_uuid_generated_by_the_server,
        "requests" : [
            {
                "uuid" : 0,
                "method" : "POST",
                "path" : "/any_url/",
                "headers" : [
                    {
                        "name" : "Content-Type",
                        "value" : "test/plain"
                    }
                ],
                "queryStrings" : [
                    {
                        "name" : "foo2",
                        "value" : "bar2"
                    }
                ],
                "messageBody" : "Swagger Socket Protocol is cool"
            }
        ]
    }
The server will proceed the set of requests and may sent responses as they come, or wait for all responses to be available before sending back the final, aggregated set of response, in the form of

    {
        "identity" : some_unique_uuid_generated_by_the_server,
        "responses" : [
            {
                "uuid" : 0,
                "status" : "status",
                "path" : "path",
                "headers" : [
                    {
                        "name" : "name",
                        "value" : "value"
                    }
                ],
                "messageBody" : "messageBody"
            }
        ]
    }
If the server is unable to process the request, and error message may be sent to cancel the overall requests

    {
        "identity" : "some_unique_uuid_generated_by_the_server",
        "status" : {
            "code" : 500,
            "reasonPhrase" : "reason"
        }
    }
or the response's status code can be set for the individual request that has failed

    "responses" : [
        {
            "uuid" : some_unique_uuid_generated_by_the_server,
            "status" : 500,
            "path" : "path",
            "headers" : [
                {
                    "name" : "name",
                    "value" : "value"
                }
            ],
            "messageBody" : "messageBody"
        }
    ]
The client can sent multiple requests and correlate the response using the request/response 'uuid' value.


