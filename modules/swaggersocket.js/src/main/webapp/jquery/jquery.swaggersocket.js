jQuery.swaggersocket = function() {

    // Disable unload
    jQuery(window).unbind("unload.atmosphere");

    return {

        version : 2.0,

        Options : {
            timeout : 300000,
            transport : 'websocket',
            maxRequest : 60,
            reconnect : true,
            maxStreamingLength : 10000000,
            method : 'POST',
            fallbackMethod : 'POST',
            fallbackTransport : 'long-polling',
            enableXDR : false,
            executeCallbackBeforeReconnect : false,
            withCredentials : false,
            trackMessageLength : false,
            messageDelimiter : '<->',
            connectTimeout : -1,
            reconnectInterval : 0,
            dropAtmosphereHeaders : true,
            readResponsesHeaders: false
        },

        _identity : 0,
        _logLevel : 'info',

        /**
         * Handshake object.
         * @private
         */
        _Handshake : function() {
            var _protocolVersion = "1.0", _protocolName = "SwaggerSocket", _dataFormat = "application/json", _method = "POST", _uuid = 0, _path = "/", _headers = null, _queryString = null, _self = {
                protocolVersion : function(protocolVersion) {
                    _protocolVersion = protocolVersion;
                    return this;
                },

                path : function(path) {
                    _path = path;
                    return this;
                },

                getPath : function() {
                    return path;
                },

                method : function(method) {
                    _method = method;
                    return this;
                },

                getMethod : function() {
                    return _method;
                },

                dataFormat : function(dataFormat) {
                    _dataFormat = dataFormat;
                    return this;
                },

                getDataFormat : function() {
                    return _dataFormat;
                },

                headers : function(headers) {
                    _headers = headers;
                    return this;
                },

                getHeaders : function() {
                    return _headers;
                },

                getQueryString : function() {
                    return _queryString;
                },

                queryString : function(queryString) {
                    _queryString = queryString;
                    return this;
                },

                toJSON : function() {
                    var s = "{ \"handshake\" : { \"protocolVersion\" : \""
                        + _protocolVersion
                        + "\",\"protocolName\" : \"" + _protocolName
                        + "\", \"uuid\" : \"" + _uuid
                        + "\", \"path\" : \"" + _path + "\","
                        + "\"dataFormat\" : \" " + _dataFormat + "\"";

                    if (_headers != null) {
                        s += ",\"headers\" : [" + jQuery.stringifyJSON(_headers) + "],";
                    }

                    if (_queryString != null) {
                        if (_headers == null) {
                            s += ",";
                        }
                        s += "\"queryString\" : [" + jQuery.stringifyJSON(_queryString) + "]";
                    }

                    s += "}}";
                    return s;
                }
            };
            return _self;
        },

        /**
         * SwaggerSocket Request object.
         */
        Request : function() {
            var _uuid = jQuery.atmosphere.guid(), _headers = null, _queryString = null, _dataFormat = "application/json", _data = "", _listener = null, _method = "POST", _path = "/", _self = {

                uuid : function(uuid) {
                    _uuid = uuid;
                    return this;
                },

                getUUID : function() {
                    return _uuid;
                },

                path : function(path) {
                    _path = path;
                    return this;
                },

                getPath : function() {
                    return _path;
                },

                method : function(method) {
                    _method = method;
                    return this;
                },

                getMethod : function() {
                    return _method;
                },

                headers : function(headers) {
                    _headers = headers;
                    return this;
                },

                getHeaders : function() {
                    return _headers;
                },

                dataFormat : function(dataFormat) {
                    _dataFormat = dataFormat;
                    return this;
                },

                getDataFormat : function() {
                    return _dataFormat;
                },

                queryString : function(queryString) {
                    _queryString = queryString;
                    return this;
                },

                getQueryString : function() {
                    return _queryString;
                },

                data : function(data) {
                    _data = data;
                    return this;
                },

                getData : function() {
                    return _data;
                },

                listener : function(listener) {
                    _listener = listener;
                    return this;
                },

                getListener : function() {
                    return _listener;
                },

                /**
                 * The
                 * @param identity
                 */
                _toCompleteJSON : function(identity) {
                    return "{ \"identity\" : \"" + identity + "\","
                        + "\"requests\" : [ {"
                        + this._toJSON()
                        + "] }";
                },

                _toJSON : function() {
                    var s = "\"uuid\" : \"" + _uuid + "\","
                        + "\"method\" : \"" + _method + "\","
                        + "\"path\" : \"" + _path + "\","
                        + "\"dataFormat\" : \"" + _dataFormat + "\"";

                    if (_headers != null) {
                        s += ",\"headers\" : [" + jQuery.stringifyJSON(_headers) + "],";
                    }

                    if (_queryString != null) {
                        if (_headers == null) {
                            s += ",";
                        }
                        s += "\"queryString\" : [" + jQuery.stringifyJSON(_queryString) + "]";
                    }

                    if (_dataFormat.toLowerCase().indexOf("json") == -1 || _data == "") {
                        s += ",\"messageBody\" : \"" + _data + "\"}";
                    } else {
                        s += ",\"messageBody\" : " + _data + "}";
                    }
                    return s;
                }
            };
            return _self;
        },

        CloseMessage : function() {
            var _reason, _identity, _self = {

                reason:function (reason) {
                    _reason = reason;
                    return this;
                },

                identity:function (identity) {
                    _identity = identity;
                    return this;
                },

                toJSON : function() {
                    var s = "{ \"close\" : { \"reason\" : \""
                        + _reason
                        + "\",\"identity\" : \"" + _identity
                        + "\" }}"
                    return s;
                }
            };
            return _self;
        },

        /**
         * A SwaggerSocket Response object.
         */
        Response : function() {

            var _uuid = 0, _request = null, _status = "200", _reasonPhrase = "OK", _path = '/', _headers = [], _data = "", _self = {

                uuid : function(uuid) {
                    _uuid = uuid;
                    return this;
                },

                getUUID : function() {
                    return _uuid;
                },

                request : function(request) {
                    _request = request;
                    return this;
                },

                getRequest : function() {
                    return _request;
                },

                path : function(path) {
                    _path = path;
                    return this;
                },

                getPath : function() {
                    return _path;
                },

                statusCode : function(status) {
                    _status = status;
                    return this;
                },

                getStatusCode : function() {
                    return _status;
                },

                headers : function(headers) {
                    _headers = headers;
                    return this;
                },

                getHeaders : function() {
                    return _headers;
                },

                reasonPhrase : function(reasonPhrase) {
                    _reasonPhrase = reasonPhrase;
                    return this;
                },

                getReasonPhrase : function() {
                    return _reasonPhrase;
                },

                data : function(data) {
                    _data = data;
                    return this;
                },

                getData : function() {
                    return _data;
                }
            };
            return _self;
        },

        /**
         * A per Request event listener.
         */
        SwaggerSocketListener : function() {
            var onResponse = function(response) {
            }, onError = function(response) {
            }, onClose = function(response) {
            }, onOpen = function(response) {
            }, onResponses = function(response) {
            }, onTransportFailure = function(response) {
            }
        },

        /**
         * Represent a SwaggerSocket connection.
         */
        SwaggerSocket : function() {
            /**
             * The Atmosphere's Connections.
             * @private
             */
            var _socket;

            /**
             * HashMap of current live request.
             * @private
             */
            var _requestsMap = new HashMap();

            /**
             * Global callback. Used for logging.
             * @param response
             * @private
             */
            var _loggingCallback = function(response) {
                jQuery.atmosphere.log("Status " + response.status);
                jQuery.atmosphere.log("Transport " + response.transport);
                jQuery.atmosphere.log("State " + response.state);
                jQuery.atmosphere.log("Data " + response.responseBody);
            };

            /**
             *
             * @param requests
             * @private
             */
            var _construct = function(requests) {
                var jsonReq = "{ \"identity\" : \"" + _identity + "\"," + "\"requests\" : [ {";
                jQuery.each(requests, function(index, req) {
                    _requestsMap.put(req.getUUID(), req);
                    jsonReq += (index == requests.length - 1) ? req._toJSON() : req._toJSON() + ",{";
                });
                jsonReq += "] }";
                return jsonReq;
            };

            var _self = {
                /**
                 * Open a SwaggerSocket connection to the server.
                 *
                 * You MUST wait for the Response before using the send method
                 * @param request
                 * @private
                 */
                open : function(request, cFunction, options) {

                    var _openFunction;

                    if (typeof(request) == "string") {
                        var path = request;
                        request = new jQuery.swaggersocket.Request();
                        request.method("POST").path(path);
                    }

                    if (typeof(cFunction) == 'function') {
                        _openFunction = cFunction;
                    }
                    function _pushResponse(response, state, listener) {
                        // handshake has been done
                        if (state == "messageReceived") {
                            switch (Object.prototype.toString.call(response)) {
                            case "[object Array]":
                                if (typeof(listener.onResponses) != 'undefined') {
                                    try {
                                        listener.onResponses(response);
                                    } catch (err) {
                                        if (jQuery.swaggersocket._logLevel == 'debug') {
                                            jQuery.atmosphere.debug(err.type);
                                        }
                                    }
                                }
                                return;
                            default:
                                if (response.getStatusCode() < 400 && typeof(listener.onResponse) != 'undefined') {
                                    try {
                                        listener.onResponse(response);
                                    } catch (err) {
                                        if (jQuery.swaggersocket._logLevel == 'debug') {
                                            jQuery.atmosphere.debug(err.type);
                                        }
                                    }
                                } else if (typeof(listener.onError) != 'undefined') {
                                    try {
                                        listener.onError(response);
                                    } catch (err) {
                                        if (jQuery.swaggersocket._logLevel == 'debug') {
                                            jQuery.atmosphere.debug(err.type);
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    };

                    // TODO : check request and options' type.
                    // TODO: Support debug level

                    var _handshake = new jQuery.swaggersocket._Handshake()
                        .path(request.getPath())
                        .dataFormat(request.getDataFormat())
                        .headers(request.getHeaders())
                        .queryString(request.getQueryString())
                        .method(request.getMethod());

                    if (typeof(options) == 'undefined') {
                        options = jQuery.swaggersocket.Options;
                    } else {
                        options = jQuery.extend(options, jQuery.swaggersocket.Options);
                    }

                    var _incompleteMessage = "";
                    _socket = jQuery.atmosphere.subscribe(request.getPath(), _loggingCallback, jQuery.atmosphere.request = {
                        logLevel : jQuery.swaggersocket._logLevel,
                        headers : { "SwaggerSocket": "1.0"},
                        transport : options.transport,
                        method : request.getMethod(),
                        fallbackTransport : options.fallbackTransport,
                        fallbackMethod : options.fallbackMethod,
                        timeout : options.timeout,
                        maxRequest :options.maxRequest,
                        reconnect : options.reconnect,
                        maxStreamingLength : options.maxStreamingLength,
                        enableXDR : options.enableXDR,
                        executeCallbackBeforeReconnect : options.executeCallbackBeforeReconnect,
                        withCredentials : options.withCredentials,
                        trackMessageLength : true,
                        messageDelimiter : options.messageDelimiter,
                        connectTimeout : options.connectTimeout,
                        reconnectInterval : options.reconnectInterval,
                        dropAtmosphereHeaders : options.dropAtmosphereHeaders,
                        data: _handshake.toJSON(),

                        callback : function(response) {
                            try {
                                var data = _incompleteMessage + response.responseBody;
                                // A long-Polling response may comes in two chunk (two connections)
                                if (_incompleteMessage != "" ) {
                                    response.state = "messageReceived";
                                }
                                var messageData = response.state != "messageReceived" 
                                    ? "" : JSON.parse(data.replace(/^\d+<->/, ''));
                                var listener = jQuery.extend(request.getListener(), new jQuery.swaggersocket.SwaggerSocketListener());
                                var r = new jQuery.swaggersocket.Response();
                                // _incompleteMessage != "" means the server sent a maxed out buffer but still invalid JSON
                                if (response.state == "messageReceived" || response.state == "opening") {
                                    _incompleteMessage = "";
                                    if (messageData.status) {
                                        // handling the handshake response
                                        r.statusCode(messageData.status.statusCode).reasonPhrase(messageData.status.reasonPhrase);
                                        if (r.getStatusCode() == 200) {
                                            _identity = messageData.identity;
                                            if (typeof(listener.onOpen) != 'undefined') {
                                                listener.onOpen(r);
                                            }
                                        } else {
                                            if (typeof(listener.onError) != 'undefined') {
                                                listener.onError(r);
                                            }
                                        }
                                    } else if (messageData.heartbeat) {
                                        if (jQuery.swaggersocket._logLevel == 'debug') {
                                            jQuery.atmosphere.debug("heartbeat" + messageData.heartbeat);
                                        }
                                    } else if (messageData.responses) {
                                        var _responses = new Array();
                                        var i = 0;
                                        jQuery.each(messageData.responses, function(index, res) {
                                            r.statusCode(res.statusCode).path(res.path).headers(res.headers).data(res.messageBody).uuid(res.uuid);

                                            /*
                                             We may run OOM here because we kept the Request object around.
                                             TODO: Need to find a way to fix that by either re-creating the request
                                             */

                                            r.request(_requestsMap.get(res.uuid));
                                            listener = jQuery.extend(r.getRequest().getListener(), new jQuery.swaggersocket.SwaggerSocketListener());

                                            _pushResponse(r, response.state, listener)
                                            _responses[i++] = r;
                                            r = new jQuery.swaggersocket.Response();
                                        });
                                        _pushResponse(_responses, response.state, listener)
                                    }
                                } else if (response.state == 're-opening') {
                                    response.request.method = 'GET';
                                    response.request.data = '';
                                } else if (response.state == "closed" && typeof(listener.onClose) != 'undefined') {
                                    r.reasonPhrase("close").statusCode(503);
                                    try {
                                        listener.onClose(r);
                                    } catch (err) {
                                        if (jQuery.swaggersocket._logLevel == 'debug') {
                                            jQuery.atmosphere.debug(err.type);
                                        }
                                    }
                                } else if (response.state == "transportFailure") {
                                    if (typeof(listener.onTransportFailure) != 'undefined') {
                                        try{
                                            listener.onTransportFailure(response);
                                        } catch (err) {
                                            jQuery.atmosphere.error(err.type);
                                        }
                                    }
                                } else if (response.state == "error" && typeof(listener.onError) != 'undefined') {
                                    r.statusCode(response.statusCode).reasonPhrase(response.reasonPhrase);
                                    try {
                                        listener.onError(r);
                                    } catch (err) {
                                        if (jQuery.swaggersocket._logLevel == 'debug') {
                                            jQuery.atmosphere.debug(err.type);
                                        }
                                    }
                                }
                            } catch (err) {
                                if (jQuery.swaggersocket._logLevel == 'debug') {
                                    jQuery.atmosphere.debug(err.type);
                                }
                                _incompleteMessage = _incompleteMessage + response.responseBody;
                            }
                        }
                    });
                    return this;
                },

                /**
                 * Send requests using the SwaggerSocket connection.
                 * @param request
                 */
                send : function(requests) {
                    if (typeof(_identity) == 'undefined') {
                        // requests may be a single request or an array
                        var listener = 
                            jQuery.extend((requests.constructor.toString().indexOf("Array") < 0 
                                           ? requests : requests[0]).getListener, 
                                          new jQuery.swaggersocket.SwaggerSocketListener());
                        var r = new jQuery.swaggersocket.Response();
                        r.statusCode("503").reasonPhrase("The open operation hasn't completed yet. Make sure your SwaggerSocketListener#onOpen has been invoked first.");

                        if (typeof(listener.onError) != "undefined") {
                            listener.onError(r);
                        }
                        jQuery.atmosphere.error("The open operation hasn't completed yet. Make sure your SwaggerSocketListener#onOpen has been invoked first.");
                        return;
                    }

                    /**
                     * Invoke the socket.
                     * @param request
                     * @private
                     */
                    function _send(data) {
                        _socket.push(jQuery.atmosphere.request = {
                            logLevel : 'debug',
                            transport : 'long-polling',
                            headers : { "SwaggerSocket": "1.0"},
                            method : "POST",
                            fallbackTransport : 'long-polling',
                            data: data
                        });
                    };

                    switch (Object.prototype.toString.call(requests)) {
                        case "[object Array]":
                            _send(_construct(requests));
                            return;
                        default:
                            _requestsMap.put(requests.getUUID(), requests);
                            _send(requests._toCompleteJSON(_identity));
                    }
                    return this;
                },

                /**
                 * Close the underlying connection.
                 */
                close : function() {
                    if (typeof(_socket) != 'undefined') {
                        var r = new jQuery.swaggersocket.CloseMessage();
                        r.reason("unload").identity(_identity);
                        _socket.push(jQuery.atmosphere.request = {
                            logLevel : 'debug',
                            transport : 'long-polling',
                            headers : { "SwaggerSocket": "1.0"},
                            method : "POST",
                            fallbackTransport : 'long-polling',
                            data: r.toJSON()
                        });
                        _socket.close();
                    }
                    return this;
                }
            };
            if (jQuery.browser.msie) {
                jQuery(window).bind("beforeunload", function(){
                    _self.close();
                });
            } else {
                jQuery(window).bind("unload.swaggersocket", function() {
                   _self.close();
               });
            }
            return _self;
        }
    }
}();

/**
 * © Mojavelinux, Inc · Document Version: 1.1 · Last Modified: Sun Mar 29, 2009 · License: Creative Commons
 */
function HashMap() {
    this.length = 0;
    this.items = new Array();
    for (var i = 0; i < arguments.length; i += 2) {
        if (typeof(arguments[i + 1]) != 'undefined') {
            this.items[arguments[i]] = arguments[i + 1];
            this.length++;
        }
    }

    this.remove = function(in_key) {
        in_key = jQuery.trim(in_key);
        var tmp_previous;
        if (typeof(this.items[in_key]) != 'undefined') {
            this.length--;
            var tmp_previous = this.items[in_key];
            delete this.items[in_key];
        }

        return tmp_previous;
    }

    this.get = function(in_key) {
        in_key = jQuery.trim(in_key);
        return this.items[in_key];
    }

    this.put = function(in_key, in_value) {
        in_key = jQuery.trim(in_key);
        var tmp_previous;
        if (typeof(in_value) != 'undefined') {
            if (typeof(this.items[in_key]) == 'undefined') {
                this.length++;
            }
            else {
                tmp_previous = this.items[in_key];
            }

            this.items[in_key] = in_value;
        }

        return tmp_previous;
    }

    this.containsKey = function(in_key) {
        in_key = jQuery.trim(in_key);
        return typeof(this.items[in_key]) != 'undefined';
    }

    this.clear = function() {
        for (var i in this.items) {
            delete this.items[i];
        }

        this.length = 0;
    }
}
