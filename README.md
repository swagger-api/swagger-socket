## [SwaggerSocket](https://github.com/wordnik/swagger-sockets/wiki/Swagger-Socket-Protocol): A REST over WebSocket Protocol

The SwaggerSocket protocol allows any existing REST Resource to be executed on top of the WebSocket Protocol. Resources can be deployed as it is, without any modification and take advantage of the SwaggerSocket protocol.

You can also add bi-directional support to your REST resource by extending it using the [Atmosphere Framework](http://github.com/Atmosphere/atmosphere).

### Getting started using the samples
The quickest way to see how the protocol works is to try the samples:

```bash
    % git clone git@github.com:wordnik/swagger-sockets.git
    % cd swagger-socket
    % mvn -N; mvn
    % cd samples/swaggersocket-echo OR samples/swaggersocket-twitter
    % mvn jetty:run
```

or take a look at [HelloWorld](https://github.com/wordnik/swagger-sockets/wiki/Getting-started-with-Swagger-Socket-and-Jersey) mini tutorial. 
You can also look at our samples :

 * Twitter's Real Time Search [client](https://github.com/wordnik/swagger-sockets/blob/master/samples/swaggersocket-twitter/src/main/webapp/index.html#L10) [server](https://github.com/wordnik/swagger-sockets/blob/master/samples/swaggersocket-twitter/src/main/java/com/wordnik/swaggersocket/samples/TwitterFeed.java#L45)
 * Wordnik's Real Time Search [client](https://github.com/wordnik/swagger-sockets/blob/master/samples/swaggersocket-wordnik/src/main/webapp/index.html#L10) [server](https://github.com/wordnik/swagger-sockets/blob/master/samples/swaggersocket-wordnik/src/main/scala/com/wordnik/swaggersocket/samples/WordnikResourceProxy.scala#L30) 
 * Simple Swagger Sockets Protocol Echo [client](https://github.com/wordnik/swagger-sockets/blob/master/samples/swaggersocket-echo/src/main/webapp/index.html#L9) [server](https://github.com/wordnik/swagger-sockets/blob/master/samples/swaggersocket-echo/src/main/scala/org/wordnik/swaggersocket/samples/SwaggerSocketResource.scala#L16)

### SwaggerSocket Protocol Server Implementation
To enable SwaggerSocket, add the following in your web.xml. Currently, SwaggerSocket only supports [Jersey](http://jersey.java.net/) for REST Resources (other implementations like RestEasy and RESTLet are coming).  

```xml
    <servlet>
        <description>SwaggerSocketServlet</description>
        <servlet-name>SwaggerSocketServlet</servlet-name>
        <servlet-class>com.wordnik.swaggersocket.server.SwaggerSocketServlet</servlet-class>
        <load-on-startup>0</load-on-startup>
    </servlet>
```

### SwaggerSocket JavaScript API
The SwaggerSocket Client is defined as

```javascript
    var swaggerSocket = new jQuery.swaggersocket.SwaggerSocket();
```

#### Listeners are per Request. The responses will be delivered as they come and can be correlated with their associated request using response.getRequest().

```javascript
    var ss = new jQuery.swaggersocket.SwaggerSocketListener();
    ss.onOpen = function(response) {};
    ss.onClose = function(Response) {}; // Called when the Websocket gets closed
    ss.onError = function(errorMessage, Response) {}; // When an error occurs
    ss.onResponse = function(Response) {}; // When a response is ready
    ss.onResponses = function (Response) {}; // A List of all the ready responses
```

#### Opening a connection

```javascript
     var request = new jQuery.swaggersocket.Request()
           .path(document.location.toString())
           .listener(ss);
     swaggerSocket.open(request);
```

#### Sending requests -- You can send an array of Requests or single Request.

```javascript
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
```

### SwaggerSocket Scala API
The SwaggerSocket protocol can also be used using the Scala language. The SwaggerSocket Scala library is using Asynchronous I/O so all the API calls are non blocking. First, you need to hanshake using

```scala
    val ss = SwaggerSocket().open(new Request.Builder().path(getTargetUrl + "/").build())
```

Then you are ready to start sending requests. As simple as:

```scala
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
```
Once completed, you just need to close

```scala
    ss.close
```

### How the protocol works
To read more about how the protocol works, take a look at the [SwaggerSocket Protocol Specification](https://github.com/wordnik/swagger-sockets/wiki/Swagger-Socket-Protocol)

