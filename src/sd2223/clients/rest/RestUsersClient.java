package sd2223.clients.rest;

import java.net.URI;
import java.util.List;

import sd2223.api.User;
import sd2223.api.java.Result;
import sd2223.api.java.Users;
import sd2223.api.rest.UsersService;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


public class RestUsersClient extends RestClient implements Users {

	final WebTarget target;
	
	public RestUsersClient( URI serverURI ) {
		super( serverURI );
		target = client.target( serverURI ).path( UsersService.PATH );
	}
	
	private Result<String> clt_createUser( User user) {
		
		Response r = target.request()
				.post(Entity.entity(user, MediaType.APPLICATION_JSON));

		return super.toJavaResult(r, String.class);
	}
	
	private Result<User> clt_getUser(String name, String pwd) {

		Response r = target.path( name )
				.queryParam(UsersService.PWD, pwd).request()
				.accept(MediaType.APPLICATION_JSON)
				.get();

		return super.toJavaResult(r, User.class);
	}
	
	private Result<Void> clt_verifyPassword(String name, String pwd) {
		Response r = target.path( name ).path(UsersService.PWD)
				.queryParam(UsersService.PWD, pwd).request()
				.get();

		return super.toJavaResult(r, Void.class);
	}
	
	@Override
	public Result<String> createUser(User user) {
		return super.reTry(() -> clt_createUser(user));
	}

	@Override
	public Result<User> getUser(String name, String pwd) {
		return super.reTry(() -> clt_getUser(name, pwd));
	}
	
	@Override
	public Result<Void> verifyPassword(String name, String pwd) {
		return super.reTry(() -> clt_verifyPassword(name, pwd));
	}

	@Override
	public Result<User> updateUser(String userId, String password, User user) {
		throw new RuntimeException("Not Implemented...");
	}

	@Override
	public Result<User> deleteUser(String userId, String password) {
		throw new RuntimeException("Not Implemented...");
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		throw new RuntimeException("Not Implemented...");
	}	
}
