package sd2223.clients;

import java.net.URI;

import sd2223.api.java.Users;
import sd2223.clients.rest.RestUsersClient;
import sd2223.clients.soap.SoapUsersClient;

public class UsersClientFactory {

	private static final String REST = "/rest";
	private static final String SOAP = "/soap";

	public static Users get(URI serverURI) {
		var uriString = serverURI.toString();

		if (uriString.endsWith(REST))
			return new RestUsersClient(serverURI);
		else if (uriString.endsWith(SOAP))
			return new SoapUsersClient(serverURI);
		else
			throw new RuntimeException("Unknown service type..." + uriString);
	}
}
