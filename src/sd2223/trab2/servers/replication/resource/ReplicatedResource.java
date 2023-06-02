package sd2223.trab2.servers.replication.resource;

import io.netty.util.Timeout;
import sd2223.trab2.api.Message;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.rest.FeedsService;
import sd2223.trab2.clients.ClientFactory;
import sd2223.trab2.servers.rest.resources.RestResource;

import static  sd2223.trab2.servers.replication.ReplicatedServer.VersionProvider;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

public class ReplicatedResource  extends RestResource implements FeedsService, VersionProvider{
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
        boolean atLeastOne = false;
        if(zk.getState() == ZookeeperClient.State.PRIMARY){
            //Primeiro, executar nos outros, e quanto pelo menos 1 responder, eu retorno o resultado

            var servers = zk.getServers();

            for (var server : servers) {
                var client = ClientFactory.getFeedsClient(server.severURI());
                var res = client.postMessage(user, pwd, msg); //Outro server tenta fazer...

                if(res.isOK()){ //Se pelo menos 1 conseguir
                    atLeastOne = true; //Agora sabemos que vamos responder
                }
            }
            if(atLeastOne) { //Se pelo menos 1 secundario responder, entao temos seguranca que pelo menos 1 replica got our back
                var res = impl.postMessage(user, pwd, msg);
                return super.fromJavaResult(res); //Entao respondemos
            }else{
                return 0; //Erro...
            }
        }else{
            //Forward to primary

            return 0;
        }
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
    public long getCurrentVersion() {
        return this.version;
    }
}
