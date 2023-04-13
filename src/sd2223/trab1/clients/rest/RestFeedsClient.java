package sd2223.trab1.clients.rest;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
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
    public Result<Void> subscribeServer(String user, String domain) {
        return super.reTry( () -> {
            var r = target.path(FeedsService.SUB)
                    .path(user).path( FeedsService.SERVER )
                    .request()
                    .post(Entity.json(null));
            return super.toJavaResult(r, Void.class);
        });
    }
}
