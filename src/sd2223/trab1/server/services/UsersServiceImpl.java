package sd2223.trab1.server.services;

import jakarta.ws.rs.WebApplicationException;
import static jakarta.ws.rs.core.Response.Status;
import sd2223.trab1.api.User;
import sd2223.trab1.api.rest.UsersService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UsersServiceImpl implements UsersService {

    private static final String USER_ADDR_FORMAT = "%s@%s";
    private static Logger Log = Logger.getLogger(UsersServiceImpl.class.getName());
    private final String domain;
    private final Map<String, User> users;

    public UsersServiceImpl(String domain){
        this.domain = domain;
        this.users = new HashMap<>(); // :)
    }

    @Override
    public String createUser(User user) {
        Log.info("createUser: user=" + user);
        if(invalidUser(user))
            throw new WebApplicationException( Status.BAD_REQUEST );

        String userName = user.getName();
        synchronized (users){
            if(users.containsKey(userName))
                throw new WebApplicationException( Status.CONFLICT );
            users.put(userName, user);
        }

        return String.format(USER_ADDR_FORMAT, userName, domain);
    }

    @Override
    public User getUser(String name, String pwd) {
        Log.info("getUser: name=" + name + " ; pwd= " + pwd);

        if( name == null || pwd == null ){
            Log.info("Username or password nil.");
            throw new WebApplicationException( Status.BAD_REQUEST );
        }

        User user;
        synchronized (users){
            user = users.get(name);
        }

        if(user == null){
            Log.info("User doesn't exist.");
            throw new WebApplicationException( Status.NOT_FOUND );
        }

        if(! user.getPwd().equals(pwd) ){
            Log.info("Wrong password");
            throw new WebApplicationException( Status.FORBIDDEN );
        }

        return user;
    }

    @Override
    public User updateUser(String name, String pwd, User user) {
        Log.info("updateUser: name=" + name + " ; pwd= " + pwd + " ; user=" + user) ;

        if( name == null || pwd == null /*|| ! name.equals(user.getName())*/ ) { // todo: ask professor
            Log.info("Invalid username or password.");
            throw new WebApplicationException( Status.BAD_REQUEST ); // todo: ask professor
        }

        User oldUser;
        synchronized (users){
            oldUser = users.get(name);
        }

        if(oldUser == null) {
            Log.info("User doesn't exist.");
            throw new WebApplicationException( Status.NOT_FOUND );
        }

        if(!  oldUser.getPwd().equals(pwd)  ) {
            Log.info("Wrong password.");
            throw new WebApplicationException( Status.FORBIDDEN);
        }

        if( user.getDisplayName() == null )
            user.setDisplayName( oldUser.getDisplayName() );

        if( user.getPwd() == null )
            user.setPwd( oldUser.getPwd() );

        user.setDomain(domain); // hello world :)
        user.setName(name);

        synchronized (users){
            users.put(name, user);
        }

        return user;
    }

    @Override
    public User deleteUser(String name, String pwd) {
        Log.info("deleteUser: name=" + name + " ; pwd=" + pwd);
        User user;

        if( name == null || pwd == null ){
            Log.info("Username or password nil.");
            throw new WebApplicationException( Status.BAD_REQUEST );
        }

        synchronized ( users ) {
            user = users.get(name);
        }

        if( user == null) {
            Log.info("User not found.");
            throw new WebApplicationException( Status.NOT_FOUND );
        }

        if( ! user.getPwd().equals(pwd) ) {
            Log.info("Wrong password.");
            throw new WebApplicationException( Status.FORBIDDEN );
        }

        synchronized ( users ){
            users.remove(name);
        }

        return user;
    }

    @Override
    public List<User> searchUsers(String pattern) {
        Log.info("searchUsers: query=" + pattern);
        synchronized (users){
            return users.values()
                    .stream()
                    .filter(u -> u.getName().contains(pattern) )
                    .collect(Collectors.toList());
        }
    }
    private boolean invalidUser(User user){
        return user.getDomain() == null || user.getName() == null || user.getDisplayName() == null
                || user.getPwd() == null || !domain.equals(user.getDomain());
    }
}
