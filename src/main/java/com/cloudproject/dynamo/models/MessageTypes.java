package com.cloudproject.dynamo.models;


public enum MessageTypes {
    PING, NODE_LIST, BUCKET_CREATE, BUCKET_DELETE, OBJECT_CREATE,
    OBJECT_READ, OBJECT_UPDATE, OBJECT_DELETE, ACKNOWLEDGEMENT, FORWARD,
    FORWARD_ACK, FORWARD_ACK_READ
}