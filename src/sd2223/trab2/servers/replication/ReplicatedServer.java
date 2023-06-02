package sd2223.trab2.servers.replication;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.rest.FeedsService;
import sd2223.trab2.servers.replication.resource.ReplicatedResource;

import javax.net.ssl.SSLContext;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class ReplicatedServer {
    private static final Logger Log = Logger.getLogger(ReplicatedServer.class.getName());

    public interface VersionProvider{
        long getCurrentVersion();
    }

    @Provider
    public static class VersionFilter implements ContainerResponseFilter {
        VersionProvider provider;

        VersionFilter( VersionProvider provider) {
            this.provider = provider;
        }

        @Override
        public void filter(ContainerRequestContext request, ContainerResponseContext response)
                throws IOException {
            response.getHeaders().add(FeedsService.HEADER_VERSION, provider.getCurrentVersion());
        }
    }

    public static void runServer(URI serverURI,  String serviceID, boolean is_primary, Feeds service) {
        try{

            var resource =  new ReplicatedResource(service, serviceID, serverURI, is_primary);
            ResourceConfig config = new ResourceConfig();
            config.register( resource );
            config.register(new VersionFilter( resource ));

            JdkHttpServerFactory.createHttpServer(serverURI, config, SSLContext.getDefault());
            Log.info(String.format("%s Replicated serverRest ready @ %s\n", is_primary ? "primary" : "secondary", serverURI));
        }catch (Exception e){
            e.printStackTrace();
        }

    }

}
