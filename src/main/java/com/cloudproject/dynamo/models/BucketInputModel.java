package com.cloudproject.dynamo.models;

/**
 * POJO for holding information
 */
public class BucketInputModel {

    public String bucketName;

    /**
     * Method used to get the bucket name
     *
     * @return Bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Method used to set the bucket name
     * @param bucketName The new name of the bucket
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
}
