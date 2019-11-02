package com.cloudproject.dynamo.msgmanager;

import java.io.Serializable;

public class ForwardPayload implements Serializable {
    private MessageTypes requestType;
    private String bucketName;
    private String objectName;
    private long txnID;

    public ForwardPayload(MessageTypes requestType, String bucketName, String objectName, long txnID) {
        this.requestType = requestType;
        this.bucketName = bucketName;
        this.objectName = objectName;
        this.txnID = txnID;
    }

    public MessageTypes getRequestType() {
        return requestType;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectName() {
        return objectName;
    }

    public long getTxnID() {
        return txnID;
    }

}
