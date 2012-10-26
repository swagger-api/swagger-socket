/**
 *  Copyright 2012 Wordnik, Inc.
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
package com.wordnik.swaggersocket.server;

import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.FrameworkConfig;
import org.atmosphere.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * The SwaggerSocket Servlet, which enable support for the SwaggerSocket Protocol.
 */
public class SwaggerSocketServlet extends AtmosphereServlet {

    private final Logger logger = LoggerFactory.getLogger(SwaggerSocketServlet.class);

    public SwaggerSocketServlet() {
        this(false);
    }

    public SwaggerSocketServlet(boolean isFilter) {
        this(isFilter, true);
    }

    public SwaggerSocketServlet(boolean isFilter, boolean autoDetectHandlers) {
        super(isFilter, autoDetectHandlers);
        framework().addInitParameter(ApplicationConfig.PROPERTY_NATIVE_COMETSUPPORT, "true");
        framework().addInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
        framework().addInitParameter(ApplicationConfig.PROPERTY_SESSION_SUPPORT, "true");
    }

    @Override
    public void init(ServletConfig sc) throws ServletException {
        super.init(sc);
        TrackMessageSizeInterceptor t = new TrackMessageSizeInterceptor();
        t.excludedContentType("application/javascript").excludedContentType("text/html").excludedContentType("text/plain").messageDelimiter("<->");
        t.configure(framework().getAtmosphereConfig());
        framework().interceptor(t);
        framework().interceptor(new SwaggerSocketProtocolInterceptor());
        logger.info("Swagger Socket installed {}", Version.getRawVersion());
    }
}
