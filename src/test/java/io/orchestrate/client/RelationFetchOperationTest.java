package io.orchestrate.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
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

        Map fakeResults = fakeResults(fakeKvItem("collection", "key", "name", "foo"));
        final Iterable<KvObject<Map>> response = operation.fromResponse(200, httpHeader, json(fakeResults), mapper);
        KvObject<Map> first = response.iterator().next();
        assertEquals(first.getValue().get("name"), "foo");
    }

    @Test
    public void allowsCustomMappedClassWithMixedTypeResults() throws IOException {
        final RelationFetchOperation<Map> operation = new RelationFetchOperation<Map>("collection", "key", Map.class, "kinds");

        Map fakeResults = fakeResults(
            fakeKvItem("collection", "key", "name", "foo"),
            fakeKvItem("collection2", "key2", "title", "test title")
        );

        final Iterable<KvObject<Map>> response = operation.fromResponse(200, httpHeader, json(fakeResults), mapper);
        Iterator<KvObject<Map>> responseIter = response.iterator();

        KvObject<Map> first = responseIter.next();
        assertEquals(first.getValue().get("name"), "foo");

        KvObject<Map> second = responseIter.next();
        assertEquals(second.getValue().get("title"), "test title");
    }

    @Test
    public void allowsCustomBeanClassWithMixedTypeResults() throws IOException {
        final RelationFetchOperation<Animal> operation = new RelationFetchOperation<Animal>("people", "marlon", Animal.class, "pets");

        Map fakeResults = fakeResults(
            fakeKvItem("pets", "fido", "type", "dog", "name", "Fido"),
            fakeKvItem("pets", "lucky", "type", "cat", "name", "Lucky", "lives", 2)
        );

        final Iterable<KvObject<Animal>> response = operation.fromResponse(200, httpHeader, json(fakeResults), mapper);
        Iterator<KvObject<Animal>> responseIter = response.iterator();

        KvObject<Animal> first = responseIter.next();
        assertEquals(first.getValue().getName(), "Fido");
        assertTrue(first.getValue() instanceof Dog);

        KvObject<Animal> second = responseIter.next();
        assertEquals(second.getValue().getName(), "Lucky");
        assertEquals(((Cat) second.getValue()).getLives(), 2);
        assertTrue(second.getValue() instanceof Cat);
    }

    private String json(Map fakeResults) throws JsonProcessingException {
        return mapper.getMapper().writeValueAsString(fakeResults);
    }

    private Map fakeResults(Map...items){
        Map<String,Object> results = new HashMap<String,Object>();
        results.put("count", results.size());
        results.put("results", items);
        return results;
    }

    private Map fakeKvItem(String collection, String key, Object...kv){
        Map<String,String> meta = new HashMap<String,String>();
        meta.put("collection", collection);
        meta.put("key", key);
        meta.put("ref", "aa");

        Map value = data(kv);

        Map<String,Object> item = new HashMap<String, Object>();
        item.put("path", meta);
        item.put("value", value);
        return item;
    }

    private Map<Object,Object> data(Object...kv){
        Map<Object,Object> data = new HashMap<Object,Object>();
        for(int i=0;i<kv.length;i+=2){
            data.put(kv[i], kv[i+1]);
        }
        return data;
    }
}
