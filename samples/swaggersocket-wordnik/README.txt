Wordnik Search Demo using SwaggerSocket
========================================================================

Preparation for the demo
---------------------------------------
This demo requires a valid Wordnik key in order to 
access the Wordnik's search service. If you do not have a valid key,
please go to http://developer.wordnik.com and receive one. The steps are
described below:

1. Go to http://developer.wordnik.com and enter your wordnik.com username
   and fill in the message field.

2. Click on "Sign me up for an API key

6. Configure the application with your API keu.

   - when using Web Application, set the following 
     init-parameters in web.xml

        <init-param>
            <param-name>com.wordnik.swagger.key</param-name>
            <param-value>${your-api-key}</param-value>
        </init-param>

   - when using nettosphere.sh, pass the API key value
     as the command line arguments.

       ./bin/nettosphere.sh ${your-api-key}

     or run the command without the argument to get 
     promted for entering the key (from 2.0.1)

Running the demo
---------------------------------------
Please refer to README.md at the samples root folder.
