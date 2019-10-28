package com.cloudproject.dynamo.models;

/**
 * Represents a node of the system.
 * This is hashed to a hash ring for consistent hashing
 */
public interface Node {

    /**
     * @return The key to be used for hashing
     */
    String getKey();
}
