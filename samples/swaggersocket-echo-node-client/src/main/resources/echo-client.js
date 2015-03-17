/**
 * echo-client.js
 * 
 * A node.js client program to call swaggersocket's echo sample.
 * 
 */

"use strict";

var reader = require('readline');
var prompt = reader.createInterface(process.stdin, process.stdout);

var swaggersocket = require('swaggersocket-client');

var ss = new swaggersocket.SwaggerSocketListener();
var swaggerSocket = new swaggersocket.SwaggerSocket();
var isopen = false;

ss.onOpen = function(r) {
    console.log("----------------------------");
    console.log("STATUS: " + r.getReasonPhrase());
    console.log("SwaggerSocket connected");
    console.log("----------------------------");
    isopen = true;

    prompt.setPrompt("message: ", 9);
    prompt.prompt();
};

ss.onResponse = function (r) {
    console.log("Response for Request: " + r.getRequest().getUUID() + (r.isLast() ? " (last)" : "") + " is '" + r.getData() + "'");

    prompt.setPrompt("message: ", 9);
    prompt.prompt();
};

ss.onClose = function(response) {
    isopen = false;
    console.log("----------------------------");
    console.log("STATUS: " + r.getReasonPhrase());
    console.log("SwaggerSocket closed; reconnecting ...");
    console.log("----------------------------");
}

ss.onError = function(response) {
    console.log("----------------------------");
    console.log("ERROR: " + r.getReasonPhrase());
    console.log("----------------------------");
};

var request = new swaggersocket.Request()
    .path('http://localhost:8080/swaggersocket')
    .dataFormat("text/plain")
    .listener(ss);
swaggerSocket.open(request);

setTimeout(function() {
   if (!isopen) {
       console.log("Unable to open a connection. Terminated.");
       process.exit(0);
   }
}, 3000);

prompt.
on('line', function(line) {
    var msg = line.trim();

    request = new swaggersocket.Request()
        .path("/echo")
        .method("POST")
        .dataFormat("text/plain")
        .data(msg)
        .listener(ss);

    console.log("Sending a request using uuid " + request.getUUID());
    swaggerSocket.send(request);

    prompt.setPrompt("> ", 2);
    prompt.prompt();
}).
on('close', function() {
    console.log("close");
    process.exit(0);
});
console.log("Connecting ...");
