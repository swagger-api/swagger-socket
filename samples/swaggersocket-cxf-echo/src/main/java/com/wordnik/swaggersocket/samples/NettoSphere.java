/**
 *  Copyright 2015 Reverb Technologies, Inc.
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
package com.wordnik.swaggersocket.samples;

import com.wordnik.swaggersocket.server.SwaggerSocketProtocolInterceptor;

import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.handler.ReflectorServletProcessor;
import org.atmosphere.nettosphere.Config;
import org.atmosphere.nettosphere.Nettosphere;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NettoSphere {

    private static final Logger logger = LoggerFactory.getLogger(NettoSphere.class);

    public static void main(String[] args) throws IOException {
    	ReflectorServletProcessor rsp = new ReflectorServletProcessor();
    	rsp.setServletClassName(CXFNonSpringJaxrsServlet.class.getName());
    	
        int p = getHttpPort();
        Config.Builder b = new Config.Builder();
        b.resource("./app")
                .initParam(ApplicationConfig.WEBSOCKET_CONTENT_TYPE, "application/json")
                .initParam(ApplicationConfig.WEBSOCKET_METHOD, "POST")
                .initParam("jaxrs.serviceClasses", 
                    SwaggerSocketResource.class.getName() + "," + FileServiceResource.class.getName())
                .initParam("jaxrs.providers", 
                    JacksonJsonProvider.class.getName())
                .initParam("com.wordnik.swaggersocket.protocol.lazywrite", "true")
                .initParam("com.wordnik.swaggersocket.protocol.emptyentity", "true")
                .interceptor(new SwaggerSocketProtocolInterceptor())
                .resource("/*", rsp)
                .port(p)
                .host("127.0.0.1")
                .build();
        Nettosphere s = new Nettosphere.Builder().config(b.build()).build();
        s.start();
        String a = "";

        logger.info("NettoSphere SwaggerSocket Server started on port {}", p);
        logger.info("Type quit to stop the server");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (!(a.equals("quit"))) {
            a = br.readLine();
        }
        System.exit(-1);
    }

    private static int getHttpPort() {
        String v = System.getProperty("nettosphere.port");
        if (v != null) {
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                // ignore;
            }
        }
        return 8080;
    }
}
