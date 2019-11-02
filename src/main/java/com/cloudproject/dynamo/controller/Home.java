package com.cloudproject.dynamo.controller;

import com.cloudproject.dynamo.models.BucketInputModel;
import com.cloudproject.dynamo.models.ObjectInputModel;
import com.cloudproject.dynamo.models.OutputModel;
import com.cloudproject.dynamo.msgmanager.DynamoServer;
import com.cloudproject.dynamo.msgmanager.MessageTypes;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.net.SocketException;

@Path("/")
public class Home {

    private static DynamoServer dynamoServer;

    /**
     * Main method, to be used for debugging purposes
     *
     * @param args array of String, may be used for debugging
     */
    public static void main(String[] args) throws SocketException, InterruptedException {
        dynamoServer = DynamoServer.startServer(args);
    }

    /**
     * <h3>Method to be used to test if the API is live and accepting requests</h3>
     * <p>
     * If the API is working correctly, it will return "Hello World!" as output.
     * </p>
     * <p>
     * Otherwise, the appropriate errors will be displayed
     * </p>
     *
     * @return the String "Hello World!
     */
    @GET
    @Path("start")
    public String start() throws SocketException, InterruptedException {
        startDynamoServer();
        return "REST server started successfully!";
    }

    @GET
    @Path("shutdown")
    public OutputModel shutdown() throws SocketException, InterruptedException {
        OutputModel outputModel = new OutputModel();
        dynamoServer.shutdownDynamoServer(outputModel);
        return outputModel;
    }

    @POST
    @Path("bucket")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public OutputModel createBucket(BucketInputModel inputModel) throws SocketException, InterruptedException {
        OutputModel outputModel = new OutputModel();
        startDynamoServer();
//        dynamoServer.createBucket(inputModel.getBucketName(), outputModel);
        dynamoServer.forwardToRandNode(MessageTypes.BUCKET_CREATE, inputModel.getBucketName(), outputModel);
//        bucketOutputModel.setResponse("Bucket " + inputModel.getBucketName() + " created successfully");
        return outputModel;
    }

    @DELETE
    @Path("bucket")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public OutputModel deleteBucket(BucketInputModel inputModel) throws SocketException, InterruptedException {
        OutputModel outputModel = new OutputModel();
        startDynamoServer();
//        bucketOutputModel.setResponse("Bucket " + inputModel.getBucketName() + " deleted successfully");
        dynamoServer.forwardToRandNode(MessageTypes.BUCKET_DELETE, inputModel.getBucketName(), outputModel);

        return outputModel;
    }

    @POST
    @Path("{bucketname}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public OutputModel createObject(ObjectInputModel inputModel, @PathParam("bucketname") String bucketName) {
        // TODO: Create the new object (file) in the required nodes
        return null;
    }

    @PUT
    @Path("{bucketname}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public OutputModel updateObject(ObjectInputModel inputModel, @PathParam("bucketname") String bucketName) {
        // TODO: replace old file with new file in each node for requested object
        return null;
    }

    @DELETE
    @Path("{bucketname}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public OutputModel deleteObject(ObjectInputModel inputModel, @PathParam("bucketname") String bucketName) {
        // TODO: delete the required object (file) from each node where requried
        return null;
    }

    private void startDynamoServer() throws SocketException, InterruptedException {
        if (dynamoServer == null) {
            dynamoServer = DynamoServer.startServer("REST-Host", "172.17.73.158:9350", "2000", "20000", "5", "true", "172.17.23.60:9350");
        }
    }
}
