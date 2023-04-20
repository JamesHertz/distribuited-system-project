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


    private static URI[] getURIs(String serviceID){
        var ds = Discovery.getInstance();
        var uris = ds.knownUrisOf(serviceID, 1);
        if( uris.length == 0){
            System.err.println("service " + serviceID + " not found.");
            System.exit(1);
        }
        return uris;
    }

    private static void printResult(Result<?> res){
        if(res.isOK())
            System.out.println("User created successfully!!!");
        else {
            System.err.println("Error: " + res.error());
        }
    }
     public static void main(String[] args) {
        String[] domains = {"fct", "di", "fct"};
        String[] names   = {"james", "hertz", "jhertz"};


         var user = new User();
         user.setDomain(domains[0]);
         user.setPwd("1234");
         user.setName("jhertz");
         user.setDisplayName("James Hertz");

         for(int i = 0; i < names.length; ++i){
             var uris = getURIs(Formatter.getServiceID(domains[i], Formatter.USERS_SERVICE));
             var client = new SoapUsersClient(uris[0]);
             user.setName(names[i]);
             user.setDomain(domains[i]);
             printResult(
                     client.createUser(user)
             );

         }
         var uris = getURIs(Formatter.getServiceID(domains[0], Formatter.FEEDS_SERVICE));
         var feedClient = new SoapFeedsClient(uris[0]);

         printResult(
                 feedClient.subscribeUser(
                         names[0] + "@" + domains[0],
                         names[1] + "@" + domains[1],
                         user.getPwd()
                 )
         );

         printResult(
                 feedClient.subscribeUser(
                         names[0] + "@" + domains[0],
                         names[2] + "@" + domains[2],
                         user.getPwd()
                 )
         );

         System.out.println("Shutting down :)");
     }

}
