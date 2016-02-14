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
package io.swagger.swaggersocket.java.jsr356.client.exception;

public class JSR356SwaggerSocketException extends RuntimeException {

    private int status;

    public JSR356SwaggerSocketException() {
        super();
    }

    public JSR356SwaggerSocketException(final String message) {
        super(message);
        status = 500;
    }

    public JSR356SwaggerSocketException(final String message, final Throwable cause) {
        super(message, cause);
        status = 500;
    }

    public JSR356SwaggerSocketException(final String message, final int status, final Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public JSR356SwaggerSocketException(final Throwable cause) {
        super(cause);
    }

    public int getStatus(){
        return status;
    }

}
