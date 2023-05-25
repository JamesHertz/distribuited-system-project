package sd2223.trab2.servers.rest;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.java.Service;
import sd2223.trab2.api.java.ServiceType;
import sd2223.trab2.api.java.Users;
import sd2223.trab2.servers.java.JavaFeeds;
import sd2223.trab2.servers.java.JavaService;
import sd2223.trab2.servers.java.JavaUsers;
import sd2223.trab2.servers.rest.resources.RestFeedsResource;
import sd2223.trab2.servers.rest.resources.RestUsersResource;
import sd2223.trab2.discovery.Discovery;

import javax.net.ssl.SSLContext;
import java.net.InetAddress;
import java.net.URI;

import static sd2223.trab2.utils.Formatter.*;

public class RestServer {

    public static void runServer(URI serverURI, ServiceType stype, Service service){
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
