package sd2223.trab2.servers.soap;


import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jakarta.xml.ws.Endpoint;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.java.Service;
import sd2223.trab2.api.java.Users;
import sd2223.trab2.servers.soap.services.SoapFeedsWebService;
import sd2223.trab2.servers.soap.services.SoapUsersWebService;

import javax.net.ssl.SSLContext;

public class SoapServer {

//		System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
//		System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
//		System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
//		System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");

	private static final Logger Log = Logger.getLogger(SoapServer.class.getName());

	public static void runServer(URI serverURI, Service.ServiceType stype, Service service){
		try {

			Object implementor = switch (stype) {
				case USERS ->  new SoapUsersWebService((Users) service);
				case FEEDS ->  new SoapFeedsWebService((Feeds) service);
				case PROXY ->  throw new RuntimeException("NOT IMPLEMENTED");
				case REPLICATION ->  throw new RuntimeException("Bad service type");
			};
			var server = HttpsServer.create(new InetSocketAddress(serverURI.getHost(), serverURI.getPort()), 0);
			var endpoint = Endpoint.create(implementor);

			server.setExecutor(Executors.newCachedThreadPool());
			server.setHttpsConfigurator(new HttpsConfigurator(SSLContext.getDefault()));

			endpoint.publish(server.createContext(serverURI.getPath()));
			server.start();

			Log.info(String.format("%s Soap Server ready @ %s\n", stype, serverURI));
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

	}

}
