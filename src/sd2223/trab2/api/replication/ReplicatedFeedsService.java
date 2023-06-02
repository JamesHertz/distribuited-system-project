package sd2223.trab2.api.replication;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import sd2223.trab2.api.Update;
import sd2223.trab2.api.rest.FeedsService;

import java.util.List;


public interface ReplicatedFeedsService extends FeedsService {

    String UPDATE = "update";
    String OPERATIONS = "operations";
    String VERSION = "version";


    @POST
    @Path(UPDATE ) // update/<{version}
    @Consumes(MediaType.APPLICATION_JSON)
    void update(@HeaderParam(HEADER_VERSION) Long version, @QueryParam(SECRET) String secret, Update update);

    @GET
    @Path( OPERATIONS + "/{" + VERSION + "}")
    @Produces(MediaType.APPLICATION_JSON)
    List<Update> getOperations(@PathParam(VERSION) Long version, @PathParam(SECRET) String secret);

}
