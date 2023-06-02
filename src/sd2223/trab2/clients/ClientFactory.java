package sd2223.trab2.clients;

import java.net.URI;

import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.java.Users;
import sd2223.trab2.api.replication.ReplicatedClient;
import sd2223.trab2.clients.rest.RestFeedsClient;
import sd2223.trab2.clients.rest.RestUsersClient;
import sd2223.trab2.clients.soap.SoapFeedsClient;
import sd2223.trab2.clients.soap.SoapUsersClient;
import sd2223.trab2.servers.replication.ReplicatedServer;

public class ClientFactory {

	private static final String REST = "/rest";
	private static final String SOAP = "/soap";

	public static Users getUsersClient(URI serverURI) {
		var uriString = serverURI.toString();

		if (uriString.endsWith(REST))
			return new RestUsersClient(serverURI);
		else if (uriString.endsWith(SOAP))
			return new SoapUsersClient(serverURI);
		else
			throw new RuntimeException("Unknown service type..." + uriString);
	}

	public static Feeds getFeedsClient(URI serverURI){
		var uriString = serverURI.toString();

		if (uriString.endsWith(REST))
			return new RestFeedsClient(serverURI);
		else if (uriString.endsWith(SOAP))
			return new SoapFeedsClient(serverURI);
		else
			throw new RuntimeException("Unknown service type..." + uriString);
	}

	public static ReplicatedClient getReplicatedClient(URI serverURI, ReplicatedServer.VersionProvider provider){
		return new RestFeedsClient(serverURI, provider);
	}
}
