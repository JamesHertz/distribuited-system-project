package sd2223.trab2.api.replication;

import sd2223.trab2.api.Update;
import sd2223.trab2.api.java.Feeds;
import sd2223.trab2.api.java.Result;

import java.util.List;

public interface ReplicatedClient extends Feeds {

    Result<Integer> update(String secret, Update update);

    Result<List<Update>> getUpdates(String secret);
}
