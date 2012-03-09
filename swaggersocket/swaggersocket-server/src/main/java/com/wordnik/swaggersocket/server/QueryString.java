package com.wordnik.swaggersocket.server;

public class QueryString {
    private String name;
    private String value;

    public QueryString(){
    }

    public QueryString(String name, String value){
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
