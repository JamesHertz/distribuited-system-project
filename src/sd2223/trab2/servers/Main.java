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

        final String DOMAIN = "domain";
        final String DOMAIN_HELP = "the server domain";
        final String BASE_NUMBER = "base_number";
        final String BASE_NUMBER_HELP = "base number for feeds server IDs";
        final String SERVICE = "service";
        final String SERVICE_CHOICES = "feeds";
        final String SERVICE_HELP = "define the service type";
        final String PRIMARY = "--primary";
        final String PRIMARY_ACTION_HELP = "if the node is primary or not";
        final String PROTOCOL = "protocol";
        final String PROTOCOL_CHOICES = "soap";
        final String PROTOCOL_HELP = "choose the protocol used";
        final String SECRET = "secret";
        final String SECRET_METAVAR = "secret";
        final String SECRET_HELP = "the secret shared by the servers";
        final String ERROR_MISSING_BASE_NUMBER = "Error: Missing base_number";
        final String REST_PROTOCOL = "rest";

        parser.addArgument(DOMAIN)
                .type(String.class)
                .help(DOMAIN_HELP);
        parser.addArgument(BASE_NUMBER)
                .type(Long.class)
                .nargs("?")
                .help(BASE_NUMBER_HELP);
        parser.addArgument("-s", "--" + SERVICE)
                .choices(SERVICE_CHOICES) // TODO: constants later
                .required(true)
                .help(SERVICE_HELP);
        parser.addArgument(PRIMARY)
                .action(storeTrue())
                .help(PRIMARY_ACTION_HELP);
        parser.addArgument("-p", "--" + PROTOCOL)
                .choices(PROTOCOL_CHOICES) // TODO: do constants later :)
                .required(true)
                .help(PROTOCOL_HELP);
        parser.addArgument("-S", "--" + SECRET)
                .metavar(SECRET_METAVAR)
                .required(true)
                .help(SECRET_HELP);

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        Secret.setSecret(ns.getString(SECRET));

        final String USERS_SERVICE = "users";
        final String FEEDS_SERVICE = "feeds";
        final String PROXY_SERVICE = "proxy";
        final String SERVER_ID_FORMAT = "%s_%s";

        String domain = ns.getString(DOMAIN);
        String protocol = ns.getString(PROTOCOL);

        Service service = null;
        Service.ServiceType serviceType = null;

        switch (ns.getString(SERVICE)) {
            case USERS_SERVICE:
                service = new JavaUsers(domain);
                break;
            case FEEDS_SERVICE:
                Long baseNumber = ns.getLong(BASE_NUMBER);
                if (baseNumber == null) {
                    System.out.println(ERROR_MISSING_BASE_NUMBER);
                    parser.printUsage();
                    System.exit(1);
                }
                serviceType = Service.ServiceType.FEEDS;
                service = new JavaFeeds(domain, baseNumber);
                break;
            case PROXY_SERVICE:
                protocol = REST_PROTOCOL;
                serviceType = Service.ServiceType.FEEDS;
                service = new Mastodon(domain);
                break;
            default:
                break;
        }

        String serverID = String.format(SERVER_ID_FORMAT, domain, serviceType.toString().toLowerCase());
        String serverName = InetAddress.getLocalHost().getHostName();

        URI serverURI;
        if (REST_PROTOCOL.equals(protocol)) {
            serverURI = getRestURI(serverName);
            RestServer.runServer(serverURI, serviceType, service);
        } else {
            serverURI = getSoapURI(serverName);
            SoapServer.runServer(serverURI, serviceType, service);
        }

        Discovery ds = Discovery.getInstance();
        ds.announce(serverID, serverURI.toString());
    }
}
