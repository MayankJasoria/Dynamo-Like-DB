package com.cloudproject.dynamo.models;

/**
 * POJO which is serialized to generate the required JSON
 * output, using the values of its non-null member variables
 */
public class ObjectOutputModel {

    private boolean status;

    private int[] vectorClocks;

    private String node;

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public int[] getVectorClocks() {
        return vectorClocks;
    }

    public void setVectorClocks(int[] vectorClocks) {
        this.vectorClocks = vectorClocks;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }
}
