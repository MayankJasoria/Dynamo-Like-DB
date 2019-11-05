package com.cloudproject.dynamo.consistenthash;

import com.cloudproject.dynamo.models.MessageTypes;
import com.cloudproject.dynamo.models.Node;
import com.cloudproject.dynamo.models.ObjectInputModel;
import com.cloudproject.dynamo.models.Quorum;
import com.cloudproject.dynamo.msgmanager.DynamoMessage;
import com.cloudproject.dynamo.msgmanager.DynamoNode;
import com.cloudproject.dynamo.msgmanager.DynamoServer;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.nio.charset.Charset;
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
        try {
            DynamoServer.startServer("Test", "test").getExecutorService().execute(new Rehash(pNode));
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private class Rehash extends Thread {

        private final T pNode;

        public Rehash(T pNode) {
            this.pNode = pNode;

        }

        @Override
        public void run() {
            if (vNodeCount < 0) {
                throw new IllegalArgumentException("Number of virtual nodes cannot be negative!");
            }
            try {
                DynamoServer server = DynamoServer.startServer("Test", "test");

                int existingReplicas = getExistingReplicas(pNode);
                for (int i = 0; i < vNodeCount; i++) {
                    VirtualNode<T> vNode = new VirtualNode<>(pNode, i + existingReplicas);
                    ring.put(hashFunction.hash(vNode.getAddress()), vNode);

                    // TODO: Rehash data from adjacent nodes, both previous one and next one
                    T lastHashNode = routeNodes(vNode.getAddress()).get(2);
                    // rehash all data (wherever applicable) from lastHashNode to pNode
                    // of which this is a vNode
                    T destNode = vNode.getPhysicalNode();
                    String path = "/buckets";

                    try {
                        if (lastHashNode.getAddress()
                                .equals((DynamoServer.startServer("Test", "test").getNode().getAddress()))) {
                            /* then directly send actions to destNode */
                            rehash(path, server, (DynamoNode) destNode);

                        } else {
                            /* send REHASH packets to lastHashNode */
                            DynamoMessage msg =
                                    new DynamoMessage(server.getNode(), MessageTypes.REHASH, destNode);
                            server.sendMessage((DynamoNode) lastHashNode, msg);
                        }

                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
        }
    }

    public void rehash(String path, DynamoServer server, DynamoNode destNode) throws IOException {
        /* buckets */
        File[] directories = new File(path).listFiles(File::isDirectory);
        if (directories != null) {
            for (File directory : directories) {
                DynamoMessage msg =
                        new DynamoMessage(server.getNode(), MessageTypes.BUCKET_CREATE, directory.getName());
//                        DynamoNode dest = new DynamoNode("dest", destNode.getAddress(), )
                DynamoNode dest = (DynamoNode) destNode;
                server.sendMessage(dest, msg);

                File[] files = directory.listFiles();
                if (files != null) {
                    for (File f : files) {
                        // check hash!
                        if (routeNodes(f.getName()).get(0).getAddress().equals(destNode.getAddress())) {
                            ObjectInputModel oim = new ObjectInputModel();
                            oim.setKey(f.getName());
                            oim.setValue(FileUtils.readFileToString(f, Charset.defaultCharset()));
                            Pair<String, ObjectInputModel> payload =
                                    new Pair<>(directory.getName(), oim);
                            DynamoMessage fileMsg = new DynamoMessage(server.getNode(),
                                    MessageTypes.OBJECT_CREATE, payload);
                            server.sendMessage(dest, fileMsg);
                            f.delete();
                        }
                    }
                }
            }
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
//        for (int i = 0; i < Quorum.getRrouteNodeseplicas(); i++) {
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
        VirtualNode<T> firstNode = ring.get(nodeHash);
        while (i < Quorum.getReplicas()) {
            VirtualNode<T> node = ring.get(nodeHash);
            if (!nodesList.contains(node.getPhysicalNode())) {
                // if physical node was not already added, add it
                nodesList.add(node.getPhysicalNode());
                i++;
            } else if (node.equals(firstNode)) {
                break;
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
