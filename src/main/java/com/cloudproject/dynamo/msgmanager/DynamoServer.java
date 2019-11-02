package com.cloudproject.dynamo.msgmanager;

import com.cloudproject.dynamo.consistenthash.CityHash;
import com.cloudproject.dynamo.consistenthash.HashFunction;
import com.cloudproject.dynamo.consistenthash.HashingManager;
import com.cloudproject.dynamo.models.ObjectInputModel;
import com.cloudproject.dynamo.models.OutputModel;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import javax.management.Notification;
import javax.management.NotificationListener;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DynamoServer implements NotificationListener {

    private final ArrayList<DynamoNode> nodeList;
    private final ArrayList<DynamoNode> deadList;

    private static DynamoServer selfServer;

    private ExecutorService executorService;
    private DatagramSocket server;
    private DatagramSocket ioServer;
    private String address;
    private DynamoNode node;
    private Random random;
    private int gossipInt;
    private int ttl;
    private HashingManager<DynamoNode> hashingManager;
    private int backups;
    private int vNodeCount;
    private int ackPort;
    private int ioPort;

    private DynamoServer(String name, String address, int gossipInt, int ttl, int vNodeCount,
                         @Nullable ArrayList<String> addr_list, int backups) throws SocketException {

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                if (!DynamoServer.this.server.isClosed()) {
                    DynamoServer.this.server.close();
                }
                if (!DynamoServer.this.ioServer.isClosed()) {
                    DynamoServer.this.ioServer.close();
                }
                System.out.println("Goodbye my friends...");
            }
        }));

        this.address = address;
        this.ackPort = 9720;
        this.ioPort = 9700;
        this.nodeList = new ArrayList<>();
        this.deadList = new ArrayList<>();
        this.executorService = Executors.newCachedThreadPool();
        this.gossipInt = gossipInt;
        this.ttl = ttl;
        if (addr_list != null) {
            for (String addr : addr_list) {
                this.nodeList.add(new DynamoNode(null, addr, this, 0, ttl));
            }
        }

        this.node = new DynamoNode(name, address, this,0, ttl);
        int port = Integer.parseInt(address.split(":")[1]);

        this.backups = (backups > 0) ? backups : 2;
        this.vNodeCount = vNodeCount;

        /* init Random */
        this.random = new Random();
        /* Listen at port number port */
        this.server = new DatagramSocket(port);
        this.ioServer = new DatagramSocket(this.ioPort);
        System.out.println("[Dynamo Server] Listening at port: " + port);
    }

    @Override
    public void handleNotification(Notification notification, Object o) {
        DynamoNode deadNode = (DynamoNode)notification.getUserData();
        System.out.println(">> LEAVE: " + deadNode.name + " has left the network");
        synchronized (DynamoServer.this.nodeList) {
            DynamoServer.this.nodeList.remove(deadNode);
        }

        synchronized (DynamoServer.this.deadList) {
            DynamoServer.this.deadList.add(deadNode);
        }

        if (hashingManager != null) {
            synchronized (hashingManager.getLock()) {
                hashingManager.removeNode(deadNode);
            }
        }

        this.printNodeList();
    }

    private void start() throws InterruptedException {

        for (DynamoNode localNode : this.nodeList) {
            if (localNode != this.node) {
                localNode.startTimer();
            }
        }
        /* read logs, update heartbeat from previous session */
        File file = new File(this.node.name + ".log");
        if (file.exists()) {
            try {
                int log_hb = Integer.parseInt(FileUtils.readFileToString(file, Charset.defaultCharset()));
                this.node.setHeartbeat(log_hb);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //exec.execute(new PingSender());
        this.executorService.execute(new GossipReceiver());
        this.executorService.execute(new Gossiper());
        this.executorService.execute(new ioReceiver());
        this.printNodeList();

//        while (true) {
//            TimeUnit.SECONDS.sleep(2);
//        }
    }

    private void printNodeList() {
        System.out.println("+------------------+");
        System.out.println("| Active node list |");
        System.out.println("+------------------+");
        for (DynamoNode localNode : this.nodeList) {
            System.out.println("|" + localNode.name + " ip: " + localNode.getAddress() + " HeartBeat: " + localNode.getHeartbeat());
        }
        System.out.println("+------------------+");
    }

    private void sendMessage(DynamoNode node, DynamoMessage msg) throws IOException {
        //vclock
//        JVec jv=new JVec(DynamoServer.this.node);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(msg);
        byte[] buf = baos.toByteArray();
//        byte[] res=jv.prepareSend(buf);
        String address = node.getAddress();
        String host = address.split(":")[0];
        int port;
        if (msg.type == MessageTypes.NODE_LIST) {
            port = Integer.parseInt(address.split(":")[1]);
        } else if (msg.type == MessageTypes.ACKNOWLEDGEMENT) {
            port = this.ackPort;
        } else {
            port = this.ioPort;
        }

        InetAddress dest;
        dest = InetAddress.getByName(host);

        System.out.println("[DynamoServer] Sending " + msg.type.name() + " (" + buf.length + ") to " + dest.getHostAddress());
//        System.out.println("[DynamoServer] Sending " + msg.type.name() + " (" + res.length + ") to " + dest.getHostAddress());

        DatagramSocket socket = new DatagramSocket();
        DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, dest, port);
//        DatagramPacket datagramPacket = new DatagramPacket(res, res.length, dest, port);
        socket.send(datagramPacket);
        socket.close();
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
        File file = new File(this.node.name + ".log");
        FileUtils.write(file, Integer.toString(this.node.getHeartbeat()), Charset.defaultCharset(), false);
        //ArrayList<DynamoNode> sendList = cloneArrayList(this.nodeList);
        synchronized (this.nodeList) {
            DynamoNode dstNode = this.getRandomNode();
            if (dstNode != null) {
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
                    /* remove self from remoteNode list */
                    remoteNodesList.remove(DynamoServer.this.node);
                     /* Do the same with rest of the nodes */
                    for (DynamoNode remoteNode : remoteNodesList) {
                        if (DynamoServer.this.nodeList.contains(remoteNode)) {
                                /* Just update the heartbeat to the latest one
                                 * and reset timer */
                                DynamoNode localNode = DynamoServer.this.nodeList.get(DynamoServer.this.nodeList.indexOf((remoteNode)));
                            if (localNode.name == null) localNode.name = remoteNode.name;
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

                                    if (hashingManager != null) {
                                        hashingManager.addNode(newNode, vNodeCount);
                                    }

                                    newNode.startTimer();
                                    System.out.println(">> JOIN: " + newNode.name + " has joined the network");
                                    this.printNodeList();
                                }

                            } else {
                                /* Probably a new member, add it to the list, use remote heartbeat,
                                 * start timer */
                                DynamoNode newNode =
                                        new DynamoNode(remoteNode.name, remoteNode.getAddress(), this, remoteNode.getHeartbeat(), this.ttl);
                                DynamoServer.this.nodeList.add(newNode);

                                if (hashingManager != null) {
                                    hashingManager.addNode(newNode, vNodeCount);
                                }

                                newNode.startTimer();
                                System.out.println(">> JOIN: " + newNode.name + " has joined the network");
                                this.printNodeList();
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

    public static DynamoServer startServer(String... args) throws SocketException, InterruptedException {
        if (selfServer == null) {
            if (args == null) {
                System.out.println("[ERROR] Arguments required");
            } else if (args.length < 5) {
                System.out.println("[ERROR] Expected at least 5 arguments, received " + args.length);
            } else {
                String[] hashParams = args[4].split(":");
                //System.out.println("args[4]: " + args[4]);
                int vNodeCount = Integer.parseInt(hashParams[0]);
                int backups = 0;
                if (hashParams.length == 2) {
                    backups = Integer.parseInt(hashParams[1]);
                }
                if (args.length == 5) {
                    selfServer =
                            new DynamoServer(args[0], args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), vNodeCount, null, backups);
                    selfServer.start();
                } else if (args.length == 6) {
                    ArrayList<String> addr_list =
                            new ArrayList<>(Arrays.asList(args[5].split(",")));
                    selfServer =
                            new DynamoServer(args[0], args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), vNodeCount, addr_list, backups);
                    selfServer.start();
                } else {
                    System.out.println("[ERROR] Expected 5 or 6 arguments, received " + args.length);
                }
            }
        }
        return selfServer;
    }

    /**
     * Method to send a request to all nodes of the system
     *
     * @param messageType The type of message to be sent
     * @param payload     the message payload
     */
    private void sendRequests(MessageTypes messageType, Object payload) {
        this.executorService.execute(new MessageSender(messageType, payload));
    }

    /**
     * Method to send a request to a set of specific nodes
     *
     * @param messageType The type of mesage to be sent
     * @param payload     The message payoad
     * @param nodeList    List of nodes which will receive the message
     */
    private void sendRequests(MessageTypes messageType, Object payload, ArrayList<? extends DynamoNode> nodeList) {
        this.executorService.execute(new MessageSender(messageType, payload, nodeList));
    }

    /**
     * Method to send a request to a specific node
     *
     * @param messageTypes The type of message tp be sent
     * @param payload      the message payload
     * @param dynamoNode   The node to which the message to be sent
     */
    private void sendRequest(MessageTypes messageTypes, Object payload, DynamoNode dynamoNode) {
        ArrayList<DynamoNode> list = new ArrayList<>();
        list.add(dynamoNode);
        sendRequests(messageTypes, payload, list);
    }


    /**
     * Method to create the bucket in the database having the specified name
     *
     * @param name The name of the bucket
     */
    public void createBucket(String name, OutputModel outputModel) {
//        // create folder in current node
//        boolean success = createFolder(name);

        // send a request to each node in the system to create the folder
        sendRequests(MessageTypes.BUCKET_CREATE, name);
        /* Spawn ack thread to collect acks, and write to output model */
        try {
            AckReceiver ackThread = new AckReceiver(outputModel);
            this.executorService.execute(ackThread);
            while (ackThread.isAlive()) {
                TimeUnit.MILLISECONDS.sleep(1);
            }
        } catch (SocketException e) {
            outputModel.setStatus(false);
            outputModel.setResponse(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * Method to create a folder in current node
     *
     * @param name Name of the folder to be created
     * @return true if folder was created successfully, false otherwise
     */
    private boolean createFolder(String name) {
        return new File("/" + name).mkdir();
    }

    /**
     * Method to delete a bucket from the database
     *
     * @param name Name of the folder to be deleted
     */
    public void deleteBucket(String name, OutputModel outputModel) {

        // send a request to each node in the system to delete the folder
        sendRequests(MessageTypes.BUCKET_DELETE, name);

        try {
            AckReceiver ackReceiver = new AckReceiver(outputModel);
            this.executorService.execute(ackReceiver);
            while (ackReceiver.isAlive()) {
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (SocketException e) {
            outputModel.setStatus(false);
            outputModel.setResponse(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * Method to delete a folder in current node
     *
     * @param name Name of the folder to be deleted
     * @return true if the folder was deleted successfully
     */
    private boolean deleteFolder(String name) {
        boolean status = false;
        try {
            FileUtils.deleteDirectory(new File("/" + name));
            status = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return status;
    }

    /**
     * Method to add a record to the database
     *
     * @param bucket     The bucket in which the record is to be added
     * @param inputModel A deserialized object of the record sent by user
     */
    public void addRecord(String bucket, ObjectInputModel inputModel) {
        if (hashingManager == null) {
            initializeHashingManager(vNodeCount, new CityHash(), backups);
        }

        sendRequests(MessageTypes.OBJECT_CREATE,
                new Pair<>(bucket, inputModel),
                hashingManager.routeNodes(inputModel.getKey()));

        // TODO: Send the actual response received through acknowledgement message

    }

    public void readRecord(String bucket, ObjectInputModel inputModel) {
        if (hashingManager == null) {
            initializeHashingManager(vNodeCount, new CityHash(), backups);
        }

        sendRequests(MessageTypes.OBJECT_READ,
                new Pair<>(bucket, inputModel),
                hashingManager.routeNodes(inputModel.getKey()));

        // TODO: Send the actual data to the application

    }

    /**
     * Method to create a file in current node
     *
     * @param folder   The folder in which the file is to be created
     * @param name     the name of the file to be created
     * @param contents the contents to be written to the file
     * @return true if file creation was successful
     */
    private boolean createFile(String folder, String name, String contents) {
        boolean status = false;
        try {
            File file = new File("/" + folder + "/" + name);
            if (!file.exists()) {
                FileUtils.write(file, contents, Charset.defaultCharset(), false);
                status = true;
            } else {
                return status;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return status;
    }

    /**
     * Method to read a file and return its contents
     *
     * @param folder The folder in which the file is present
     * @param name   the name of hte file
     * @return A string representing the contents of the file
     */
    private String readFile(String folder, String name) {
        String contents = null;
        File file = new File("/" + folder + "/" + name);
        if (file.exists()) {
            try {
                contents = FileUtils.readFileToString(file, Charset.defaultCharset());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return contents;
    }

    /**
     * Method to update a file in the current node (only overwrites existing files,
     * reports failure if file doesn't exist)
     *
     * @param folder   The folder in which the file is to be updated
     * @param name     the name of the file to be updated
     * @param contents the contents to be written to the file
     * @return true if the file was updated successfully
     */
    private boolean updateFile(String folder, String name, String contents) {
        boolean status = false;
        File file = new File("/" + folder + "/" + name);
        if (file.exists()) {
            try {
                FileUtils.write(file, contents, Charset.defaultCharset(), false);
                status = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return status;
    }

    /**
     * Method to delete a file in the current node
     *
     * @param folder The folder in which the file is to be deleted
     * @param name   the name of the file to be deleted
     * @return true if the file was deleted successfully
     */
    private boolean deleteFile(String folder, String name) {
        boolean status = false;
        File file = new File("/" + folder + "/" + name);
        if (file.exists()) {
            status = file.delete();
        }
        return status;
    }

    private class GossipReceiver implements Runnable {
        private AtomicBoolean keepRunning;

        public GossipReceiver() {
            keepRunning = new AtomicBoolean(true);
        }

        public void run() {
            while (keepRunning.get()) {
                /* Logic for receiving */
                System.out.println("ghot");
                /* init a buffer where the packet will be placed */
                byte[] buf = new byte[1500];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try {
                    DynamoServer.this.server.receive(p);
                    /* Parse this packet into an object */
                    //vclock
//                    JVec jv=new JVec(DynamoServer.this.node);
//                    byte[] res=jv.unpackReceive(p.getData());
//                     ByteArrayInputStream bais = new ByteArrayInputStream(res);
                    ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Object readObject = ois.readObject();
                    if (readObject instanceof DynamoMessage) {
                        DynamoMessage msg = (DynamoMessage) readObject;
                        boolean status;
                        String bucketName = null;
                        Pair<String, ObjectInputModel> obj = null;
                        switch (msg.type) {
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

    private class MessageSender implements Runnable {
        DynamoMessage sendMsg;
        ArrayList<? extends DynamoNode> nodeList;

        MessageSender(MessageTypes type, Object payload) {
            sendMsg =
                    new DynamoMessage(DynamoServer.this.node, type, payload);
            nodeList = DynamoServer.this.nodeList;
        }

        MessageSender(MessageTypes type, Object payload, ArrayList<? extends DynamoNode> nodeList) {
            this(type, payload);
            this.nodeList = nodeList;
        }

        public void run() {

            while (true) {
                try {
                    TimeUnit.MILLISECONDS.sleep(2000);
                    for (DynamoNode node : nodeList) {
                        try {
                            DynamoServer.this.sendMessage(node, sendMsg);
                        } catch (IOException e) {
                            System.out.println("[WARN] Could not send " + sendMsg.type.name() +
                                    " to " + node.name + " (" + node.getAddress() + ")");
                            e.printStackTrace();
                        }
                    }
                    if (sendMsg.type != MessageTypes.PING) {
                        // this message should be sent only once, no need to loop
                        break;
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

    /**
     * Method to initialize an inctance of {@link HashingManager} for first time use
     *
     * @param vNodeCount   the number of replicates of a node to be maintained in the hash ring
     * @param hashFunction an instance of the hash function to be used
     * @param backups      the number of copies of the objects, including original, to be maintained
     */
    private void initializeHashingManager(int vNodeCount, HashFunction hashFunction, int backups) {
        if (hashingManager == null) {
            // initialize hashingManager only if it is null
            if (backups != 2) {
                hashingManager = new HashingManager<>(nodeList, vNodeCount, hashFunction, backups);
            } else {
                hashingManager = new HashingManager<>(nodeList, vNodeCount, hashFunction);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private class ioReceiver implements Runnable {
        private AtomicBoolean keepRunning;

        public ioReceiver() {
            keepRunning = new AtomicBoolean(true);
        }

        public void run() {
            while (keepRunning.get()) {
                /* Logic for receiving */
                System.out.println("ioGhot");
                /* init a buffer where the packet will be placed */
                byte[] buf = new byte[1500];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try {
                    DynamoServer.this.ioServer.receive(p);
                    /* Parse this packet into an object */
                    ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Object readObject = ois.readObject();
                    if (readObject instanceof DynamoMessage) {
                        DynamoMessage msg = (DynamoMessage) readObject;
                        boolean status;
                        String bucketName = null;
                        Pair<String, ObjectInputModel> obj = null;
                        switch (msg.type) {
                            case PING:
                                System.out.println("[Dynamo Server] PING recieved from " + msg.srcNode.name);
                                break;
                            case BUCKET_CREATE:
                                bucketName = (String) msg.payload;
                                status = createFolder(bucketName);
                                System.out.println("[" + node.name + "] Folder " + bucketName + " created: " + status);
                                /* TODO: Change txnID when implementing parallel IO */
                                sendMessage(msg.srcNode, new DynamoMessage(DynamoServer.this.node,
                                        MessageTypes.ACKNOWLEDGEMENT,
                                        new AckPayload(MessageTypes.BUCKET_CREATE, bucketName, 0, status)));
                                break;
                            case BUCKET_DELETE:
                                bucketName = (String) msg.payload;
                                status = deleteFolder(bucketName);
                                System.out.println("[" + node.name + "] Folder " + bucketName + " deleted: " + status);
                                /* TODO: Change txnID when implementing parallel IO */
                                sendMessage(msg.srcNode, new DynamoMessage(DynamoServer.this.node,
                                        MessageTypes.ACKNOWLEDGEMENT,
                                        new AckPayload(MessageTypes.BUCKET_DELETE, bucketName, 0, status)));
                                break;
                            /* TODO: replace Pair<> with AckPayload */
                            case OBJECT_CREATE:
                                obj = (Pair<String, ObjectInputModel>) msg.payload;
                                status = createFile(obj.getKey(), obj.getValue().getKey(), obj.getValue().getValue());
                                System.out.println("[" + node.name + "] File /" + obj.getKey() + "/"
                                        + obj.getValue().getKey() + " created: " + status);
                                sendMessage(msg.srcNode, new DynamoMessage(DynamoServer.this.node,
                                        MessageTypes.ACKNOWLEDGEMENT, status));
                                break;
                            case OBJECT_READ:
                                obj = (Pair<String, ObjectInputModel>) msg.payload;
                                String contents = readFile(obj.getKey(), obj.getValue().getKey());
                                System.out.println("[" + node.name + "] File /" + obj.getKey() + "/"
                                        + obj.getValue().getKey() + " read: " + contents);
                                sendMessage(msg.srcNode, new DynamoMessage(DynamoServer.this.node,
                                        MessageTypes.ACKNOWLEDGEMENT, contents));
                                break;
                            case OBJECT_UPDATE:
                                obj = (Pair<String, ObjectInputModel>) msg.payload;
                                status = updateFile(obj.getKey(), obj.getValue().getKey(), obj.getValue().getValue());
                                System.out.println("[" + node.name + "] File /" + obj.getKey() + "/"
                                        + obj.getValue().getKey() + " updated: " + status);
                                sendMessage(msg.srcNode, new DynamoMessage(DynamoServer.this.node,
                                        MessageTypes.ACKNOWLEDGEMENT, status));
                                break;
                            case OBJECT_DELETE:
                                obj = (Pair<String, ObjectInputModel>) msg.payload;
                                status = deleteFile(obj.getKey(), obj.getValue().getKey());
                                System.out.println("[" + node.name + "] File /" + obj.getKey() + "/"
                                        + obj.getValue().getKey() + " deleted: " + status);
                                sendMessage(msg.srcNode, new DynamoMessage(DynamoServer.this.node,
                                        MessageTypes.ACKNOWLEDGEMENT, status));
                                break;
                            default:
                                System.out.println("Unrecognized packet type: " + msg.type.name());
                        }
                    } else {
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

    private class AckReceiver extends Thread {
        private AtomicBoolean keepRunning;
        private DatagramSocket ackServer;
        private OutputModel outputModel;

        public AckReceiver(OutputModel outputModel) throws SocketException {
            keepRunning = new AtomicBoolean(true);
            ackServer = new DatagramSocket(DynamoServer.this.ackPort);
            this.outputModel = outputModel;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            // TODO: change quorum to actual quorum-based implementation
            int quorum = nodeList.size();
            int receives = 0;

            while (keepRunning.get()) {
                /* Logic for receiving */
                System.out.println("AckGhot");
                /* init a buffer where the packet will be placed */
                byte[] buf = new byte[1500];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                try {
                    this.ackServer.receive(p);
                    /* Parse this packet into an object */
                    ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Object readObject = ois.readObject();
                    if (readObject instanceof DynamoMessage) {
                        // TODO: Update method to manage successful receives vs. no. of receives
                        receives++;
                        DynamoMessage msg = (DynamoMessage) readObject;
                        AckPayload payload = (AckPayload) msg.payload;
                        if (receives > 0) {
                            outputModel.setStatus(outputModel.isStatus() &
                                    (payload.isStatus()));
                        } else {
                            outputModel.setStatus(payload.isStatus());
                        }
                        /* TODO: track separate receives by txnID */
                        if (receives >= quorum) {
                            switch (payload.getRequestType()) {
                                case BUCKET_CREATE:
                                    outputModel.setResponse("Bucket " + payload.getIdentifier()
                                            + ((outputModel.isStatus()) ? " created successfully" : "creation failed"));
                                    break;
                                case BUCKET_DELETE:
                                    outputModel.setResponse("Bucket " + payload.getIdentifier()
                                            + ((outputModel.isStatus()) ? " deleted successfully" : "deletion failed"));
                                    break;
                                default:
                                    System.out.println("Unrecognized packet type!");
                            }
                            keepRunning.set(false);
                        }
                    } else {
                        System.out.println("Malformed packet!");
                    }
                } catch (IOException e) {
                    outputModel.setResponse(e.getMessage());
                    outputModel.setStatus(false);
                    //e.printStackTrace();
                    keepRunning.set(false);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if (!ackServer.isClosed()) {
                ackServer.close();
            }
        }
    }

    public void shutdownDynamoServer(OutputModel outputModel) {
        System.out.println("Forcing shutdown...");
        System.out.println("Goodbye my friends...");
        this.executorService.shutdownNow();
        if (!DynamoServer.this.server.isClosed()) {
            DynamoServer.this.server.close();
        }
        if (!DynamoServer.this.ioServer.isClosed()) {
            DynamoServer.this.ioServer.close();
        }
        outputModel.setResponse("Server successfully shutdown");
        outputModel.setStatus(true);
    }

}
