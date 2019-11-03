package com.cloudproject.dynamo.consistenthash;

import com.cloudproject.dynamo.models.Node;
import com.cloudproject.dynamo.models.Quorum;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class for managing the hashing of nodes and data objects into nodes in a consistent manner
 *
 * @param <T> An object that extends the {@link Node} interface
 */
public class HashingManager<T extends Node> {

    private final TreeMap<Long, VirtualNode<T>> ring;
    private final HashFunction hashFunction;
    private final int vNodeCount;
    private final Object lock;

    public HashingManager(Collection<T> pNodes, @NotNull HashFunction hashFunction) {
        this.hashFunction = hashFunction;
        this.ring = new TreeMap<>();

        this.vNodeCount = 5;

        lock = new Object();

        // Add all existing nodes to the hash ring
        for (T pNode : pNodes) {
            addNode(pNode);
        }
    }

    /**
     * Adds a new physical node to the hash ring, with specified number of replicas
     *
     * @param pNode the physical node to be added to the ring
     */
    public void addNode(T pNode) {
        if (vNodeCount < 0) {
            throw new IllegalArgumentException("Number of virtual nodes cannot be negative!");
        }
        int existingReplicas = getExistingReplicas(pNode);
        for (int i = 0; i < vNodeCount; i++) {
            VirtualNode<T> vNode = new VirtualNode<>(pNode, i + existingReplicas);
            ring.put(hashFunction.hash(vNode.getAddress()), vNode);

            // TODO: Rehash data from adjacent nodes, both previous one and next one
        }
    }

    /**
     * Removes a physical node completely from the hash ring
     *
     * @param pNode the physical node to be removed form the hash ring
     */
    public void removeNode(T pNode) {
        ring.keySet().removeIf(key -> ring.get(key).isVirtualNodeOf(pNode));

        // TODO: Rehash data from next node (restore multiplicity)
//        Iterator<Long> it = ring.keySet().iterator();
//        while (it.hasNext()) {
//            if(ring.get(it.next()).isVirtualNodeOf(pNode)) {
//                it.remove();
//            }
//        }
    }

    /**
     * Method that returns all the nodes to which a data object will be hashed
     *
     * @param objectKey the key of the object to be hashed
     * @return list of nodes to which the object will be hashed
     */
    public ArrayList<T> routeNodes(@NotNull String objectKey) {
        if (ring.isEmpty()) {
            return null;
        }
        Long hash = hashFunction.hash(objectKey);
        SortedMap<Long, VirtualNode<T>> tailMap = ring.tailMap(hash);
//        Long nodeHash = (!tailMap.isEmpty()) ? tailMap.firstKey() : ring.firstKey();
//        ring.get(nodeHash).getPhysicalNode();
        ArrayList<T> nodesList = new ArrayList<>();
//        for (int i = 0; i < Quorum.getReplicas(); i++) {
//            if (tailMap.isEmpty()) {
//                // loop around
//                tailMap = ring.tailMap(ring.firstKey());
//            }
//            if (!nodesList.contains(ring.get(tailMap.firstKey()).getPhysicalNode())) {
//                // new physical node, add to list
//                nodesList.add(ring.get(tailMap.firstKey()).getPhysicalNode());
//            } else {
//                // physical node already exists in list, find another node
//                i--;
//            }
//            // get next element of the ring
//            tailMap = ring.tailMap(tailMap.firstKey());
//        }
        int i = 0;

        Long nodeHash = (!tailMap.isEmpty()) ? tailMap.firstKey() : ring.firstKey();
        while (i < Quorum.getReplicas()) {
            VirtualNode<T> node = ring.get(nodeHash);
            if (!nodesList.contains(node.getPhysicalNode())) {
                // if physical node was not already added, add it
                nodesList.add(node.getPhysicalNode());
                i++;
            }

            // get key for next node; loop around if higherKey does not exist
            nodeHash = (ring.higherKey(nodeHash) != null ? ring.higherKey(nodeHash) : ring.firstKey());
        }

        return nodesList;
    }

    /**
     * Returns the number of existing replicas of a physical node
     *
     * @param pNode the physical node whose number of replicas is required
     * @return (int) the number of existing replicas of the given physical node
     */
    private int getExistingReplicas(T pNode) {
        int replicas = 0;
        for (VirtualNode<T> vNode : ring.values()) {
            if (vNode.isVirtualNodeOf(pNode)) {
                replicas++;
            }
        }
        return replicas;
    }

    public Object getLock() {
        return lock;
    }

}
