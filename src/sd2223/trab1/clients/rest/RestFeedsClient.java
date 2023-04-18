package sd2223.trab1.clients.rest;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.rest.FeedsService;

import java.net.URI;
import java.util.List;

public class RestFeedsClient extends RestClient implements Feeds {

    final WebTarget target;
    public RestFeedsClient(URI serverURI) {
        super(serverURI);
        target = client.target( serverURI ).path(FeedsService.PATH);
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
        throw new RuntimeException("Not Implemented...");
    }

    @Override
    public Result<List<Message>> getMessages(String user, long time) {
        throw new RuntimeException("Not Implemented...");
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
    public Result<List<Message>> subscribeServer(String domain, String user) {
        return super.reTry( () -> {
            var r = target.path( FeedsService.SERVERSUB)
                    .path(domain).path(user)
                    .request()
                    .post(Entity.json(null));
            return super.toJavaResult(r, new GenericType<List<Message>>(){});
        });
    }

    @Override
    public Result<Void> unsubscribeServer(String domain, String user) {
        return super.reTry( () -> {
            var r = target.path(FeedsService.SERVERSUB)
                    .path(domain).path(user)
                    .request()
                    .delete();
            return super.toJavaResult(r, Void.class);
        });
    }

    @Override
    public Result<Void> createFeed(String user) {
        return super.reTry( () -> {
            var r = target.request()
                    .post(Entity.entity(user, MediaType.APPLICATION_JSON_TYPE));
            return super.toJavaResult(r, Void.class);
        });
    }

    @Override
    public Result<Void> createExtFeedMessage(String user, Message msg) {
        return super.reTry(() -> {
            var r = target.path(FeedsService.EXTERNAL)
                    .path(user)
                    .request()
                    .post(Entity.entity(msg, MediaType.APPLICATION_JSON_TYPE ));
            return super.toJavaResult(r, Void.class);
        });
    }

    @Override
    public Result<Void> removeExtFeedMessage(String user, long mid) {
        return super.reTry(() -> {
            var res = target.path(FeedsService.EXTERNAL)
                    .path(user)
                    .path(String.valueOf(mid))
                    .request()
                    .delete();
            return super.toJavaResult(res, Void.class);
        });
    }

    @Override
    public Result<Void> removeFeed(String user) {
        return super.reTry(() -> {
            var res = target.path(user)
                    .request()
                    .delete();

            return super.toJavaResult(res, Void.class);
        });
    }

    @Override
    public Result<Void> removeExtFeed(String user) {
        return super.reTry(() -> {
            var res = target.path(FeedsService.EXTERNAL)
                    .path(user)
                    .request()
                    .delete();

            return super.toJavaResult(res, Void.class);
        });
    }

}
