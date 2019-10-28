package com.cloudproject.dynamo.msgmanager;

import java.io.Serializable;

public class DynamoMessage implements Serializable {
    public DynamoNode srcNode;
    public MessageTypes type;
    public Object payload;

    DynamoMessage(DynamoNode srcNode, MessageTypes type, Object payload) {
        this.srcNode = srcNode;
        this.type = type;
        this.payload = payload;
    }
}



