package sd2223.trab2.servers.rest;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.java.Service;
import sd2223.trab2.api.java.Users;
import sd2223.trab2.servers.rest.resources.RestFeedsResource;
import sd2223.trab2.servers.rest.resources.RestUsersResource;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.util.logging.Logger;

public class RestServer {

    private static final Logger Log = Logger.getLogger(RestServer.class.getName());
    public static void runServer(URI serverURI, Service.ServiceType stype, Service service){
        try {
            ResourceConfig config = new ResourceConfig();
            switch (stype) {
                case USERS -> config.register(new RestUsersResource((Users) service));
                case FEEDS, PROXY -> config.register(new RestFeedsResource((Feeds) service));
            }

            JdkHttpServerFactory.createHttpServer(serverURI, config, SSLContext.getDefault());
            Log.info(String.format("%s Rest Server ready @ %s\n", stype, serverURI));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }
}
