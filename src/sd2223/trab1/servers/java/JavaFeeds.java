package sd2223.trab1.servers.java;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.java.Users;
import sd2223.trab1.clients.ClientFactory;
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
/*
how to update the messages of others peers?

cache version:
    When getMessage is ask we will ask for the respective feeds server for the messages
    if the messages is not already on cache
    Once we get the reply we store the result in a cache so the next time we can avoid some RTT

post version:
    When a user publish a message it's feed server will be responsible to announce to
    all it's subscribers server about the changes.

 */
/*
TODO:
    - change remove
    - change create
    - add a method to remove a user
    - add a method to subscribe to an user of another server
    - add a method to unsubscribe to an user of another server
    - complete method check user
 */

// TODO: think about TTL in Discovery
public class JavaFeeds implements Feeds {

    private static final Logger Log = Logger.getLogger(JavaFeeds.class.getName());

    private final String domain;
    private final IDGenerator generator;
    private final Map<String, FeedUser> allUserInfo;
    // private final Map<String, RemoteUser> remoteUsers;

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

        LocalUser userInfo = this.getLocalUser(user);
        if (userInfo == null) {
            Log.info("User feed not found.");
            return Result.error(ErrorCode.NOT_FOUND);
        }

        Users usersServer = getMyUsersServer();
        // if (usersServer == null) {
        //     Log.info("Unable to contact the user server.");
        //     return Result.error(ErrorCode.TIMEOUT);
        // }

        // optmize later :)
        Result<Void> err;
        if (!(err = usersServer.verifyPassword(address.username(), pwd)).isOK()) {
            System.out.println("Problems checking user.");
            return Result.error(err.error());
        }


        var messages = userInfo.getUserMessages();
        long mid = generator.nextID();
        msg.setId(mid);
        msg.setDomain(domain);
        msg.setCreationTime(System.currentTimeMillis());
        messages.put(mid, msg);

