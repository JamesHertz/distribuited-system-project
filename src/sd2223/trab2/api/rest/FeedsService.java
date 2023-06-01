package sd2223.trab2.api.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import sd2223.trab2.api.Message;

import java.util.List;

@Path(FeedsService.PATH)
public interface FeedsService {
	
	String MID = "mid";
	String PWD = "pwd";
	String USER = "user";
	String TIME = "time";
	String DOMAIN = "domain";
	String USERSUB = "userSub";
	String SERVERSUB = "sub/server";
	String EXTERNAL = "external";
	String PATH = "/feeds";
	String SECRET = "secret";
	String HEADER_VERSION = "X-FEEDS-version";

	/**
	 * Posts a new message in the feed, associating it to the feed of the specific user.
	 * A message should be identified before publish it, by assigning an ID.
	 * A user must contact the server of her domain directly (i.e., this operation should not be
	 * propagated to other domain)
	 *
	 * @param version
	 * @param user    user of the operation (format user@domain)
	 * @param pwd     password of the user sending the message
	 * @param msg     the message object to be posted to the server
	 * @return 200 the unique numerical identifier for the posted message;
	 * 404 if the publisher does not exist in the current domain
	 * 403 if the pwd is not correct
	 * 400 otherwise
	 */
	@POST
	@Path("/{" + USER + "}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	long postMessage(@HeaderParam(HEADER_VERSION) Long version, @PathParam(USER) String user, @QueryParam(PWD) String pwd, Message msg);

	/**
	 * Removes the message identified by mid from the feed of user.
	 * A user must contact the server of her domain directly (i.e., this operation should not be
	 * propagated to other domain)
	 *
	 * @param version
	 * @param user    user feed being accessed (format user@domain)
	 * @param mid     the identifier of the message to be deleted
	 * @param pwd     password of the user
	 * @return 204 if ok
	 * 403 if the pwd is not correct
	 * 404 is generated if the message does not exist in the server or if the user does not exist
	 */
	@DELETE
	@Path("/{" + USER + "}/{" + MID + "}")
	void removeFromPersonalFeed(@HeaderParam(HEADER_VERSION) Long version, @PathParam(USER) String user, @PathParam(MID) long mid, @QueryParam(PWD) String pwd);

	/**
	 * Obtains the message with id from the feed of user (may be a remote user)
	 *
	 * @param version
	 * @param user    user feed being accessed (format user@domain)
	 * @param mid     id of the message
	 * @return 200 the message if it exists;
	 * 404 if the user or the message does not exists
	 */
	@GET
	@Path("/{" + USER + "}/{" + MID + "}")
	@Produces(MediaType.APPLICATION_JSON)
	Message getMessage(@HeaderParam(HEADER_VERSION) Long version, @PathParam(USER) String user, @PathParam(MID) long mid);

	/**
	 * Returns a list of all messages stored in the server for a given user newer than time
	 * (note: may be a remote user)
	 *
	 * @param version
	 * @param user    user feed being accessed (format user@domain)
	 * @param time    the oldest time of the messages to be returned
	 * @return 200 a list of messages, potentially empty;
	 * 404 if the user does not exist.
	 */
	@GET
	@Path("/{" + USER +"}")
	@Produces(MediaType.APPLICATION_JSON)
	List<Message> getMessages(@HeaderParam(HEADER_VERSION) Long version, @PathParam(USER) String user, @QueryParam(TIME) long time);

	/**
	 * Subscribe a user.
	 * A user must contact the server of her domain directly (i.e., this operation should not be
	 * propagated to other domain)
	 *
	 * @param version
	 * @param user    the user subscribing (following) other user (format user@domain)
	 * @param userSub the user to be subscribed (followed) (format user@domain)
	 * @param pwd     password of the user
	 * @return 204 if ok
	 * 404 is generated if the user or the user to be subscribed does not exist
	 * 403 is generated if the pwd is not correct
	 */
	@POST
	@Path("/sub/{" + USER + "}/{" + USERSUB + "}")
	void subUser(@HeaderParam(HEADER_VERSION) Long version, @PathParam(USER) String user, @PathParam(USERSUB) String userSub, @QueryParam(PWD) String pwd);

	/**
	 * UnSubscribe a user
	 * A user must contact the server of her domain directly (i.e., this operation should not be
	 * propagated to other domain)
	 *
	 * @param version
	 * @param user    the user unsubscribing (following) other user (format user@domain)
	 * @param userSub the identifier of the user to be unsubscribed
	 * @param pwd     password of the user
	 * @return 204 if ok
	 * 404 is generated if the user or the user to be unsubscribed does not exist
	 * 403 is generated if the pwd is not correct
	 */
	@DELETE
	@Path("/sub/{" + USER + "}/{" + USERSUB + "}")
	void unsubscribeUser(@HeaderParam(HEADER_VERSION) Long version, @PathParam(USER) String user, @PathParam(USERSUB) String userSub, @QueryParam(PWD) String pwd);

	/**
	 * Subscribed users.
	 *
	 * @param version
	 * @param user    user being accessed (format user@domain)
	 * @return 200 if ok
	 * 404 is generated if the user does not exist
	 */
	@GET
	@Path("/sub/list/{" + USER + "}")
	@Produces(MediaType.APPLICATION_JSON)
	List<String> listSubs(@HeaderParam(HEADER_VERSION) Long version, @PathParam(USER) String user);

	// FROM HERE ONE THE LIST OF OPERATIONS WE'VE ADDED ;)

	/**
	 * Creates a feed for a user in the feeds server.
	 * The user domain should be the same as the feeds.
	 * @param user the user address of the user to be created (user@domain)
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	void createFeed( String user , @QueryParam(SECRET) String secret);

	/**
	 * Deletes the feeds of the user that belongs to this domain (we've named it LocalUser)
	 * @param user the user address  (user@domain)
	 * @returns 200 if ok,
	 *          404 if the user doesn't exist,
	 *          400 if the user address is invalid or if the domain,
	 *              of the address is not the same as of this server
	 */
	@DELETE
	@Path("/{" + USER + "}")
	void removeFeed(@PathParam(USER) String user, @QueryParam(SECRET) String secret);

	/**
	 * Subscribe an outsider feeds server to a user of the receiving feeds.
	 * This means this server will be notified whenever a message is posted
	 * in this user feeds.
	 * @param domain the domain of the server
	 * @param user user to subscribe
	 * @returns 200 a list with all the messages of the user so far,
	 *          404 if the user doesn't exist,
	 *          400 if the user address is invalid o if it's domain is the same as this servers.
	 *
	 */
	@POST
	@Path("/" + SERVERSUB + "/{" + DOMAIN + "}/{" + USER + "}")
	@Produces(MediaType.APPLICATION_JSON)
	List<Message> subscribeServer( @PathParam(DOMAIN) String domain, @PathParam(USER) String user, @QueryParam(SECRET) String secret);

	/**
	 *  Unsubscribe a server from a user.  (This means this server will stop receiving the
	 *  messages that this user posts)
	 *
	 * @param domain domain of the feeds server
	 * @param user user address (user@domain)
	 *
	 */
	@DELETE
	@Path("/" + SERVERSUB + "/{" + DOMAIN + "}/{" + USER + "}")
	void unsubscribeServer( @PathParam(DOMAIN) String domain, @PathParam(USER) String user,  @QueryParam(SECRET) String secret);

	/**
	 * Creates a message on the external user feed ( cache of the feed of a user that
	 * doesn't belong to this domain)
	 * @param user user address (user@domain)
	 * @param msg message id
	 * @returns 200 if ok
	 *          404 if the user does exist
	 *          400 if the user address is invalid or if the domain in it is the same as
	 *              the one of this feeds server
	 */
	@POST
	@Path(EXTERNAL + "/{" + USER +"}")
	@Consumes(MediaType.APPLICATION_JSON)
	void createExtFeedMessage(@PathParam(USER) String user,  @QueryParam(SECRET) String secret, Message msg);

	/**
	 *  Removes a message from the external user feed (cache of the feed of a user that
	 *  doesn't long to the domain of this feed server)
	 * @param user user address (user@domain)
	 * @param mid message id
	 * @returns  200 if ok,
	 *           404 if the user or the message doesn't,
	 *           400 if user address is invalid of if the domain belongs
	 *               is the same as this feed server domain
	 */
	@DELETE
	@Path(EXTERNAL + "/{" + USER +"}/{" + MID + "}")
	void removeExtFeedMessage(@PathParam(USER) String user, @PathParam(MID) long mid,  @QueryParam(SECRET) String secret);


	/**
	 * Removes the feed of an external user (cache of the feed of a user that doesn't
	 * belong to this domain) and removes it from the subscription list of all the users
	 * of this domain that are subscribed at it.
	 *
	 * @param user user address (user@domain)
	 * @returns 200 if ok
	 *          404 if the user doesn't exist
	 *          400 if the user address is invalid or if the domain of the address is the same as this.
	 */
	@DELETE
	@Path(EXTERNAL + "/{" + USER + "}")
	void removeExtFeed(@PathParam(USER) String user,  @QueryParam(SECRET) String secret);

}
