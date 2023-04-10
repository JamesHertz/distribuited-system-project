package sd2223.trab1.servers.java;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.java.Users;
import sd2223.trab1.clients.UsersClientFactory;
import sd2223.trab1.discovery.Discovery;
import sd2223.trab1.utils.Formatter;
import sd2223.trab1.utils.IDGenerator;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static sd2223.trab1.api.java.Result.ErrorCode;

// next test 3b :)
public class JavaFeeds implements Feeds {

    private static final Logger Log = Logger.getLogger(JavaFeeds.class.getName());

    private final String domain;
    private final IDGenerator generator;
    private final Map<String, FeedUser> allUserInfo;

    public JavaFeeds(String domain, long baseNumber) {
        this.domain = domain;
        this.generator = new IDGenerator(baseNumber);
        this.allUserInfo = new HashMap<>();
    }

    @Override
    public Result<Long> postMessage(String user, String pwd, Message msg) {
        Log.info("PostMessage: user=" + user + " ; pwd=" + pwd + " ; msg=" + msg);
        var address = Formatter.getUserAddress(user);

        if (address == null || !this.domain.equals(address.domain())
                || pwd == null || msg.getDomain() == null || msg.getUser() == null) {
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        Users usersServer = getDomainUserServer();

        if (usersServer == null) {
            System.out.println("Unable to contact the user server.");
            return Result.error(ErrorCode.TIMEOUT);
        }

        // optmize later :)
        Result<Void> err;
        if (!(err = usersServer.verifyPassword(address.username(), pwd)).isOK()) {
            System.out.println("Problems checking user.");
            return Result.error(err.error());
        }

        LocalUser userInfo;
        synchronized (allUserInfo) {
            userInfo = (LocalUser) allUserInfo.computeIfAbsent(user, k -> new LocalUser());
        }

        var messages = userInfo.getUserMessages();
        long mid = generator.nextID();
        msg.setId(mid);
        msg.setDomain(domain);
        msg.setCreationTime(System.currentTimeMillis());
        messages.put(mid, msg);

        // TODO: send the message to all subscribers
        return Result.ok(mid);
    }

    @Override
    public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
        var address = Formatter.getUserAddress(user);

        if (address == null || !this.domain.equals(address.domain()) || pwd == null) {
            Log.info("Invalid user, domain or password.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        var usersServer = this.getDomainUserServer();
        if (usersServer == null) {
            Log.info("Unable to connect user server.");
            return Result.error(ErrorCode.TIMEOUT);
        }

        Result<Void> err;
        if (!(err = usersServer.verifyPassword(address.username(), pwd)).isOK()) {
            Log.info("User doesn't exist or credentials are wrong..");
            return Result.error(err.error());
        }

        FeedUser userInfo;
        synchronized (allUserInfo) {
            userInfo = allUserInfo.get(user);
        }

        if (userInfo != null) {
            var messages = userInfo.getUserMessages();
            if (messages.remove(mid) != null)
                return Result.error(ErrorCode.NO_CONTENT);
        }

        Log.info("Message not found.");
        return Result.error(ErrorCode.NOT_FOUND);
    }

    @Override
    public Result<Message> getMessage(String user, long mid) {
        Log.info(String.format("getMessages: user=%s ; mid=%d", user, mid));

        synchronized (allUserInfo) {

            var userInfo = getLocalUser(user);
            if (userInfo == null) {
                Log.info("Messages not found.");
                return Result.error(ErrorCode.NOT_FOUND);
            }

            Optional<Message> res;
            synchronized (userInfo) {
                res = userInfo.getAllFeedMessages()
                        .map(msgs -> msgs.get(mid))
                        .filter(Objects::nonNull)
                        .findFirst();
            }

            if (res.isEmpty()) {
                Log.info("User exists but the message is not here.");
                return Result.error(ErrorCode.NOT_FOUND);
            }

            return Result.ok(res.get());
        }
    }

    @Override
    public Result<List<Message>> getMessages(String user, long time) {
        Log.info(String.format("getMessages: user=%s ; time=%d", user, time));

        synchronized (allUserInfo) {

            var userInfo = getLocalUser(user);

            if (userInfo == null) {
                Log.info("Messages not found.");
                return Result.error(ErrorCode.NOT_FOUND);
            }

            synchronized (userInfo) {
                return Result.ok(
                        userInfo.getAllFeedMessages().map(m -> m.values().stream())
                                .reduce(Stream.empty(), Stream::concat)
                                .filter(m -> m.getCreationTime() > time)
                                .toList()
                );
            }

        }
    }

    @Override
    public Result<Void> subscribeUser(String user, String userSub, String pwd) {
        /*
        Log.info(String.format("subscribeUser: user=%s ; userSub=%s ; pwd=%s", user, userSub, pwd));
        var localUserAddress = Formatter.getUserAddress(user);
        var subUserAddress   = Formatter.getUserAddress(userSub);

        if(     localUserAddress == null //Bad local address given
                || subUserAddress == null //Bad sub address given
                || ! this.domain.equals(localUserAddress.domain()) //User trying to subscribe is not from here
                || pwd == null //No pwd given for some reason
        ){
            Log.info("Bad address, domain or pwd.");
            return Result.error( ErrorCode.BAD_REQUEST );
        }

        var userServer = this.getDomainUserServer();
        if( userServer == null ){
            Log.info("Problems getting to user server.");
            return Result.error( ErrorCode.TIMEOUT );
        }

        Result<Void> err;
        if( ! (err = userServer.verifyPassword(localUserAddress.username(), pwd) ).isOK() ) {
            Log.info("User doesn't exist or pwd is wrong.");
            return Result.error( err.error() );
        }


         */
        // TODO: check that the user exists :)
        /* Iago stuff
                            one way to do it
         var userSubServer = this.getDomainUserServer(subUserAddress.domain());
         List usersMatched = userSubServer.searchUSer(subUserAddress.username());
         if( ! usersMatched.contains(userSub) )
            return 404

                            other way to do it
          // Ask (somehow) the userSub-domain-feed/user-server if they have the user
          Me ----(do the work)---> OtherServer
           |                              |
           ^----------(ok done/exists)----<
          // Wait response? deadlock? don't we need his password to verify if he exists or shall we also use searchUsers there? uga buga?
        */
        /*
        Feeds feedsService = this.getFeedService( subUserAddress.domain() )
        private Resul<Feeds> getFeedService( String domain ) {
            if( domain.equals(this.domain) )
                return this;
            return FeedsClientFactor.newFeedsService( domain );
        }
                Before the code on top of this
            if subUserAddress.domain() == this.domain()
                //check locally if user exists...
        ForeignFeed foreign;
        synchronized (foreignFeeds){
            foreign = foreignFeeds.computeIfAbsent(userSub, k -> new ForeignFeed()); // TODO: what if the user is local?
        }
        synchronized (foreign){
            foreign.incSubs();
        }

        UserFeed feed;
        synchronized (localFeeds){
            feed = localFeeds.computeIfAbsent(user, k -> new UserFeed());
        }
        var subs = feed.subscriptions();
        synchronized (subs){
            subs.add(userSub);
        }

        // TODO: subscribe to other server
        Iago stuff
                            one way to do it
         (still with var userSubServer)
         //Now makes it difficult to subscribe since that we do on the feeds Server...

                            other way to do it
          // Ask (somehow) the userSub-domain-feed/user-server to make this user, his subscriber
          Me ----(do the work)---> OtherServer
           |                              |
           ^-----(ok done/subscribed)-----<
          // Wait response? deadlock? uga buga?
        */
        // Wait... did james wanted something like (Map<String, String (the serverDomain) > serversThatThisUserSubscribedTo.putIfAbsent(thisUser, subDomain) ??)

        return null;
    }

    private LocalUser getLocalUser(String userAddress) {
        return (LocalUser) allUserInfo.get(userAddress);
        // var user = allUserInfo.get(userAddress);
        // return user instanceof LocalUser ? (LocalUser) user : null;
    }

    @Override
    public Result<Void> unSubscribeUser(String user, String userSub, String pwd) {
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public Result<List<String>> listSubs(String user) {
        Log.info("listSubs: user=" + user);
        var address = Formatter.getUserAddress(user);

        if( address == null || !this.domain.equals(address.domain()) ) {
            Log.info("Bad request");
            return Result.error( ErrorCode.BAD_REQUEST );
        }

        LocalUser userInfo;
        synchronized (allUserInfo){
            userInfo = getLocalUser(user);
        }

        if( userInfo == null ){
            Log.info("User doesn't exist.");
            return Result.error( ErrorCode.NOT_FOUND );
        }

        synchronized (userInfo){
            return Result.ok(
                    userInfo.getSubscribers()
                            .stream()
                            .toList()
            );
        }


    }

    private Users getDomainUserServer() {
        var ds = Discovery.getInstance();
        URI[] serverURIs = ds.knownUrisOf(Formatter.getServiceID(this.domain, Formatter.USERS_SERVICE), 1);
        if (serverURIs.length == 0) return null;
        return UsersClientFactory.get(serverURIs[0]);
    }


    private class FeedUser {
        private final Map<Long, Message> userMessages;

        public FeedUser() {
            this.userMessages = new ConcurrentHashMap<>(); // do we really need this:
            // this.userMessages = new HashMap<>();
        }

        public Map<Long, Message> getUserMessages() {
            return userMessages;
        }
    }

    private class LocalUser extends FeedUser {
        private Set<String> subscriptions;
        private Set<String> subscribers;

        public LocalUser() {
            super();
            subscribers = new HashSet<>();
            subscriptions = new HashSet<>();
        }

        public Set<String> getSubscriptions() {
            return subscriptions;
        }

        public Set<String> getSubscribers() {
            return subscribers;
        }

        public Stream<Map<Long, Message>> getAllFeedMessages() {
            return Stream.concat(
                    Stream.of(this.getUserMessages()),
                    // is there a problem over here??
                    subscriptions.stream().map(sub -> allUserInfo.get(sub).getUserMessages())
            );
        }
    }

    private class ForeignUser {
        private int counter;

        public ForeignUser() {
            super();
            this.counter = 0;
        }

        public void incSubs() {
            this.counter++;
        }

        public void decSubs() {
            this.counter--;
        }

        public boolean isOver() {
            return this.counter == 0;  // I am so sad :)
        }
    }

}
