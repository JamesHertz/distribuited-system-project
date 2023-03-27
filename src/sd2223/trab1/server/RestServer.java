package sd2223.trab1.server;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import sd2223.trab1.discovery.Discovery;
import sd2223.trab1.server.services.FeedsServiceImpl;
import sd2223.trab1.server.services.UsersServiceImpl;
import static sd2223.trab1.server.services.Utils.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

public class RestServer {
    // <nome-do-domínio>:<serviço><tab><uri-do-servidor>
    // private static final Pattern PATTERN = Pattern.compile("(.+):(feeds|users)\t(.+)");

    private static Logger Log = Logger.getLogger(RestServer.class.getName());
    public static final int PORT = 8080;
    //private static final String SERVER_URI_FMT = "http://%s:%s/rest";
    //private static final String SERVER_NAME_FORMAT = "%s:%s";
    //private static final String USERS_SERVICE = "users";
    //private static final String FEEDS_SERVICE = "feeds";

    public static void main(String[] args) {
        if(args.length < 2){
            System.out.println("usage: <domain> <service>");
            System.out.println("ERROR: wrong number of arguments");
            System.exit(1);
        }
        try {

            String domain =  args[0]; //"nova";
            String service = args[1]; //"users";
            String serverID = getServiceID(domain, service);//String.format(SERVER_NAME_FORMAT, domain, service);
            String serverName = InetAddress.getLocalHost().getHostName();
            URI serverURI = getRestURI(serverName, PORT); //URI.create(String.format(SERVER_URI_FMT, serverName, PORT));

            Discovery ds = Discovery.getInstance();
            ds.announce(serverID, serverURI.toString());

            ResourceConfig config = new ResourceConfig();
            switch (service) {
                case USERS_SERVICE -> config.register(new UsersServiceImpl(domain));
                case FEEDS_SERVICE -> config.register(new FeedsServiceImpl(domain));
                default -> {
                    System.out.println("ERROR: invalid service: " + service);
                    System.exit(1);
                }
            }

            JdkHttpServerFactory.createHttpServer(serverURI, config);
            System.out.printf("%s Server running %s service @ %s\n", serverName, service, serverURI);
        } catch (IOException e) {
            Log.severe(e.getMessage());
        }

    }
}
