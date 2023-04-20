package sd2223.trab1.clients.soap;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import sd2223.trab1.api.Message;
import sd2223.trab1.api.User;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.soap.FeedsService;
import sd2223.trab1.discovery.Discovery;
import sd2223.trab1.utils.Formatter;

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
            this.stub = service.getPort(sd2223.trab1.api.soap.FeedsService.class);
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
        return super.reTry( () -> super.toJavaResult( () -> stub().subscribeSever(domain, user) ) );
    }

    @Override
    public Result<Void> createFeed(String user) {
        return super.reTry( () -> super.toJavaResult( () -> stub().createFeed( user ) ) );
    }

    @Override
    public Result<Void> createExtFeedMessage(String user, Message msg) {
        return super.reTry( () -> super.toJavaResult( () -> stub().createExtFeedMessage(user, msg) ));
    }

    @Override
    public Result<Void> removeExtFeedMessage(String user, long mid) {
        return super.reTry( () -> super.toJavaResult( () -> stub().removeExtFeedMessage(user, mid) ));
    }

    @Override
    public Result<Void> removeFeed(String user) {
        return super.reTry( () -> super.toJavaResult( () -> stub().removeFeed(user) ));
    }

    @Override
    public Result<Void> removeExtFeed(String user) {
        return super.reTry( () -> super.toJavaResult( () -> stub().removeExtFeed(user) ));
    }

    @Override
    public Result<Void> unsubscribeServer(String domain, String user) {
         return super.reTry( () -> super.toJavaResult( () -> stub().unsubscribeSever(domain, user) ));
    }


    // public static void main(String[] args) {
    //     var domain = args[0];
    //     var ds = Discovery.getInstance();
    //     var uris = ds.knownUrisOf(Formatter.getServiceID(domain, Formatter.USERS_SERVICE), 1);
    //     if( uris.length == 0){
    //         System.err.println("users service for domain: " + domain + " not found.");
    //         System.exit(1);
    //     }

    //     var user = new User();
    //     user.setDomain(domain);
    //     user.setPwd("1234");
    //     user.setName("jhertz");
    //     user.setDisplayName("James Hertz");

    //     var client = new SoapUsersClient(uris[0]);
    //     var res = client.createUser(user);
    //     if(res.isOK())
    //         System.out.println("User created successfully!!!");
    //     else {
    //         System.err.println("Error: " + res.error());
    //     }
    // }

}
