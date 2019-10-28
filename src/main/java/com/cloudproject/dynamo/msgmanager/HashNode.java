package com.cloudproject.dynamo.msgmanager;

import com.cloudproject.dynamo.models.Node;

public class HashNode extends DynamoNode implements Node {

    public HashNode(String name, String address, DynamoServer server, int heartbeat, int ttl) {
        super(name, address, server, heartbeat, ttl);
    }

}
