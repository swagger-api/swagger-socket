Echo Demo using SwaggerSocket with CXF on Karaf
========================================================================

Running the demo
---------------------------------------
This demo uses Apache Karaf OSGi container and 
it is the OSGi version of the SwaggerSocket CXF echo sample.

### Preparation

Download apache-karaf-3.0.4.tar.gz from one of the [mirror sites](http://www.apache.org/dyn/closer.cgi/karaf/3.0.4/apache-karaf-3.0.4.tar.gz) and unpack the archive.

```bash
$ wget -N http://ftp.halifax.rwth-aachen.de/apache/karaf/3.0.4/apache-karaf-3.0.4.tar.gz
$ tar -zxf apache-karaf-3.0.4.tar.gz
$ cd apache-karaf-3.0.4
```

### Starting Karaf

Start Karaf by running bin/karaf at the Karaf folder, as shown below.

```bash
$ bin/karaf

        __ __                  ____      
       / //_/____ __________ _/ __/      
      / ,<  / __ `/ ___/ __ `/ /_        
     / /| |/ /_/ / /  / /_/ / __/        
    /_/ |_|\__,_/_/   \__,_/_/         

  Apache Karaf (3.0.4)

Hit '<tab>' for a list of available commands
and '[cmd] --help' for help on a specific command.
Hit '<ctrl-d>' or type 'system:shutdown' or 'logout' to shutdown Karaf.

karaf@root()>
```

### Install SwaggerSocket feature

Run the following Karaf console commands.

```bash
feature:repo-add mvn:io.swagger/swaggersocket-karaf-features/2.2.0-SNAPSHOT/xml/features
feature:install swaggersocket-server
```

The above will install the swaggersocket-server feature that includes SwaggerSocket's server components
including the depending components, as shown below.

```bash
karaf@root()> feature:repo-add mvn:io.swagger/swaggersocket-karaf-features/2.2.0-SNAPSHOT/xml/features
Adding feature url mvn:io.swagger/swaggersocket-karaf-features/2.2.0-SNAPSHOT/xml/features
karaf@root()> feature:install swaggersocket-server
karaf@root()> 
```

### Install CXF's JAXRS and Karaf's War features

Run the following Karaf commands.

```bash
feature:repo-add cxf 3.0.7
feature:install cxf-jaxrs
feature:install war
```

The above will install the necessary CXF components and Karaf's war support component, as shown below.

```bash
karaf@root()> feature:repo-add cxf 3.0.7
Adding feature url mvn:org.apache.cxf.karaf/apache-cxf/3.0.7/xml/features
karaf@root()> feature:install cxf-jaxrs
Refreshing bundles org.ops4j.pax.web.pax-web-jetty (79), org.apache.geronimo.specs.geronimo-jaspic_1.0_spec (69), org.eclipse.jetty.aggregate.jetty-all-server (70), org.ops4j.pax.web.pax-web-runtime (78)
karaf@root()> feature:install war
karaf@root()>
```

### Install SwaggerSocket OSGi CXF Echo Sample 

Finally, run the following command to install this sample bundle.

```bash
install -s mvn:io.swagger/swaggersocket-cxf-sample-osgi-echo/2.2.0-SNAPSHOT
```

This will install and start the sample bundle, as shown below.

```
karaf@root()> install -s mvn:io.swagger/swaggersocket-cxf-sample-osgi-echo/2.2.0-SNAPSHOT
Bundle ID: 130
karaf@root()> 
```

Verify whether the bundle is successfully installed and started by using command list.

```bash
karaf@root()> list
START LEVEL 100 , List Threshold: 50
 ID | State  | Lvl | Version        | Name                              
------------------------------------------------------------------------
 89 | Active |  80 | 2.3.6          | atmosphere-runtime                
 90 | Active |  80 | 2.2.0.SNAPSHOT | swaggersocket-protocol            
 91 | Active |  80 | 2.2.0.SNAPSHOT | swaggersocket-server              
130 | Active |  80 | 2.2.0.SNAPSHOT | swaggersocket-cxf-sample-osgi-echo
karaf@root()> 
```

If everything is fine, command web:list should show the installed web application.

```bash
karaf@root()> web:list
ID  | State       | Web-State   | Level | Web-ContextPath         | Name                                               
-----------------------------------------------------------------------------------------------------------------------
130 | Active      | Deployed    | 80    | /swaggersocket-cxf-echo | swaggersocket-cxf-sample-osgi-echo (2.2.0.SNAPSHOT)
karaf@root()> 
```


### Run the Demo

Open [http://localhost:8181/swaggersocket-cxf-echo/](http://localhost:8181/swaggersocket-cxf-echo/) using Browser
to access the sample echo page.


### Note

This sample uses Atmosphere which uses the SwaggerSocket protocol handler to host CXF's JAXRS service.
An alternative approach is to use CXF's integrated Atmosphere transport.
CXF will support the Atmosphere based transport directly and the SwaggerSocket protocol handler can be embedded in the CXF's transport. Thus, SwaggerSocket can be enabled on CXF endpoints without configuring an Atmosphere or SwaggerSocket servlet.

The echo service supports the following operations:


* echo: a POST based service that returns the input message. There are special messages: "secret" 
  triggers an exception and "sleep n" sleeps for n seconds before returning the response.
* ohce: a POST based service that returns the reversed input message.
* xbox: a GET based service that returns an XML enveloped response.
* jbox: a GET based service that returns an JSON enveloped response.
* put: a PUT based service that returns no response.
