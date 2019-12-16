package com.cloudproject.dynamo.models;

/**
 * Enum to categorize each message that is exchanged within the system
 */
public enum MessageTypes {
    PING, NODE_LIST, BUCKET_CREATE, BUCKET_DELETE, OBJECT_CREATE,
    OBJECT_READ, OBJECT_UPDATE, OBJECT_DELETE, ACKNOWLEDGEMENT, FORWARD,
    FORWARD_ACK, FORWARD_ACK_READ
}