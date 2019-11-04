package com.cloudproject.dynamo.msgmanager;

import com.cloudproject.dynamo.models.MessageTypes;

import java.io.Serializable;

public class DynamoMessage implements Serializable {
    DynamoNode srcNode;
    MessageTypes type;
    Object payload;

    public DynamoMessage(DynamoNode srcNode, MessageTypes type, Object payload) {
        this.srcNode = srcNode;
        this.type = type;
        this.payload = payload;
    }
}



