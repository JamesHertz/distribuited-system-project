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

    // it's the address of a user
    public record UserAddress(String username, String domain) {};

    /**
     * @param domain server domain
     * @param service  the server service
     * @return Makes the ID of the server be advertised in the multicast address
     */
    public static String getServiceID(String domain, String service){
       return String.format(SERVER_NAME_FORMAT, domain, service);
    }

    /**
     * @param host host of the server
     * @param port the server port
     * @return a rest URI (ends with /rest)
     */
    public static URI getRestURI(String host, int port){
        return URI.create(String.format(REST_SERVER_FMT, host, port));
    }

    /**
     * @param host host of the server
     * @param port the server port
     * @return a SOAP URI (ends with /soap)
     */
    public static URI getSoapURI(String host, int port) {
        return URI.create(String.format(SOAP_SERVER_FMT, host, port));
    }

    /**
     * @param username name of the user
     * @param domain the domain of the user
     * @return the string address of a user
     */
    public static String makeUserAddress(String username, String domain) {
       return username + USER_ADDRESS_SEP + domain;
    }

    /**
     * @param userAddress the string that represents the user address (user@domain)
     * @return a record with attributes for the name and the domain
     */
    public static UserAddress getUserAddress(String userAddress) {
        String[]  info = userAddress.split(USER_ADDRESS_SEP);
        return (info.length != 2) ? null : new UserAddress(info[0], info[1]);
    }
}
