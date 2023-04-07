package sd2223.trab1.servers.java;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.java.Users;
import sd2223.trab1.clients.UsersClientFactory;
import sd2223.trab1.discovery.Discovery;
import sd2223.trab1.servers.java.feedutils.Creator;
import sd2223.trab1.servers.java.feedutils.Feed;
import sd2223.trab1.servers.java.feedutils.FeedUser;
import sd2223.trab1.utils.Formatter;
import sd2223.trab1.utils.IDGenerator;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import static sd2223.trab1.api.java.Result.ErrorCode;

// next test 3b :)
public class JavaFeeds implements Feeds {

    private static final Logger Log = Logger.getLogger(JavaFeeds.class.getName());

    private final String domain;
    private final IDGenerator generator;
    private final Map<String, Map<Long, Message>> localUserFeeds;
    private final Map<String, FeedUser> feedUsers;
    private final Map<String, Creator> creators;
    // private Map<String, Set<String>> subscriptions;
    // private Map<String, Map<Long, Message>> cache;

    //  creators      -> (creator -> all messages)
    //  subscriptions -> (user -> creator[])
    //  FeedUser -> Feeds
    /*

        Map<S, Feed> creators;
        Map<S, FeeUser> users;

        Feed {
            getMessage()
            addMessage()
            removeMessage();
            getAllMessages()
        }
iago@fct
james@di

iago@fct -> subscribe( james@di )

fct -> i_am_interested (di, james@di)

di -> map[james].addSubscriptions( fct )


        FeedUser {
            Feed feeds;
            Set<URI> subscriptions;
            Map<String, Feed> ..;
            getFeed();
            subscribe(String, Creator);
            unsubscribe(String);
            Creator getSubscription(String);
        }

        FeedUsers {
        }
     */
    public JavaFeeds(String domain, long baseNumber){
        this.domain = domain;
        this.generator = new IDGenerator(baseNumber);
        this.localUserFeeds = new HashMap<>();
        this.feedUsers = new HashMap<>();
        this.creators  = new HashMap<>();
    }

    @Override
    public Result<Long> postMessage(String user, String pwd, Message msg) {
        Log.info("PostMessage: user=" + user + " ; pwd=" + pwd + " ; msg=" + msg);
        var address = Formatter.getUserAddress(user);

        if( address == null || ! this.domain.equals(address.domain())
               || pwd == null || msg.getDomain() == null || msg.getUser() == null  ){
            return Result.error(ErrorCode.BAD_REQUEST);
        }

        Users usersServer = getDomainUserServer();

        if(usersServer == null ){
            System.out.println("Unable to contact the user server.");
            return Result.error( ErrorCode.TIMEOUT );
        }

        // optmize later :)
        Result<Void> err;
        if(! ( err = usersServer.verifyPassword(address.username(), pwd) ).isOK() ){
            System.out.println("Problems checking user.");
            return Result.error( err.error() );
        }

        FeedUser aux;
        synchronized (feedUsers){
            aux = feedUsers.computeIfAbsent(user, k -> new FeedUser());
        }

        synchronized (aux){
            Feed feeds = aux.getPersonalFeed();
            long mid = generator.nextID();
            msg.setId(mid);
            msg.setDomain(domain);
            msg.setCreationTime(System.currentTimeMillis());
            feeds.addMessage(mid, msg);
            return Result.ok(mid);
        }

        //synchronized (localUserFeeds){
        //    var userFeed = localUserFeeds.computeIfAbsent(user, k -> new HashMap<>());
        //    long mid = generator.nextID();
        //    msg.setId(mid);
        //    msg.setDomain(domain);
        //    msg.setCreationTime(System.currentTimeMillis());
        //    userFeed.put(mid, msg);
        //}
    }

    @Override
    public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
        var address = Formatter.getUserAddress(user);

        if( address == null || ! this.domain.equals(address.domain()) || pwd == null ){
            Log.info("Invalid user, domain or password.");
            return Result.error( ErrorCode.BAD_REQUEST );
        }

