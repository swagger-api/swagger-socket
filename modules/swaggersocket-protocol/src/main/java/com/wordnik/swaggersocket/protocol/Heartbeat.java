package com.wordnik.swaggersocket.protocol;

public class Heartbeat {

    private String heartbeat;
    private String identity;

    public Heartbeat() {
    }

    public Heartbeat(String heartbeat, String identity) {
        this.heartbeat = heartbeat;
        this.identity = identity;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(String heartbeat) {
        this.heartbeat = heartbeat;
    }
}
