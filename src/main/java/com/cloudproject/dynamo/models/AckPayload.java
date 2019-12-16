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

    public MessageTypes getRequestType() {
        return requestType;
    }

    public String getIdentifier() {
        return identifier;
    }

    public long getTxnID() {
        return txnID;
    }

    public boolean isStatus() {
        return status;
    }
}
