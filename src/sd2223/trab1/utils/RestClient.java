package sd2223.trab1.utils;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import sd2223.trab1.api.User;
import sd2223.trab1.api.rest.FeedsService;
import sd2223.trab1.api.rest.UsersService;

import java.net.URI;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class RestClient {
	private static final Logger Log = Logger.getLogger(RestClient.class.getName());

	protected static final int READ_TIMEOUT = 5000;
	protected static final int CONNECT_TIMEOUT = 5000;

	protected static final int RETRY_SLEEP = 3000;
	protected static final int MAX_RETRIES = 10;

	final URI serverURI;
	final Client client;
	final ClientConfig config;
	final WebTarget target;

	RestClient(URI serverURI) {
		this.serverURI = serverURI;
		this.config = new ClientConfig();

		config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
		config.property( ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);
		
		this.client = ClientBuilder.newClient(config);

		// todo: change late :)
		this.target = client.target( serverURI );
	}

	protected <T> T reTry(Supplier<T> func) {
		for (int i = 0; i < MAX_RETRIES; i++)
			try {
				return func.get();
			} catch (ProcessingException x) {
				System.err.println( x.getMessage() );
				Log.fine("ProcessingException: " + x.getMessage());
				sleep(RETRY_SLEEP);
			} catch (Exception x) {
				Log.fine("Exception: " + x.getMessage());
				x.printStackTrace();
				break;
			}
		return null;
	}

	private void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException x) { // nothing to do...
		}
	}

	public User getUser(String userID, String pwd){
		return reTry(() -> {
			Response r = target.path( UsersService.PATH )
					.path( userID )
					.queryParam( UsersService.PWD, pwd).request()
					.get();
			if(r.getStatus() == Response.Status.OK.getStatusCode() && r.hasEntity())
				return  r.readEntity(User.class);

			return null;
		});
	}
}
