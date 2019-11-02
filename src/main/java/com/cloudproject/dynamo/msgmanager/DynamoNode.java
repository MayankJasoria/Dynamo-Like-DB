package com.cloudproject.dynamo.msgmanager;

import com.cloudproject.dynamo.models.Node;

import java.io.Serializable;

public class DynamoNode implements Serializable, Cloneable, Node {
    public String name;
    private String address;
    private int heartbeat;
    private transient TimeoutTimer timeoutTimer;
    private boolean apiNode;

    public DynamoNode(String name, String address, DynamoServer server, int heartbeat, int ttl, boolean apiNode) {
        this.name = name;
        this.address = address;
        this.heartbeat = heartbeat;
        this.timeoutTimer = new TimeoutTimer(ttl, server, this);
        this.apiNode = apiNode;
    }

    public String getAddress() {
        return address;
    }

    public int getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(int heartbeat) {
        this.heartbeat = heartbeat;
    }

    public void startTimer() {
        this.timeoutTimer.start();
    }

    public void resetTimer() {
        this.timeoutTimer.reset();
    }

    @Override
    public String toString() {
        return "DynamoNode <address=" + address + ", heartbeat=" + heartbeat + ">";
    }

    public boolean isApiNode() {
        return apiNode;
    }

    //    @Override
//    protected Object clone() {
//        return new DynamoNode(name, address, heartbeat, );
//    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DynamoNode other = (DynamoNode) obj;
        if (address == null) {
            return other.address == null;
        } else return address.equals(other.address);
    }
}
