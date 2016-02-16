/**
 *  Copyright 2016 SmartBear Software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.swagger.swaggersocket.java.jsr356.client;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class EmbeddedTomcatTestBase {

    protected static int port;
    protected static Tomcat tomcat;

    @BeforeClass
    public static void startTomcat() throws IOException {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(0);
            port = serverSocket.getLocalPort();
        }
        finally {
            serverSocket.close();
        }

        try {
            tomcat = new Tomcat();
            tomcat.setPort(port);

            final String webAppDir = System.getProperty("web.app.dir", "src/main/webapp");
            tomcat.addWebapp("", new File(webAppDir).getAbsolutePath());
            System.out.println("configuring app with basedir: " + new File("./src/main/webapp/").getAbsolutePath());

            final Connector nioConnector = new Connector(Http11NioProtocol.class.getName());
            nioConnector.setPort(port);
            nioConnector.setSecure(false);
            nioConnector.setScheme("http");
            nioConnector.setProtocol("Http/1.1");

            try {
                nioConnector.setProperty("address", InetAddress.getByName("localhost").getHostAddress());
            }
            catch (final Exception e){
                throw new RuntimeException("NIO Connector Exception", e);
            }

            tomcat.getService().removeConnector(tomcat.getConnector());
            tomcat.getService().addConnector(nioConnector);
            tomcat.setConnector(nioConnector);

            tomcat.start();
        } catch (Exception e) {
            try {
                tomcat.stop();
            } catch (final Exception e2) {
                throw new RuntimeException("Failed to Stop Embedded Tomcat!");
            }

            throw new RuntimeException("Failed to Start Embedded Tomcat!");
        }
    }

    @AfterClass
    public static void stopTomcat() {
        try {
            tomcat.stop();
            FileUtils.deleteDirectory(new File(String.format("tomcat.%d", port)));
        } catch (final Exception e) {
            throw new RuntimeException("Failed to Stop Embedded Tomcat!");
        }
    }

}
