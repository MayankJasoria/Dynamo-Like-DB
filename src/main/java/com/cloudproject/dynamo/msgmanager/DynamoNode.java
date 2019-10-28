package com.cloudproject.dynamo.msgmanager;

import com.cloudproject.dynamo.models.Node;

import java.io.Serializable;

public class DynamoNode implements Serializable, Node {
    public String name;
    private String address;
    private int heartbeat;

    public DynamoNode(String name, String address, int heartbeat) {
        this.name = name;
        this.address = address;
        this.heartbeat = heartbeat;
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

    @Override
    public String toString() {
        return "DynamoNode <address=" + address + ", heartbeat=" + heartbeat + ">";
    }
}
