package sd2223.trab1.servers.java;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import sd2223.trab1.api.User;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.java.Result.ErrorCode;
import sd2223.trab1.api.java.Users;
import sd2223.trab1.clients.ClientFactory;
import sd2223.trab1.discovery.Discovery;
import sd2223.trab1.utils.Formatter;

public class JavaUsers implements Users {
	private final Map<String,User> users;
	private final String domain;

	public JavaUsers(){
		this(null);
	}
	public JavaUsers(String domain){
		this.users = new HashMap<>();
		this.domain = domain;
	}
	private static Logger Log = Logger.getLogger(JavaUsers.class.getName());

	@Override
	public Result<String> createUser(User user) {
		Log.info("createUser : " + user);
		
		// Check if user data is valid
		if(user.getName() == null || user.getPwd() == null || user.getDisplayName() == null
				|| user.getDomain() == null || !user.getDomain().equals(domain) ) {
			Log.info("User object invalid.");
			return Result.error( ErrorCode.BAD_REQUEST );
		}

		synchronized (users){
			// Insert user, checking if name already exists
			if( users.putIfAbsent(user.getName(), user) != null ) {
				Log.info("User already exists.");
				return Result.error( ErrorCode.CONFLICT);
			}
		}

		var userAddress =  Formatter.makeUserAddress( user.getName() , this.domain );

		// TODO: make this persistent :)
		var feedsServer = this.getMyFeedsServer();
		feedsServer.createFeed( userAddress );
		return Result.ok( userAddress );
	}

	@Override
	public Result<User> getUser(String name, String pwd) {
		Log.info("getUser : user = " + name + "; pwd = " + pwd);
		
		// Check if user is valid
		if(name == null || pwd == null) {
			Log.info("Name or Password null.");
			return Result.error( ErrorCode.BAD_REQUEST);
		}

		User user;

		synchronized (users){
			user = users.get(name);
		}

		// Check if user exists
		if( user == null ) {
			Log.info("User does not exist.");
			return Result.error( ErrorCode.NOT_FOUND);
		}
		
		//Check if the password is correct
		if( !user.getPwd().equals( pwd ) ) {
			Log.info("Password is incorrect.");
			return Result.error( ErrorCode.FORBIDDEN );
		}
		
		return Result.ok(user);
	}

	@Override
	public Result<User> updateUser(String name, String pwd, User user) {
		Log.info("updateUser: name=" + name + " ; pwd= " + pwd + " ; user=" + user) ;

		if( user != null && name != null && pwd != null ){
			User oldUser;
			synchronized (users){
				oldUser = users.get(name);
			}

			if( oldUser == null ){
				Log.info("User not found.");
				return Result.error( ErrorCode.NOT_FOUND );
			}

			if(! oldUser.getPwd().equals(pwd) ) {
				Log.info("Wrong password.");
				return Result.error(ErrorCode.FORBIDDEN);
			}

			if ( ! name.equals(user.getName())) {
				Log.info("Invalid username.");
				return Result.error( ErrorCode.BAD_REQUEST );
			}

			if( user.getDisplayName() == null ) user.setDisplayName( oldUser.getDisplayName() );

			if( user.getPwd() == null ) user.setPwd( oldUser.getPwd() );

			user.setDomain(this.domain);
			//user.setName(name);

			synchronized (users){
				users.put(name, user);
			}

			return Result.ok(user);

		} else {
			return Result.error( ErrorCode.BAD_REQUEST );
		}
	}

	@Override
	public Result<User> deleteUser(String name, String pwd) {
		Log.info("deleteUser: name=" + name + " ; pwd=" + pwd);

		if( name == null || pwd == null ) {
			Log.info("Username or password nil.");
			return Result.error(ErrorCode.BAD_REQUEST);
		}

		User user;

		synchronized ( users ) {
			user = users.get(name);
		}

		if( user == null ) {
			Log.info("User does not exist.");
			return Result.error( ErrorCode.NOT_FOUND);
		}

		if( !user.getPwd().equals( pwd)) {
			Log.info("Password is incorrect.");
			return Result.error( ErrorCode.FORBIDDEN);
		}

		synchronized ( users ) {
			users.remove(name);
		}

		var server = this.getMyFeedsServer();
		server.removeFeed( Formatter.makeUserAddress(name, this.domain) );
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

	@Override
	public Result<Void> verifyPassword(String name, String pwd) {
		var res = getUser(name, pwd);
		if( res.isOK() )
			return Result.ok();
		else
			return Result.error( res.error() );
	}


	private Feeds getMyFeedsServer(){
		var ds = Discovery.getInstance();
		URI[] serverURIs = ds.knownUrisOf(Formatter.getServiceID(this.domain, Formatter.FEEDS_SERVICE), 1);
		return ClientFactory.getFeedsClient(serverURIs[0]);
	}

}
