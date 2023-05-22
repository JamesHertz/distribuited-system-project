package sd2223.trab2.api.soap;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import sd2223.trab2.api.Message;

import java.util.List;

@WebService(serviceName=FeedsService.NAME, targetNamespace=FeedsService.NAMESPACE, endpointInterface=FeedsService.INTERFACE)
public interface FeedsService {

	static final String NAME = "feeds";
	static final String NAMESPACE = "http://sd2223";
	static final String INTERFACE = "sd2223.trab2.api.soap.FeedsService";
	
	/**
	 * Posts a new message in the feed, associating it to the feed of the specific user.
	 * A message should be identified before publish it, by assigning an ID.
	 * A user must contact the server of her domain directly (i.e., this operation should not be
	 * propagated to other domain)
	 *
	 * @param user user of the operation (format user@domain)
	 * @param msg the message object to be posted to the server
	 * @param pwd password of the user sending the message
	 * @return	the unique numerical identifier for the posted message;
	 * @throws 	FORBIDDEN if the publisher does not exist in the current domain or if the pwd is not correct
	 *			BAD_REQUEST otherwise
	 */
	@WebMethod
	long postMessage(String user, String pwd, Message msg) throws FeedsException;

	/**
	 * Removes the message identified by mid from the feed of user.
	 * A user must contact the server of her domain directly (i.e., this operation should not be
	 * propagated to other domain)
	 * 
	 * @param user user feed being accessed (format user@domain)
	 * @param mid the identifier of the message to be deleted
	 * @param pwd password of the user
	 * @throws	FORBIDDEN if the publisher does not exist in the current domain or if the pwd is not correct
	 *			BAD_REQUEST otherwise
	 */
	@WebMethod
	void removeFromPersonalFeed(String user, long mid, String pwd) throws FeedsException;

	/**
	 * Obtains the message with id from the feed of user (may be a remote user)
	 * 
	 * @param user user feed being accessed (format user@domain)
	 * @param mid id of the message
	 *
	 * @return	the message if it exists;
	 * @throws	NOT_FOUND	if the user or the message does not exist
	 */
	@WebMethod
	Message getMessage(String user, long mid) throws FeedsException;

	/**
	 * Returns a list of all messages stored in the server for a given user newer than time
	 * (note: may be a remote user)
	 * 
	 * @param user user feed being accessed (format user@domain)
	 * @param time the oldest time of the messages to be returned
	 * @return	a list of messages, potentially empty;
	 * @throws 	NOT_FOUND if the user does not exist.
	 */
	@WebMethod
	List<Message> getMessages(String user, long time) throws FeedsException;

	/**
	 * Subscribe a user.
	 * A user must contact the server of her domain directly (i.e., this operation should not be
	 * propagated to other domain)
	 *
	 * @param user the user subscribing (following) other user (format user@domain)
	 * @param userSub the user to be subscribed (followed) (format user@domain)
	 * @param pwd password of the user to subscribe
	 * @throws	NOT_FOUND if the user to be subscribed does not exist
	 * 			FORBIDDEN if the user does not exist or if the pwd is not correct
	 */
	@WebMethod
	void subUser(String user, String userSub, String pwd) throws FeedsException;

	/**
	 * UnSubscribe a user
	 * A user must contact the server of her domain directly (i.e., this operation should not be
	 * propagated to other domain)
	 *
	 * @param user the user unsubscribing (following) other user (format user@domain)
	 * @param userSub the identifier of the user to be unsubscribed
	 * @param pwd password of the user to subscribe
	 * @throws FORBIDDEN if the user does not exist or if the pwd is not correct
	 * 		   NOT_FOUND the userSub is not subscribed
	 */
	@WebMethod
	void unsubscribeUser(String user, String userSub, String pwd) throws FeedsException;

	/**
	 * Subscribed users.
	 *
	 * @param user user being accessed (format user@domain)
	 * @throws 	NOT_FOUND if the user does not exist
	 */
	@WebMethod
	List<String> listSubs(String user) throws FeedsException;


	// FROM HERE ON THE METHODS WE'VE ADDED.
	// NOTE: they are equivalent to the rest ones.

	/**
	 *  A feeds server subscribe to an user. (This means it will receive all the new messages
	 *  of such user through createExtFeedMessage RPC)
	 *
	 * @param domain the one to subscribe
	 * @param user the one to subscribe to
	 * @return the list of all the message of the user
	 * @throws  NOT_FOUND if the user does't exist
	 *          BAD_REQUEST if the user address is invalid
	 */
	@WebMethod
	List<Message>subscribeSever(String domain, String user, String secret) throws FeedsException;

	/**
	 * Unsubscribe the server from a user
	 * @param domain the sever domain
	 * @param user the user address (user@domain)
	 * @throws NOT_FOUND if the user doesn't exist
	 *         BAD_REQUEST if the user address is invalid
	 */
	@WebMethod
	 void unsubscribeServer(String domain, String user, String secret) throws FeedsException;

	/**
	 * Creates a feed for a user in the feeds server.
	 * The user domain should be the same as the feeds.
	 * @param user user which feeds is created (format user@domain)
	 * @throws BAD_REQUEST if the user address is invalid or the user doesn't belong to this server
	 */
	@WebMethod
	void createFeed(String user, String secret)  throws FeedsException;

	/**
	 * Remove a feed
	 * @param user user address
	 * @throws NOT_FOUND if the user doesn't exist
	 *         BAD_REQUEST if the user doesn't belong to this server
	 */
	@WebMethod
	void removeFeed(String user, String secret)  throws FeedsException;

	/**
	 * Removes an feed of an external user (the cache of the user that doesn't belong to this server)
	 * along with all subscriptions on it.
	 * @param user user address (user@domain)
	 * @throws NOT_FOUND if the user doesn't exist
	 *         BAD_REQUEST if the address is invalid
	 */
	@WebMethod
	void removeExtFeed(String user, String secret) throws FeedsException;

	/**
	 *  Creates a message in the cache feed of a user that doesn't belong to this server
	 * @param user the user address (user@domain)
	 * @param msg the new message
	 * @throws NOT_FOUND if the user doesn't exist
	 *         BAD_REQUEST if the user address is invalid
	 */
	@WebMethod
	void createExtFeedMessage(String user, String secret, Message msg) throws FeedsException;

	/**
	 * Removes a message from the cache feed of a user that doesn't belong to this server
	 * @param user user address (user@domain)
	 * @param mid message id
	 * @throws NOT_FOUND if the user doesn't exist
	 *         BAD_REQUEST if the user address is invalid
	 */
	@WebMethod
	void removeExtFeedMessage(String user, long mid, String secret) throws FeedsException;

}
