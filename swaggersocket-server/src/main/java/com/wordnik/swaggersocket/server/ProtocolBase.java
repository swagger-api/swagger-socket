package com.wordnik.swaggersocket.server;

import java.util.List;

public class ProtocolBase {
    protected List<Header> headers;
    protected List<QueryString> queryString;
    protected String path;
    protected String uuid;
    protected String method;
    protected Object messageBody;
    protected String dataFormat;

    public ProtocolBase(){
    }

    public String getDataFormat() {
        return dataFormat;
    }

    public void setDataFormat(String dataFormat) {
        this.dataFormat = dataFormat;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<Header> getHeaders() {
        return headers;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setHeaders(List<Header> headers) {
        this.headers = headers;
    }

    public List<QueryString> getQueryString() {
        return queryString;
    }

    public void setQueryString(List<QueryString> queryString) {
        this.queryString = queryString;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(Object messageBody) {
        this.messageBody = messageBody;
    }

}