        var usersServer = this.getDomainUserServer();
        if(usersServer == null){
            Log.info("Unable to connect user server.");
            return Result.error( ErrorCode.TIMEOUT );
        }

        Result<Void> err;
        if( ! (err = usersServer.verifyPassword(address.username(), pwd ) ).isOK() ){
            Log.info("User doesn't exist or credentials are wrong..");
            return Result.error( err.error() );
        }

        FeedUser aux = this.getUserInfo(user);

        if( aux != null ){
            synchronized (aux){
                var feeds = aux.getPersonalFeed();
                if(feeds.removeMessage(mid))
                    return Result.error(ErrorCode.NO_CONTENT);
            }
        }

        Log.info("Message not found.");
        return Result.error( ErrorCode.NOT_FOUND);
        //synchronized (aux){
        //    if(aux == null || ! aux.getPersonalFeed().removeMessage(mid) ){
        //        Log.info("Message not found.");
        //        return Result.error( ErrorCode.NOT_FOUND);
        //    }
        //}
        //
        //Map<Long, Message> userFeed;
        //synchronized (localUserFeeds){
        //    userFeed = localUserFeeds.get(user);
        //}

        //synchronized (userFeed){
        //    if(userFeed.remove(mid) == null){
        //        Log.info("Message not found.");
        //        return Result.error( ErrorCode.NOT_FOUND);
        //    }
        //}

        //return Result.error(ErrorCode.NO_CONTENT);
    }

    @Override
    public Result<Message> getMessage(String user, long mid) {

        FeedUser info = this.getUserInfo(user);
        Message res = null;

        if( info != null ){
            synchronized (info){
                var feeds  = info.getPersonalFeed();
                if( (res = feeds.getMessage(mid)) == null ) {
                    Iterator<String> subs = info.getAllSubscriptions().iterator();
                    while(subs.hasNext() && res == null){
                        var creator = this.getCreator(subs.next());
                        synchronized (creator){
                           res = creator.getFeed().getMessage(mid);
                        }
                    }
                }
            }
        }

        //Map<Long, Message> userFeed;
        //Message res = null;

        //synchronized (localUserFeeds){
        //    userFeed = localUserFeeds.get(user);
        //}

        //if(userFeed != null){
        //    synchronized (userFeed){
        //        res = userFeed.get(mid);
        //    }
        //}

        if(res == null){
            Log.info("Message doesn't exit.");
            return Result.error( ErrorCode.NOT_FOUND );
        }

        return Result.ok(res);
    }

    @Override
    public Result<List<Message>> getMessages(String user, long time) {
        Map<Long, Message> userFeed;

        synchronized (localUserFeeds) {
            userFeed = localUserFeeds.get(user);
        }

        if(userFeed == null){
             Log.info("Messages not found.");
             return Result.error( ErrorCode.NOT_FOUND );
        }

        synchronized (userFeed){
            return Result.ok(
                   userFeed.values()
                       .stream()
                       .filter( m -> m.getCreationTime() > time)
                       .toList()
            );
        }
    }

    @Override
    public Result<Void> subscribeUser(String user, String userSub, String pwd) {
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED );
    }

    @Override
    public Result<Void> unSubscribeUser(String user, String userSub, String pwd) {
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED );
    }

    @Override
    public Result<List<String>> listSubs(String user) {
        return Result.error(Result.ErrorCode.NOT_IMPLEMENTED );
    }

    private Users getDomainUserServer(){
        var ds = Discovery.getInstance();
        URI[] serverURIs = ds.knownUrisOf(Formatter.getServiceID(this.domain, Formatter.USERS_SERVICE), 1);
        if(serverURIs.length == 0) return null;
        return UsersClientFactory.get(serverURIs[0]);
    }

    private FeedUser getUserInfo(String user){
        synchronized (feedUsers){
           return feedUsers.get(user);
        }
    }
    public Creator getCreator(String userAddress){
        synchronized (creators){
            return creators.get(userAddress);
        }
    }
}
