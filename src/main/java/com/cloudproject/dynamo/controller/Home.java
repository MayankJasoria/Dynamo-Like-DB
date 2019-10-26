package com.cloudproject.dynamo.controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class Home {

    public static void main(String[] args) {
        // use for debugging purposes: check behavior of internals
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

}
