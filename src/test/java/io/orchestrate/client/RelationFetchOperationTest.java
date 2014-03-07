package io.orchestrate.client;

import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.glassfish.grizzly.http.HttpHeader;
import org.junit.Test;

public class RelationFetchOperationTest {
    private static final JacksonMapper mapper = new JacksonMapper();
    private static final HttpHeader httpHeader = null;

    @Test
    public void jsonIsNull() throws IOException {
        final RelationFetchOperation operation = new RelationFetchOperation("collection", "key", "kinds");
        final Iterable<KvObject<String>> response = operation.fromResponse(404, httpHeader, null, mapper);
        assertFalse(response.iterator().hasNext());
    }

    @Test
    public void jsonIsBlank() throws IOException {
        final RelationFetchOperation operation = new RelationFetchOperation("collection", "key", "kinds");
        final Iterable<KvObject<String>> response = operation.fromResponse(404, httpHeader, "", mapper);
        assertFalse(response.iterator().hasNext());
    }

    @Test
    public void jsonIsEmpty() throws IOException {
        final RelationFetchOperation operation = new RelationFetchOperation("collection", "key", "kinds");
        final Iterable<KvObject<String>> response = operation.fromResponse(404, httpHeader, "{}", mapper);
        assertFalse(response.iterator().hasNext());
    }
}
