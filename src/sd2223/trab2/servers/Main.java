package sd2223.trab2.servers;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import sd2223.trab2.api.java.RepFeeds;
import sd2223.trab2.api.java.Service;

import static sd2223.trab2.api.java.Service.Protocol;
import static sd2223.trab2.api.java.Service.ServiceType;
import sd2223.trab2.discovery.Discovery;
import sd2223.trab2.servers.java.JavaFeeds;
import sd2223.trab2.servers.java.JavaUsers;
import sd2223.trab2.servers.java.JavaRepFeeds;
import sd2223.trab2.servers.proxy.Mastodon;
import sd2223.trab2.servers.replication.ReplicatedServer;
import sd2223.trab2.servers.rest.RestServer;
import sd2223.trab2.servers.soap.SoapServer;
import sd2223.trab2.utils.Formatter;
import sd2223.trab2.utils.Secret;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;

import static sd2223.trab2.utils.Formatter.*;


public class Main {
    private static final String DOMAIN = "domain";
    private static final String DOMAIN_HELP = "the server domain";
    private static final String BASE_NUMBER = "base_number";
    private static final String SECRET_HELP = "the secret shared by the servers";

    public static void main(String[] args) throws UnknownHostException {
        System.out.println("args: " + Arrays.toString(args));
        var parser = ArgumentParsers.newFor("MyProj2").build()
                .defaultHelp(true).description("our second distributed system project");

        parser.addArgument(DOMAIN)
                .type(String.class)
                .help(DOMAIN_HELP);
        parser.addArgument(BASE_NUMBER)
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
                .help(SECRET_HELP);

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        var domain = ns.getString("domain");
        var service = ns.getString("service");
        Secret.setSecret( ns.getString("secret") );

        var serverProtocol = switch (service){
            case "rest_feeds", "rest_users", "proxy" -> Protocol.REST;
            case "soap_feeds", "soap_users" -> Protocol.SOAP;
            default -> Protocol.REPLICATION; // "replication"
        };

        var serviceType= switch (service){
            case "rest_feeds", "soap_feeds" -> ServiceType.FEEDS;
            case "rest_users", "soap_users" -> ServiceType.USERS;
            case "replication" -> ServiceType.REPLICATION;
            default -> ServiceType.PROXY; // "proxy"
        };

        Service svc = switch (serviceType){
            case USERS -> new JavaUsers(domain);
            case PROXY -> new Mastodon(domain);
            case FEEDS -> new JavaFeeds(domain, getBaseNumber(ns));
            case REPLICATION -> new JavaRepFeeds(domain, getBaseNumber(ns));
        };

        String serverName = InetAddress.getLocalHost().getHostName();
        String serverID   = Formatter.getServiceID(domain, serviceType == ServiceType.USERS? USERS_SERVICE : FEEDS_SERVICE);
        URI serverURI     = serverProtocol == Protocol.SOAP? getSoapURI(serverName) : getRestURI(serverName);

        switch (serverProtocol){
            case REST -> RestServer.runServer(serverURI, serviceType, svc);
            case SOAP -> SoapServer.runServer(serverURI, serviceType, svc);
            case REPLICATION -> ReplicatedServer.runServer(serverURI, serverID, (RepFeeds) svc);
        }

        Discovery ds = Discovery.getInstance();
        ds.announce(serverID, serverURI.toString());
    }

    private static long getBaseNumber(Namespace ns){
        var baseNumber = ns.getLong("base_number");
        if(baseNumber == null ){
            System.err.println("Error: Missing base_number.");
            System.err.println("use fla --help to display the usage.");
            System.exit(1);
        }
        return baseNumber;
    }
}
