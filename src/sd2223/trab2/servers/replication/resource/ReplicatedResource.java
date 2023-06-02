package sd2223.trab2.servers.replication.resource;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import sd2223.trab2.api.Message;
import static  sd2223.trab2.api.Operations.*;
import sd2223.trab2.api.Update;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.java.Result;
import sd2223.trab2.api.replication.ReplicatedFeedsService;
import sd2223.trab2.clients.ClientFactory;
import sd2223.trab2.servers.rest.resources.RestResource;
import sd2223.trab2.utils.JSON;
import sd2223.trab2.utils.Secret;

import static  sd2223.trab2.servers.replication.ReplicatedServer.VersionProvider;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ReplicatedResource  extends RestResource implements ReplicatedFeedsService, VersionProvider{
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
        switch (this.zk.getState()){
            case PRIMARY -> {
                Update up = Update.toUpdate(
                        CREATE_MESSAGE, user, pwd, JSON.encode(msg)
                );
                CountDownLatch cd = new CountDownLatch(1); // todo: look at this later
                for(var server : this.zk.getServers()){
                    if(server.serverID() == this.zk.getServerID()) continue;
                    new Thread( () -> {
                        var client = ClientFactory.getReplicatedClient(server.severURI(), this);
                        var res = client.update(Secret.getSecret(), up);
                        if(res.isOK()) cd.countDown();
                    }).start();
                }
                try{
                    cd.await(10, TimeUnit.SECONDS);
                }catch (InterruptedException ignore){ }
                if( cd.getCount() == 2 ) {
                    // error
                }
            }
            case OTHER -> {
                throw new WebApplicationException(
                        Response.temporaryRedirect(
                                this.zk.getPrimaryNode().severURI()
                        ).build()
                );
            }
            case DISCONNECTED -> {
                System.out.println("server down return error :)");
            }
        }
        return 0L;
    }

    @Override
    public void removeFromPersonalFeed(Long version, String user, long mid, String pwd) {

    }

    @Override
    public Message getMessage(Long version, String user, long mid) {
        //Verificar versao...
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
    public void update(Long version, String secret, Update update) {

    }

    @Override
    public List<Update> getOperations(Long version, String secret) {
        return null;
    }


    @Override
    public synchronized long getCurrentVersion() {
        return this.version;
    }

}
