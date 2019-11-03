package com.cloudproject.dynamo.models;

/**
 * Class which defines global definition of number of replicas, read quorum and write quorum
 */
public class Quorum {

    /**
     * Method to return the number of nodes into which
     * an object is to be hashed (and read from)
     *
     * @return no. of nodes into which an object is attempted to be hashed
     */
    public static int getReplicas() {
        return 3;
    }

    /**
     * Method to return the number of nodes from which a read request
     * must be successful to return a success message
     *
     * @return read quorum
     */
    public static int getReadQuorum() {
        return 2;
    }

    /**
     * Method to return the number of nodes into which a write must
     * be successfully performed to return a success message
     *
     * @return write quorum
     */
    public static int getWriteQuorum() {
        return 2;
    }
}
