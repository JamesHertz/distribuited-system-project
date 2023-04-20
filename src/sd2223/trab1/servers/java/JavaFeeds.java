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
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static sd2223.trab1.api.java.Result.ErrorCode;

// next test 4a
public class JavaFeeds extends JavaService implements Feeds {

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
            long mid = generator.nextID();
            msg.setId(mid);
            msg.setDomain(domain);
            msg.setCreationTime(System.currentTimeMillis());
            messages.put(mid, msg);

            userInfo.getServerSubscribers()
                    .forEach( domain -> {
                        /*
                            var feedServer = this.getFeedServer(domain);
                            assert feedServer != null; // by now :)
                            var e = feedServer.createExtFeedMessage(user, msg);
                         */
                         super.addRequest(
                                 domain,
                                 server -> server.createExtFeedMessage(user, msg)
                         );
                    });
            Log.info(String.format("Message %d created", mid));
            return Result.ok(mid);
        }
    }

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
                            /*
                                var server = this.getFeedServer(domain);
                                assert server != null;
                                server.removeExtFeedMessage(user, mid);
                             */
                            super.addRequest(
                                    domain,
                                    server -> server.removeExtFeedMessage(user, mid)
                            );
                        });

                Log.info("Message " + mid + " removed.");
                return Result.error( ErrorCode.NO_CONTENT );
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

        Log.info("Feed " + user + " created.");
        return Result.ok();
    }


    @Override
    public Result<Message> getMessage(String user, long mid) {
        Log.info(String.format("getMessage: user=%s ; mid=%d", user, mid));
        var address = Formatter.getUserAddress(user);

        if (address == null /*|| this.isForeignDomain(address.domain())*/) {
            Log.info("Bad address");
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        if(this.isForeignDomain( address.domain() )){
            return this.forwardRequest(
                    address.domain(),
                    server -> server.getMessage(user, mid)
            );
        }

        var userInfo = this.getLocalUser(user);
        if (userInfo != null) {
            Optional<Message> res;
            synchronized (userInfo) {
                res = userInfo.getAllFeedMessages()
                        .map(msgs -> msgs.get(mid))
                        .filter(Objects::nonNull)
                        .findFirst();
            }

            //  users@domain                  feed@domain
            // addUser(username)     -->   createFeed(useraddress)
            // removeUser(username)  -->   removeFeed(useraddress)
            // requests
            if(res.isPresent()) {
                Log.info("Message " + mid + " found.");
                return Result.ok(res.get());
            }
        }

        Log.info("Messages not found.");
        return Result.error(ErrorCode.NOT_FOUND);
    }

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
        if (subUserInfo == null) {
            var feedsServer = super.getFeedServer(subUserAddress.domain());
            // if we weren't able to contact the subUser feeds server of the subUser
            // domain is our domain ( which by now means it doesn't exist )
            if (feedsServer == null) {
                Log.info("subUser doesn't exist or unable to contact with it's feeds server.");
                return Result.error( ErrorCode.NOT_FOUND );
            }

            Result<List<Message>> res;
            if (!(res = feedsServer.subscribeServer(this.domain, userSub)).isOK()) {
                Log.info("Could not subscribe server :(");
                return Result.error(res.error());
            }

            synchronized (allUserInfo){
                subUserInfo =  allUserInfo.computeIfAbsent(userSub, k -> new RemoteUser(res.value()));
            }
        }

        synchronized (localUser) {
            var subs = localUser.getSubscriptions();
            subs.add(userSub); // great :)
        }

        synchronized (subUserInfo){
            var subsUsers = subUserInfo.getUsersSubscribers();
            subsUsers.add(user);
        }

        Log.info("subscription performed");
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

        this.removeFromSubscribers(user, userSub);
        Log.info("unsubscription performed");
        return Result.error( ErrorCode.NO_CONTENT );
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
            Log.info("Listing subscriptions.");
            return Result.ok(
                    userInfo.getSubscriptions()
                            .stream()
                            .toList()
            );
        }

    }

    @Override
    public Result<List<Message>> subscribeServer(String domain, String user) { // and external server subscribing in one of my local user
        Log.info("subscribeServer: domain=" + domain + " user=" + user);
        var address = Formatter.getUserAddress(user);
        if (address == null || this.isForeignDomain( address.domain() ) || !this.isForeignDomain(domain) ) {
            Log.info("Bad user address.");
            return Result.error(ErrorCode.BAD_REQUEST);
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

    @Override
    public Result<Void> createExtFeedMessage(String user, Message msg) {
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

        Log.info("Message "  + msg.getId() + " created.");
        return Result.ok();
    }

    @Override
    public Result<Void> removeExtFeedMessage(String user, long mid) {
        Log.info(String.format("removeFeedMessage: user=%s ; mid=%d", user, mid));
        var address = Formatter.getUserAddress(user);
        if(address == null || !this.isForeignDomain( address.domain() )){
            Log.info("Bad request.");
            return Result.error(ErrorCode.BAD_REQUEST);
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


    @Override
    public Result<Void> removeFeed(String user) {
        Log.info("removeFeed: user=" + user);
        var address = Formatter.getUserAddress(user);
        if(address == null || this.isForeignDomain( address.domain() ) ){
            Log.info("Bad address.");
            return Result.error( ErrorCode.BAD_REQUEST );
        }

        return doRemove(user);
    }

    @Override
    public Result<Void> removeExtFeed(String user) {
        Log.info("removeFeed: user=" + user);
        var address = Formatter.getUserAddress(user);
        if(address == null || !this.isForeignDomain( address.domain() ) ){
            Log.info("Bad address.");
            return Result.error( ErrorCode.BAD_REQUEST );
        }

        return doRemove(user);
    }

    @Override
    public Result<Void> unsubscribeServer(String domain, String user) {
        Log.info(String.format("unsubscribeServer: domain=%s ; user=%s", domain, user));
        var address = Formatter.getUserAddress(user);

        if(address == null || this.isForeignDomain( address.domain() )){
            Log.info("Bad address.");
            return Result.error( ErrorCode.BAD_REQUEST );
        }

        LocalUser userInfo = this.getLocalUser(user);
        if(userInfo != null ){
            synchronized (userInfo){
                var serverSubs = userInfo.getServerSubscribers();
                if (serverSubs.remove(domain)) return Result.ok();
                Log.info("serverSubs: " + serverSubs);
            }
        }

        Log.info("User or subscription not found.");
        return Result.error( ErrorCode.NOT_FOUND );
    }

    private void removeFromSubscribers(String user, String userSub){
        var userInfo = this.getUser(userSub);
        assert  userInfo != null;
        synchronized (userInfo) {
            var subs = userInfo.getUsersSubscribers();
            subs.remove(user);
        }
        if(userInfo instanceof RemoteUser &&
                ((RemoteUser) userInfo).isOver()){
            synchronized (allUserInfo){
                allUserInfo.remove(userSub);
            }
            /*
                var server = this.getFeedServer( Formatter.getUserAddress(userSub).domain() );
                assert server != null;
                server.unsubscribeServer(this.domain, userSub);
             */
            super.addRequest(
                    Formatter.getUserAddress(userSub).domain(),
                    server -> server.unsubscribeServer(this.domain, userSub)
            );
        }
    }

    private <T> Result<T> forwardRequest(String domain, Function<Feeds, Result<T>> request){
        var server = super.getFeedServer(domain);
        if(server == null){
            Log.info("forwardRequest: server not found.");
            return Result.error( ErrorCode.NOT_FOUND );
        }
        Log.info("Message forwarded.");
        return request.apply(server);
    }

    private Result<Void> doRemove(String user){
        var userInfo = getUser(user);
        if (userInfo == null) {
            Log.info("User not found.");
            return Result.error( ErrorCode.NOT_FOUND );
        }

        synchronized (userInfo){
            userInfo.getUsersSubscribers()
                    .forEach(addr -> {
                        LocalUser local = this.getLocalUser(addr);
                        assert local != null;
                        synchronized (local){
                            var subs = local.getSubscriptions();
                            subs.remove(user);
                        }
                    });
            if(userInfo instanceof LocalUser){
                LocalUser aux = ((LocalUser) userInfo);
                aux.getServerSubscribers()
                   .forEach( domain -> {
                       /*
                           var server = this.getFeedServer(domain);
                           assert server != null;
                           server.removeExtFeed(user);
                        */
                       super.addRequest(
                               domain,
                               server -> server.removeExtFeed(user)
                       );
                   });
                aux.getSubscriptions()
                    .forEach(subUser -> {
                        this.removeFromSubscribers(user, subUser);
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
         var res = this.getMyUsersServer().verifyPassword(username, pwd);
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

    /*
    private Feeds getFeedServer(String serverDomain) {
        var ds = Discovery.getInstance();
        URI[] serverURI = ds.knownUrisOf(Formatter.getServiceID(serverDomain, Formatter.FEEDS_SERVICE), 1);
        if (serverURI.length == 0) return null;
        return ClientFactory.getFeedsClient(serverURI[0]);
    }
     */

    private static abstract class FeedUser {
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
                    subscriptions.stream().map(sub -> getUser(sub).getUserMessages() )
            );
        }
    }

    private class RemoteUser extends FeedUser {
        public RemoteUser(List<Message> msgs){
            var userMessages = this.getUserMessages();
            for(Message msg : msgs)
                userMessages.put(msg.getId(), msg);
        }
        public synchronized boolean isOver(){
            return this.getUsersSubscribers().isEmpty();
        }

    }

}
