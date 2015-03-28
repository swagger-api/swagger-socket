Echo Demo using SwaggerSocket with CXF on Karaf
========================================================================

Running the demo
---------------------------------------
This demo uses Apache Karaf OSGi container and 
it is the OSGi version of the SwaggerSocket CXF echo sample.

### Preparation

Build SwaggerSocket.

```bash
$ clone https://github.com/swagger-api/swagger-socket.git
$ cd swagger-socket
$ mvn install
```

Download apache-karaf-3.0.3.tar.gz from one of the [mirror sites](http://www.apache.org/dyn/closer.cgi/karaf/3.0.3/apache-karaf-3.0.3.tar.gz) and unpack the archive.

```bash
$ wget -N http://ftp.halifax.rwth-aachen.de/apache/karaf/3.0.3/apache-karaf-3.0.3.tar.gz
$ tar -zxf apache-karaf-3.0.3.tar.gz
$ cd apache-karaf-3.0.3
```

### Starting Karaf

```bash
$ bin/karaf

        __ __                  ____      
       / //_/____ __________ _/ __/      
      / ,<  / __ `/ ___/ __ `/ /_        
     / /| |/ /_/ / /  / /_/ / __/        
    /_/ |_|\__,_/_/   \__,_/_/         

  Apache Karaf (3.0.3)

Hit '<tab>' for a list of available commands
and '[cmd] --help' for help on a specific command.
Hit '<ctrl-d>' or type 'system:shutdown' or 'logout' to shutdown Karaf.

karaf@root()>
```

### Install SwaggerSocket feature

```bash
karaf@root()> feature:repo-add mvn:com.wordnik/swaggersocket-karaf-features/2.0.1-SNAPSHOT/xml/features
karaf@root()> feature:install swaggersocket-server
```

The above will install the swaggersocket-server feature that includes SwaggerSocket's server components
including the depending components.

### Install CXF's JAXRS and Karaf's War features

```bash
karaf@root()> feature:repo-add cxf 3.0.4
karaf@root()> feature:install cxf-jaxrs
karaf@root()> feature:install war
```

The above will install the necessary CXF components and Karaf's war support component.

### Install SwaggerSocket OSGi CXF Echo Sample 

```bash
karaf@root()> install -s mvn:com.wordnik/swaggersocket-cxf-sample-osgi-echo/2.0.1-SNAPSHOT
```

Verify whether the bundle is successfully installed and started by using command list.
If everything is fine, command web:list should show the installed web application.

```bash
karaf@root()> list
START LEVEL 100 , List Threshold: 50
 ID | State  | Lvl | Version        | Name                              
------------------------------------------------------------------------
 90 | Active |  80 | 2.2.6          | atmosphere-runtime                
 91 | Active |  80 | 2.0.1.SNAPSHOT | swaggersocket-protocol            
 92 | Active |  80 | 2.0.1.SNAPSHOT | swaggersocket-server              
130 | Active |  80 | 2.0.1.SNAPSHOT | swaggersocket-cxf-sample-osgi-echo
karaf@root()> web:list
ID  | State       | Web-State   | Level | Web-ContextPath         | Name                                               
-----------------------------------------------------------------------------------------------------------------------
130 | Active      | Deployed    | 80    | /swaggersocket-cxf-echo | swaggersocket-cxf-sample-osgi-echo (2.0.1.SNAPSHOT)
karaf@root()> 
```


### Run the Demo

Open [http://localhost:8181/swaggersocket-cxf-echo/](http://localhost:8181/swaggersocket-cxf-echo/) using Browser
to access the sample echo page.


### Note

This sample uses Atmosphere using the SwaggerSocket protocol handler to host CXF's JAXRS service.
CXF 3.0.5 supports the Atmosphere based transport and the SwaggerSocket protocol handler can be
integrated in the CXF's transport.

