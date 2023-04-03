package sd2223.utils;

import java.net.URI;

public class Formatter {

    public static final String SERVER_URI_FMT = "http://%s:%s/rest";
    public static final String SERVER_NAME_FORMAT = "%s:%s";
    public static final String USERS_SERVICE = "users";
    public static final String FEEDS_SERVICE = "feeds";
    public static final int PORT = 8080;
    public static final String USER_FORMAT_SEP = "@";

    public static String getServiceID(String domain, String service){
       return String.format(SERVER_NAME_FORMAT, domain, service);
    }

    public static URI getRestURI(String host, int port){
        return URI.create(String.format(SERVER_URI_FMT, host, port));
    }
}
