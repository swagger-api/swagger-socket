Echo Demo using SwaggerSocket
========================================================================

Running the demo
---------------------------------------
Please refer to README.md at the samples root folder.

The echo service supports the following operations.

* echo: a POST based service that returns the input message. There are special messages: "secret" 
  triggers an exception and "sleep n" sleeps for n seconds before returning the response.
* ohce: a POST based service that returns the reversed input message.
* xbox: a GET based service that returns an XML enveloped response.
* jbox: a GET based service that returns an JSON enveloped response.

