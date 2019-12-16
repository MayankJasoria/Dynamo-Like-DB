package com.cloudproject.dynamo.models;

import java.io.Serializable;

/**
 * POJO for handling IO payloads
 */
public class ObjectIOModel implements Serializable {
    private long version;
    private String value;

    public ObjectIOModel(long version, String value) {
        this.version = version;
        this.value = value;
    }

    public ObjectIOModel() {
        // necessary public constructor for serialization
    }

    /**
     * Method to return the version number of the object associated with the server
     *
     * @return the version number of requested object registered with the server
     */
    public long getVersion() {
        return version;
    }

    /**
     * Method to return the value of the requested object from the server
     * @return the value of the requested object
     */
    public String getValue() {
        return value;
    }

    /**
     * Method to set the version number (received from object in the server)
     * @param version the version number associated with the object in server
     */
    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * Method to set the value associated with the object in server
     * @param value the value associated with the object in server
     */
    public void setValue(String value) {
        this.value = value;
    }
}
