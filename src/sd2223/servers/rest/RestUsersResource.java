package sd2223.servers.rest;

import java.util.List;

import sd2223.api.User;
import sd2223.api.java.Users;
import sd2223.api.rest.UsersService;
import sd2223.servers.java.JavaUsers;
import jakarta.inject.Singleton;

@Singleton
public class RestUsersResource extends RestResource implements UsersService {

	final Users impl;
	public RestUsersResource() {
		this.impl = new JavaUsers();
	}
	
	@Override
	public String createUser(User user) {
		return super.fromJavaResult( impl.createUser( user));
	}

	@Override
	public User getUser(String name, String pwd) {
		return super.fromJavaResult( impl.getUser(name, pwd));
	}

	@Override
	public void verifyPassword(String name, String pwd) {
		super.fromJavaResult( impl.verifyPassword(name, pwd));
	}
	
	@Override
	public User updateUser(String name, String pwd, User user) {
		throw new RuntimeException("Not Implemented...");
	}

	@Override
	public User deleteUser(String name, String pwd) {
		throw new RuntimeException("Not Implemented...");
	}

	@Override
	public List<User> searchUsers(String pattern) {
		throw new RuntimeException("Not Implemented...");
	}

		
}
