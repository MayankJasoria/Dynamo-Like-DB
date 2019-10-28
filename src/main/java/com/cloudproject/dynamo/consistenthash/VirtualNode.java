package com.cloudproject.dynamo.consistenthash;

import com.cloudproject.dynamo.models.Node;

public class VirtualNode<T extends Node> implements Node {

    private final T physicalNode;
    private final int replicaIndex;

    public VirtualNode(T physicalNode, int replicaIndex) {
        this.physicalNode = physicalNode;
        this.replicaIndex = replicaIndex;
    }

    @Override
    public String getKey() {
        return physicalNode.getKey() + "-" + replicaIndex;
    }

    /**
     * Method to check if this represents a virtual node of a given
     * physical node
     *
     * @param pNode An instance of the physical node
     * @return true if this is a virtual node of pNode, false otherwise
     */
    public boolean isVirtualNodeOf(T pNode) {
        return physicalNode.getKey().equals(pNode.getKey());
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
