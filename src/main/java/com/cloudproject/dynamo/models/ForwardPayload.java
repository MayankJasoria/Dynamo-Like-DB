package com.cloudproject.dynamo.models;

import java.io.Serializable;

public class ForwardPayload implements Serializable {
    private MessageTypes requestType;
    private String bucketName;
    private Object inputModel;
    private long txnID;

    public ForwardPayload(MessageTypes requestType, String bucketName, Object inputModel, long txnID) {
        this.requestType = requestType;
        this.bucketName = bucketName;
        this.inputModel = inputModel;
        this.txnID = txnID;
    }

    public MessageTypes getRequestType() {
        return requestType;
    }

    public String getBucketName() {
        return bucketName;
    }

    public Object getInputModel() {
        return inputModel;
    }

    public long getTxnID() {
        return txnID;
    }

}
