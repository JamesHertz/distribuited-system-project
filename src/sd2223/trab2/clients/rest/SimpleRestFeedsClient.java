package sd2223.trab2.clients.rest;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import sd2223.trab2.api.Message;
import sd2223.trab2.api.Update;
import sd2223.trab2.api.java.Result;
import sd2223.trab2.api.replication.ReplicatedClient;
import sd2223.trab2.api.replication.ReplicatedFeedsService;
import sd2223.trab2.api.rest.FeedsService;
import static  sd2223.trab2.servers.replication.ReplicatedServer.VersionProvider;

import java.net.URI;
import java.util.List;

public class SimpleRestFeedsClient extends RestClient implements ReplicatedClient {

    static final VersionProvider defaultVersionProvider = () -> 0L; // TODO: change this default to return whatever I received :)
    WebTarget target;
    final VersionProvider provider;
    public SimpleRestFeedsClient(URI serverURI) {
        this(serverURI, defaultVersionProvider);
    }

    public SimpleRestFeedsClient(URI serverURI, VersionProvider provider) {
        super(serverURI);
        this.provider = provider;
        this.target = client.target( serverURI ).path(FeedsService.PATH);
    }

    @Override
    public Result<Long> postMessage(String user, String pwd, Message msg) {
        throw new RuntimeException("Not Implemented...");
    }

    @Override
    public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
        throw new RuntimeException("Not Implemented...");
    }

    @Override
    public Result<Message> getMessage(String user, long mid) {
        return this.reTry(() -> {
            var r = target.path(user).path(String.valueOf(mid))
                    .request()
                    .header(FeedsService.HEADER_VERSION, provider.getCurrentVersion())
                    .get();
            return this.toJavaResult(r, Message.class);
        });
    }

    @Override
    public Result<List<Message>> getMessages(String user, long time) {
        return this.reTry(() -> {
            var r = target.path(user)
                    .queryParam(FeedsService.TIME, time)
                    .request()
                    .header(FeedsService.HEADER_VERSION, provider.getCurrentVersion())
                    .get();
            return this.toJavaResult(r, new GenericType<>(){});
        });
    }

    @Override
    public Result<Void> subscribeUser(String user, String userSub, String pwd) {
        throw new RuntimeException("Not Implemented...");
    }

    @Override
    public Result<Void> unSubscribeUser(String user, String userSub, String pwd) {
        throw new RuntimeException("Not Implemented...");
    }

    @Override
    public Result<List<String>> listSubs(String user) {
        throw new RuntimeException("Not Implemented...");
    }

    @Override
    public Result<List<Message>> subscribeServer(String domain, String user, String secret) {
        return this.reTry( () -> {
            var r = target.path( FeedsService.SERVERSUB)
                    .path(domain).path(user)
                    .queryParam(FeedsService.SECRET, secret)
                    .request()
                    .header(FeedsService.HEADER_VERSION, provider.getCurrentVersion())
                    .post(Entity.json(null));
            return this.toJavaResult(r, new GenericType<>(){});
        });
    }

    @Override
    public Result<Void> unsubscribeServer(String domain, String user, String secret) {
        return this.reTry( () -> {
            var r = target.path(FeedsService.SERVERSUB)
                    .path(domain).path(user)
                    .queryParam(FeedsService.SECRET, secret)
                    .request()
                    .header(FeedsService.HEADER_VERSION, provider.getCurrentVersion())
                    .delete();
            return this.toJavaResult(r, Void.class);
        });
    }

    @Override
    public Result<Void> createFeed(String user, String secret) {
        return this.reTry( () -> {
            var r = target.queryParam(FeedsService.SECRET, secret)
                    .request()
                    .header(FeedsService.HEADER_VERSION, provider.getCurrentVersion())
                    .post(Entity.entity(user, MediaType.APPLICATION_JSON_TYPE));
            return this.toJavaResult(r, Void.class);
        });
    }

    @Override
    public Result<Void> createExtFeedMessage(String user, String secret, Message msg) {
        return this.reTry(() -> {
            var r = target.path(FeedsService.EXTERNAL)
                    .path(user)
                    .queryParam(FeedsService.SECRET, secret)
                    .request()
                    .header(FeedsService.HEADER_VERSION, provider.getCurrentVersion())
                    .post(Entity.entity(msg, MediaType.APPLICATION_JSON_TYPE ));
            return this.toJavaResult(r, Void.class);
        });
    }

    @Override
    public Result<Void> removeExtFeedMessage(String user, long mid, String secret) {
        return this.reTry(() -> {
            var res = target.path(FeedsService.EXTERNAL)
                    .path(user)
                    .path(String.valueOf(mid))
                    .queryParam(FeedsService.SECRET, secret)
                    .request()
                    .header(FeedsService.HEADER_VERSION, provider.getCurrentVersion())
                    .delete();
            return this.toJavaResult(res, Void.class);
        });
    }

    @Override
    public Result<Void> removeFeed(String user, String secret) {
        return this.reTry(() -> {
            var res = target.path(user)
                    .queryParam(FeedsService.SECRET, secret)
                    .request()
                    .header(FeedsService.HEADER_VERSION, provider.getCurrentVersion())
                    .delete();

            return this.toJavaResult(res, Void.class);
        });
    }

    @Override
    public Result<Void> removeExtFeed(String user, String secret) {
        return this.reTry(() -> {
            var res = target.path(FeedsService.EXTERNAL)
                    .path(user)
                    .queryParam(FeedsService.SECRET, secret)
                    .request()
                    .header(FeedsService.HEADER_VERSION, provider.getCurrentVersion())
                    .delete();

            return this.toJavaResult(res, Void.class);
        });
    }


    @Override
    public Result<Integer> update(String secret, Update update) {
        return this.reTry( () -> {
            var res = target.path(ReplicatedFeedsService.UPDATE)
                    .queryParam(FeedsService.SECRET, secret)
                    .request()
                    .header(FeedsService.HEADER_VERSION, provider.getCurrentVersion())
                    .post(Entity.entity(update, MediaType.APPLICATION_JSON));

            return this.toJavaResult(res, Integer.class);
        });
    } // threshold -> [ 20 ]

    @Override
    public Result<List<Update>> getOperations(long version, String secret) {
        return this.reTry( () -> {
            var res = target.path(ReplicatedFeedsService.OPERATIONS).path(Long.toString(version))
                    .queryParam(FeedsService.SECRET, secret)
                    .request()
                    .get();

            return this.toJavaResult(res, new GenericType<>(){});
        });
    }
}
