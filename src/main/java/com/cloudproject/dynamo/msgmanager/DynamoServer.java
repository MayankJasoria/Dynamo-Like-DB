package com.cloudproject.dynamo.msgmanager;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DynamoServer {

    private ArrayList<DynamoNode> nodeList;
    private ArrayList<DynamoNode> deadList;

    private DatagramSocket server;
    private String address;
    private DynamoNode node;

    public DynamoServer(String name, String address, ArrayList<String> addr_list) throws SocketException {
        this.address = address;
        this.nodeList = new ArrayList<>();
        for (String addr : addr_list) {
            this.nodeList.add(new DynamoNode(null, addr, 0));
        }
        this.node = new DynamoNode(name, address, 0);
        int port = Integer.parseInt(address.split(":")[1]);

        /* Listen at port number port */
        this.server = new DatagramSocket(port);
        System.out.println("[Dynamo Server] Listening at port: " + port);
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
                                /* TODO: Implement NODE LIST RECV */
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

    private void start() throws InterruptedException {
        ExecutorService exec = Executors.newCachedThreadPool();
        exec.execute(new Receiver());
        exec.execute(new PingSender());

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
            DynamoServer server = new DynamoServer(args[0], args[1], addr_list);
            server.start();
        }
    }
}
