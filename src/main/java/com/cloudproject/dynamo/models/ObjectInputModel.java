package com.cloudproject.dynamo.models;

import java.io.Serializable;

/**
 * Class used to deserialize an input JSON into a POJO
 */
public class ObjectInputModel implements Serializable {

    private String key;
    private String value;

    /**
     * Method to get the key
     *
     * @return key of the record
     */
    public String getKey() {
        return key;
    }

    /**
     * Method to set the key. Necessary for deserialization
     * @param key The key of the record
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Method to get the value of the record associated with the key
     * @return The value of the record
     */
    public String getValue() {
        return value;
    }

    /**
     * Method to set the value associated with the key. Used for deserialization
     * @param value The value of the record
     */
    public void setValue(String value) {
        this.value = value;
    }
}
