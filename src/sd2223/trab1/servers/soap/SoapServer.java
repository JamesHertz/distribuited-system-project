package sd2223.trab1.servers.soap;


import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.xml.ws.Endpoint;
import sd2223.trab1.discovery.Discovery;
import sd2223.trab1.servers.java.JavaFeeds;
import sd2223.trab1.servers.java.JavaUsers;
import sd2223.trab1.servers.soap.services.SoapFeedsWebService;
import sd2223.trab1.servers.soap.services.SoapUsersWebService;
import sd2223.trab1.servers.soap.services.SoapWebService;

import static sd2223.trab1.utils.Formatter.*;

public class SoapServer {

	public static final int PORT = 8081;


//		System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
//		System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
//		System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
//		System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");


	public static void main(String[] args) {
		if(args.length < 2){
			System.out.println("usage: <domain> <service>");
			System.out.println("ERROR: wrong number of arguments");
			System.exit(1);
		}
		try {

			String domain =  args[0];
			String service = args[1];
			long baseNumber = 0;

			if( args.length > 2 ) {
				service = args[2];
				baseNumber = Long.parseLong(args[1]);
			}

			String serverID = getServiceID(domain, service);
			String serverName = InetAddress.getLocalHost().getHostName();
			String serverURI = getSoapURI(serverName, PORT).toString();

			Discovery ds = Discovery.getInstance();
			ds.announce(serverID, serverURI);

			Object implementor = null;
			switch (service) {
				case USERS_SERVICE -> implementor = new SoapUsersWebService(new JavaUsers(domain));
				case FEEDS_SERVICE ->  implementor = new SoapFeedsWebService(new JavaFeeds(domain, baseNumber));
				default -> {
					System.out.println("ERROR: invalid service: " + service);
					System.exit(1);
				}
			}

			Endpoint.publish(serverURI.replace(serverName, "0.0.0.0"), implementor);
			System.out.printf("%s Soap Server ready @ %s\n", service, serverURI);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}

	}

}
