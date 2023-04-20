package sd2223.trab1.servers.soap.services;

import jakarta.jws.WebService;
import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.soap.FeedsException;
import sd2223.trab1.api.soap.FeedsService;

import java.util.List;

@WebService(serviceName=FeedsService.NAME, targetNamespace=FeedsService.NAMESPACE, endpointInterface=FeedsService.INTERFACE)
public class SoapFeedsWebService extends SoapWebService<FeedsException> implements FeedsService {
    private final Feeds impl;

    public SoapFeedsWebService(Feeds impl){
        super(res ->  new FeedsException(res.error().toString()));
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
        var res = impl.subscribeUser(user, userSub, pwd);
        System.out.println("subUser: error=" + res.error());
        super.fromJavaResult( res );
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
    public List<Message> subscribeSever(String domain, String user) throws FeedsException {
        return super.fromJavaResult( impl.subscribeServer(domain, user) );
    }

    @Override
    public void unsubscribeSever(String domain, String user) throws FeedsException {
        super.fromJavaResult( impl.unsubscribeServer(domain, user) );
    }

    @Override
    public void createFeed(String user) throws FeedsException {
        super.fromJavaResult( impl.createFeed( user ) );
    }

    @Override
    public void createExtFeedMessage(String user, Message msg) throws FeedsException {
        super.fromJavaResult( impl.createExtFeedMessage(user, msg) );
    }

    @Override
    public void removeExtFeedMessage(String user, long mid) throws FeedsException {
        super.fromJavaResult( impl.removeExtFeedMessage(user, mid) );
    }

    @Override
    public void removeFeed(String user) throws FeedsException {
        super.fromJavaResult( impl.removeFeed(user) );
    }

    @Override
    public void removeExtFeed(String user) throws FeedsException {
        super.fromJavaResult( impl.removeExtFeed(user) );
    }

}
