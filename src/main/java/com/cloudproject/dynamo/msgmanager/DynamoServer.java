package com.cloudproject.dynamo.msgmanager;

import com.cloudproject.dynamo.config.AppConfig;
import com.cloudproject.dynamo.consistenthash.CityHash;
import com.cloudproject.dynamo.consistenthash.HashFunction;
import com.cloudproject.dynamo.consistenthash.HashingManager;
import com.cloudproject.dynamo.models.*;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DynamoServer implements NotificationListener {

    private final ArrayList<DynamoNode> nodeList;
    private final ArrayList<DynamoNode> deadList;

    private static DynamoServer selfServer;

    private final ExecutorService executorService;
    private final DatagramSocket server;
    private final DatagramSocket ioServer;
    private final DynamoNode node;
    private final Random random;
    private int gossipInt;
    private int ttl;
    private HashingManager<DynamoNode> hashingManager;
    private int ackPort;
    private int ioPort;

    private DynamoServer(String name, String address, int gossipInt, int ttl,
                         @Nullable ArrayList<String> addr_list, boolean apiNode) throws SocketException {

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

        this.ackPort = 9720;
        this.ioPort = 9700;
        this.nodeList = new ArrayList<>();
        this.deadList = new ArrayList<>();
        this.executorService = Executors.newCachedThreadPool();
        this.gossipInt = gossipInt;
        this.ttl = ttl;
        if (addr_list != null) {
            for (String addr : addr_list) {
                this.nodeList.add(new DynamoNode(null, addr, this, 0, ttl, false));
            }
        }

        this.node = new DynamoNode(name, address, this, 0, ttl, apiNode);
        int port = Integer.parseInt(address.split(":")[1]);

        /* init Random */
        this.random = new Random();
        /* Listen at port number port */
        this.server = new DatagramSocket(port);
        this.ioServer = new DatagramSocket(this.ioPort);
        System.out.println("[Dynamo Server] Listening at port: " + port);
        initializeHashingManager(new CityHash());
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

    private void start() {

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

    public void sendMessage(DynamoNode node, DynamoMessage msg) throws IOException {
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
        } else if (msg.type == MessageTypes.ACKNOWLEDGEMENT
                | msg.type == MessageTypes.FORWARD_ACK
                | msg.type == MessageTypes.FORWARD_ACK_READ) {
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

    /**
     * Method to get a random node except API gateway
     *
     * @return instance of a random node from nodeList
     */
    private DynamoNode getRandomNode(boolean excludeAPIgateway) {

        ArrayList<DynamoNode> randList = new ArrayList<>(nodeList);

        if (excludeAPIgateway) {
            for (DynamoNode node : randList) {
                if (node.isApiNode()) {
                    randList.remove(node);
                    break;
                }
            }
        }

        DynamoNode node = null;

        if (randList.size() > 0) {
            int rand = random.nextInt(randList.size());
            node = randList.get(rand);
        }

        return node;
    }

    private void sendMembershipList() throws IOException {
        this.node.setHeartbeat(this.node.getHeartbeat() + 1);
        File file = new File(this.node.name + ".log");
        FileUtils.write(file, Integer.toString(this.node.getHeartbeat()), Charset.defaultCharset(), false);
        //ArrayList<DynamoNode> sendList = cloneArrayList(this.nodeList);
        synchronized (this.nodeList) {
            DynamoNode dstNode = this.getRandomNode(false);
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
//                    for (int i = 0; i < remoteNodesList.size(); i++) {
//                        if(remoteNodesList.get(i).equals(DynamoServer.this.node)) {
//                            remoteNodesList.remove(i);
//                            i--;
//                        }
//                    }

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
                                DynamoNode localDeadNode =
                                        DynamoServer.this.deadList.get(DynamoServer.this.deadList.indexOf(remoteNode));
                                if (remoteNode.getHeartbeat() > localDeadNode.getHeartbeat()) {
                                    DynamoServer.this.deadList.remove(localDeadNode);
                                    DynamoNode newNode =
                                            new DynamoNode(remoteNode.name, remoteNode.getAddress(),
                                                    this, remoteNode.getHeartbeat(),
                                                    this.ttl, remoteNode.isApiNode());
                                    DynamoServer.this.nodeList.add(newNode);

                                    if (hashingManager != null && !newNode.isApiNode()) {
                                        hashingManager.addNode(newNode);
                                    }

                                    newNode.startTimer();
                                    System.out.println(">> JOIN: " + newNode.name + " has joined the network");
                                    this.printNodeList();
                                }

                            } else {
                                /* Probably a new member, add it to the list, use remote heartbeat,
                                 * start timer */
                                DynamoNode newNode =
                                        new DynamoNode(remoteNode.name, remoteNode.getAddress(),
                                                this, remoteNode.getHeartbeat(), this.ttl, remoteNode.isApiNode());
                                DynamoServer.this.nodeList.add(newNode);

                                if (hashingManager != null && !newNode.isApiNode()) {
                                    hashingManager.addNode(newNode);
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

    /**
     * Method to start the dynamo server
     *
     * @param args command line arguments related to the server
     * @return instance of DynamoServer
     * @throws SocketException
     */
    public static DynamoServer startServer(String... args) throws SocketException {
        if (selfServer == null) {
            if (args == null) {
                System.out.println("[ERROR] Arguments required");
            } else if (args.length < 5) {
                System.out.println("[ERROR] Expected at least 5 arguments, received " + args.length);
            } else {
                if (args.length == 5) {
                    selfServer =
                            new DynamoServer(args[0], args[1], Integer.parseInt(args[2]),
                                    Integer.parseInt(args[3]), null,
                                    Boolean.parseBoolean(args[4]));
                    selfServer.start();
                } else if (args.length == 6) {
                    ArrayList<String> addr_list =
                            new ArrayList<>(Arrays.asList(args[5].split(",")));
                    selfServer =
                            new DynamoServer(args[0], args[1], Integer.parseInt(args[2]),
                                    Integer.parseInt(args[3]), addr_list,
                                    Boolean.parseBoolean(args[4]));
                    selfServer.start();
                } else {
                    System.out.println("[ERROR] Expected 5 or 6 arguments, received " + args.length);
                }
            }
        }
        return selfServer;
    }

    /**
     * Method to shut down the server
     *
     * @param outputModel POJO which contains the response message
     */
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
        selfServer = null;
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
     * @param messageType The type of message to be sent
     * @param payload     The message payload
     * @param sendList    List of nodes which will receive the message
     */
    private void sendRequests(MessageTypes messageType, Object payload, ArrayList<DynamoNode> sendList) {
        this.executorService.execute(new MessageSender(messageType, payload, sendList));
    }

    /**
     * Method to send a request to a specific node
     *
     * @param payload the message payload
     */
    private void sendRequestToRandNode(Object payload)
            throws IOException {
//        ArrayList<DynamoNode> list = new ArrayList<>();
//        list.add(dynamoNode);
//        sendRequests(MessageTypes.FORWARD, payload, list);
        this.sendMessage(getRandomNode(true),
                new DynamoMessage(this.node, MessageTypes.FORWARD, payload));
    }

    /**
     * Method to forward Create/Delete operations of a bucket of the database
     *
     * @param messageType the type of operation to be performed
     * @param bucketName the name of the bucket
     * @param outputModel POJO which will return the response
     */
    public void forwardToRandNode(MessageTypes messageType, String bucketName, OutputModel outputModel) {
        try {
            Future future = this.executorService.submit(new ReceiveFromRandNode(outputModel));
            sendRequestToRandNode(new ForwardPayload(messageType, bucketName, null, 0));
            future.get(20, TimeUnit.SECONDS);

            // outputModel contains status, read status and set message
            switch (messageType) {
                case BUCKET_CREATE:
                    outputModel.setResponse("Bucket " + bucketName +
                            (outputModel.isStatus() ? " created successfully" : " creation failed"));
                    break;
                case BUCKET_DELETE:
                    outputModel.setResponse("Bucket " + bucketName +
                            (outputModel.isStatus() ? " deleted successfully" : " deletion failed"));
                    break;
            }

        } catch (ExecutionException | InterruptedException | IOException e) {
            outputModel.setStatus(false);
            outputModel.setResponse(e.getMessage());
        } catch (TimeoutException e) {
            System.out.println(">> Response timeout! Use sloppy quorum!");
            e.printStackTrace();
        }
    }

    /**
     * Method to perform CRUD operations on objects of the database
     *
     * @param messageType THe type of operation to be performed
     * @param bucketName  the name of the bucket in which the object resides
     * @param inputObject POJO containing the key and value of the object
     * @param outputModel POJO which will return the response
     */
    public void forwardToRandNode(MessageTypes messageType, String bucketName,
                                  Object inputObject, OutputModel outputModel) {
        try {
            Future future = this.executorService.submit(new ReceiveFromRandNode(outputModel));
            sendRequestToRandNode(new ForwardPayload(messageType, bucketName, inputObject, 2));
            future.get(10, TimeUnit.SECONDS);
            /* TODO: [TEMP DONE] Since we will not be using the Receiver for randNode, kill the thread,
             *  it is occupying a port and CPU time without any reason.
             */
            if (!future.isDone()) {
                future.cancel(true);
                outputModel.setStatus(false);
            }
            // outputModel contains status, read status and set message
            switch (messageType) {
                case OBJECT_CREATE:
                    outputModel.setResponse("Record " +
                            ((ObjectInputModel) inputObject).getKey() + " : " + ((ObjectInputModel) inputObject).getValue() +
                            (outputModel.isStatus() ? " creation successfully" : " creation failed"));
                    break;
                case OBJECT_READ:
                    // value should already be written
                    break;
                case OBJECT_UPDATE:
                    outputModel.setResponse("Record " +
                            ((ObjectInputModel) inputObject).getKey() + " : " + ((ObjectInputModel) inputObject).getValue() +
                            (outputModel.isStatus() ? " updated successfully" : " update failed"));
                    break;
                case OBJECT_DELETE:
                    outputModel.setResponse("Record " + inputObject +
                            (outputModel.isStatus() ? " removed successfully" : " removal failed"));
                    break;
            }
        } catch (ExecutionException | InterruptedException | IOException e) {
            outputModel.setStatus(false);
            outputModel.setResponse(e.getMessage());
            e.printStackTrace();
        } catch (TimeoutException e) {
            System.out.println(">> Response timeout! Use sloppy quorum!");
            e.printStackTrace();
        }
    }

    /**
     * Re-forwards a request from API gateway to a valid co-ordinator in the hash nodes.
     *
     * @param payload    Payload to forward
     * @param hashNodes  list of hashNodes
     * @param apiGateway The DynamoNode instance representing API Gateway
     */
    private void forwardToRandomNode(ForwardPayload payload, ArrayList<DynamoNode> hashNodes, DynamoNode apiGateway) {
        /* This node is not a part of the hashnodes, forward request to one of the hashNodes */
        if (hashNodes.size() > 0) {
            DynamoNode newCoord = hashNodes.get(random.nextInt(hashNodes.size()));

            /* Send forward to this node, src being the API gateway */
            DynamoMessage msg = new DynamoMessage(apiGateway, MessageTypes.FORWARD, payload);
            try {
                this.sendMessage(newCoord, msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method to create the bucket in the database having the specified name
     *
     * @param name The name of the bucket
     * @return true if the bucket was created successfully, false otherwise
     */
    private boolean createBucket(String name) {
//        // create folder in current node
        AtomicBoolean success = new AtomicBoolean(false);
        success.set(createFolder(name));

        /* Spawn ack thread to collect acks, and write to output model */
        try {
            AckReceiver ackThread = new AckReceiver(success);
            Future future = this.executorService.submit(ackThread);

            // send a request to each node in the system to create the folder
            sendRequests(MessageTypes.BUCKET_CREATE, name);

            // wait for thread termination
            future.get(10, TimeUnit.SECONDS);
            /* TODO: [TEMP DONE] Since we will not be using the Receiver for randNode, kill the thread,
             *  it is occupying a port and CPU time without any reason.
             */
            if (!future.isDone()) {
                future.cancel(true);
                success.set(false);
            }

        } catch (SocketException | InterruptedException | ExecutionException | TimeoutException e) {
            success.set(false);
            e.printStackTrace();
        }

        return success.get();

    }

    /**
     * Method to create a folder in current node
     *
     * @param name Name of the folder to be created
     * @return true if folder was created successfully, false otherwise
     */
    private boolean createFolder(String name) {
        return new File("/buckets/" + name).mkdir();
    }

    /**
     * Method to delete a bucket from the database
     *
     * @param name Name of the folder to be deleted
     * @return true if folder was deleted successfully, false otherwise
     */
    private boolean deleteBucket(String name) {

        AtomicBoolean success = new AtomicBoolean(false);
        success.set(deleteFolder(name));

        try {
            AckReceiver ackThread = new AckReceiver(success);
            Future future = this.executorService.submit(ackThread);

            // send a request to each node in the system to delete the folder
            sendRequests(MessageTypes.BUCKET_DELETE, name);

            // wait for thread termination
            future.get(10, TimeUnit.SECONDS);
            /* TODO: [TEMP DONE] Since we will not be using the Receiver for randNode, kill the thread,
             *  it is occupying a port and CPU time without any reason.
             */
            if (!future.isDone()) {
                future.cancel(true);
                success.set(false);
            }
        } catch (SocketException | InterruptedException | ExecutionException | TimeoutException e) {
            success.set(false);
            e.printStackTrace();
        }
        return success.get();

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
            File file = new File("/buckets/" + name);
            if (file.exists()) {
                FileUtils.deleteDirectory(file);
                status = true;
            }
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
     * @param hashNodes  list of hash nodes
     * @return true if object was created successfully, false otherwise
     */
    private boolean addRecord(String bucket, ObjectInputModel inputModel, ArrayList<DynamoNode> hashNodes) {
        AtomicBoolean success = new AtomicBoolean(true);
        success.set(createFile(bucket, inputModel.getKey(), inputModel.getValue(), true));

        // send requests to all appropriate nodes and await response
        if (hashNodes != null && hashNodes.size() > 0) {
            hashNodes.remove(this.node);
            try {
                System.out.println("Sending CREATE request to " + hashNodes.size() + " other nodes");
                for (DynamoNode node : hashNodes) {
                    System.out.println(node.name + " " + node.getAddress());
                }
                AckReceiver ackThread = new AckReceiver(success, hashNodes.size(),
                        (success.get() ? Quorum.getWriteQuorum() - 1 : Quorum.getWriteQuorum()));
                Future future = this.executorService.submit(ackThread);

                // send a request to each relevant hash-node to create the object
                sendRequests(MessageTypes.OBJECT_CREATE, new Pair<>(bucket, inputModel), hashNodes);

                // wait for thread termination
                future.get(10, TimeUnit.SECONDS);
                /* TODO: [TEMP DONE] Since we will not be using the Receiver for randNode, kill the thread,
                 *  it is occupying a port and CPU time without any reason.
                 */
                if (!future.isDone()) {
                    future.cancel(true);
                    success.set(false);
                }
            } catch (SocketException | InterruptedException | ExecutionException | TimeoutException e) {
                success.set(false);
                e.printStackTrace();
            }
        }

        return success.get();
    }

    /**
     * Method to delete a record from the database
     *
     * @param bucketName The name of the bucket which contains the record
     * @param key        the key of the object ot be deleted
     * @return true if deletion was successful, false otherwise
     */
    private boolean deleteRecord(String bucketName, String key, ArrayList<DynamoNode> hashNodes) {
        // track success of operation
        AtomicBoolean success = new AtomicBoolean(true);

        // key present in coordinator
        if (hashNodes.contains(this.node)) {
            System.out.println("Key to be deleted present in coordinator " + this.node.name);
            success.set(deleteFile(bucketName, key));
            hashNodes.remove(this.node);
        }

        // key present in other nodes
        if (hashNodes.size() > 0) {
            try {
                System.out.println("Sending DELETE request to " + hashNodes.size() + " other nodes");
                AckReceiver ackReceiver = new AckReceiver(success, hashNodes.size(), Quorum.getWriteQuorum());

                // initialize Acknowledgement Receiver thread to listen for acknowledgements
                Future future = this.executorService.submit(ackReceiver);

                // send a request to each relevant hash-node to create the object
                sendRequests(MessageTypes.OBJECT_DELETE, new Pair<>(bucketName, key), hashNodes);

                // wait for acknowledgement thread termination
                future.get(20, TimeUnit.SECONDS);
            } catch (SocketException | InterruptedException | ExecutionException | TimeoutException e) {
                success.set(false);
                e.printStackTrace();
            }
        }

        return success.get();
    }

    private boolean updateRecord(String bucket, ObjectInputModel inputModel, ArrayList<DynamoNode> hashNodes) {
        AtomicBoolean success = new AtomicBoolean(true);

        // this node is one of the hash replicas, create object here
        success.set(updateFile(bucket, inputModel.getKey(), inputModel.getValue(), true));

        // send requests to all appropriate nodes and await response
        if (hashNodes != null && hashNodes.size() > 0) {
            hashNodes.remove(this.node);
            try {
                System.out.println("Sending UPDATE request to " + hashNodes.size() + " other nodes");
                for (DynamoNode node : hashNodes) {
                    System.out.println(node.name + " " + node.getAddress());
                }
                AckReceiver ackThread = new AckReceiver(success, hashNodes.size(),
                        (success.get() ? Quorum.getWriteQuorum() - 1 : Quorum.getWriteQuorum()));
                Future future = this.executorService.submit(ackThread);

                // send a request to each relevant hash-node to create the object
                sendRequests(MessageTypes.OBJECT_UPDATE, new Pair<>(bucket, inputModel), hashNodes);

                // wait for thread termination
                future.get(10, TimeUnit.SECONDS);
                /* TODO: [TEMP DONE] Since we will not be using the Receiver for randNode, kill the thread,
                 *  it is occupying a port and CPU time without any reason.
                 */
                if (!future.isDone()) {
                    future.cancel(true);
                    success.set(false);
                }
            } catch (SocketException | InterruptedException | ExecutionException | TimeoutException e) {
                success.set(false);
                e.printStackTrace();
            }
        }

        return success.get();
    }

    /**
     * Method to read the contents of a record
     *
     * @param bucket the name of the bucket which contains the record
     * @param key    the key whose value is to be read
     * @param hashNodes the list of hash nodes
     */
    private ArrayList<ObjectIOModel> readRecord(String bucket,
                                                String key,
                                                ArrayList<DynamoNode> hashNodes) {

        /* if is a coord then read from this node and decrement read quorum if applicable */
        int readQuorum = Quorum.getReadQuorum();
        AtomicBoolean success = new AtomicBoolean(false);

        ArrayList<ObjectIOModel> out = new ArrayList<>();

        if (isCoordinator(hashNodes)) {
            ObjectIOModel ioModel = readFile(bucket, key);
            if (ioModel != null && !ioModel.getValue().isEmpty()) {
                hashNodes.remove(this.node);
                readQuorum--;
            }
        }

        try {
            ReadReceiver readThread = new ReadReceiver(hashNodes.size(), readQuorum, out, success);
            // send a request to each relevant hash-node to create the object
            Future future = this.executorService.submit(readThread);
            sendRequests(MessageTypes.OBJECT_READ, new Pair<>(bucket, key), hashNodes);

            // wait for thread termination
            future.get(10, TimeUnit.SECONDS);
            /* TODO: [TEMP DONE] Since we will not be using the Receiver for randNode, kill the thread,
             *  it is occupying a port and CPU time without any reason.
             */
            if (!future.isDone()) {
                future.cancel(true);
                success.set(false);
            }


        } catch (SocketException | InterruptedException | TimeoutException | ExecutionException e) {
            e.printStackTrace();
        }
        return out;
    }

    /**
     * Method to create a file in current node
     *
     * @param folder   The folder in which the file is to be created
     * @param name     the name of the file to be created
     * @param contents the contents to be written to the file
     * @param isCoord  true if the caller node is the coordinator, false otherwise
     * @return true if file creation was successful
     */
    private boolean createFile(String folder, String name, String contents, boolean isCoord) {
        ObjectIOModel ioModel = new ObjectIOModel((isCoord) ? 1 : 0, contents);
        String json = AppConfig.getParser().serialize(ioModel);
        boolean status = false;
        try {
            File parent = new File("/buckets/" + folder);
            if (parent.exists()) {
                File file = new File(parent, name);
                if (!file.exists()) {
                    FileUtils.write(file, json, Charset.defaultCharset(), false);
                    status = true;
                }
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
    private ObjectIOModel readFile(String folder, String name) {
        ObjectIOModel contents = null;
        File file = new File("/buckets/" + folder + "/" + name);
        if (file.exists()) {
            try {
                contents = AppConfig.getParser().deserialize(
                        FileUtils.readFileToString(file, Charset.defaultCharset()), ObjectIOModel.class);
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
     * @param isCoord  true if the caller node is the coordinator, false otherwise
     * @return true if the file was updated successfully
     */
    private boolean updateFile(String folder, String name, String contents, boolean isCoord) {
        boolean status = false;
        File file = new File("/buckets/" + folder + "/" + name);
        if (file.exists()) {
            try {
                // read file contents into ObjectIOModel
                ObjectIOModel ioModel = readFile(folder, name);
                if (isCoord) {
                    ioModel.setVersion(ioModel.getVersion() + 1);
                }
                ioModel.setValue(contents);
                FileUtils.write(file, AppConfig.getParser().serialize(ioModel),
                        Charset.defaultCharset(), false);
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
        File file = new File("/buckets/" + folder + "/" + name);
        if (file.exists()) {
            status = file.delete();
        }
        return status;
    }

    private class GossipReceiver implements Runnable {
        private AtomicBoolean keepRunning;

        GossipReceiver() {
            keepRunning = new AtomicBoolean(true);
        }

        public void run() {

            System.out.println("[Dynamo Server] Gossip Receiver started");

            while (keepRunning.get()) {
                /* init a buffer where the packet will be placed */
                byte[] buf = new byte[1500];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                System.out.print("[Dynamo Server] GOSSIP received");
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
                        switch (msg.type) {
                            case PING:
                                System.out.print("[Dynamo Server] PING received from " + msg.srcNode.name);
                                break;
                            case NODE_LIST:
                                System.out.println(" from " + msg.srcNode.name);
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
        private DynamoMessage sendMsg;
        private ArrayList<DynamoNode> sendList;

        MessageSender(MessageTypes type, Object payload) {
            this.sendMsg = new DynamoMessage(DynamoServer.this.node, type, payload);
            this.sendList = new ArrayList<>(DynamoServer.this.nodeList);
        }

        MessageSender(MessageTypes type, Object payload, ArrayList<DynamoNode> sendList) {
            this(type, payload);
            this.sendList = sendList;
        }

        public void run() {
            do {
                if (this.sendMsg.type == MessageTypes.PING) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                for (DynamoNode node : this.sendList) {
                    try {
                        if (!node.isApiNode()) {
                            DynamoServer.this.sendMessage(node, this.sendMsg);
                        }
                    } catch (IOException e) {
                        System.out.println("[WARN] Could not send " + this.sendMsg.type.name() +
                                " to " + node.name + " (" + node.getAddress() + ")");
                        e.printStackTrace();
                    }
                }
            } while (this.sendMsg.type == MessageTypes.PING);
        }
    }

    private class Gossiper implements Runnable {
        private AtomicBoolean keepRunning;

        Gossiper() {
            this.keepRunning = new AtomicBoolean(true);
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
     * Method to initialize an instance of {@link HashingManager} for first time use
     *
     * @param hashFunction an instance of the hash function to be used
     */
    private void initializeHashingManager(HashFunction hashFunction) {
        if (hashingManager == null) {
            // initialize hashingManager only if it is null
            ArrayList<DynamoNode> hashNodes = new ArrayList<>();
            for (DynamoNode node : nodeList) {
                if (!node.isApiNode()) {
                    hashNodes.add(node);
                }
            }

            hashNodes.add(this.node);

            hashingManager = new HashingManager<>(hashNodes, hashFunction);
        }
    }

    /**
     * Method to return list of hash nodes
     *
     * @param key key of object
     * @return list of hash nodes
     */
    private ArrayList<DynamoNode> getHashNodes(String key) {
        if (hashingManager == null) {
            initializeHashingManager(new CityHash());
        }

        // get all the nodes to which this record should be written
        return hashingManager.routeNodes(key);
    }

    /**
     * Method to check if current node is a coordinator or not
     *
     * @param hashNodes List of hash nodes
     * @return true if this node is a coordinator, false otherwise
     */
    private boolean isCoordinator(@Nullable ArrayList<DynamoNode> hashNodes) {
        if (hashNodes == null) {
            return true;
        }
        return hashNodes.contains(this.node);
    }

    /**
     * Thread to receive IO related messages and take action according to the type of message
     */
    @SuppressWarnings("unchecked")
    private class ioReceiver implements Runnable {
        private AtomicBoolean keepRunning;

        ioReceiver() {
            keepRunning = new AtomicBoolean(true);
        }

        public void run() {

            System.out.println("[Dynamo Server] IO receiver started");

            while (keepRunning.get()) {
                /* init a buffer where the packet will be placed */
                byte[] buf = new byte[1500];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                System.out.print("[Dynamo Server] IO Request received");
                try {
                    DynamoServer.this.ioServer.receive(p);
                    /* Parse this packet into an object */
                    ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Object readObject = ois.readObject();
                    if (readObject instanceof DynamoMessage) {
                        DynamoMessage msg = (DynamoMessage) readObject;
                        System.out.println(" from " + msg.srcNode.name);
                        System.out.println(msg.srcNode.name);
                        boolean status;
                        ArrayList<ObjectIOModel> list = null;
                        String bucketName = null;
                        Pair<String, ?> obj = null;
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
                            case OBJECT_CREATE:
                                obj = (Pair<String, ObjectInputModel>) msg.payload;
                                status = createFile(obj.getKey(), ((ObjectInputModel) obj.getValue()).getKey(),
                                        ((ObjectInputModel) obj.getValue()).getValue(), false);
                                System.out.println("[" + node.name + "] File /" + obj.getKey() + "/"
                                        + ((ObjectInputModel) obj.getValue()).getKey() + " created: " + status);
                                sendMessage(msg.srcNode, new DynamoMessage(DynamoServer.this.node,
                                        MessageTypes.ACKNOWLEDGEMENT,
                                        new AckPayload(MessageTypes.OBJECT_CREATE,
                                                ((ObjectInputModel) obj.getValue()).getKey(),
                                                2, status)));
                                break;
                            case OBJECT_READ:
                                /* TODO: Read using ObjectIOModel and get content and version both
                                 * and serialize payload in form Pair<String, Long> */
                                obj = (Pair<String, String>) msg.payload;
                                ObjectIOModel contents = readFile(obj.getKey(), String.valueOf(obj.getValue()));
                                System.out.println("[" + node.name + "] File /" + obj.getKey() + "/"
                                        + obj.getValue() + " read: " + contents);
                                sendMessage(msg.srcNode, new DynamoMessage(DynamoServer.this.node,
                                        MessageTypes.ACKNOWLEDGEMENT, contents));
                                break;
                            case OBJECT_UPDATE:
                                obj = (Pair<String, ObjectInputModel>) msg.payload;
                                status = updateFile(obj.getKey(),
                                        ((ObjectInputModel) obj.getValue()).getKey(),
                                        ((ObjectInputModel) obj.getValue()).getValue(), false);
                                System.out.println("[" + node.name + "] File /" + obj.getKey() + "/"
                                        + ((ObjectInputModel) obj.getValue()).getKey() + " updated: " + status);
                                sendMessage(msg.srcNode, new DynamoMessage(DynamoServer.this.node,
                                        MessageTypes.ACKNOWLEDGEMENT,
                                        new AckPayload(MessageTypes.OBJECT_UPDATE,
                                                obj.getKey() + "/" + ((ObjectInputModel) obj.getValue()).getKey(),
                                                2, status)));
                                break;
                            case OBJECT_DELETE:
                                obj = (Pair<String, String>) msg.payload;
                                status = deleteFile(obj.getKey(), String.valueOf(obj.getValue()));
                                System.out.println("[" + node.name + "] File /" + obj.getKey() + "/"
                                        + obj.getValue() + " deleted: " + status);
                                sendMessage(msg.srcNode, new DynamoMessage(DynamoServer.this.node,
                                        MessageTypes.ACKNOWLEDGEMENT,
                                        new AckPayload(MessageTypes.OBJECT_DELETE,
                                                obj.getKey() + "/" + obj.getValue(),
                                                2, status)));
                                break;

                            case REHASH:
                                executorService.execute(new Rehash((DynamoNode) msg.payload));
                                break;
                            case FORWARD:
                                ForwardPayload payload = (ForwardPayload) msg.payload;

                                // get the list of hash nodes if applicable
                                ArrayList<DynamoNode> hashNodes = null;
                                if (payload.getRequestType() == MessageTypes.OBJECT_DELETE ||
                                        payload.getRequestType() == MessageTypes.OBJECT_READ) {
                                    hashNodes = getHashNodes(String.valueOf(payload.getInputModel()));
                                } else if (payload.getRequestType() == MessageTypes.OBJECT_CREATE ||
                                        payload.getRequestType() == MessageTypes.OBJECT_UPDATE) {
                                    hashNodes = getHashNodes(((ObjectInputModel) payload.getInputModel()).getKey());
                                }

                                boolean isCoord = isCoordinator(hashNodes);

                                switch (payload.getRequestType()) {
                                    case BUCKET_CREATE:
                                        status = createBucket(payload.getBucketName());
                                        break;
                                    case BUCKET_DELETE:
                                        status = deleteBucket(payload.getBucketName());
                                        break;
                                    case OBJECT_CREATE:
                                        if (isCoord) {
                                            System.out.println("~DEBUG~ addRecord() being called");
                                            assert hashNodes != null;
                                            status = addRecord(payload.getBucketName(),
                                                    (ObjectInputModel) payload.getInputModel(),
                                                    hashNodes);
                                        } else {
                                            // forward to random node from hashNodes
                                            forwardToRandomNode(payload, hashNodes, msg.srcNode);
                                            status = true; /* TODO: temp */
                                        }
                                        break;
                                    case OBJECT_UPDATE:
                                        if (isCoord) {
                                            System.out.println("~DEBUG~ updateRecord() being called");
                                            assert hashNodes != null;
                                            status = updateRecord(payload.getBucketName(),
                                                    (ObjectInputModel) payload.getInputModel(),
                                                    hashNodes);
                                        } else {
                                            // forward to random node from hashNodes
                                            forwardToRandomNode(payload, hashNodes, msg.srcNode);
                                            status = true; /* TODO: temp */
                                        }
                                        break;
                                    case OBJECT_DELETE:
                                        System.out.println("~DEBUG~ deleteRecord() being called");
                                        assert hashNodes != null;
                                        status = deleteRecord(payload.getBucketName(),
                                                String.valueOf(payload.getInputModel()), hashNodes);
                                        break;
                                    case OBJECT_READ:
                                        System.out.println("~DEBUG~ readRecord() being called");
                                        /* Make it return a list, and pass the list to
                                            FORWARD_ACK_READ
                                         */
                                        assert hashNodes != null;
                                        list = readRecord(payload.getBucketName(),
                                                String.valueOf(payload.getInputModel()), hashNodes);
//                                        if (list.size() < Quorum.getReadQuorum()) {
//                                            status = false;
//                                        }
                                        status = true;  /* TODO: [TEMP DONE]  temp */
                                        break;
                                    default:
                                        System.out.println(">> Unknown request forwarded!");
                                        status = false;
                                }

                                // return to ForwardReceiver
                                    if (payload.getRequestType() == MessageTypes.OBJECT_READ) {
                                        /* something more needs to be sent back in case of READ */
                                        sendMessage(msg.srcNode, new DynamoMessage(DynamoServer.this.node,
                                                MessageTypes.FORWARD_ACK_READ, list));
                                    } else if ((payload.getRequestType() == MessageTypes.OBJECT_CREATE && isCoord) ||
                                            (payload.getRequestType() == MessageTypes.OBJECT_UPDATE && isCoord) ||
                                            (payload.getRequestType() == MessageTypes.BUCKET_CREATE) ||
                                            (payload.getRequestType() == MessageTypes.BUCKET_DELETE) ||
                                            (payload.getRequestType() == MessageTypes.OBJECT_DELETE)) {
                                        sendMessage(msg.srcNode, new DynamoMessage(DynamoServer.this.node,
                                                MessageTypes.FORWARD_ACK, status));
                                    }
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

    private class Rehash extends Thread {

        private final DynamoNode destNode;

        Rehash(DynamoNode destNode) {
            this.destNode = destNode;
        }

        @Override
        public void run() {
            if (hashingManager == null) {
                initializeHashingManager(new CityHash());
            }

            try {
                hashingManager.rehash("/buckets", DynamoServer.this, this.destNode);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
        }
    }

    private class AckReceiver extends Thread {
        private AtomicBoolean keepRunning;
        private DatagramSocket ackServer;
        private AtomicBoolean status;
        private int numReplicas;
        private int quorum;

        AckReceiver(AtomicBoolean status) throws SocketException {
            this.keepRunning = new AtomicBoolean(true);
            this.ackServer = new DatagramSocket(DynamoServer.this.ackPort);
            this.status = status;
            this.numReplicas = DynamoServer.this.nodeList.size() - 1;
            this.quorum = numReplicas;
        }

        AckReceiver(AtomicBoolean status, int size, int quorum) throws SocketException {
            this(status);
            this.numReplicas = size;
            this.quorum = quorum;
        }

        @Override
        public void run() {
            int receives = 0;
            int success = 0;
            System.out.println(">> ACK: quorum init: " + numReplicas + " receives init : " + receives);
            while (keepRunning.get()) {
                /* init a buffer where the packet will be placed */
                byte[] buf = new byte[1500];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                System.out.print("[Dynamo Server] Acknowledgement received ");
                try {
                    this.ackServer.receive(p);
                    /* Parse this packet into an object */
                    ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Object readObject = ois.readObject();
                    if (readObject instanceof DynamoMessage) {
                        // TODO: Update method to manage successful receives vs. no. of receives
                        receives++;
                        System.out.println(">> ACK: quorum: " + numReplicas + " receives: " + receives);
                        DynamoMessage msg = (DynamoMessage) readObject;
                        System.out.println(" from " + msg.srcNode.name);
                        AckPayload payload = (AckPayload) msg.payload;
//                        this.status.set(this.status.get() & (payload.isStatus()));
                        if (payload.isStatus()) {
                            success++;
                        }
                        /* TODO: track separate receives by txnID */
                        if (receives >= numReplicas || success >= quorum) {
                            if (success >= quorum) {
                                status.set(true);
                            } else {
                                status.set(false);
                            }
                            System.out.println(">> ACK: Quorum achieved! Success!");
                            switch (payload.getRequestType()) {
                                case BUCKET_CREATE:
                                    System.out.println(">> ACK: Quorum achieved for " + payload.getIdentifier()
                                            + ": Setting BUCKET_CREATE response");
                                    break;
                                case BUCKET_DELETE:
                                    System.out.println(">> ACK: Quorum achieved for " + payload.getIdentifier()
                                            + ": Setting BUCKET_DELETE response");
                                    break;
                                case OBJECT_CREATE:
                                    System.out.println(">> ACK: Quorum achieved for " + payload.getIdentifier()
                                            + ": Setting OBJECT_CREATE response");
                                    break;
                                case OBJECT_READ:
                                    System.out.println(">> ACK: Quorum achieved for " + payload.getIdentifier()
                                            + ": Setting OBJECT_READ response");
                                    break;
                                case OBJECT_UPDATE:
                                    System.out.println(">> ACK: Quorum achieved for " + payload.getIdentifier()
                                            + ": Setting OBJECT_UPDATE response");
                                    break;
                                case OBJECT_DELETE:
                                    System.out.println(">> ACK: Quorum achieved for " + payload.getIdentifier()
                                            + ": Setting OBJECT_DELETE response");
                                    break;
                                default:
                                    System.out.println("Unrecognized packet type!");
                            }
                            this.keepRunning.set(false);
                        }
                    } else {
                        System.out.println("Malformed packet!");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    this.keepRunning.set(false);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if (!this.ackServer.isClosed()) {
                this.ackServer.close();
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
            if (!this.ackServer.isClosed()) {
                this.ackServer.close();
            }
            this.keepRunning.set(false);
        }
    }

    private class ReadReceiver extends Thread {
        private AtomicBoolean keepRunning;
        private DatagramSocket readServer;
        private AtomicBoolean status;
        private int quorum;
        private int numReplicas;
        private ArrayList<ObjectIOModel> out;

        ReadReceiver(int size, int quorum, ArrayList<ObjectIOModel> out, AtomicBoolean status) throws SocketException {
            keepRunning = new AtomicBoolean(true);
            readServer = new DatagramSocket(DynamoServer.this.ackPort);
            this.quorum = quorum;
            this.numReplicas = size;
            this.out = out;
            this.status = status;
        }

        @Override
        public void run() {
            int receives = 0;
            int success = 0;
            System.out.println(">> READ RECEIVE: quorum init: " + quorum + " receives init : " + receives);
            while (keepRunning.get()) {
                /* Logic for receiving */
                /* init a buffer where the packet will be placed */
                byte[] buf = new byte[1500];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                System.out.print("[Dynamo Server] Read packet received");
                try {
                    this.readServer.receive(p);
                    /* Parse this packet into an object */
                    ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Object readObject = ois.readObject();
                    if (readObject instanceof DynamoMessage) {
                        DynamoMessage msg = (DynamoMessage) readObject;
                        System.out.println(" from " + msg.srcNode.name);
                        receives++;
                        System.out.println(">> READ RECEIVE: quorum: " + quorum + " receives: " + receives);
                        ObjectIOModel payload = (ObjectIOModel) msg.payload;
                        if (payload != null && !payload.getValue().isEmpty()) {
                            success++;
                            this.out.add(payload);
                        }

                        if (success >= quorum) {
                            this.status.set(true);
                        }
                        /* TODO: track separate receives by txnID */
                        if (receives >= numReplicas) {
                            System.out.println(">> READ RECEIVE: Replicas response achieved! Success!");
                            this.keepRunning.set(false);
                        }
                    } else {
                        System.out.println("Malformed packet!");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    this.keepRunning.set(false);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if (!this.readServer.isClosed()) {
                this.readServer.close();
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
            if (!this.readServer.isClosed()) {
                this.readServer.close();
            }
            this.keepRunning.set(false);
        }
    }

    @SuppressWarnings("unchecked")
    private class ReceiveFromRandNode extends Thread {
        //        private AtomicBoolean keepRunning;
        private DatagramSocket randRecvServer;
        private OutputModel outputModel;

        ReceiveFromRandNode(OutputModel outputModel) throws SocketException {
//            keepRunning = new AtomicBoolean(true);
            randRecvServer = new DatagramSocket(DynamoServer.this.ackPort);
            this.outputModel = outputModel;
        }

        @Override
        public void run() {
//            while (keepRunning.get()) {
            /* Logic for receiving */
            System.out.println(">> REST: randRecv: init");
            /* init a buffer where the packet will be placed */
            byte[] buf = new byte[1500];
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            try {
                this.randRecvServer.receive(p);
                /* Parse this packet into an object */
                ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());
                ObjectInputStream ois = new ObjectInputStream(bais);
                Object readObject = ois.readObject();
                if (readObject instanceof DynamoMessage) {
                    // Receive from rand node and set output
                    DynamoMessage msg = (DynamoMessage) readObject;
                    switch (msg.type) {
                        case FORWARD_ACK:
                            boolean status = (boolean) ((DynamoMessage) readObject).payload;
                            outputModel.setStatus(status);
                            break;
                        case FORWARD_ACK_READ:
                            ArrayList<ObjectIOModel> list =
                                    (ArrayList<ObjectIOModel>) msg.payload;
                            if (list.size() < Quorum.getReadQuorum()) {
                                outputModel.setResponse("Read quorum failed");
                                outputModel.setStatus(false);
                            } else {
                                StringBuilder str = new StringBuilder();
                                /* iterate list and append to output string */
                                for (ObjectIOModel oim : list) {
                                    str.append("<value: ").append(oim.getValue())
                                            .append(" version: ").append(oim.getVersion()).append("> ");
                                }
                                outputModel.setResponse(str.toString());
                                outputModel.setStatus(true);
                            }

                            break;
                        default:
                            System.out.println("Unrecognized Ack");
                    }
                } else {
                    System.out.println("Malformed packet!");
                }
            } catch (IOException e) {
                outputModel.setResponse(e.getMessage());
                outputModel.setStatus(false);
                //e.printStackTrace();
//                    keepRunning.set(false);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
//            }
            if (!randRecvServer.isClosed()) {
                randRecvServer.close();
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
            if (!this.randRecvServer.isClosed()) {
                this.randRecvServer.close();
            }
        }
    }

    public DynamoNode getNode() {
        return node;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
}
