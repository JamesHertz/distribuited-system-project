package sd2223.trab2.servers.java;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sd2223.trab2.api.User;
import sd2223.trab2.api.java.Result;
import sd2223.trab2.api.java.Result.ErrorCode;
import sd2223.trab2.api.java.Users;
import sd2223.trab2.utils.Formatter;
import sd2223.trab2.utils.Secret;

public class JavaUsers extends JavaService implements Users {
	private static Logger Log = LoggerFactory.getLogger(JavaUsers.class);

	//Map to save all Users from this domain
	private final Map<String,User> users;
	private final String domain;

	private final String secret;
	public JavaUsers(String domain){
		this.users = new HashMap<>();
		this.domain = domain;
		this.secret = Secret.getSecret();
	}

	/**
	 * 1- Checks if the user is valid
	 * 2- Puts the user in our Map users,
	 * 3- Make a request to our FeedsServer to create an empty feed for him
	 * @param user
	 * @return userAdress || Error
	 */
	@Override
	public Result<String> createUser(User user) {
		Log.info("createUser: {} ", user);
		
		// Check if user data is valid
		if(user.getName() == null || user.getPwd() == null || user.getDisplayName() == null
				|| user.getDomain() == null || !user.getDomain().equals(domain) ) {
			Log.error("User object invalid.");
			return Result.error( ErrorCode.BAD_REQUEST );
		}

		synchronized (users){
			// Insert user, checking if name already exists
			if( users.putIfAbsent(user.getName(), user) != null ) {
				Log.error("User already exists.");
				return Result.error( ErrorCode.CONFLICT);
			}
		}

		var userAddress =  Formatter.makeUserAddress( user.getName() , this.domain );
		super.addRequest(
				this.domain,
				server -> server.createFeed( userAddress , secret )
		);
		Log.info("User created.");
		return Result.ok( userAddress );
	}

	/**
	 * Gets the info of a user given a name and a password, in case the info is correct
	 * @param name
	 * @param pwd
	 * @return User || Error
	 */
	@Override
	public Result<User> getUser(String name, String pwd) {
		Log.info("getUser : user = {} ; pwd = {}", name, pwd);
		
		// Check if user is valid
		if(name == null || pwd == null) {
			Log.error("Name or Password null.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}

		var err = this.authenticateUser(name, pwd);
		if(! err.isOK() ) return err;

		var user = err.value();

		Log.info("User found and returned!");
		return Result.ok(user);
	}

	/**
	 * Updates the info of a user given a name, a password and the new user info, in case the info is correct
	 * @param name
	 * @param pwd
	 * @param user
	 * @return User || Error
	 */
	@Override
	public Result<User> updateUser(String name, String pwd, User user) {
		Log.info("updateUser: name= {} ; pwd= {} ; user= {}", name, pwd, user);

		if( user != null && name != null && pwd != null ){

			var err = this.authenticateUser(name, pwd);
			if(! err.isOK() ) return err;

			var oldUser= err.value();

			if ( ! name.equals(user.getName())) {
				Log.error("Invalid username.");
				return Result.error( ErrorCode.BAD_REQUEST );
			}

			if( user.getDisplayName() == null ) user.setDisplayName( oldUser.getDisplayName() );

			if( user.getPwd() == null ) user.setPwd( oldUser.getPwd() );

			user.setDomain(this.domain);
			//user.setName(name);

			synchronized (users){
				users.put(name, user);
			}

			Log.info("Updates performed!!");
			return Result.ok(user);

		} else {
			return Result.error( ErrorCode.BAD_REQUEST );
		}
	}

	/**
	 * Deletes a user given a name and a password, in case the info is correct (Also delete all feeds info everywhere)
	 * @param name
	 * @param pwd
	 * @return Deleted User || Error
	 */
	@Override
	public Result<User> deleteUser(String name, String pwd) {
		Log.info("deleteUser: name= {} ; pwd= {}", name, pwd);

		if( name == null || pwd == null ) {
			Log.error("Username or password nil.");
			return Result.error(ErrorCode.BAD_REQUEST);
		}

		var err = this.authenticateUser(name, pwd);
		if(! err.isOK() ) return err;

		var user = err.value();

		synchronized ( users ) {
			users.remove(name);
		}
		//Deleting feeds info...
		super.addRequest(
				this.domain,
				server -> server.removeFeed(
						Formatter.makeUserAddress(name, this.domain), secret
				)
		);
		return Result.ok( user );
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		Log.info("searchUsers: query=" + pattern);

		if(pattern == null){
			Log.info("Invalid query.");
			return Result.error( ErrorCode.BAD_REQUEST );
		}

		String auxPattern = pattern.toLowerCase();
		synchronized (users){
			return Result.ok(
					users.values()
					     .stream()
					     .filter(u -> u.getName().toLowerCase().contains(auxPattern) )
						 .map( u ->  new User(u.getName(), "", u.getDomain(), u.getDisplayName()))
					     .collect(Collectors.toList())
			);
		}
	}

	/**
	 * Auxiliar method to verify if password of some user (given by the name) is correct
	 * @param name
	 * @param pwd
	 * @return Ok Result || Error Result
	 */
	@Override
	public Result<Void> verifyPassword(String name, String pwd) {
		var res = getUser(name, pwd);
		if( res.isOK() )
			return Result.ok();
		else
			return Result.error( res.error() );
	}

	/**
	 * Gets the user from our Map of users
	 * @param username
	 * @return
	 */
	private User getUser(String username){
		synchronized (users){
			return users.get(username);
		}
	}

	private Result<User> authenticateUser(String name, String pwd){
		var user = this.getUser(name);

		if( user == null ) {
			Log.error("User does not exist.");
			return Result.error( ErrorCode.NOT_FOUND);
		}

		if( !user.getPwd().equals( pwd)) {
			Log.error("Password is incorrect.");
			return Result.error( ErrorCode.FORBIDDEN);
		}
		return Result.ok(user);
	}
	/*
		private Feeds getMyFeedsServer(){
			var ds = Discovery.getInstance();
			URI[] serverURIs = ds.knownUrisOf(Formatter.getServiceID(this.domain, Formatter.FEEDS_SERVICE), 1);
			return ClientFactory.getFeedsClient(serverURIs[0]);
		}
	 */

}
