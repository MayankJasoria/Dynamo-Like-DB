package com.cloudproject.dynamo.models;

import java.io.Serializable;

public class AckPayload implements Serializable {
    private MessageTypes requestType;
    private String identifier;
    private long txnID;
    private boolean status;

    public AckPayload(MessageTypes requestType, String identifier, long txnID, boolean status) {
        this.requestType = requestType;
        this.identifier = identifier;
        this.txnID = txnID;
        this.status = status;
    }

    /**
     * Method to return the request type (instance of {@link MessageTypes}
     *
     * @return type of the request
     */
    public MessageTypes getRequestType() {
        return requestType;
    }

    /**
     * Method to return the identifier associated with the request
     * @return identifier of the request
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Method to return the transaction ID of the request [not currently in use]
     * @return
     */
    public long getTxnID() {
        return txnID;
    }

    public boolean isStatus() {
        return status;
    }
}
