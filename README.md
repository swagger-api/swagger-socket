## [SwaggerSocket](https://github.com/wordnik/swagger-sockets/wiki/Swagger-Socket-Protocol): A REST over WebSocket Protocol

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

