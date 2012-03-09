package com.wordnik.swaggersocket.server;

public class HandshakeMessage {

    private Handshake handshake;

    public HandshakeMessage(){
    }

    public Handshake getHandshake() {
        return handshake;
    }

    public void setHandshake(Handshake handshake) {
        this.handshake = handshake;
    }
}
