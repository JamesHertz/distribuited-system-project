package sd2223.trab2.servers.proxy;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.reflect.TypeToken;
import sd2223.trab2.api.Message;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.java.Result;
import sd2223.trab2.utils.JSON;

import java.util.List;

import static sd2223.trab2.api.java.Result.ErrorCode.INTERNAL_ERROR;
import static sd2223.trab2.api.java.Result.ErrorCode.NOT_IMPLEMENTED;
import static sd2223.trab2.api.java.Result.error;
import static sd2223.trab2.api.java.Result.ok;

public class Mastodon implements Feeds {
	
	static String MASTODON_NOVA_SERVER_URI = "http://10.170.138.52:3000";

	static String MASTODON_SERVER_URI = MASTODON_NOVA_SERVER_URI;
	
	private static final String clientKey = "wrByLw0MomgtlxIsrPq3cuh1O0zTfbTu1Mb9GqUlB4A";
	private static final String clientSecret = "sKCdrhT48_mYRDo-G00vcUxZjS2QAKEqoncm6t3-Cg4";
	private static final String accessTokenStr = "IGfNFsn2KhsJE2iUbFLPzNU8CHuQ7_3FJxecydOyAn0";

	static final String STATUSES_PATH= "/api/v1/statuses";
	static final String STATUSES_ID_PATH = STATUSES_PATH +"/%d";
	static final String TIMELINES_PATH = "/api/v1/timelines/home";
	static final String ACCOUNT_FOLLOWING_PATH = "/api/v1/accounts/%s/following";
	static final String VERIFY_CREDENTIALS_PATH = "/api/v1/accounts/verify_credentials";
	static final String SEARCH_ACCOUNTS_PATH = "/api/v1/accounts/search";
	static final String ACCOUNT_FOLLOW_PATH = "/api/v1/accounts/%s/follow";
	static final String ACCOUNT_UNFOLLOW_PATH = "/api/v1/accounts/%s/unfollow";
	
	private static final int HTTP_OK = 200;

	protected OAuth20Service service;
	protected OAuth2AccessToken accessToken;

	private static Mastodon impl;
	
	protected Mastodon() {
		try {
			service = new ServiceBuilder(clientKey).apiSecret(clientSecret).build(MastodonApi.instance());
			accessToken = new OAuth2AccessToken(accessTokenStr);
		} catch (Exception x) {
			x.printStackTrace();
			System.exit(0);
		}
	}

	synchronized public static Mastodon getInstance() {
		if (impl == null)
			impl = new Mastodon();
		return impl;
	}

	private String getEndpoint(String path, Object ... args ) {
		var fmt = MASTODON_SERVER_URI + path;
		return String.format(fmt, args);
	}
	
	@Override
	public Result<Long> postMessage(String user, String pwd, Message msg) {
		try {
			final OAuthRequest request = new OAuthRequest(Verb.POST, getEndpoint(STATUSES_PATH));

			JSON.toMap( new PostStatusArgs(msg.getText())).forEach( (k, v) -> {
				request.addBodyParameter(k, v.toString());
			});
			
			service.signRequest(accessToken, request);

			Response response = service.execute(request);
			if (response.getCode() == HTTP_OK) {
				var res = JSON.decode(response.getBody(), PostStatusResult.class);
				return ok(res.getId());
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		return error(INTERNAL_ERROR);
	}

	@Override
	public Result<List<Message>> getMessages(String user, long time) {
		try {
			final OAuthRequest request = new OAuthRequest(Verb.GET, getEndpoint(TIMELINES_PATH));

			service.signRequest(accessToken, request);

			Response response = service.execute(request);

			if (response.getCode() == HTTP_OK) {
				List<PostStatusResult> res = JSON.decode(response.getBody(), new TypeToken<List<PostStatusResult>>() { });
				return ok(res.stream().map(PostStatusResult::toMessage).toList());
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		return error(INTERNAL_ERROR);
	}

	@Override
	public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
		try{
			final var request = new OAuthRequest(Verb.DELETE, getEndpoint(STATUSES_ID_PATH, mid));
			service.signRequest(accessToken, request);
			var res = service.execute(request);
			if( res.getCode() == HTTP_OK ){
				System.out.println(res.getBody());
				return ok();
			}
		}catch (Exception x){
			x.printStackTrace();
		}
		return error(INTERNAL_ERROR);
	}

	@Override
	public Result<Message> getMessage(String user, long mid) {
		try{
			final var request = new OAuthRequest(Verb.GET, getEndpoint(STATUSES_ID_PATH, mid));
			service.signRequest(accessToken, request);
			var res = service.execute(request);
			if( res.getCode() == HTTP_OK ){
				var aux = JSON.decode(res.getBody(), PostStatusResult.class);
				return ok(aux.toMessage());
			}
		}catch (Exception x){
			x.printStackTrace();
		}
		return error(INTERNAL_ERROR);
	}

	// need to do thins here :)

	@Override
	public Result<Void> unSubscribeUser(String user, String userSub, String pwd) {
		return null;
	}

	@Override
	public Result<Void> subscribeUser(String user, String userSub, String pwd) {
		return error(NOT_IMPLEMENTED);
	}

	@Override
	public Result<List<String>> listSubs(String user) {
		return error(NOT_IMPLEMENTED);
	}

	@Override
	public Result<List<Message>> subscribeServer(String domain, String user, String secret) {
		return null;
	}

	@Override
	public Result<Void> createFeed(String user, String secret) {
		return null;
	}

	@Override
	public Result<Void> createExtFeedMessage(String user, String secret, Message msg) {
		return null;
	}

	@Override
	public Result<Void> removeExtFeedMessage(String user, long mid, String secret) {
		return null;
	}

	@Override
	public Result<Void> removeFeed(String user, String secret) {
		return null;
	}

	@Override
	public Result<Void> removeExtFeed(String user, String secret) {
		return null;
	}

	@Override
	public Result<Void> unsubscribeServer(String domain, String user, String secret) {
		return null;
	}


	// other things :)
	private record MastodonAccount(String id, String username) { };
	private record PostStatusArgs(String status, String visibility) {

		public PostStatusArgs(String msg) {
			this(msg, "private");
		}
	}
	private record PostStatusResult(String id, String content, String created_at, MastodonAccount account) {

		public long getId() {
			return Long.valueOf(id);
		}

		long getCreationTime() {
			return 0;
		}

		public String getText() {
			return content;
		}

		public Message toMessage() {
			var m = new Message( getId(), "61177", "fct.unl.pt", getText());
			m.setCreationTime( getCreationTime() );
			return m;
		}
	}
}
