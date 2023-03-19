package sd2223.trab1.server;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import sd2223.trab1.server.services.FeedsServiceImpl;
import sd2223.trab1.server.services.UsersServiceImpl;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    // <nome-do-domínio>:<serviço><tab><uri-do-servidor>
    private static final Pattern PATTERN = Pattern.compile("(.+):(.+)\t(.+)");

    public static void main(String[] args) {
        //if(args.length < 1){
        //    System.out.println("usage: <server-config>");
        //    System.out.println("ERROR: wrong number of arguments");
        //    System.exit(1);
        //}

        String example = "fct:users\thttp://localhost:8080/rest";
        Matcher matcher = PATTERN.matcher(example);
        if(matcher.matches()){
            String serverName = matcher.group(1);
            String service    = matcher.group(2);
            URI serverURI     = URI.create(matcher.group(3));
            ResourceConfig config = new ResourceConfig();
            switch (service){
                case "feeds" -> config.register(FeedsServiceImpl.class);
                case "users" -> config.register(UsersServiceImpl.class);
                default -> {
                    System.out.println("ERROR: invalid service: " + service);
                    System.exit(1);
                }
            }

            JdkHttpServerFactory.createHttpServer(serverURI, config);
            System.out.printf("%s Server running %s service @ %s\n", serverName, service, serverURI);
        } else {
            System.out.println("ERROR: invalid server-config");
        }

    }
}
