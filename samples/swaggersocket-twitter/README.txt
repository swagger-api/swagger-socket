Twitter Search Demo using SwaggerSocket
========================================================================

Preparation for the demo
---------------------------------------
This demo requires a valid Twitter Consumer key and secret in order to 
access the Twitter's search service. If you do not have a valid key pair,
please go to http://apps.twitter.com/ and receive one. The steps are
described below:

1. Go to http://apps.twitter.com/ and login with your Twitter account.

2. Click on "Create New App"

3. Enter Name, Description, Website.

4. Accept the Agreement and click on "Create your Twitter application".

5. Once you have created your application, you can find your Consumer Key
   at its Application Settings section and its detail under the link
   "manage key and and access tokens".

6. Configure the application with your Consumer Key and Secret.

   - when using Web Application, set the following 
     init-parameters in web.xml

        <init-param>
            <param-name>com.twitter.consumer.key</param-name>
            <param-value>${your-consumer-key}</param-value>
        </init-param>
        <init-param>
            <param-name>com.twitter.consumer.secret</param-name>
            <param-value>${your-consumer-secret}</param-value>
        </init-param>

   - when using nettosphere.sh, pass the consumer key and secret
     values as the command line arguments.

       ./bin/nettosphere.sh ${your-consumer-key} ${your-consumer-secret}

     or run the command without the argument to get 
     promted for entering the key and secret (from 2.0.1)


Running the demo
---------------------------------------
Please refer to README.md at the samples root folder.
