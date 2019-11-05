package com.cloudproject.dynamo.controller;

import com.cloudproject.dynamo.models.BucketInputModel;
import com.cloudproject.dynamo.models.MessageTypes;
import com.cloudproject.dynamo.models.ObjectInputModel;
import com.cloudproject.dynamo.models.OutputModel;
import com.cloudproject.dynamo.msgmanager.DynamoServer;

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
    public static void main(String[] args) throws SocketException {
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
    public String start() throws SocketException {
        startDynamoServer();
        return "REST server started successfully!";
    }

    @GET
    @Path("shutdown")
    public OutputModel shutdown() {
        OutputModel outputModel = new OutputModel();
        dynamoServer.shutdownDynamoServer(outputModel);
        dynamoServer = null;
        return outputModel;
    }

    @POST
    @Path("bucket")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public OutputModel createBucket(BucketInputModel inputModel) throws SocketException {
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
    public OutputModel deleteBucket(BucketInputModel inputModel) throws SocketException {
        OutputModel outputModel = new OutputModel();
        startDynamoServer();
//        bucketOutputModel.setResponse("Bucket " + inputModel.getBucketName() + " deleted successfully");
        dynamoServer.forwardToRandNode(MessageTypes.BUCKET_DELETE, inputModel.getBucketName(), outputModel);

        return outputModel;
    }

    @POST
    @Path("{bucketName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public OutputModel createObject(ObjectInputModel inputModel, @PathParam("bucketName") String bucketName) throws SocketException {
        OutputModel outputModel = new OutputModel();
        startDynamoServer();

        dynamoServer.forwardToRandNode(MessageTypes.OBJECT_CREATE, bucketName, inputModel, outputModel);

        return outputModel;
    }

    @PUT
    @Path("{bucketName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public OutputModel updateObject(ObjectInputModel inputModel, @PathParam("bucketName") String bucketName)
            throws SocketException {
        OutputModel outputModel = new OutputModel();
        startDynamoServer();

        dynamoServer.forwardToRandNode(MessageTypes.OBJECT_UPDATE, bucketName, inputModel, outputModel);

        return outputModel;
    }

    @DELETE
    @Path("{bucketName}/{objectKey}")
    @Produces(MediaType.APPLICATION_JSON)
    public OutputModel deleteObject(@PathParam("bucketName") String bucketName,
                                    @PathParam("objectKey") String key) throws SocketException {
        OutputModel outputModel = new OutputModel();
        startDynamoServer();

        dynamoServer.forwardToRandNode(MessageTypes.OBJECT_DELETE, bucketName, key, outputModel);

        return outputModel;
    }

    @GET
    @Path("{bucketName}/{objectKey}")
    @Produces(MediaType.APPLICATION_JSON)
    public OutputModel readObject(@PathParam("bucketName") String bucketName,
                                  @PathParam("objectKey") String objectKey) throws SocketException {
        OutputModel outputModel = new OutputModel();
        startDynamoServer();

        dynamoServer.forwardToRandNode(MessageTypes.OBJECT_READ, bucketName, objectKey, outputModel);

        return outputModel;
    }

    private void startDynamoServer() throws SocketException {
        if (dynamoServer == null) {
            dynamoServer = DynamoServer.startServer("REST-Host",
                    "172.17.73.158:9350", "2000", "20000", "true",
                    "172.17.200.222:9350,172.17.200.223:9350");
        }
    }
}
