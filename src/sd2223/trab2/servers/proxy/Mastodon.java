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

	// Mastodon URL
	static String MASTODON_NOVA_SERVER_URI   = "http://10.170.138.52:3000";
	static String MASTODON_SOCIAL_SERVER_URI = "https://mastodon.social/";
	static String MASTODON_SERVER_URI        =  MASTODON_NOVA_SERVER_URI;

	// APIs keys
	/*
	// social mastodon keys
	private static final String clientKey = "Ptf9io1AU8nBV1rJyO3dzNqAuDDrRNDwpQ3o9VS1Kl8";
	private static final String clientSecret = "XGB_flbn_AAHNcFEx9nc_vMofcHWk-pVdiw5nJe7rHg";
	private static final String accessTokenStr = "NHpqkU29rL--mKcNwh7E6QVxYPwutQuyoK2xoTACrmo";
	 */


	// profs mastodon keys
	private static final String clientKey      = "wrByLw0MomgtlxIsrPq3cuh1O0zTfbTu1Mb9GqUlB4A";
	private static final String clientSecret   = "sKCdrhT48_mYRDo-G00vcUxZjS2QAKEqoncm6t3-Cg4";
	private static final String accessTokenStr = "ItbQPXfq0eDQqNnsNIfsmqXAuAxdd2hFcFEOK9WWCmQ";

	// APIs paths
	static final String STATUSES_PATH               = "/api/v1/statuses";
	static final String STATUSES_ID_PATH            = STATUSES_PATH +"/%d";
	static final String TIMELINES_PATH              = "/api/v1/timelines/home";
	static final String ACCOUNT_FOLLOWING_PATH      = "/api/v1/accounts/%s/following";
	static final String VERIFY_CREDENTIALS_PATH     = "/api/v1/accounts/verify_credentials";
	static final String SEARCH_ACCOUNTS_PATH        = "/api/v1/accounts/search";
	static final String ACCOUNT_FOLLOW_PATH         = "/api/v1/accounts/%s/follow";
	static final String ACCOUNT_UNFOLLOW_PATH       = "/api/v1/accounts/%s/unfollow";
	static final String SEARCH_ACCOUNTS_QUERY_PARAM = "q";

	// useful things :)
	private static final Logger Log = Logger.getLogger(Mastodon.class.getName());
	private static final int HTTP_OK = 200;
	private static final String USER_NAME = "61177";
	private static final String DEFAULT_MASTODON_DOMAIN     = "mastodon.social";


	// local variables
	private final OAuth20Service service;
	private final OAuth2AccessToken accessToken;
	private final String domain;
	private final AtomicBoolean userExists;
	private final long accountID; // this user id

	public Mastodon(String domain) {
		this.service = new ServiceBuilder(clientKey).apiSecret(clientSecret).build(MastodonApi.instance());
		this.accessToken = new OAuth2AccessToken(accessTokenStr);
		this.domain = domain;
		this.userExists = new AtomicBoolean(false);
		this.accountID = 110321412226138318L; //this.getAccountID(); // by now (TODO: change Clients to receive clientID)
		Log.info("accountID=" + accountID);
	}

	private long getAccountID(){
		try{
			final OAuthRequest request = new OAuthRequest(Verb.GET, getEndpoint(VERIFY_CREDENTIALS_PATH));
			synchronized (service){
				service.signRequest(accessToken, request);
			}
			var res = service.execute(request);
			if(res.getCode() == HTTP_OK){
				var account = JSON.decode(res.getBody(), MastodonAccount.class);
				return account.accountID();
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
		var err = checkUserAndPassword(user, pwd);
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
		if( this.badLocalUserAddress(addr) ) {
			Log.severe("Bad address.");
			return Result.error( ErrorCode.BAD_REQUEST );
		}

		if(! userExists(addr.username()) ){
			Log.severe("User doesn't exist.");
			return Result.error( ErrorCode.NOT_FOUND );
		}

		try {
			final OAuthRequest request = new OAuthRequest(Verb.GET, getEndpoint(TIMELINES_PATH));
			synchronized (service){
				service.signRequest(accessToken, request);
			}
			Response response = service.execute(request);
			if (response.getCode() == HTTP_OK) {
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
		var aux = checkUserAndPassword(user, pwd);
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
		if( this.badLocalUserAddress(addr) ) {
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

		var subAddr = this.validateSubUnsubParameters(user, userSub, pwd);
		if(!subAddr.isOK()) return Result.error( subAddr.error() );

		var addr = subAddr.value();
		var subUserID = this.searchAccountID(addr.username());

		if(!subUserID.isOK()){
			Log.severe("Error getting subUserID");
			return Result.error( subUserID.error() );
		}

		try{
			var request = new OAuthRequest(Verb.POST, getEndpoint(ACCOUNT_FOLLOW_PATH, subUserID.value()));
			synchronized (service){
				service.signRequest(accessToken, request);
			}
			var res = service.execute(request);
			if( res.getCode() == HTTP_OK ){
				Log.info("Subscription performed.");
				return Result.ok();
			}
			Log.severe(res.getBody());
			return Result.error( res.getCode() );
		}catch (Exception e){
			e.printStackTrace();
			return Result.error( ErrorCode.INTERNAL_ERROR );
		}
	}

	@Override
	public Result<Void> unSubscribeUser(String user, String userSub, String pwd) {
		Log.info(String.format("unSubscribeUser: user=%s ; userSub=%s ; pwd=%s", user, userSub, pwd));
		var check = this.validateSubUnsubParameters(user, userSub, pwd);

		// verify if parameters are ok
		if(!check.isOK()) return Result.error( check.error() );
		var subAddr = check.value();

		// getting subs list
		var subs = this.getSubList();
		if(!subs.isOK()) return Result.error( subs.error() );

		var userSubID = subs.value().stream()
				.filter(acc -> acc.acct.equals(userSub) || acc.acct.equals(subAddr.username()))
				.map(MastodonAccount::accountID)
				.findFirst();

		if(userSubID.isEmpty()) {
			Log.severe("Subscription doesn't exist!");
			return Result.error( ErrorCode.NOT_FOUND );
		}

		try{
			var request = new OAuthRequest(Verb.POST, getEndpoint(ACCOUNT_UNFOLLOW_PATH, userSubID.get()));
			synchronized (service) {
				service.signRequest(accessToken, request);
			}
			var res = service.execute(request);
			if(res.getCode() == HTTP_OK){
				Log.info("Unfollow performed.");
				return Result.ok();
			}

			Log.severe( res.getBody() );
			return Result.error( res.getCode() );
		}catch (Exception e){
			e.printStackTrace();
			return Result.error( ErrorCode.INTERNAL_ERROR );
		}

	}


	@Override
	public Result<List<String>> listSubs(String user) {
		Log.info(String.format("listSubs: user=%s ", user));

		var addr = Formatter.getUserAddress(user);
		if( badLocalUserAddress(addr) ){
			Log.severe("Bad request.");
			return Result.error(ErrorCode.BAD_REQUEST);
		}

		if(! userExists( addr.username() )){
			Log.severe("User not found.");
			return Result.error(ErrorCode.NOT_FOUND);
		}

		var res = this.getSubList();
		return res.isOK() ? Result.ok(
					res.value().stream().map(MastodonAccount::acct).toList()
		) : Result.error( res.error() );
	}

	@Override
	public Result<Void> createFeed(String user, String secret) {
		Log.info(String.format("createFeed: user=%s ; secret=%s", user, secret));

		if(! Secret.getSecret().equals(secret) ){
			Log.severe("Wrong secret, cannot perform operation");
			return Result.error( ErrorCode.FORBIDDEN );
		}

		var addr = Formatter.getUserAddress(user);
		if( badLocalUserAddress(addr) || !USER_NAME.equals(addr.username()) ){
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


	private Result<List<MastodonAccount>> getSubList(){
		try{
			final var request = new OAuthRequest(Verb.GET, getEndpoint(ACCOUNT_FOLLOWING_PATH, accountID));
			synchronized (service){
				service.signRequest(accessToken, request);
			}
			var res = service.execute(request);
			if(res.getCode() == HTTP_OK){
				return Result.ok(
						JSON.decode( res.getBody(), new TypeToken<List<MastodonAccount>>() { } )
				);
			}
			Log.severe(res.getBody());
			return Result.error( res.getCode() );
		}catch (Exception e){
			e.printStackTrace();
			return Result.error(ErrorCode.INTERNAL_ERROR);
		}
	}
	private Result<UserAddress> validateSubUnsubParameters(String user, String userSub, String pwd){
		var localAddr = Formatter.getUserAddress(user);
		var subAddr   = Formatter.getUserAddress(userSub);
		if( pwd == null || subAddr == null || user.equals(userSub) || badLocalUserAddress(localAddr) ){
			Log.severe("Bad user address.");
			return Result.error( ErrorCode.BAD_REQUEST );
		}

		if( !this.userExists(localAddr.username()) || subAddr.domain().equals(this.domain) ){
			Log.severe("One of user doesn't exists.");
			return Result.error( ErrorCode.NOT_FOUND);
		}

		var aux = this.checkPassword(pwd);
		if( !aux.isOK() ){
			Log.severe("Wrong credentials.");
			return Result.error( aux.error() );
		}

		return Result.ok( subAddr );
	}

	// my private methods
	private Result<Void> checkUsersServerRequest(String user, String secret){
		var realSecret = Secret.getSecret();
		if(!realSecret.equals(secret)) {
			Log.severe("Wrong secret.");
			return Result.error( ErrorCode.FORBIDDEN );
		}

		var address = Formatter.getUserAddress(user);
		if( badLocalUserAddress(address) ){
			Log.severe("Bad request.");
			return Result.error(ErrorCode.BAD_REQUEST);
		}

		if( ! this.userExists(address.username()) ){
			Log.severe("User not found.");
			return Result.error( ErrorCode.NOT_FOUND );
		}

		return Result.ok();
	}

	private Result<Void> checkUserAndPassword(String user, String pwd){
		var addr = Formatter.getUserAddress(user);
		if( pwd == null || this.badLocalUserAddress( addr ) ){
			Log.severe("Bad address.");
			return Result.error( ErrorCode.BAD_REQUEST );
		}

		if( !this.userExists( addr.username() ) ){
			Log.severe("User not found.");
			return Result.error( ErrorCode.NOT_FOUND );
		}

		return this.checkPassword( pwd );
	}

	private Result<Void> checkPassword(String pwd){
		// TODO: add error here :)
		Log.info("serviceID=" + Formatter.getServiceID(this.domain, Formatter.USERS_SERVICE));

		var uris = Discovery.getInstance().knownUrisOf(
				Formatter.getServiceID(this.domain, Formatter.USERS_SERVICE),
				1
		);
		var server = ClientFactory.getUsersClient( uris[0] );
		return server.verifyPassword(USER_NAME, pwd);
	}

	private Message toMessage(PostStatusResult res){
		var addr = Formatter.getUserAddress(res.account.acct);
		if(addr == null) {
			addr = new UserAddress(
					res.account.acct(),
					res.account.acct().equals(USER_NAME) ?  this.domain : DEFAULT_MASTODON_DOMAIN
			);
		}
		var m = new Message( res.getId(), addr.username(), addr.domain(), res.getText());
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

	private boolean badLocalUserAddress(UserAddress addr){
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

	private Result<Long> searchAccountID(String user){

		try{
			final var request = new OAuthRequest(Verb.GET, getEndpoint(SEARCH_ACCOUNTS_PATH));
			request.addQuerystringParameter(SEARCH_ACCOUNTS_QUERY_PARAM, user);
			synchronized (service){
				service.signRequest(accessToken, request);
			}

			var res = service.execute(request);
			if( res.getCode()  == HTTP_OK ){
				List<MastodonAccount> accounts = JSON.decode(res.getBody(), new TypeToken<List<MastodonAccount>>() { });
				Log.info("searchAccountID: acc=" + accounts.toString());

				return accounts.isEmpty() ? Result.error( ErrorCode.NOT_FOUND ) : Result.ok( accounts.get(0).accountID() );
			}
			Log.info(res.getBody());
			return Result.error( res.getCode() );
		}catch (Exception e){
			e.printStackTrace();
			return Result.error( ErrorCode.INTERNAL_ERROR );
		}

	}

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


	private record MastodonAccount(String id, String acct, String display_name){
		public long accountID(){
			return Long.parseLong(this.id);
		}
	}

}