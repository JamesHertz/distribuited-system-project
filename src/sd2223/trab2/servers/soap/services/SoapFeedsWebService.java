package sd2223.trab2.servers.soap.services;

import jakarta.jws.WebService;
import sd2223.trab2.api.Message;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.soap.FeedsException;
import sd2223.trab2.api.soap.FeedsService;

import java.util.List;

@WebService(serviceName=FeedsService.NAME, targetNamespace=FeedsService.NAMESPACE, endpointInterface=FeedsService.INTERFACE)
public class SoapFeedsWebService extends SoapWebService<FeedsException> implements FeedsService {
    private final Feeds impl;

    public SoapFeedsWebService(Feeds impl){
        super(res ->  new FeedsException( res.error().toString() ));
        this.impl = impl;
    }

    @Override
    public long postMessage(String user, String pwd, Message msg) throws FeedsException {
        return super.fromJavaResult( impl.postMessage(user, pwd, msg));
    }

    @Override
    public void removeFromPersonalFeed(String user, long mid, String pwd) throws FeedsException {
        super.fromJavaResult( impl.removeFromPersonalFeed(user, mid, pwd));
    }

    @Override
    public Message getMessage(String user, long mid) throws FeedsException {
        return super.fromJavaResult( impl.getMessage(user, mid));
    }

    @Override
    public List<Message> getMessages(String user, long time) throws FeedsException {
        return super.fromJavaResult( impl.getMessages(user, time) );
    }

    @Override
    public void subUser(String user, String userSub, String pwd) throws FeedsException {
        super.fromJavaResult( impl.subscribeUser(user, userSub, pwd) );
    }

    @Override
    public void unsubscribeUser(String user, String userSub, String pwd) throws FeedsException {
        super.fromJavaResult( impl.unSubscribeUser(user, userSub, pwd) );
    }

    @Override
    public List<String> listSubs(String user) throws FeedsException {
        return super.fromJavaResult( impl.listSubs(user) );
    }

    @Override
    public List<Message> subscribeSever(String domain, String user, String secret) throws FeedsException {
        return super.fromJavaResult( impl.subscribeServer(domain, user, secret) );
    }

    @Override
    public void unsubscribeServer(String domain, String user, String secret) throws FeedsException {
        super.fromJavaResult( impl.unsubscribeServer(domain, user, secret) );
    }

    @Override
    public void createFeed(String user, String secret) throws FeedsException {
        super.fromJavaResult( impl.createFeed( user, secret) );
    }

    @Override
    public void createExtFeedMessage(String user, String secret, Message msg) throws FeedsException {
        super.fromJavaResult( impl.createExtFeedMessage(user, secret, msg) );
    }

    @Override
    public void removeExtFeedMessage(String user, long mid, String secret) throws FeedsException {
        super.fromJavaResult( impl.removeExtFeedMessage(user, mid, secret) );
    }

    @Override
    public void removeFeed(String user, String secret) throws FeedsException {
        super.fromJavaResult( impl.removeFeed(user, secret) );
    }

    @Override
    public void removeExtFeed(String user, String secret) throws FeedsException {
        super.fromJavaResult( impl.removeExtFeed(user, secret) );
    }

}
