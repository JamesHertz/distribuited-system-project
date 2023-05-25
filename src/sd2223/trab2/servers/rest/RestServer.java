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

public class RestServer {

    public static void runServer(URI serverURI, Service.ServiceType stype, Service service){
        try {
            ResourceConfig config = new ResourceConfig();
            switch (stype) {
                case USERS -> config.register(new RestUsersResource((Users) service));
                case FEEDS -> config.register(new RestFeedsResource((Feeds) service));
            }

            JdkHttpServerFactory.createHttpServer(serverURI, config, SSLContext.getDefault());
            System.out.printf("%s Rest Server ready @ %s\n", service, serverURI);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

    }
}
