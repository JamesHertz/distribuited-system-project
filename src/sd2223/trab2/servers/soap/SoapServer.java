package sd2223.trab2.servers.soap;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jakarta.xml.ws.Endpoint;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.java.Users;
import sd2223.trab2.servers.java.JavaService;
import sd2223.trab2.servers.soap.services.SoapFeedsWebService;
import sd2223.trab2.servers.soap.services.SoapUsersWebService;

import javax.net.ssl.SSLContext;

import static sd2223.trab2.utils.Formatter.*;

public class SoapServer {

//		System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
//		System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
//		System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
//		System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");


	public static void runFeedsService(String domain){

	}

	public static void runServer(URI serverURI, String service, JavaService jService){
		try {

			Object implementor = null;
			switch (service) {
				case USERS_SERVICE -> implementor = new SoapUsersWebService((Users) jService);
				case FEEDS_SERVICE -> implementor = new SoapFeedsWebService((Feeds) jService);
				default -> {
					System.out.println("ERROR: invalid service: " + service);
					System.exit(1);
				}
			}
			var server = HttpsServer.create(new InetSocketAddress(serverURI.getHost(), serverURI.getPort()), 0);
			server.setExecutor(Executors.newCachedThreadPool());
			server.setHttpsConfigurator(new HttpsConfigurator(SSLContext.getDefault()));

			var endpoint = Endpoint.create(implementor);

			endpoint.publish(server.createContext(serverURI.getPath()));
			server.start();

			System.out.printf("%s Soap Server ready @ %s\n", service, serverURI);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

	}

}
