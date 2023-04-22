package sd2223.trab1.clients.rest;

import static sd2223.trab1.api.java.Result.error;
import static sd2223.trab1.api.java.Result.ok;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import jakarta.ws.rs.core.GenericType;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.java.Result.ErrorCode;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class RestClient {
    private static Logger Log = Logger.getLogger(RestClient.class.getName());

    protected static final int READ_TIMEOUT = 5000;
    protected static final int CONNECT_TIMEOUT = 5000;

    protected static final int MAX_RETRIES = 10;
    protected static final int RETRY_SLEEP = 3000;

    final URI serverURI;
    final Client client;
    final ClientConfig config;

    RestClient(URI serverURI) {
        this.serverURI = serverURI;
        this.config = new ClientConfig();

        config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);

        this.client = ClientBuilder.newClient(config);
    }

    /**
     * Function that tries to execute "func", for a maximum of MAX_RETRIES in case it fails
     */
    protected <T> Result<T> reTry(Supplier<Result<T>> func) {
        for (int i = 0; i < MAX_RETRIES; i++)
            try {
                return func.get();
            } catch (ProcessingException x) {
                Log.fine("Timeout: " + x.getMessage());
                sleep_ms(RETRY_SLEEP);
            } catch (Exception x) {
                x.printStackTrace();
                return Result.error(ErrorCode.INTERNAL_ERROR);
            }
        return Result.error(ErrorCode.TIMEOUT);
    }

    protected <T> Result<T> toJavaResult(Response r, Class<T> entityType) {
        return this.toJavaResult(r, () -> r.readEntity(entityType));
    }

    protected <T> Result<T> toJavaResult(Response r, GenericType<T> genericType) {
        return this.toJavaResult(r, () -> r.readEntity(genericType));
    }

    /**
     * Verify if the response is valid, and return a Result with an appropriated response (ok with value, ok without value, error)
     */
    private <T> Result<T> toJavaResult(Response r, Supplier<T> reader) {
        try {
            var status = r.getStatusInfo().toEnum();
            if (status == Status.OK && r.hasEntity())
                return ok(reader.get());
            else if (status == Status.NO_CONTENT) return ok();

            return error(getErrorCodeFrom(status.getStatusCode()));
        } finally {
            r.close();
        }
    }


    public static ErrorCode getErrorCodeFrom(int status) {
        return switch (status) {
            // todo: add timeout :)
            case 200, 209 -> ErrorCode.OK;
            case 409 -> ErrorCode.CONFLICT;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 400 -> ErrorCode.BAD_REQUEST;
            case 501 -> ErrorCode.NOT_IMPLEMENTED;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }

    @Override
    public String toString() {
        return serverURI.toString();
    }

    private void sleep_ms(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
