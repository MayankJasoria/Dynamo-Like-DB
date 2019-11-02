package com.cloudproject.dynamo.consistenthash;

import com.cloudproject.dynamo.models.Node;
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
    private final int backups;

    private final Object lock;

    public HashingManager(Collection<T> pNodes, int vNodeCount, @NotNull HashFunction hashFunction, int backups) {
        this.hashFunction = hashFunction;
        this.ring = new TreeMap<>();
        this.backups = backups;

        lock = new Object();

        // Add all existing nodes to the hash ring
        for (T pNode : pNodes) {
            addNode(pNode, vNodeCount);
        }
    }

    public HashingManager(Collection<T> pNodes, int vNodeCount, @NotNull HashFunction hashFunction) {
        this(pNodes, vNodeCount, hashFunction, 2);
    }

    /**
     * Adds a new physical node to the hash ring, with specified number of replicas
     *
     * @param pNode      the physiclal node to be added to the ring
     * @param vNodeCount the number of replicas of the required node
     */
    public void addNode(T pNode, int vNodeCount) {
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
        for (int i = 0; i < backups; i++) {
            if (tailMap.isEmpty()) {
                tailMap = ring.tailMap(ring.firstKey());
            }
            if (!nodesList.contains(ring.get(tailMap.firstKey()).getPhysicalNode())) {
                nodesList.add(ring.get(tailMap.firstKey()).getPhysicalNode());
            }
            tailMap = ring.tailMap(tailMap.firstKey());
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
