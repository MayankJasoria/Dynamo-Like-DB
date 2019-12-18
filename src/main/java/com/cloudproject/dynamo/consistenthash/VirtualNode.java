package com.cloudproject.dynamo.consistenthash;

import com.cloudproject.dynamo.models.Node;

/**
 * Concrete implementation of Node which is used primarily for representing a
 * virtual copy of a physical node to add multiplicity in the hash ring
 *
 * @param <T> An object that extends the {@link Node} interface
 */
public class VirtualNode<T extends Node> implements Node {

    private final T physicalNode;
    private final int replicaIndex;

    public VirtualNode(T physicalNode, int replicaIndex) {
        this.physicalNode = physicalNode;
        this.replicaIndex = replicaIndex;
    }

    /**
     * @inheritDoc
     */
    @Override
    public String getAddress() {
        return physicalNode.getAddress() + "-" + replicaIndex;
    }

    /**
     * Method to check if this represents a virtual node of a given
     * physical node
     *
     * @param pNode An instance of the physical node
     * @return true if this is a virtual node of pNode, false otherwise
     */
    public boolean isVirtualNodeOf(T pNode) {
        return physicalNode.getAddress().equals(pNode.getAddress());
    }

    /**
     * Method to return the physical node
     *
     * @return The physical node of this virtual node
     */
    public T getPhysicalNode() {
        return physicalNode;
    }
}
