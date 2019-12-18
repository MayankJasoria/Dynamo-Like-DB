package com.cloudproject.dynamo.models;

import java.io.Serializable;

/**
 * POJO which is serialized to generate the required JSON
 * output, using the values of its non-null member variables
 */
public class OutputModel implements Serializable {

    private String response;
    private boolean status;

    /**
     * Method to get the status of the request
     *
     * @return true if the request was successfully executed, false otherwise
     */
    public boolean isStatus() {
        return status;
    }

    /**
     * Method to set the status of the request. Used by the receiver of the request
     * @param status the status of the request. True if successful, false otherwise
     */
    public void setStatus(boolean status) {
        this.status = status;
    }

    /**
     * Method to get the response string as a result of the request
     * @return The response string
     */
    public String getResponse() {
        return response;
    }

    /**
     * Method to set the response string according to the request and its status.
     * Used by the receiver of the request
     * @param response The response string
     */
    public void setResponse(String response) {
        this.response = response;
    }
}
