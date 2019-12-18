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

    /**
     * Method to get the request type
     *
     * @return type of the request
     */
    public MessageTypes getRequestType() {
        return requestType;
    }

    /**
     * Method to get the bucket name
     * @return the name of the bucket
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Method to get the Input Model associated with the request
     * @return an input model associated object with the request
     */
    public Object getInputModel() {
        return inputModel;
    }

    /**
     * Method to get the transaction ID (currently not in use)
     * @return transaction ID of the request
     */
    public long getTxnID() {
        return txnID;
    }

}
