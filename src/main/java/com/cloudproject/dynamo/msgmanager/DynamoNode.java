package com.cloudproject.dynamo.msgmanager;

import com.cloudproject.dynamo.models.Node;
import com.cloudproject.dynamo.vector_clock.vclock.VClock;

import java.io.Serializable;

public class DynamoNode implements Serializable, Cloneable, Node {
    public String name;
    private String address;
    private int heartbeat;
    private transient TimeoutTimer timeoutTimer;

    //vclock
    private VClock vc;
    public VClock getVc() {
		return vc;
	}

	public void setVc(VClock vc) {
		this.vc = vc;
	}

	public DynamoNode(String name, String address, DynamoServer server, int heartbeat, int ttl) {
        this.name = name;
        System.out.println("dn "+name);
        this.address = address;
        this.heartbeat = heartbeat;
        this.timeoutTimer = new TimeoutTimer(ttl, server, this);
        this.vc = new VClock();
        
        this.vc.tick(name);
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
