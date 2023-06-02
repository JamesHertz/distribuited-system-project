package sd2223.trab2.servers.replication.resource;

import sd2223.trab2.api.Message;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.rest.FeedsService;

import static  sd2223.trab2.servers.replication.ReplicatedServer.VersionProvider;

import java.net.URI;
import java.util.List;

public class ReplicatedResource implements FeedsService, VersionProvider {
    private final Feeds impl;
    private final ZookeeperClient zk;
    private long version = 0L;

    public ReplicatedResource(Feeds impl, String serviceID, URI serverURI) throws  Exception{
        this.impl = impl;
        this.zk = new ZookeeperClient(serviceID, serverURI.toString(), w -> {
            System.out.println("doing something fun :)");
        });
    }

    @Override
    public long postMessage(Long version, String user, String pwd, Message msg) {
        var res = impl.postMessage(user, pwd, msg);
        return 0;
    }

    @Override
    public void removeFromPersonalFeed(Long version, String user, long mid, String pwd) {

    }

    @Override
    public Message getMessage(Long version, String user, long mid) {
        return null;
    }

    @Override
    public List<Message> getMessages(Long version, String user, long time) {
        return null;
    }

    @Override
    public void subUser(Long version, String user, String userSub, String pwd) {

    }

    @Override
    public void unsubscribeUser(Long version, String user, String userSub, String pwd) {

    }

    @Override
    public List<String> listSubs(Long version, String user) {
        return null;
    }

    @Override
    public void createFeed(Long version, String user, String secret) {

    }

    @Override
    public void removeFeed(Long version, String user, String secret) {

    }

    @Override
    public List<Message> subscribeServer(Long version, String domain, String user, String secret) {
        return null;
    }

    @Override
    public void unsubscribeServer(Long version, String domain, String user, String secret) {

    }

    @Override
    public void createExtFeedMessage(Long version, String user, String secret, Message msg) {

    }

    @Override
    public void removeExtFeedMessage(Long version, String user, long mid, String secret) {

    }

    @Override
    public void removeExtFeed(Long version, String user, String secret) {

    }

    @Override
    public long getCurrentVersion() {
        return this.version;
    }
}
