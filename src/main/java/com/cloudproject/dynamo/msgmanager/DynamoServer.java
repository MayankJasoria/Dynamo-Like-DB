package com.cloudproject.dynamo.msgmanager;

import javax.management.NotificationListener;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.Notification;

public class DynamoServer implements NotificationListener {

    private final ArrayList<DynamoNode> nodeList;
    private final ArrayList<DynamoNode> deadList;

    private DatagramSocket server;
    private String address;
    private DynamoNode node;
    private Random random;
    private int gossipInt;
    private int ttl;

    public DynamoServer(String name, String address, ArrayList<String> addr_list, int gossipInt, int ttl) throws SocketException {
        this.address = address;
        this.nodeList = new ArrayList<>();
        this.deadList = new ArrayList<>();
        this.gossipInt = gossipInt;
        this.ttl = ttl;
        for (String addr : addr_list) {
            this.nodeList.add(new DynamoNode(null, addr, this,0, ttl));
        }
        this.node = new DynamoNode(name, address, this,0, ttl);
        int port = Integer.parseInt(address.split(":")[1]);

        /* init Random */
        this.random = new Random();
        /* Listen at port number port */
        this.server = new DatagramSocket(port);
        System.out.println("[Dynamo Server] Listening at port: " + port);
    }

    @Override
    public void handleNotification(Notification notification, Object o) {
        DynamoNode deadNode = (DynamoNode)notification.getUserData();
        System.out.println(">> Server leave: " + deadNode.name + "has left the network");
        synchronized (DynamoServer.this.nodeList) {
            DynamoServer.this.nodeList.remove(deadNode);
        }

        synchronized (DynamoServer.this.deadList) {
            DynamoServer.this.deadList.add(deadNode);
        }
    }

    private class Receiver implements Runnable {
        private AtomicBoolean keepRunning;

