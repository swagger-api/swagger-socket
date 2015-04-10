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
