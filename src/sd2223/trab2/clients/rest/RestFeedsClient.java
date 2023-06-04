package sd2223.trab2.clients.rest;

import sd2223.trab2.api.java.Result;
import sd2223.trab2.api.rest.FeedsService;
import sd2223.trab2.discovery.Discovery;
import sd2223.trab2.utils.Formatter;

import java.net.URI;
import java.util.function.Supplier;

public class RestFeedsClient extends SimpleRestFeedsClient {

    private final String serverID;
    public RestFeedsClient(URI serverURI, String serverID){
        super(serverURI);
        this.serverID = serverID;
    }


    @Override
    protected <T> Result<T> reTry(Supplier<Result<T>> func) {
        var res = super.reTry(func);
        System.out.println("res=" + res);
        if(!res.isOK() && res.error()  == Result.ErrorCode.TIMEOUT ){
            var newServerURI = Discovery.getInstance().getRandomUriOf(serverID);
            if(newServerURI != null)
                this.target = this.client.target(newServerURI).path(FeedsService.PATH);
            System.out.println("newURI: " + newServerURI);
        }
        return res;
    }

    @Override
    public String toString() {
        return this.serverID;
    }
}