        public Receiver() {
            keepRunning = new AtomicBoolean(true);
        }
        public void run() {
            while (keepRunning.get()) {
                /* Logic for receiving */
                System.out.println("ghot");
                /* init a buffer where the packet will be placed */
                byte[] buf = new byte[500];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try {
                    DynamoServer.this.server.receive(p);
                    /* Parse this packet into an object */
                    ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Object readObject = ois.readObject();
                    if (readObject instanceof DynamoMessage) {
                        DynamoMessage msg = (DynamoMessage)readObject;
                        switch(msg.type) {
                            case PING:
                                System.out.println("[Dynamo Server] PING recieved from " + msg.srcNode.name);
                                break;
                            case NODE_LIST:
                                DynamoServer.this.mergeMembershipLists(msg.srcNode, msg.payload);
                                break;
                            default:
                                System.out.println("Unrecognized packet type: " + msg.type.name());
                        }
                    } else {
                        /* TODO: Handle unrecognized packet */
                        System.out.println("Malformed packet!");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    keepRunning.set(false);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendMessage(DynamoNode node, DynamoMessage msg) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(msg);
        byte[] buf = baos.toByteArray();

        String address = node.getAddress();
        String host = address.split(":")[0];
        int port = Integer.parseInt(address.split(":")[1]);

        InetAddress dest;
        dest = InetAddress.getByName(host);

        System.out.println("[DynamoServer] Sending " + msg.type.name() + " (" + buf.length + ") to " + dest.getHostAddress());

        DatagramSocket socket = new DatagramSocket();
        DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, dest, port);
        socket.send(datagramPacket);
        socket.close();
    }

    private class PingSender implements Runnable {
        DynamoMessage pingMsg;
        PingSender() {
            pingMsg =
                    new DynamoMessage(DynamoServer.this.node, MessageTypes.PING, null);
        }

        public void run() {

            while (true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(2000);
                    for (DynamoNode node : DynamoServer.this.nodeList) {
                        try {
                            DynamoServer.this.sendMessage(node, pingMsg);
                        } catch (IOException e) {
                            /* TODO: Change address to node name after gossip implementation */
                            System.out.println("[WARN] Could not send PING to " + node.getAddress());
                            e.printStackTrace();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class Gossiper implements Runnable {
        private AtomicBoolean keepRunning;

        Gossiper() {
            keepRunning = new AtomicBoolean(true);
        }

        public void run() {
            while (this.keepRunning.get()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(DynamoServer.this.gossipInt);
                    DynamoServer.this.sendMembershipList();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    this.keepRunning.set(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            this.keepRunning = null;
        }
    }

    private DynamoNode getRandomNode() {
        DynamoNode node = null;

        if (this.nodeList.size() > 0) {
            int rand = random.nextInt(this.nodeList.size());
            node = this.nodeList.get(rand);
        }

        return node;
    }

    private void sendMembershipList() throws IOException {
        this.node.setHeartbeat(this.node.getHeartbeat() + 1);
        //ArrayList<DynamoNode> sendList = cloneArrayList(this.nodeList);
        synchronized (this.nodeList) {
            DynamoNode dstNode = this.getRandomNode();
            if (node != null) {
                DynamoMessage listMsg =
                        new DynamoMessage(this.node, MessageTypes.NODE_LIST, this.nodeList);
                this.sendMessage(dstNode, listMsg);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeMembershipLists(DynamoNode srcNode, Object payload) {
        if (payload instanceof ArrayList<?>) {
            ArrayList<DynamoNode> remoteNodesList = (ArrayList<DynamoNode>) payload;
            /* add srcNode to list too, since it has sent, so should be alive ! */
            remoteNodesList.add(srcNode);
            synchronized (DynamoServer.this.deadList) {
                synchronized (DynamoServer.this.nodeList) {
                     /* Do the same with rest of the nodes */
                    for (DynamoNode remoteNode : remoteNodesList) {
                        if (DynamoServer.this.nodeList.contains(remoteNode)) {
                                /* Just update the heartbeat to the latest one
                                 * and reset timer */
                                DynamoNode localNode = DynamoServer.this.nodeList.get(DynamoServer.this.nodeList.indexOf((remoteNode)));
                                if (remoteNode.getHeartbeat() > localNode.getHeartbeat()) {
                                    localNode.setHeartbeat(remoteNode.getHeartbeat());
                                    localNode.resetTimer();
                                }
                        } else {
                                /* local list does not contain remoteNode */

                            if (DynamoServer.this.deadList.contains(remoteNode)) {
                                /* The remoteNode previously was there in the local list
                                 * but timed out. So revive it!!
                                 * Updated heartbeat to the one received latest, and start timer*/
                                DynamoNode localDeadNode = DynamoServer.this.deadList.get(DynamoServer.this.deadList.indexOf(remoteNode));
                                if (remoteNode.getHeartbeat() > localDeadNode.getHeartbeat()) {
                                    DynamoServer.this.deadList.remove(localDeadNode);
                                    DynamoNode newNode =
                                            new DynamoNode(remoteNode.name, remoteNode.getAddress(), this, remoteNode.getHeartbeat(), this.ttl);
                                    DynamoServer.this.nodeList.add(newNode);
                                    newNode.startTimer();
                                }

                            } else {
                                /* Probably a new member, add it to the list, use remote heartbeat,
                                 * start timer */
                                DynamoNode newNode =
                                        new DynamoNode(remoteNode.name, remoteNode.getAddress(), this, remoteNode.getHeartbeat(), this.ttl);
                                DynamoServer.this.nodeList.add(newNode);
                                newNode.startTimer();
                            }
                        }
                    }
                }
            }
        } else {
            System.out.println("[WARN] List received is not instanceof ArrayList");
        }

    }

//    private ArrayList<DynamoNode> cloneArrayList(ArrayList<DynamoNode> nodeList) {
//        ArrayList<DynamoNode> newList = new ArrayList<>();
//        for(DynamoNode node : nodeList) {
//            newList.add((DynamoNode)node.clone());
//        }
//        return newList;
//    }

    private void start() throws InterruptedException {
        ExecutorService exec = Executors.newCachedThreadPool();
        exec.execute(new Receiver());
        //exec.execute(new PingSender());
        exec.execute(new Gossiper());

        while (true) {
            TimeUnit.SECONDS.sleep(10);
        }
    }


    public static void startServer(String[] args) throws SocketException, InterruptedException {
        if (args == null) {
            System.out.println("[ERROR] Arguments required");
        } else if (args.length != 3) {
            System.out.println("[ERROR] Expected 3 arguments, received " + args.length);
        } else {
            ArrayList<String> addr_list =
                    new ArrayList<>(Arrays.asList(args[2].split(",")));
            DynamoServer server =
                    new DynamoServer(args[0], args[1], addr_list, Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            server.start();
        }
    }
}
