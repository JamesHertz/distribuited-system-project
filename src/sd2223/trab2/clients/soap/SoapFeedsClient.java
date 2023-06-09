package sd2223.trab2.clients.soap;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import sd2223.trab2.api.Message;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.java.Result;
import sd2223.trab2.api.soap.FeedsService;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.List;

public class SoapFeedsClient extends SoapClient implements Feeds {
    public SoapFeedsClient(URI serverURI) {
        super(serverURI);
    }

    private FeedsService stub;
    private synchronized FeedsService stub(){
        if(stub == null ){
            QName QNAME  = new QName(FeedsService.NAMESPACE, FeedsService.NAME);
            Service service = Service.create(super.toURL(super.uri + WSDL), QNAME);
            this.stub = service.getPort(sd2223.trab2.api.soap.FeedsService.class);
            super.setTimeouts((BindingProvider) this.stub);
        }
        return stub;
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
        return super.reTry( () -> super.toJavaResult( () -> stub().getMessage(user, mid) ));
    }

    @Override
    public Result<List<Message>> getMessages(String user, long time) {
        return super.reTry( () -> super.toJavaResult( () -> stub().getMessages(user, time) ));
    }

    @Override
    public Result<Void> subscribeUser(String user, String userSub, String pwd) {
        return super.reTry( () -> super.toJavaResult( () -> stub().subUser(user, userSub, pwd) ) );
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
        return super.reTry( () -> super.toJavaResult( () -> stub().subscribeSever(domain, user, secret) ) );
    }

    @Override
    public Result<Void> createFeed(String user, String secret) {
        return super.reTry( () -> super.toJavaResult( () -> stub().createFeed( user, secret ) ) );
    }

    @Override
    public Result<Void> createExtFeedMessage(String user, String secret, Message msg) {
        return super.reTry( () -> super.toJavaResult( () -> stub().createExtFeedMessage(user, secret, msg) ));
    }

    @Override
    public Result<Void> removeExtFeedMessage(String user, long mid, String secret) {
        return super.reTry( () -> super.toJavaResult( () -> stub().removeExtFeedMessage(user, mid, secret) ));
    }

    @Override
    public Result<Void> removeFeed(String user, String secret) {
        return super.reTry( () -> super.toJavaResult( () -> stub().removeFeed(user, secret) ));
    }

    @Override
    public Result<Void> removeExtFeed(String user, String secret) {
        return super.reTry( () -> super.toJavaResult( () -> stub().removeExtFeed(user, secret) ));
    }

    @Override
    public Result<Void> unsubscribeServer(String domain, String user, String secret) {
         return super.reTry( () -> super.toJavaResult( () -> stub().unsubscribeServer(domain, user, secret) ));
    }
}
