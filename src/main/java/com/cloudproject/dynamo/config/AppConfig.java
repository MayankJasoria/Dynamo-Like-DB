package com.cloudproject.dynamo.config;

import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;
import com.owlike.genson.ext.jaxrs.GensonJaxRSFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

/**
 * Class for managing the root of API calls. Here, it is relevant for
 * registering {@link Genson} with the required configuration as the
 * JSON Parser of the app
 */
@ApplicationPath("/db")
public class AppConfig extends ResourceConfig {
    public AppConfig() {
        Genson genson = new GensonBuilder().setSkipNull(true).create();
        register(new GensonJaxRSFeature().use(genson));
        packages("com.cloudproject.dynamo.controller");
    }
}
