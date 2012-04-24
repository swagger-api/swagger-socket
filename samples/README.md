## Three ways to run a sample

### Using mvn jetty:run

```bash
 % cd swaggersocket-{name}
 % mvn jetty:run
 ```

 ### Using an external container

 Just deploy the swaggersocket-{name}.war to your favorite WebServer. It is important to note that your Server must support the WebSocket Protocol. Take a look at this [blog](http://jfarcand.wordpress.com/2012/04/19/websockets-or-comet-or-both-whats-supported-in-the-java-ee-land/) if you aren't sure.

 ### Using the [NettoSphere](https://github.com/Atmosphere/nettosphere) embedded server.

 ```bash
  % cd swaggersocket-{name}/target
  % unzip swaggersocket-{name}-distribution.zip
  % chmod a+x bin/nettosphere.sh
  % ./bin/nettosphere.sh
```