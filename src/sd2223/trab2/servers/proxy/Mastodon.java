package sd2223.trab2.servers.proxy;

import java.time.ZonedDateTime;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import sd2223.trab2.api.Message;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.java.Result;
import sd2223.trab2.clients.ClientFactory;
import sd2223.trab2.discovery.Discovery;
import sd2223.trab2.utils.Formatter;
import static sd2223.trab2.utils.Formatter.UserAddress;
import sd2223.trab2.utils.JSON;
import sd2223.trab2.utils.Secret;

import static sd2223.trab2.api.java.Result.ErrorCode;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class Mastodon implements Feeds {

	// urls :)
	static String MASTODON_NOVA_SERVER_URI = "http://10.170.138.52:3000";
	static String MASTODON_SOCIAL_SERVER_URI = "https://mastodon.social/";
	static String MASTODON_SERVER_URI =  MASTODON_NOVA_SERVER_URI;

	// APIs keys

	/*
	// social mastodon keys
	private static final String clientKey = "Ptf9io1AU8nBV1rJyO3dzNqAuDDrRNDwpQ3o9VS1Kl8";
	private static final String clientSecret = "XGB_flbn_AAHNcFEx9nc_vMofcHWk-pVdiw5nJe7rHg";
	private static final String accessTokenStr = "uHYGEW4R2ODaAH8Tly_l_230kxpLcVb5AdMDhuqaaWQ";
	 */


	// profs mastodon keys
	private static final String clientKey = "wrByLw0MomgtlxIsrPq3cuh1O0zTfbTu1Mb9GqUlB4A";
	private static final String clientSecret = "sKCdrhT48_mYRDo-G00vcUxZjS2QAKEqoncm6t3-Cg4";
	private static final String accessTokenStr = "Tl5gfuLz_B-TO0V_sA4c-i4VYcI1aIg9i6zvLgjIErM";

	// APIs paths
	static final String STATUSES_PATH= "/api/v1/statuses";
	static final String STATUSES_ID_PATH = STATUSES_PATH +"/%d";
	static final String TIMELINES_PATH = "/api/v1/timelines/home";
	static final String ACCOUNT_FOLLOWING_PATH = "/api/v1/accounts/%s/following";
	static final String VERIFY_CREDENTIALS_PATH = "/api/v1/accounts/verify_credentials";
	static final String SEARCH_ACCOUNTS_PATH = "/api/v1/accounts/search";
	static final String ACCOUNT_FOLLOW_PATH = "/api/v1/accounts/%s/follow";
	static final String ACCOUNT_UNFOLLOW_PATH = "/api/v1/accounts/%s/unfollow";
	
	private static final int HTTP_OK = 200;
	private static final String USER_NAME = "61177";
	private static final Logger Log = Logger.getLogger(Mastodon.class.getName());

	private final OAuth20Service service;
	private final OAuth2AccessToken accessToken;
	private final String domain;
	private final AtomicBoolean userExists;

	private final long accountID;

	public Mastodon(String domain) {
		this.service = new ServiceBuilder(clientKey).apiSecret(clientSecret).build(MastodonApi.instance());
		this.accessToken = new OAuth2AccessToken(accessTokenStr);
		this.domain = domain;
		this.userExists = new AtomicBoolean(false);
		this.accountID = this.getAccountID();
	}

	private long getAccountID(){
		try{
			final OAuthRequest request = new OAuthRequest(Verb.GET, getEndpoint(VERIFY_CREDENTIALS_PATH));
			synchronized (service){
				service.signRequest(accessToken, request);
			}
			var res = service.execute(request);
			if(res.getCode() == HTTP_OK){
				var values = JSON.jsonToMap(res.getBody());
				return Long.parseLong( values.get("id").toString() );
			}
			Log.severe(res.getBody());
		}catch (Exception e) {
			e.printStackTrace();
		}
		throw new RuntimeException("Unable to get account ID.");
	}

	@Override
	public Result<Long> postMessage(String user, String pwd, Message msg) {
		Log.info(String.format("postMessage: user=%s ; pwd=%s; msg=%s", user, pwd, msg));
		var err = checkParameters(user, pwd);
		if(!err.isOK()) return Result.error( err.error() );

		try {
			final OAuthRequest request = new OAuthRequest(Verb.POST, getEndpoint(STATUSES_PATH));

			JSON.toMap( new PostStatusArgs(msg.getText())).forEach( (k, v) -> {
				request.addBodyParameter(k, v.toString());
			});

			synchronized (service){
				service.signRequest(accessToken, request);
			}

			Response response = service.execute(request);
			if (response.getCode() == HTTP_OK) {
				var res = JSON.decode(response.getBody(), PostStatusResult.class);
				return Result.ok(res.getId());
			}

			Log.severe(response.getBody());
			return Result.error(response.getCode());
		} catch (Exception x) {
			x.printStackTrace();
			return Result.error(ErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public Result<List<Message>> getMessages(String user, long time) {
		Log.info(String.format("getMessages: user=%s ; time=%d", user, time));
		var addr = Formatter.getUserAddress( user );
		if( this.badAddress(addr) ) {
			Log.severe("Bad address.");
			return Result.error( ErrorCode.BAD_REQUEST );
		}

		try {
			final OAuthRequest request = new OAuthRequest(Verb.GET, getEndpoint(TIMELINES_PATH));
			synchronized (service){
				service.signRequest(accessToken, request);
			}
			Response response = service.execute(request);
			if (response.getCode() == HTTP_OK) {
				// System.out.println("getMessage: " + response.getBody());
				List<PostStatusResult> res = JSON.decode(response.getBody(), new TypeToken<List<PostStatusResult>>() { });
				return Result.ok(
						res.stream()
						   .map(this::toMessage)
						   .filter( m -> m.getCreationTime() > time)
						   .toList()
				);
			}
			Log.severe(response.getBody());
			return Result.error(response.getCode());
		} catch (Exception x) {
			x.printStackTrace();
			return Result.error(Result.ErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
		Log.info(String.format("getMessages: user=%s ; mid=%d; pwd=%s", user, mid, pwd));
		var aux = checkParameters(user, pwd);
		if(!aux.isOK()) return Result.error( aux.error() );

		try{
			final var request = new OAuthRequest(Verb.DELETE, getEndpoint(STATUSES_ID_PATH, mid));
			synchronized (service){
				service.signRequest(accessToken, request);
			}
			var res = service.execute(request);
			if( res.getCode() == HTTP_OK ){
				return Result.ok();
			}
			Log.severe(res.getBody());
			return Result.error( res.getCode() );
		}catch (Exception x){
			x.printStackTrace();
			return Result.error(ErrorCode.INTERNAL_ERROR);
		}

	}

	@Override
	public Result<Message> getMessage(String user, long mid) {
		Log.info(String.format("getMessages: user=%s ; mid=%d", user, mid));
		var addr = Formatter.getUserAddress( user );
		if( this.badAddress(addr) ) {
			Log.severe("Bad address.");
			return Result.error( ErrorCode.BAD_REQUEST );
		}

		try{
			final var request = new OAuthRequest(Verb.GET, getEndpoint(STATUSES_ID_PATH, mid));
			synchronized (service){
				service.signRequest(accessToken, request);
			}
			var res = service.execute(request);
			if( res.getCode() == HTTP_OK ){
				var aux = JSON.decode(res.getBody(), PostStatusResult.class);
				return Result.ok(this.toMessage( aux ));
			}
			Log.severe(res.getBody());
			return Result.error(res.getCode());
		}catch (Exception x){
			x.printStackTrace();
			return Result.error(ErrorCode.INTERNAL_ERROR);
		}
	}


	@Override
	public Result<Void> subscribeUser(String user, String userSub, String pwd) {
		Log.info(String.format("subscribeUser: user=%s ; userSub=%s ; pwd=%s", user, userSub, pwd));
		return Result.error(ErrorCode.NOT_IMPLEMENTED);
	}

	@Override
	public Result<Void> unSubscribeUser(String user, String userSub, String pwd) {
		Log.info(String.format("unSubscribeUser: user=%s ; userSub=%s ; pwd=%s", user, userSub, pwd));
		return Result.error(ErrorCode.NOT_IMPLEMENTED);
	}

	@Override
	public Result<List<String>> listSubs(String user) {
		Log.info(String.format("listSubs: user=%s ", user));

		var addr = Formatter.getUserAddress(user);
		if( badAddress(addr) ){
			Log.severe("Bad request.");
			return Result.error(ErrorCode.BAD_REQUEST);
		}

		if(! userExists( addr.username() )){
			Log.severe("User not found.");
			return Result.error(ErrorCode.NOT_FOUND);
		}

		try{
			final var request = new OAuthRequest(Verb.GET, getEndpoint(ACCOUNT_FOLLOWING_PATH, accountID));
			synchronized (service){
				service.signRequest(accessToken, request);
			}
			var res = service.execute(request);
			if(res.getCode() == HTTP_OK){
				List<FollowersResult> values = JSON.decode(res.getBody(), new TypeToken<List<FollowersResult>>() { });
				return Result.ok(
						values.stream()
								.map(FollowersResult::acct) // TODO: if is from our domain add @domain because it won't have it :)
								.toList()
				);
			}
			Log.severe(res.getBody());
			return Result.error( res.getCode() );
		}catch (Exception e){
			e.printStackTrace();
			return Result.error(ErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public Result<Void> createFeed(String user, String secret) {
		Log.info(String.format("createFeed: user=%s ; secret=%s", user, secret));

		if(! Secret.getSecret().equals(secret) ){
			Log.severe("Wrong secret, cannot perform operation");
			return Result.error( ErrorCode.FORBIDDEN );
		}

		var addr = Formatter.getUserAddress(user);
		if( badAddress(addr) || !USER_NAME.equals(addr.username()) ){
			Log.severe("Bad user address.");
			return Result.error( ErrorCode.BAD_REQUEST );
		}

		userExists.set(true);

		Log.info("Feed created.");
		return Result.ok();
	}

	@Override
	public Result<Void> removeFeed(String user, String secret) {
		Log.info(String.format("createFeed: user=%s ; secret=%s", user, secret));
		var err = this.checkUsersServerRequest(user, secret);
		if( !err.isOK() ) return Result.error( err.error() );
		// TODO: add check to exists
		userExists.set(false);

		Log.info("User removed with success.");
		return Result.ok();
	}


	// my private methods
	private Result<Void> checkUsersServerRequest(String user, String secret){
		var address = Formatter.getUserAddress(user);
		if( badAddress(address) ){
			Log.severe("Bad request.");
			return Result.error(ErrorCode.BAD_REQUEST);
		}

		var realSecret = Secret.getSecret();
		if(!realSecret.equals(secret)) {
			Log.severe("Wrong secret.");
			return Result.error( ErrorCode.FORBIDDEN );
		}

		if( ! this.userExists(address.username()) ){
			Log.severe("User not found.");
			return Result.error( ErrorCode.NOT_FOUND );
		}

		return Result.ok();
	}

	private Result<Void> checkParameters(String user, String pwd){
		var addr = Formatter.getUserAddress(user);
		if( pwd == null || this.badAddress( addr ) ){
			Log.severe("Bad address.");
			return Result.error( ErrorCode.BAD_REQUEST );
		}

		if( !this.userExists( addr.username() ) ){
			Log.severe("User not found.");
			return Result.error( ErrorCode.NOT_FOUND );
		}

		// look at this later :)
		var uris = Discovery.getInstance().knownUrisOf(
				Formatter.getServiceID(this.domain, Formatter.USERS_SERVICE),
				1
		);
		var server = ClientFactory.getUsersClient( uris[0] );
		return server.verifyPassword(addr.username(), pwd);
	}

	private Message toMessage(PostStatusResult res){
		var m = new Message( res.getId(), USER_NAME, this.domain, res.getText());
		m.setCreationTime( res.getCreationTime() );
		return m;
	}

	private boolean userExists(String username){
		return userExists.get() && USER_NAME.equals(username);
	}

	private String getEndpoint(String path, Object ... args ) {
		var fmt = MASTODON_SERVER_URI + path;
		return String.format(fmt, args);
	}

	private boolean badAddress(UserAddress addr){
		return addr == null || !this.domain.equals(addr.domain());
	}

	// others methods :)
	@Override
	public Result<List<Message>> subscribeServer(String domain, String user, String secret) {
		return Result.error(ErrorCode.NOT_IMPLEMENTED);
	}

	@Override
	public Result<Void> createExtFeedMessage(String user, String secret, Message msg) {
		return Result.error(ErrorCode.NOT_IMPLEMENTED);
	}

	@Override
	public Result<Void> removeExtFeedMessage(String user, long mid, String secret) {
		return Result.error(ErrorCode.NOT_IMPLEMENTED);
	}

	@Override
	public Result<Void> removeExtFeed(String user, String secret) {
		return Result.error(ErrorCode.NOT_IMPLEMENTED);
	}

	@Override
	public Result<Void> unsubscribeServer(String domain, String user, String secret) {
		return Result.error( ErrorCode.NOT_IMPLEMENTED );
	}

	private record MastodonAccount(String id, String username) { }
	private record PostStatusArgs(String status, String visibility) {
		public PostStatusArgs(String msg) {
			this(msg, "private");
		}
	}

	private record PostStatusResult(String id, String content, String created_at, MastodonAccount account) {
		public long getId() {
			return Long.parseLong(id);
		}
		long getCreationTime() {
			return ZonedDateTime.parse(created_at)
					.toInstant().toEpochMilli();
		}
		public String getText() {
			return content.replaceAll("<.*?>", "");
		}
	}

	private record FollowersResult(String acct, String display_name){
		// public User toUser(){
		// 	var addr = Formatter.getUserAddress(this.acct);
		// 	return new User(addr.username(), "", addr.domain(), this.display_name);
		// }
	}


}
