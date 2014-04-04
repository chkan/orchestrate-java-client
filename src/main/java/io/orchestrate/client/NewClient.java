package io.orchestrate.client;

import java.io.IOException;

// TODO
public interface NewClient {

    // TODO
    public void close() throws IOException;

    // TODO
    public OrchestrateRequest<Boolean> deleteCollection(final String collection);

    // TODO
    public KvResource kv(final String collection, final String key);

    // TODO
    public void ping() throws IOException;

}
