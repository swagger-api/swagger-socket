Node.js Echo Demo using swaggerSocket.js
========================================================================

Running the demo
---------------------------------------

Start the server side of the SwaggerSocket Echo sample.
Please refer to the swaggersocket-echo sample page at
https://github.com/swagger-api/swagger-socket/blob/master/samples/swaggersocket-echo/

After the server has been started, start this client by executing the following shell commands.
(NOTE: the previous version of atmosphere.js 2.2.12 had some issue with node.js v4 and thus
 we will need atmosphere.js 2.2.13.)

```bash
% cd src/main/resources
% npm install swaggersocket-client
% node echo-client.js
```
