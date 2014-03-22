package io.orchestrate.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Map;

import org.glassfish.grizzly.http.HttpHeader;
import org.junit.Test;

public class RelationFetchOperationTest {
    private static final JacksonMapper mapper = new JacksonMapper();
    private static final HttpHeader httpHeader = null;

    @Test
    public void jsonIsNull() throws IOException {
        final RelationFetchOperation<String> operation = new RelationFetchOperation<String>("collection", "key", String.class, "kinds");
        final Iterable<KvObject<String>> response = operation.fromResponse(404, httpHeader, null, mapper);
        assertFalse(response.iterator().hasNext());
    }

    @Test
    public void jsonIsBlank() throws IOException {
        final RelationFetchOperation<String> operation = new RelationFetchOperation<String>("collection", "key", String.class, "kinds");
        final Iterable<KvObject<String>> response = operation.fromResponse(404, httpHeader, "", mapper);
        assertFalse(response.iterator().hasNext());
    }

    @Test
    public void jsonIsEmpty() throws IOException {
        final RelationFetchOperation<String> operation = new RelationFetchOperation<String>("collection", "key", String.class, "kinds");
        final Iterable<KvObject<String>> response = operation.fromResponse(404, httpHeader, "{}", mapper);
        assertFalse(response.iterator().hasNext());
    }

    @Test
    public void allowsCustomMappedClass() throws IOException {
        final RelationFetchOperation<Map> operation = new RelationFetchOperation<Map>("collection", "key", Map.class, "kinds");
        final Iterable<KvObject<Map>> response = operation.fromResponse(200, httpHeader,
            "{\"count\":1,\"results\":[{\"path\":{\"collection\":\"foo\",\"key\":\"key\",\"ref\":\"aa\"},\"value\":{\"name\":\"foo\"}}]}", mapper);
        KvObject<Map> first = response.iterator().next();
        assertEquals(first.getValue().get("name"), "foo");
    }
}
