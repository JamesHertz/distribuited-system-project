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

    /**
     * Receives updates from the primary
     * @param version the primary version
     * @param secret shared secret
     * @param update the update
     * @return 200 and the response code of the operation if it was able to execute the operation
     *        400 if the request is invalid
     */
    @POST
    @Path(UPDATE )
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    int update(@HeaderParam(FeedsService.HEADER_VERSION) Long version, @QueryParam(FeedsService.SECRET) String secret, Update update);

    /**
     *
     * @param version client version
     * @param secret shared secret
     * @return 200, all the updates from version to the most recent version that the servers knows
     *        404 if the sever version is lower the client version
     */
    @GET
    @Path( OPERATIONS + "/{" + VERSION + "}")
    @Produces(MediaType.APPLICATION_JSON)
    List<Update> getOperations(@PathParam(VERSION) long version, @QueryParam(FeedsService.SECRET) String secret);

}