        synchronized (userInfo){
            userInfo.getServerSubscribers()
                    .stream()
                    .forEach( domain -> {
                        var feedServer = this.getFeedServer(domain);
                        assert feedServer != null; // by now :)
                        feedServer.receiveMessage(user, msg);
                    });
        }
        // TODO: send the message to all subscribers
        return Result.ok(mid);
    }

    @Override
    public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
        Log.info(String.format("remoteFromPersonalFeed: user=%d ; long=%d ; pwd=%s", user, mid, pwd));
        var address = Formatter.getUserAddress(user);

        if (address == null || this.isForeignDomain(address.domain()) || pwd == null) {
            Log.info("Invalid user, domain or password.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }


        LocalUser userInfo = this.getLocalUser(user);

        if (userInfo != null) {
            var usersServer = this.getMyUsersServer();

            // if (usersServer == null) {
            //     Log.info("Unable to connect user server.");
            //     return Result.error(ErrorCode.TIMEOUT);
            // }

            Result<Void> err;
            if (!(err = usersServer.verifyPassword(address.username(), pwd)).isOK()) {
                Log.info("Problem checking credentials.");
                return Result.error(err.error());
            }

            var messages = userInfo.getUserMessages();
            if (messages.remove(mid) != null) {
                return Result.error(ErrorCode.NO_CONTENT);
                // TODO propagate message :)
            }
        }

        Log.info("Message not found.");
        return Result.error(ErrorCode.NOT_FOUND);
    }

    @Override
    public Result<Void> createFeed(String user) {
        Log.info("createFeed: user=" + user);
        var address = Formatter.getUserAddress(user);
        if (address == null || this.isForeignDomain(address.domain())) {
            Log.info("Invalid user address.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        synchronized (allUserInfo) {
            allUserInfo.putIfAbsent(user, new LocalUser());
        }

        return Result.ok();
    }

    @Override
    public Result<Void> receiveMessage(String user, Message msg) {
        Log.info(String.format("receiveMessage: user=%s ; msg=%s", user, msg));
        var address = Formatter.getUserAddress(user);
        if( msg == null || address == null || !this.isForeignDomain(address.domain()) ) { // is it worthed worrying about msg fields being null??0
            Log.info("Bad request.");
            return Result.error( ErrorCode.BAD_REQUEST );
        }

        var userInfo = this.getUser(user);
        synchronized (userInfo){
            var msgs = userInfo.getUserMessages();
            msgs.put(msg.getId(), msg);
        }

        return Result.ok();
    }

    @Override
    public Result<Message> getMessage(String user, long mid) {
        Log.info(String.format("getMessages: user=%s ; mid=%d", user, mid));
        var address = Formatter.getUserAddress(user);

        if (address == null || this.isForeignDomain(address.domain())) {
            Log.info("Bad address");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        var userInfo = this.getLocalUser(user);
        if (userInfo != null) {

            // TODO: put some prints to help you understand
            Optional<Message> res;
            synchronized (userInfo) {
                res = userInfo.getAllFeedMessages()
                        .map(msgs -> msgs.get(mid))
                        .filter(Objects::nonNull)
                        .findFirst();
            }

            if(res.isPresent()) return Result.ok(res.get());
        }

        Log.info("Messages not found.");
        return Result.error(ErrorCode.NOT_FOUND);
    }

    @Override
    public Result<List<Message>> getMessages(String user, long time) {
        Log.info(String.format("getMessages: user=%s ; time=%d", user, time));

        var address = Formatter.getUserAddress(user);
        if (address == null || this.isForeignDomain( address.domain() )) {
            Log.info("Invalid address");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        var userInfo = this.getLocalUser(user);
        if (userInfo == null) {
            Log.info("Messages not found.");
            return Result.error(ErrorCode.NOT_FOUND);
        }

        // reminder delete user last :)
        synchronized (userInfo) {
            return Result.ok(
                    userInfo.getAllFeedMessages().map(m -> m.values().stream())
                            .reduce(Stream.empty(), Stream::concat)
                            .filter(m -> m.getCreationTime() > time)
                            .toList()
            );
        }

    }

    @Override
    public Result<Void> subscribeUser(String user, String userSub, String pwd) {
        Log.info(String.format("subscribeUser: user=%s ; userSub=%s ; pwd=%s", user, userSub, pwd));

        // make this thing work :)
        var localUserAddress = Formatter.getUserAddress(user);
        var subUserAddress   = Formatter.getUserAddress(userSub);
        if (localUserAddress == null || subUserAddress == null || user.equals(userSub)
                || this.isForeignDomain( localUserAddress.domain() ) || pwd == null) {
            Log.info("Bad address, domain or pwd.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        var localUser = this.getLocalUser( user );

        if(localUser == null){
            Log.info("User not found :(");
            return Result.error( ErrorCode.NOT_FOUND );
        }

        var usersServer = this.getMyUsersServer();
        // if (usersServer == null) {
        //     Log.info("Problems getting to user server.");
        //     return Result.error(ErrorCode.TIMEOUT);
        // }

        Result<Void> err;
        if (!(err = usersServer.verifyPassword(localUserAddress.username(), pwd)).isOK()) {
            Log.info("Problems checking user credentials.");
            return Result.error(err.error());
        }

        var subUserInfo = this.getUser(userSub);
        if (subUserInfo == null) {
            var feedsServer = this.getFeedServer(subUserAddress.domain());
            // if we weren't able to contact the subUser feeds server of the subUser
            // domain is our domain ( which by now means it doesn't exist )
            if (feedsServer == null) {
                Log.info("SubUser doesn't exist or unable to contact with it's feeds server.");
                return Result.error( ErrorCode.NOT_FOUND );
            }

            Result<Void> res;
            if (!(res = feedsServer.subscribeServer(this.domain, userSub)).isOK()) {
                Log.info("Could not subscribe server :(");
                return Result.error(res.error());
            }

            synchronized (allUserInfo){
                subUserInfo =  allUserInfo.computeIfAbsent(userSub, k -> new RemoteUser());
            }
        }

        synchronized (localUser) {
            var subs = localUser.getSubscriptions();
            subs.add(userSub); // great :)
        }

        synchronized (subUserInfo){
            var subsUsers = subUserInfo.getUsersSubscribers();
            subsUsers.add(user);
            System.out.println(subsUsers);
        }

        return Result.error(ErrorCode.NO_CONTENT);
    }


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

        var subUserInfo = this.getUser(userSub);

        // TODO: what if it becomes null ???
        synchronized (subUserInfo){
            var subscribers = subUserInfo.getUsersSubscribers();
            subscribers.remove(user);
        }

        // TODO: can this possibly go wrong??
        if(subUserInfo instanceof RemoteUser
             && ((RemoteUser) subUserInfo).isOver()) {
            synchronized (allUserInfo){
                allUserInfo.remove(userSub);
            }
        }
        Log.info("unSubscribedUser successfully.");
        return Result.ok();
    }


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
            return Result.ok(
                    userInfo.getSubscriptions()
                            .stream()
                            .toList()
            );
        }


    }

    @Override
    public Result<Void> subscribeServer(String domain, String user) { // and external server subscribing in one of my local user
        Log.info("Subscribe Server: domain=" + domain + " user=" + user);
        var address = Formatter.getUserAddress(user);
        if (address == null || this.isForeignDomain( address.domain() )) {
            Log.info("Bad user address.");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        var userInfo = this.getLocalUser(user);

        if(userInfo == null){
            Log.info("UserNot found to found.");
            return Result.error( ErrorCode.NOT_FOUND );
        }

        if ( this.isForeignDomain(domain) ) { // TODO: look at this
            synchronized (userInfo) {
                var subs = userInfo.getServerSubscribers();
                subs.add(domain);
                // Log.info("servers: " + subs.toString());
            }
        }

        return Result.ok();
    }

    private Result<LocalUser> checkPassword(String user, String pwd){
         var userInfo = this.getLocalUser(user);
         if(userInfo == null) return Result.error( ErrorCode.NOT_FOUND );
         var username = Formatter.getUserAddress(user).username();
         var res = this.getMyUsersServer().verifyPassword(username, pwd);
         return res.isOK() ? Result.ok(userInfo) : Result.error( res.error() );
     }

  //   private Result<LocalUser> checkPassword(String user, String pwd){
  //       var localUser = this.getLocalUser(user);
  //       if(localUser == null) return Result.error(ErrorCode.NOT_FOUND);
  //       return this.getMyUsersServer().verifyPassword(user)
  //   }

    private boolean isForeignDomain(String domain) {
        return !this.domain.equals(domain);
    }


    private LocalUser getLocalUser(String userAddress) {
        synchronized (allUserInfo) {
            return (LocalUser) allUserInfo.get(userAddress);
        }
        // var user = allUserInfo.get(userAddress);
        // return user instanceof LocalUser ? (LocalUser) user : null;
    }

    private FeedUser getUser(String userAddress) {
        synchronized (allUserInfo) {
            return allUserInfo.get(userAddress);
        }
    }

    // @SuppressWarnings("unchecked")
    // private <T extends FeedUser> T doSomething(String userAddress){
    //      synchronized (allUserInfo) {
    //         return (T) allUserInfo.get(userAddress);
    //     }
    // }

    private Users getMyUsersServer() {
        var server = this.getUserServer(this.domain);
        assert server != null;
        return server;
    }

    private Users getUserServer(String serverDomain) {
        var ds = Discovery.getInstance();
        URI[] serverURIs = ds.knownUrisOf(Formatter.getServiceID(serverDomain, Formatter.USERS_SERVICE), 1);
        if (serverURIs.length == 0) return null;
        return ClientFactory.getUsersClient(serverURIs[0]);
    }

    private Feeds getFeedServer(String serverDomain) {
        if (this.domain.equals(serverDomain)) return null;

        var ds = Discovery.getInstance();
        URI[] serverURI = ds.knownUrisOf(Formatter.getServiceID(serverDomain, Formatter.FEEDS_SERVICE), 1);
        if (serverURI.length == 0) return null;
        return ClientFactory.getFeedsClient(serverURI[0]);
    }

    /*
        iago@fct
        james@fct
        inscreve( iago@fct, james@fct )

        user(iago@fct).addSubscription( james@fct )
        delete(james@fct)
        user(iago@fct).deleteSubscription( james@fct )
     */


    private static abstract class FeedUser {
        private final Map<Long, Message> userMessages;
        private final Set<String> usersSubscribers;

        public FeedUser() {
            this.userMessages = new ConcurrentHashMap<>(); // do we really need this:
            this.usersSubscribers = new HashSet<>();
            // this.userMessages = new HashMap<>();
        }

        public Map<Long, Message> getUserMessages() {
            return userMessages;
        }

        public Set<String> getUsersSubscribers(){
            return usersSubscribers;
        }
    }

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

        public Stream<Map<Long, Message>> getAllFeedMessages() {
            return Stream.concat(
                    Stream.of(this.getUserMessages()),
                    // is there a problem over here??
                    subscriptions.stream().map(sub -> allUserInfo.get(sub).getUserMessages())
            );
        }
    }

    private class RemoteUser extends FeedUser {
        public boolean isOver(){
            return this.getUsersSubscribers().isEmpty();
        }

    }

}
