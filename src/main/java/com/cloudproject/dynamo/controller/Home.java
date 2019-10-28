package com.cloudproject.dynamo.controller;

import com.cloudproject.dynamo.models.BucketInputModel;
import com.cloudproject.dynamo.models.BucketOutputModel;
import com.cloudproject.dynamo.models.ObjectInputModel;
import com.cloudproject.dynamo.models.ObjectOutputModel;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/")
public class Home {

    /**
     * Main method, to be used for debugging purposes
     *
     * @param args array of String, may be used for debugging
     */
    public static void main(String[] args) {
        System.out.println("Hello World!");
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
    @Path("test")
    public String helloWorld() {
        return "Hello World!";
    }

    @POST
    @Path("Bucket")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BucketOutputModel createBucket(BucketInputModel inputModel) {
        // TODO: Create a folder in each node
        BucketOutputModel bucketOutputModel = new BucketOutputModel();
        bucketOutputModel.setResponse("Bucket " + inputModel.getBucketName() + " created successfully");
        return bucketOutputModel;
    }

    @DELETE
    @Path("Bucket")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BucketOutputModel deleteBucket(BucketInputModel inputModel) {
        // TODO: Delete the required folder from each node (along with all its data)
        BucketOutputModel bucketOutputModel = new BucketOutputModel();
        bucketOutputModel.setResponse("Bucket " + inputModel.getBucketName() + " deleted successfully");
        return bucketOutputModel;
    }

    @POST
    @Path("{bucketname}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ObjectOutputModel createObject(ObjectInputModel inputModel, @PathParam("bucketname") String bucketName) {
        // TODO: Create the new object (file) in the required nodes
        return null;
    }

    @PUT
    @Path("{bucketname}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ObjectOutputModel updateObject(ObjectInputModel inputModel, @PathParam("bucketname") String bucketName) {
        // TODO: replace old file with new file in each node for requested object
        return null;
    }

    @DELETE
    @Path("{bucketname}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ObjectOutputModel deleteObject(ObjectInputModel inputModel, @PathParam("bucketname") String bucketName) {
        // TODO: delete the required object (file) from each node where requried
        return null;
    }
}
