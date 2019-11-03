package com.cloudproject.dynamo.models;

import java.io.Serializable;

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

    public long getVersion() {
        return version;
    }

    public String getValue() {
        return value;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
