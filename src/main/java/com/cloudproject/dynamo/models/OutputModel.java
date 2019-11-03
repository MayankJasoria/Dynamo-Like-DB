package com.cloudproject.dynamo.models;

/**
 * POJO which is serialized to generate the required JSON
 * output, using the values of its non-null member variables
 */
public class OutputModel {

    private String response;

    private boolean status;

    private String vectorClocks;

    private String node;

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getVectorClocks() {
        return vectorClocks;
    }

    public void setVectorClocks(String vectorClocks) {
        this.vectorClocks = vectorClocks;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
