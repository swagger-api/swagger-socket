## [SwaggerSocket](https://github.com/wordnik/swaggersocket/wiki/SwaggerSocket-Protocol): A REST over WebSocket Protocol

[![Build Status](https://travis-ci.org/swagger-api/swagger-socket.png)](https://travis-ci.org/swagger-api/swagger-socket)

The SwaggerSocket protocol allows any existing REST Resources to be executed on top of the WebSocket Protocol. Resources can be deployed as it is, without any modification and take advantage of the SwaggerSocket protocol.

### Join the community

You can subscribe to our [Google Group](https://groups.google.com/forum/?fromgroups#!forum/swagger-swaggersocket).
<!-- or follow us on [Twitter](https://twitter.com/#!/swaggersocket)-->

### Download SwaggerSocket

Using Maven or SBT

```xml
    <!-- Server side -->
    <dependency>
       <groupId>com.wordnik</groupId>
       <artifactId>swaggersocket-server</artifactId>
       <version>2.0.1</version>
    </dependency>

    <!-- Client side when using jquery.swaggersocket.js --> 
    <dependency>
       <groupId>com.wordnik</groupId>
       <artifactId>swaggersocket.jquery</artifactId>
       <version>2.0.1</version>
       <type>war</type>
    </dependency>   

    <!-- Client side when using swaggersocket.js --> 
    <dependency>
       <groupId>com.wordnik</groupId>
       <artifactId>swaggersocket.js</artifactId>
       <version>2.0.1</version>
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
By default, jetty9 is used in this case, but this can be changed with profile -Pjetty8

```bash
  % git clone https://github.com/swagger-api/swagger-socket.git
  % cd swagger-socket
  % mvn 
  % cd samples/swaggersocket-{sample_name}
  % mvn jetty:run
```

Take a look at [HelloWorld](https://github.com/wordnik/swaggersocket/wiki/Getting-started-with-SwaggerSocket-and-Jersey) mini tutorial. 
You can also look at our real time samples:

 * Twitter's Real Time Search [client code](https://github.com/swagger-api/swaggersocket/blob/master/samples/swaggersocket-twitter/src/main/webapp/index.html) | [server code](https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-twitter/src/main/java/com/wordnik/swaggersocket/samples/TwitterFeed.java) | [download sample](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.wordnik%22%20AND%20a%3A%22swaggersocket-sample-twitter%22) | [README](https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-twitter/README.txt)
 * Wordnik's Real Time Search [client code](https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-wordnik/src/main/webapp/index.html) | [server code](https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-wordnik/src/main/scala/com/wordnik/swaggersocket/samples/WordnikResourceProxy.scala) | [download sample](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.wordnik%22%20AND%20a%3A%22swaggersocket-sample-wordnik%22) | [README](https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-wordnik/README.txt)
 * Simple Echo [client code](https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-echo/src/main/webapp/index.html) | [server code](https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-echo/src/main/scala/com/wordnik/swaggersocket/samples/SwaggerSocketResource.scala) | [download sample](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.wordnik%22%20AND%20a%3A%22swaggersocket-sample-echo%22) | [README](https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-echo/README.txt)
 * Simple HelloWorld [client code](https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-helloworld/src/main/webapp/index.html) | [server code](https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-helloworld/src/main/scala/com/wordnik/swaggersocket/samples/HelloWorld.scala) | [download sample](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.wordnik%22%20AND%20a%3A%22swaggersocket-sample-helloword%22) | [README](https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-helloworld/README.txt)
 * Simple Echo using CXF [client code](https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-cxf-echo/src/main/webapp/index.html) | [server code](https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-cxf-echo/src/main/java/com/wordnik/swaggersocket/samples/SwaggerSocketResource.java) | [download sample](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.wordnik%22%20AND%20a%3A%22swaggersocket-cxf-sample-echo%22) | [README](https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-cxf-echo/README.txt)
 * Simple Echo using Node.js [client code](https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-echo-node-client/src/main/resources/echo-client.js) | [download sample](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.wordnik%22%20AND%20a%3A%22swaggersocket-sample-echo-node-client%22) | [README](https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-echo-node-client/README.txt)

For Webapp samples, you can also download our [war files](http://search.maven.org/#search|ga|1|swaggersocket) and deploy them to any WebServer [supporting WebSockets](https://github.com/Atmosphere/atmosphere/wiki/Supported-WebServers-and-Browsers).

Note that both both Wordnik and Twitter Samples require a valid key to be configured in their corresponding web.xml file or passed as the command arguments when using nettosphere.sh. For details, refer to the README file of each sample project.



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
In addition, configure additional init-param parameters required to run the application. For example,
when using jersey to load resources under package com.wordnik.swaggersocket.samples.

```xml
    <servlet>
        ...
        <init-param>
            <param-name>com.sun.jersey.config.property.packages</param-name>
            <param-value>com.wordnik.swaggersocket.samples</param-value>
        </init-param>
```

### SwaggerSocket JavaScript API
The SwaggerSocket Client is defined as

```javascript
    // use new jQuery.swaggersocket... when using jQuery, otherwise use new swaggersocket...
    var swaggerSocket = new jQuery.swaggersocket.SwaggerSocket();
```

#### Listeners are per Request. The responses will be delivered as they come and can be correlated with their associated request using response.getRequest().

```javascript
    // use new jQuery.swaggersocket... when using jQuery, otherwise use new swaggersocket...
    var ss = new jQuery.swaggersocket.SwaggerSocketListener();
    ss.onOpen = function(response) {};
    ss.onClose = function(Response) {}; // Called when the Websocket gets closed
    ss.onError = function(Response) {}; // When an error occurs
    ss.onResponse = function(Response) {}; // When a response is ready
    ss.onResponses = function (Response) {}; // A List of all the ready responses
```

#### Opening a connection

```javascript
    // use new jQuery.swaggersocket... when using jQuery, otherwise use new swaggersocket...
    var request = new jQuery.swaggersocket.Request()
          .path(document.location.toString())
          .listener(ss);
    swaggerSocket.open(request);
```

#### Sending requests -- You can send an array of Requests or single Request.

```javascript
    var requests = new Array();
    // use new jQuery.swaggersocket... when using jQuery, otherwise use new swaggersocket...
    requests[0] = new jQuery.swaggersocket.Request()
            .path("path1")
            .method("POST")
            .data("FOO")
            .dataFormat("text/plain")
            .listener(ss);
    requests[1] = new jQuery.swaggersocket.Request()
            .path("/path2")
            .method("POST")
            .data("BAR")
            .dataFormat("text/plain")
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

### SwaggerSocket on Node.js
SwaggerSocket client (From 2.0.0) is available for Node.js. To see how it works, see [the swaggersocket-echo-node-client sample](samples/swaggersocket-echo-node-client).

### SwaggerSocket on OSGi
SwaggerSocket (From 2.0.1) is not only OSGi enabled but also available as a Karaf feature. To see how it works, see [the swaggersocket-cxf-osgi-echo sample](samples/swaggersocket-cxf-osgi-echo).

### How the protocol works
To read more about how the protocol works, take a look at the [SwaggerSocket Protocol Specification](https://github.com/swagger-api/swagger-socket/wiki/SwaggerSocket-Protocol)
