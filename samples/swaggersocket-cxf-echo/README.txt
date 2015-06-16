Echo Demo using SwaggerSocket with CXF
========================================================================

Running the demo
---------------------------------------
Please refer to README.md at the samples root folder.


Note
---------------------------------------
This sample uses Atmosphere which uses the SwaggerSocket protocol handler to host CXF's JAXRS service.
An alternative approach is to use CXF's integrated Atmosphere transport.
CXF 3.0.5 will support the Atmosphere based transport directly and the SwaggerSocket protocol handler can be embedded in the CXF's transport. Thus, SwaggerSocket can be enabled on CXF endpoints without configuring an Atmosphere or SwaggerSocket servlet.

The echo service supports the following operations.

* echo: a POST based service that returns the input message. There are special messages: "secret" 
  triggers an exception and "sleep n" sleeps for n seconds before returning the response.
* ohce: a POST based service that returns the reversed input message.
* xbox: a GET based service that returns an XML enveloped response.
* jbox: a GET based service that returns an JSON enveloped response.
* put: a PUT based service that returns no response.
