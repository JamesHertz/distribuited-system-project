package sd2223.trab2.api.replication;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import sd2223.trab2.api.Update;
import sd2223.trab2.api.rest.FeedsService;

import java.util.List;


@Path(FeedsService.PATH)
public interface ReplicatedFeedsService extends FeedsService{

    String UPDATE = "update";
    String OPERATIONS = "operations";
    String VERSION = "version";


    @POST
    @Path(UPDATE ) // update/<{version}
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    int update(@HeaderParam(FeedsService.HEADER_VERSION) Long version, @QueryParam(FeedsService.SECRET) String secret, Update update);

    @GET
    @Path( OPERATIONS + "/{" + VERSION + "}")
    @Produces(MediaType.APPLICATION_JSON)
    List<Update> getOperations(@PathParam(VERSION) Long version, @PathParam(FeedsService.SECRET) String secret);

}
