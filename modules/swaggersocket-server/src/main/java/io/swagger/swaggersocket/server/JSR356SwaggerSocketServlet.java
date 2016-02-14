/**
 * Copyright 2016 SmartBear Software
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.swagger.swaggersocket.server;

import org.atmosphere.cpr.ApplicationConfig;

public class JSR356SwaggerSocketServlet extends SwaggerSocketServlet {

    public JSR356SwaggerSocketServlet() {
        this(false);
    }

    public JSR356SwaggerSocketServlet(boolean isFilter) {
        this(isFilter, true);
    }

    public JSR356SwaggerSocketServlet(boolean isFilter, boolean autoDetectHandlers) {
        super(isFilter, autoDetectHandlers);
        framework().addInitParameter(ApplicationConfig.PROPERTY_NATIVE_COMETSUPPORT, "false");
        framework().addInitParameter(ApplicationConfig.WEBSOCKET_SUPPORT_SERVLET3, "true");
        framework().addInitParameter(ApplicationConfig.WEBSOCKET_PROTOCOL_EXECUTION, "true");
    }

}
