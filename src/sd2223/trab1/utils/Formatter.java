package sd2223.trab1.utils;

import java.net.URI;

public class Formatter {

    public static final String REST_SERVER_FMT = "http://%s:%s/rest";
    public static final String SOAP_SERVER_FMT = "http://%s:%s/soap";
    public static final int REST_PORT = 8080;
    public static final int SOAP_PORT = 8081;
    public static final String SERVER_NAME_FORMAT = "%s:%s";
    public static final String USERS_SERVICE      = "users";
    public static final String FEEDS_SERVICE      = "feeds";
    public static final String USER_ADDRESS_SEP   = "@";

    public record UserAddress(String username, String domain) {};
    
    public static String getServiceID(String domain, String service){
       return String.format(SERVER_NAME_FORMAT, domain, service);
    }

    public static URI getRestURI(String host, int port){
        return URI.create(String.format(REST_SERVER_FMT, host, port));
    }

    public static URI getSoapURI(String host, int port) {
        return URI.create(String.format(SOAP_SERVER_FMT, host, port));
    }

    public static String makeUserAddress(String username, String domain) {
       return username + USER_ADDRESS_SEP + domain;
    }

    public static UserAddress getUserAddress(String userAddress) {
        String[]  info = userAddress.split(USER_ADDRESS_SEP);
        return (info.length != 2) ? null : new UserAddress(info[0], info[1]);
    }
}
