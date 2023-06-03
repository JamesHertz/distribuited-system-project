package sd2223.trab2.servers.java;

import sd2223.trab2.api.Message;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.java.Result;
import sd2223.trab2.api.java.Users;
import sd2223.trab2.clients.ClientFactory;
import sd2223.trab2.discovery.Discovery;
import sd2223.trab2.utils.Formatter;
import sd2223.trab2.utils.IDGenerator;
import sd2223.trab2.utils.Secret;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static sd2223.trab2.api.java.Result.ErrorCode;

// next test 4a
public class JavaFeeds extends JavaService implements Feeds {

    private static final Logger Log = Logger.getLogger(JavaFeeds.class.getName());

    private final String domain;
    protected final IDGenerator generator;
    private final Map<String, FeedUser> allUserInfo;
    private final String secret;
    private Users myUsersServer;
    // private final Map<String, RemoteUser> remoteUsers;

    public JavaFeeds(String domain, long baseNumber) {
        this.domain = domain;
        this.generator = new IDGenerator(baseNumber);
        this.allUserInfo = new HashMap<>();
        this.myUsersServer = null;
        this.secret = Secret.getSecret();
    }

    /**
     * Post a message on the feed of the user, and his subscribers
     * @param user
     * @param pwd
     * @param msg
     * @return
     */
    @Override
    public Result<Long> postMessage(String user, String pwd, Message msg) {
        Log.info("PostMessage: user=" + user + " ; pwd=" + pwd + " ; msg=" + msg);
        var address = Formatter.getUserAddress(user);

        if (address == null || !this.domain.equals(address.domain())
                || pwd == null || msg.getDomain() == null || msg.getUser() == null) {
            Log.info("Bad request.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        Result<LocalUser> err;
        if (!(err = this.checkPassword(user, pwd)).isOK()) {
            Log.info("User doesn't exist of credentials are wrong.");
            return Result.error(err.error());
        }

        var userInfo = err.value();
        synchronized (userInfo){
            var messages = userInfo.getUserMessages();

            msg.setDomain(domain);
            this.setupMessage(msg); // set's message current time and ID :)
            var mid = msg.getId();

            messages.put(mid, msg);
            userInfo.getServerSubscribers()
                    .forEach( domain -> {
                         super.addRequest(
                                 domain,
                                 server -> server.createExtFeedMessage(user, secret, msg),
                                 true // forceBackground (Iago's idea :D)
                         );
                    });
            Log.info(String.format("Message %d created", mid));
            return Result.ok(mid);
        }
    }

    /**
     * Removes message from personal feed, and request the server os the subscribers to do the same (with a diferent method)
     * @param user
     * @param mid
     * @param pwd
     * @return
     */
    @Override
    public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
        Log.info(String.format("remoteFromPersonalFeed: user=%s ; long=%d ; pwd=%s", user, mid, pwd));
        var address = Formatter.getUserAddress(user);

        if (address == null || this.isForeignDomain(address.domain()) || pwd == null) {
            Log.info("Invalid user, domain or password.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        Result<LocalUser> res = this.checkPassword(user, pwd);
        if(! res.isOK()) {
            Log.info("User doesn't exist of credentials are wrong.");
            return Result.error( res.error() );
        }

        var userInfo = res.value();
        synchronized (userInfo){
            var msgs = userInfo.getUserMessages();
            if(msgs.remove(mid) != null){
                userInfo.getServerSubscribers()
                        .forEach( domain -> {
                            super.addRequest(
                                    domain,
                                    server -> server.removeExtFeedMessage(user, mid, secret)
                            );
                        });

                Log.info("Message " + mid + " removed.");
                return Result.error( ErrorCode.NO_CONTENT );
            }
        }

        Log.info("Message not found.");
        return Result.error(ErrorCode.NOT_FOUND);
    }

    /**
     * Creates a feed for a user (Just to add new users to the feeds database (Map allUserInfo))
     * @param user
     * @return
     */
    @Override
    public Result<Void> createFeed(String user, String secret) {
        Log.info(String.format("createFeed: user=%s ; secret=%s", user, secret));
        var address = Formatter.getUserAddress(user);
        if (address == null || this.isForeignDomain(address.domain())) {
            Log.info("Invalid user address.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        if(!this.secret.equals(secret)) {
            Log.info("Bad secret.");
            return Result.error(ErrorCode.FORBIDDEN);
        }

        synchronized (allUserInfo) {
            allUserInfo.putIfAbsent(user, new LocalUser());
        }

        Log.info("Feed " + user + " created.");
        return Result.ok();
    }


    /**
     * Gets a message on a user's feed by the id of the message (Can be a message of a remote user)
     * @param user
     * @param mid
     * @return Message || Error
     */
    @Override
    public Result<Message> getMessage(String user, long mid) {
        Log.info(String.format("getMessage: user=%s ; mid=%d", user, mid));
        var address = Formatter.getUserAddress(user);

        if (address == null /*|| this.isForeignDomain(address.domain())*/) {
            Log.info("Bad address");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        if(this.isForeignDomain( address.domain() )){ //If user is remote, ask the server if it has the message
            return this.forwardRequest(
                    address.domain(),
                    server -> server.getMessage(user, mid)
            );
        }

        var userInfo = this.getLocalUser(user);// If it is local, go find the message
        if (userInfo != null) {
            Optional<Message> res;
            synchronized (userInfo) {
                res = userInfo.getAllFeedMessages()
                        .map(msgs -> msgs.get(mid))
                        .filter(Objects::nonNull)
                        .findFirst();
            }

            if(res.isPresent()) {
                Log.info("Message " + mid + " found.");
                return Result.ok(res.get());
            }
        }

        Log.info("Messages not found.");
        return Result.error(ErrorCode.NOT_FOUND);
    }

    /**
     * Gets all the messages of a user's feed (Including messages of remote users)
     * @param user
     * @param time
     * @return List of messages || Error
     */
    @Override
    public Result<List<Message>> getMessages(String user, long time) {
        Log.info(String.format("getMessages: user=%s ; time=%d", user, time));

        var address = Formatter.getUserAddress(user);
        if (address == null /*|| this.isForeignDomain( address.domain() )*/) {
            Log.info("Invalid address");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        if(this.isForeignDomain( address.domain() )) {
            return this.forwardRequest(
                    address.domain(),
                    server -> server.getMessages(user, time)
            );
        }

        var userInfo = this.getLocalUser(user);
        if (userInfo == null) {
            Log.info("Messages not found.");
            return Result.error(ErrorCode.NOT_FOUND);
        }

        synchronized (userInfo) {
            Log.info("Messages found.");
            return Result.ok(
                    userInfo.getAllFeedMessages().map(m -> m.values().stream())
                            .reduce(Stream.empty(), Stream::concat)
                            .filter(m -> m.getCreationTime() > time)
                            .toList()
            );
        }
    }

    /**
     * Subscribes the user to the userSub, given password to confirm identity of the user
     * @param user
     * @param userSub
     * @param pwd
     * @return
     */
    @Override
    public Result<Void> subscribeUser(String user, String userSub, String pwd) {
        Log.info(String.format("subscribeUser: user=%s ; userSub=%s ; pwd=%s", user, userSub, pwd));

        var localUserAddress = Formatter.getUserAddress(user);
        var subUserAddress   = Formatter.getUserAddress(userSub);
        if (localUserAddress == null || subUserAddress == null || user.equals(userSub)
                || this.isForeignDomain( localUserAddress.domain() ) || pwd == null) {
            Log.info("Bad address, domain or pwd.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        Result<LocalUser> err;
        if (!(err = this.checkPassword(user, pwd)).isOK()) {
            Log.info("Problems checking user credentials.");
            return Result.error(err.error());
        }

        var localUser = err.value();
        var subUserInfo = this.getUser(userSub);

        // If user not in our Map of allUserInfo of this server, means he is not from here for sure
        // (Cause every user when created, get into the map)
        if (subUserInfo == null) {
            var feedsServer = super.getFeedServer(subUserAddress.domain());
            if (feedsServer == null) {
                Log.info("subUser doesn't exist or unable to contact with it's feeds server.");
                return Result.error( ErrorCode.NOT_FOUND );
            }

            Result<List<Message>> res;
            if (!(res = feedsServer.subscribeServer(this.domain, userSub, secret )).isOK()) {
                Log.info("Could not subscribe server :(");
                return Result.error(res.error());
            }

            synchronized (allUserInfo){
                subUserInfo =  allUserInfo.computeIfAbsent(userSub, k -> new RemoteUser(res.value()));
            }
        }
        //Actually subscribing
        synchronized (localUser) {
            var subs = localUser.getSubscriptions();
            subs.add(userSub);
        }

        synchronized (subUserInfo){
            var subsUsers = subUserInfo.getUsersSubscribers();
            subsUsers.add(user);
        }

        Log.info("subscription performed");
        return Result.error(ErrorCode.NO_CONTENT);
    }

    /**
     * Unsubscribes user from userSub's Subscribers
     * @param user
     * @param userSub
     * @param pwd
     * @return
     */
    @Override
    public Result<Void> unSubscribeUser(String user, String userSub, String pwd) {
        Log.info(String.format("unSubscribeUser: user=%s ; userSub=%s ; pwd=%s", user, userSub, pwd));
        var userAddress = Formatter.getUserAddress(user);
        var userSubAddress = Formatter.getUserAddress(userSub);

        if(userAddress == null || userSubAddress == null
                || this.isForeignDomain( userAddress.domain()) || pwd == null ){
            Log.info("Bad request.");
            return Result.error( ErrorCode.BAD_REQUEST );
        }

        var res = checkPassword(user, pwd);
        if(! res.isOK() ) {
           Log.info("User doesn't exist or pwd is wrong.");
           return Result.error( res.error() );
        }

        var userInfo = res.value();
        synchronized (userInfo){
            var subs = userInfo.getSubscriptions();
            if( ! subs.remove(userSub) )
                return Result.error( ErrorCode.NOT_FOUND );
        }

        this.removeFromSubscribers(user, userSub);
        Log.info("unsubscription performed");
        return Result.error( ErrorCode.NO_CONTENT );
    }

    /**
     * Lists all Subscriptions of a user
     * @param user
     * @return
     */
    @Override
    public Result<List<String>> listSubs(String user) {
        Log.info("listSubs: user=" + user);
        var address = Formatter.getUserAddress(user);

        if (address == null || this.isForeignDomain( address.domain() )) {
            Log.info("Bad request");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        var userInfo = this.getLocalUser(user);

        if (userInfo == null) {
            Log.info("User doesn't exist.");
            return Result.error(ErrorCode.NOT_FOUND);
        }

        synchronized (userInfo) {
            Log.info("Listing subscriptions.");
            return Result.ok(
                    userInfo.getSubscriptions()
                            .stream()
                            .toList()
            );
        }

    }

    /**
     * Subscribes a remote server to a local user here
     * @param domain
     * @param user
     * @return A List of messages of this user to the remote server
     */
    @Override
    public Result<List<Message>> subscribeServer(String domain, String user, String secret) { // and external server subscribing in one of my local user
        Log.info(String.format("subscribeServer: domain=%s ; user=%s ; secret=%s", domain, user, secret));
        var address = Formatter.getUserAddress(user);
        if (address == null || this.isForeignDomain( address.domain() ) || !this.isForeignDomain(domain) ) {
            Log.info("Bad user address.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        if(!this.secret.equals(secret)) {
            Log.info("Bad secret.");
            return Result.error(ErrorCode.FORBIDDEN);
        }

        var userInfo = this.getLocalUser(user);

        if(userInfo == null){
            Log.info("UserNot found to found.");
            return Result.error( ErrorCode.NOT_FOUND );
        }

        synchronized (userInfo) {
            var subs = userInfo.getServerSubscribers();
            subs.add(domain);

            Log.info("Subscription performed.");
            return Result.ok(
                    userInfo.getUserMessages()
                            .values()
                            .stream()
                            .toList()
            );
        }
    }

    /**
     * Operation made to be used by other servers when they want to post a message here
     * @param user
     * @param msg
     * @return
     */
    @Override
    public Result<Void> createExtFeedMessage(String user, String secret, Message msg) {
        Log.info(String.format("receiveMessage: user=%s ; secret=%s ; msg=%s", user, secret, msg));
        var address = Formatter.getUserAddress(user);
        if( msg == null || address == null || !this.isForeignDomain(address.domain()) ) { // is it worthed worrying about msg fields being null??0
            Log.info("Bad request.");
            return Result.error( ErrorCode.BAD_REQUEST );
        }

        if(!this.secret.equals(secret)) {
            Log.info("Bad secret.");
            return Result.error(ErrorCode.FORBIDDEN);
        }

        var userInfo = this.getUser(user);
        synchronized (userInfo){
            var msgs = userInfo.getUserMessages();
            msgs.put(msg.getId(), msg);
        }

        Log.info("Message "  + msg.getId() + " created.");
        return Result.ok();
    }

    /**
     * Operation made to be used by other servers when they want to remove a message here
     * @param user
     * @param mid
     * @return
     */
    @Override
    public Result<Void> removeExtFeedMessage(String user, long mid, String secret) {
        Log.info(String.format("removeFeedMessage: user=%s ; mid=%d", user, mid));
        var address = Formatter.getUserAddress(user);
        if(address == null || !this.isForeignDomain( address.domain() )){
            Log.info("Bad request.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        if(!this.secret.equals(secret)) {
            Log.info("Bad secret.");
            return Result.error(ErrorCode.FORBIDDEN);
        }

        var userInfo = this.getUser(user);

        if( userInfo == null ){
            Log.info("User or message not found.");
            return Result.error(ErrorCode.NOT_FOUND);
        }

        synchronized (userInfo){
            var msgs = userInfo.getUserMessages();
            msgs.remove(mid);
        }

        Log.info("Message "  + mid + " removed**.");
        return Result.ok();
    }

    /**
     * Operation made when we want to remove an entire feed here (e.g. When a user is deleted)
     * @param user
     * @return
     */
    @Override
    public Result<Void> removeFeed(String user, String secret) {
        Log.info(String.format("removeFeed: user=%s ; secret=%s", user, secret));

        Log.info("removeFeed: user=" + user);
        var address = Formatter.getUserAddress(user);
        if(address == null || this.isForeignDomain( address.domain() ) ){
            Log.info("Bad address.");
            return Result.error( ErrorCode.BAD_REQUEST );
        }

        if(!this.secret.equals(secret)) {
            Log.info("Bad secret.");
            return Result.error(ErrorCode.FORBIDDEN);
        }

        return doRemove(user);
    }
    /**
     * Operation made by a remote server, when we want to remove an entire feed here (e.g. When a user is deleted)
     * @param user
     * @return
     */
    @Override
    public Result<Void> removeExtFeed(String user, String secret) {
        Log.info(String.format("removeFeed: user=%s ; secret=%s", user, secret));

        Log.info("removeFeed: user=" + user);
        var address = Formatter.getUserAddress(user);
        if(address == null || !this.isForeignDomain( address.domain() ) ){
            Log.info("Bad address.");
            return Result.error( ErrorCode.BAD_REQUEST );
        }

        if(!this.secret.equals(secret)) {
            Log.info("Bad secret.");
            return Result.error(ErrorCode.FORBIDDEN);
        }

        return doRemove(user);
    }

    /**
     * Unsubscribes a server for a given user (This method is always called remotely)
     * @param domain
     * @param user
     * @return
     */
    @Override
    public Result<Void> unsubscribeServer(String domain, String user, String secret) {
        Log.info(String.format("unsubscribeServer: domain=%s ; user=%s ; secret=%s", domain, user, secret));

        var address = Formatter.getUserAddress(user);

        if(address == null || this.isForeignDomain( address.domain() )){
            Log.info("Bad address.");
            return Result.error( ErrorCode.BAD_REQUEST );
        }

        if(!this.secret.equals(secret)) {
            Log.info("Bad secret.");
            return Result.error(ErrorCode.FORBIDDEN);
        }

        LocalUser userInfo = this.getLocalUser(user);
        if(userInfo != null ){
            synchronized (userInfo){
                var serverSubs = userInfo.getServerSubscribers();
                if (serverSubs.remove(domain)) {
                    Log.info("Subscription of [ " + domain + " -> " + user + "] removed.");
                    return Result.ok();
                }
            }
        }

        Log.info("User or subscription not found.");
        return Result.error( ErrorCode.NOT_FOUND );
    }

    /**
     * Removes user from userSub's subscribers
     * @param user
     * @param userSub
     */
    private void removeFromSubscribers(String user, String userSub){
        var userInfo = this.getUser(userSub);
        assert  userInfo != null;
        synchronized (userInfo) {
            var subs = userInfo.getUsersSubscribers();
            subs.remove(user);
        }
        //If no more people from here is subscribed to this guy, removes him from our allUserInfo
        if(userInfo instanceof RemoteUser &&
                ((RemoteUser) userInfo).isOver()){
            synchronized (allUserInfo){
                allUserInfo.remove(userSub);
            }
            super.addRequest(
                    Formatter.getUserAddress(userSub).domain(),
                    //And asks the other server to unsubscribe us
                    server -> server.unsubscribeServer(this.domain, userSub, secret)
            );
        }
    }

    /**
     * Returns the result of the request to the given domain server
     * @param domain
     * @param request
     * @return
     * @param <T>
     */
    private <T> Result<T> forwardRequest(String domain, Function<Feeds, Result<T>> request){
        var server = super.getFeedServer(domain);
        if(server == null){
            Log.info("forwardRequest: server not found.");
            return Result.error( ErrorCode.NOT_FOUND );
        }
        Log.info("Message forwarded.");
        return request.apply(server);
    }

    /**
     * Used when deleting a User
     * @param user
     * @return
     */
    private Result<Void> doRemove(String user){
        var userInfo = getUser(user);
        if (userInfo == null) {
            Log.info("User not found.");
            return Result.error( ErrorCode.NOT_FOUND );
        }

        synchronized (userInfo){
            userInfo.getUsersSubscribers() //For all my subscribers
                    .forEach(addr -> {
                        LocalUser local = this.getLocalUser(addr); //(Local subs only)
                        assert local != null;
                        synchronized (local){
                            var subs = local.getSubscriptions();
                            subs.remove(user); //Will delete this user from their subscriptions
                            // (Note that when deleting the user of their subscriptions, they cannot access this user's feed anymore,
                            // therefore deletes also all messages related to this user from all users)
                        }
                    });

            //This method will execute in other servers eventually, so we need to make sure that
            // we go on from here if we are in the main server of the user being deleted
            if(userInfo instanceof LocalUser){
                LocalUser aux = ((LocalUser) userInfo);
                aux.getServerSubscribers() //Now taking care of remote subs
                   .forEach( domain -> {
                       super.addRequest(
                               domain,
                               server -> server.removeExtFeed(user, secret) //Telling them that we want to remove the feed of this user
                       );
                   });
                aux.getSubscriptions()
                    .forEach(subUser -> {
                        this.removeFromSubscribers(user, subUser);//And Remove
                    });
            }
        }
        synchronized (allUserInfo){
            allUserInfo.remove(user);
        }
        Log.info("Feed " + user + " removed.");
        return Result.ok();
    }

    private Result<LocalUser> checkPassword(String user, String pwd){
         var userInfo = this.getLocalUser(user);
         if(userInfo == null) return Result.error( ErrorCode.NOT_FOUND );
         var username = Formatter.getUserAddress(user).username();
         Result<Void> res;
         synchronized (this){
            res = this.getMyUsersServer().verifyPassword(username, pwd);
         }
         return res.isOK() ? Result.ok(userInfo) : Result.error( res.error() );
     }

    private boolean isForeignDomain(String domain) {
        return !this.domain.equals(domain);
    }

    private LocalUser getLocalUser(String userAddress) {
        synchronized (allUserInfo) {
            return (LocalUser) allUserInfo.get(userAddress);
        }
    }

    private FeedUser getUser(String userAddress) {
        synchronized (allUserInfo) {
            return allUserInfo.get(userAddress);
        }
    }

    private Users getMyUsersServer() {
        if(this.myUsersServer == null){
            this.myUsersServer = this.getUserServer(this.domain);
        }
        return this.myUsersServer;
    }

    private Users getUserServer(String serverDomain) {
        var ds = Discovery.getInstance();
        URI[] serverURIs = ds.knownUrisOf(Formatter.getServiceID(serverDomain, Formatter.USERS_SERVICE), 1);
        if (serverURIs.length == 0) return null;
        return ClientFactory.getUsersClient(serverURIs[0]);
    }

    private static abstract class FeedUser { // represents a feed of a user (local or remote)
        private final Map<Long, Message> userMessages;
        private final Set<String> usersSubscribers;

        public FeedUser() {
            this.userMessages = new ConcurrentHashMap<>(); // do we really need this:
            this.usersSubscribers = new HashSet<>();
        }

        public Map<Long, Message> getUserMessages() {
            return userMessages;
        }

        public Set<String> getUsersSubscribers(){
            return usersSubscribers;
        }
    }

    // set message ID and creation time
    protected void setupMessage(Message msg){
        long mid = generator.nextID();
        msg.setId(mid);
        msg.setCreationTime(System.currentTimeMillis());
    }

    // the feed of a Local user which is a user
    // that effective belongs to this server
    // (this means a user whose domain is the same as this)
    private class LocalUser extends FeedUser {
        private final Set<String> subscriptions;
        private final Set<String> serverSubscribers;

        public LocalUser() {
            super();
            serverSubscribers = new HashSet<>();
            subscriptions = new HashSet<>();
        }

        public Set<String> getSubscriptions() {
            return subscriptions;
        }

        public Set<String> getServerSubscribers() {
            return serverSubscribers;
        }

        /**
         * Gets all messages of the user and of his subscriptions
         * @return a list of Map<String, Message> from the user a user and all its subscriptions.
         */
        public Stream<Map<Long, Message>> getAllFeedMessages() {
            return Stream.concat(
                    Stream.of(this.getUserMessages()),
                    subscriptions.stream().map(sub -> getUser(sub).getUserMessages() )
            );
        }
    }

    // It's the cache of a user that doesn't belong to this feeds server
    // but in which at least one of the users that does belong to this server
    // is subscribed to.
    private class RemoteUser extends FeedUser {
        public RemoteUser(List<Message> msgs){
            super();
            var userMessages = this.getUserMessages();
            for(Message msg : msgs)
                userMessages.put(msg.getId(), msg);
        }

        /**
         * @return true if these user has no more subscriptions (so we can delete this cache :>)
         */
        public synchronized boolean isOver(){
            return this.getUsersSubscribers().isEmpty();
        }

    }

}
