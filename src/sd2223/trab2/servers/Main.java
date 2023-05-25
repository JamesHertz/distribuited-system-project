package sd2223.trab2.servers;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import sd2223.trab2.discovery.Discovery;
import sd2223.trab2.servers.java.JavaFeeds;
import sd2223.trab2.servers.java.JavaService;
import sd2223.trab2.servers.java.JavaUsers;
import sd2223.trab2.servers.proxy.MockingFeedsServer;
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
        parser.addArgument("-s", "--service")
                .choices("feeds", "users", "proxy") // TODO: constants later
                .required(true)
                .help("define the service type");
        parser.addArgument("--primary") // define a boolean flag :)
                .action(storeTrue())
                .help("if there the node is primary or not");
        parser.addArgument("-p" ,"--protocol") // define a boolean flag :)
                .choices("soap", "rest") // TODO: do constants later :)
                .required(true)
                .help("choose the protocol used");
        parser.addArgument("-S", "--secret")
                .metavar("secret")
                .required(true)
                .help("the secret share by the servers");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        var domain = ns.getString("domain");
        var service = ns.getString("service");
        var protocol = ns.getString("protocol");
        Secret.setSecret( ns.getString("secret") );

        JavaService javaService= switch (service){
            case USERS_SERVICE -> new JavaUsers(domain);
            case FEEDS_SERVICE -> {
                var baseNumber = ns.getLong("base_number");
                if(baseNumber == null ){
                    System.out.println("Error: Missing base_number");
                    parser.printUsage();
                    System.exit(1);
                }
                yield new JavaFeeds(domain, baseNumber);
            }
            case "proxy" -> {
               service = FEEDS_SERVICE;
               protocol = "rest";
               yield new MockingFeedsServer();
            }
            default ->  null;
        };

        String serverID = getServiceID(domain, service);
        String serverName = InetAddress.getLocalHost().getHostName();

        URI serverURI;
        if("rest".equals(protocol)){
            serverURI = getRestURI(serverName, REST_PORT);
            RestServer.runServer(serverURI, service, javaService);
        } else {
            serverURI = getSoapURI(serverName, SOAP_PORT);
            SoapServer.runServer(serverURI, service, javaService);
        }

        Discovery ds = Discovery.getInstance();
        ds.announce(serverID, serverURI.toString());
    }
}
