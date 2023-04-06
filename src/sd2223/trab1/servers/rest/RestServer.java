package sd2223.trab1.servers.rest;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import sd2223.trab1.servers.java.JavaFeeds;
import sd2223.trab1.servers.java.JavaUsers;
import sd2223.trab1.servers.rest.resources.RestFeedsResource;
import sd2223.trab1.servers.rest.resources.RestUsersResource;
import sd2223.trab1.discovery.Discovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;

import static sd2223.trab1.utils.Formatter.*;

public class RestServer {
    // <nome-do-domínio>:<serviço><tab><uri-do-servidor>
    // private static final Pattern PATTERN = Pattern.compile("(.+):(feeds|users)\t(.+)");

    public static void main(String[] args) {
        if(args.length < 2){
            System.out.println("usage: <domain> <service>");
            System.out.println("ERROR: wrong number of arguments");
            System.exit(1);
        }
        try {

            String domain =  args[0]; //"nova";
            String service = args[1];
            long baseNumber = 0;

            if( args.length > 2 ) {
                service = args[2]; //"users";
                baseNumber = Long.parseLong(args[1]);
            }

            String serverID = getServiceID(domain, service);//String.format(SERVER_NAME_FORMAT, domain, service);
            String serverName = InetAddress.getLocalHost().getHostName();
            URI serverURI = getRestURI(serverName, PORT); //URI.create(String.format(SERVER_URI_FMT, serverName, PORT));

            Discovery ds = Discovery.getInstance();
            ds.announce(serverID, serverURI.toString());

            ResourceConfig config = new ResourceConfig();
            switch (service) {
                case USERS_SERVICE -> config.register(new RestUsersResource(new JavaUsers(domain)));
                case FEEDS_SERVICE -> config.register(new RestFeedsResource(new JavaFeeds(domain, baseNumber)));
                default -> {
                    System.out.println("ERROR: invalid service: " + service);
                    System.exit(1);
                }
            }

            JdkHttpServerFactory.createHttpServer(serverURI, config);
            System.out.printf("%s Server running %s service @ %s\n", serverName, service, serverURI);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

    }
}
