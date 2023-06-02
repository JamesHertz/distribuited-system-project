package sd2223.trab2.servers;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import sd2223.trab2.api.java.Service;
import sd2223.trab2.discovery.Discovery;
import sd2223.trab2.servers.java.JavaFeeds;
import sd2223.trab2.servers.java.JavaUsers;
import sd2223.trab2.servers.proxy.Mastodon;
import sd2223.trab2.servers.rest.RestServer;
import sd2223.trab2.servers.soap.SoapServer;
import sd2223.trab2.utils.Secret;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;

import static net.sourceforge.argparse4j.impl.Arguments.storeTrue;
import static sd2223.trab2.utils.Formatter.*;


// last-test: 104b
public class Main {
    // TODO: use secret on JavaFeeds
    public static void main(String[] args) throws UnknownHostException {
        System.out.println("args: " + Arrays.toString(args));
        var parser = ArgumentParsers.newFor("MyProj2").build()
                .defaultHelp(true).description("our second distributed system project");

        parser.addArgument("domain")
                .type(String.class)
                .help("the server domain");
        parser.addArgument("base_number")
                .type(Long.class)
                .nargs("?")
                .help("base number for feeds server IDs");
        parser.addArgument("-s","--service")
                .choices("proxy", "rest_feeds", "rest_users", "soap_feeds", "soap_users", "replication")
                .required(true)
                .help("the type of the server");
        parser.addArgument("-S", "--secret")
                .metavar("secret")
                .required(true)
                .help("the secret share by the servers");

        // server_type = { proxy, rest_feeds, rest_users, soap_feeds, soap_users, replication }
        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        var domain = ns.getString("domain");
        var service = ns.getString("protocol");
        Secret.setSecret( ns.getString("secret") );

        /*
        var stype = Service.ServiceType.USERS;
        Service service = switch (ns.getString("service")){
            case USERS_SERVICE -> new JavaUsers(domain);
            case FEEDS_SERVICE -> {
                var baseNumber = ns.getLong("base_number");
                if(baseNumber == null ){
                    System.out.println("Error: Missing base_number");
                    parser.printUsage();
                    System.exit(1);
                }
                stype = Service.ServiceType.FEEDS;
                yield new JavaFeeds(domain, baseNumber);
            }
            case "proxy" -> {
               stype = Service.ServiceType.FEEDS;
               yield new Mastodon(domain);
            }
            default ->  null;
        };

        String serverID = getServiceID(domain, stype.toString().toLowerCase());
        String serverName = InetAddress.getLocalHost().getHostName();

        URI serverURI;
        if("rest".equals(protocol)){
            serverURI = getRestURI(serverName);
            RestServer.runServer(serverURI, stype, service);
        } else {
            serverURI = getSoapURI(serverName);
            SoapServer.runServer(serverURI, stype, service);
        }

        Discovery ds = Discovery.getInstance();
        ds.announce(serverID, serverURI.toString());
         */
    }
}
