## [SwaggerSocket](https://github.com/wordnik/swaggersocket/wiki/SwaggerSocket-Protocol): A REST over WebSocket Protocol

The SwaggerSocket protocol allows any existing REST Resources to be executed on top of the WebSocket Protocol. Resources can be deployed as it is, without any modification and take advantage of the SwaggerSocket protocol.

### Join the community

You can subscribe to our [Google Group](https://groups.google.com/forum/?fromgroups#!forum/swagger-swaggersocket) or follow us on [Twitter](https://twitter.com/#!/swaggersocket)

### Download SwaggerSocket

Using Maven or SBT

```xml
    <!-- Server side -->
    <dependency>
       <groupId>com.wordnik</groupId>
       <artifactId>swaggersocket</artifactId>
       <version>1.3.0</version>
    </dependency>
    <!-- Client side --> 
     <dependency>
       <groupId>com.wordnik</groupId>
       <artifactId>swaggersocket.js</artifactId>
       <version>1.3.0</version>
       <type>war</type>
    </dependency>   
```

Manual download [here](http://search.maven.org/#search|ga|1|swaggersocket)

### Getting Started
The quickest way to see how the protocol works is to try the samples. You can download them from [here](http://search.maven.org/#search|ga|1|swaggersocket). Just do

```bash
  % unzip swaggersocket-{sample_name}-distribution.zip
  % chmod a+x ./bin/nettosphere.sh
  % ./bin/nettosphere.sh
```

and then point your browser to http://127.0.0.1:8080

You can also build the sample yourself and use Jetty instead of NettoSphere.

```bash
  % git clone git@github.com:wordnik/swagger-sockets.git
  % cd swagger-sockets
  % mvn 
  % cd samples/swaggersocket-echo OR samples/swaggersocket-twitter
  % mvn jetty:run
```

Take a look at [HelloWorld](https://github.com/wordnik/swaggersocket/wiki/Getting-started-with-SwaggerSocket-and-Jersey) mini tutorial. 
You can also look at our real time samples:

 * Twitter's Real Time Search [client code](https://github.com/wordnik/swaggersocket/blob/master/samples/swaggersocket-twitter/src/main/webapp/index.html#L10) | [server code](https://github.com/wordnik/swaggersocket/blob/master/samples/swaggersocket-twitter/src/main/java/com/wordnik/swaggersocket/samples/TwitterFeed.java#L45) | [download sample](hhttp://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.wordnik%22%20AND%20a%3A%22swaggersocket-twitter%22)
 * Wordnik's Real Time Search [client code](https://github.com/wordnik/swaggersocket/blob/master/samples/swaggersocket-wordnik/src/main/webapp/index.html#L10) | [server code](https://github.com/wordnik/swaggersocket/blob/master/samples/swaggersocket-wordnik/src/main/scala/com/wordnik/swaggersocket/samples/WordnikResourceProxy.scala#L30) | [download sample](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.wordnik%22%20AND%20a%3A%22swaggersocket-wordnik%22)
 * Simple Swagger Sockets Protocol Echo [client code](https://github.com/wordnik/swaggersocket/blob/master/samples/swaggersocket-echo/src/main/webapp/index.html#L9) | [server code](https://github.com/wordnik/swaggersocket/blob/master/samples/swaggersocket-echo/src/main/scala/org/wordnik/swaggersocket/samples/SwaggerSocketResource.scala#L16) | [download sample](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.wordnik%22%20AND%20a%3A%22swaggersocket-echo%22)

You can also download our [war files](http://search.maven.org/#search|ga|1|swaggersocket) and deploy them to any WebServer [supporting WebSockets](https://github.com/Atmosphere/atmosphere/wiki/Supported-WebServers-and-Browsers).

### Add bi-directional support to your REST application

You can also add bi-directional support to your REST resources by extending your application using the [Atmosphere Framework](http://github.com/Atmosphere/atmosphere).

## Quick Overview
To enable SwaggerSocket, add the following in your web.xml.  

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
    ss.onError = function(Response) {}; // When an error occurs
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
      .dataFormat("application/json")
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
To read more about how the protocol works, take a look at the [SwaggerSocket Protocol Specification](https://github.com/wordnik/swaggersocket/wiki/SwaggerSocket-Protocol)
