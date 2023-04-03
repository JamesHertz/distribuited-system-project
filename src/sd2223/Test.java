package sd2223;

import java.net.URI;

import sd2223.api.User;
import sd2223.clients.rest.RestUsersClient;
import sd2223.clients.soap.SoapUsersClient;
import sd2223.servers.rest.resources.RestUsersServer;
import sd2223.servers.soap.SoapUsersServer;

public class Test {
	public static void main(String[] args) throws Exception {

		RestUsersServer.main(new String[] {} );
		SoapUsersServer.main(new String[] {} );
		
		var uriRest = URI.create("http://localhost:8080/rest");
		var uriSoap = URI.create("http://localhost:8081/soap");
		
		var restClt = new RestUsersClient(uriRest);
		var soapClt = new SoapUsersClient(uriSoap);
		
		var user = new User("nmp", "12345", "nova", "Preguiça");
		
		var res1 = restClt.createUser( user );
		System.out.println( res1 );
		var res2 = restClt.getUser("nmp", "9999");
		System.out.println( res2 );
		var res3 = restClt.verifyPassword("nmp", "12345");
		System.out.println( res3 );

		var res4 = soapClt.createUser( user );
		System.out.println( res4 );
		var res5 = soapClt.getUser("nmp", "9999");
		System.out.println( res5 );
		var res6 = soapClt.verifyPassword("nmp", "12345");
		System.out.println( res6 );

	}
}
