/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.orchestrate.client.integration;

import io.orchestrate.client.*;
import org.junit.Test;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * {@link io.orchestrate.client.KvFetchOperation},
 * {@link io.orchestrate.client.EventFetchOperation},
 * {@link io.orchestrate.client.RelationFetchOperation}.
 */
public final class FetchTest extends OperationTest {

    private <T> KvObject<T> result(final KvFetchOperation<T> kvFetchOp)
            throws InterruptedException, ExecutionException, TimeoutException {
        OrchestrateFuture<KvObject<T>> future = client().execute(kvFetchOp);
        return future.get(3, TimeUnit.SECONDS);
    }

    private <T> Iterable<Event<T>> result(final EventFetchOperation<T> eventFetchOp)
            throws InterruptedException, ExecutionException, TimeoutException {
        OrchestrateFuture<Iterable<Event<T>>> future = client().execute(eventFetchOp);
        return future.get(3, TimeUnit.SECONDS);
    }

    @Test
    public void fetchObject()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString();
        final String value = "{}";

        KvStoreOperation kvStoreOp = new KvStoreOperation(TEST_COLLECTION, key, value);
        Future<KvMetadata> future_1 = client().execute(kvStoreOp);
        KvMetadata kvMetadata = future_1.get(3, TimeUnit.SECONDS);

        KvFetchOperation<String> kvFetchOp = new KvFetchOperation<String>(TEST_COLLECTION, key, String.class);
        KvObject<String> result = result(kvFetchOp);

        assertNotNull(kvMetadata);
        assertNotNull(result);
        assertEquals(key, result.getKey());
        assertEquals(value, result.getValue());
        assertEquals(kvMetadata.getRef(), result.getRef());
    }

    @Test
    public void fetchObjectAsPojo()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString();
        final MyObject value = new MyObject();

        KvStoreOperation kvStoreOp = new KvStoreOperation(TEST_COLLECTION, key, value);
        Future<KvMetadata> future_1 = client().execute(kvStoreOp);
        KvMetadata kvMetadata = future_1.get(3, TimeUnit.SECONDS);

        KvFetchOperation<MyObject> kvFetchOp = new KvFetchOperation<MyObject>(TEST_COLLECTION, key, MyObject.class);
        KvObject<MyObject> result = result(kvFetchOp);

        assertNotNull(kvMetadata);
        assertNotNull(result);
        assertEquals(key, result.getKey());
        assertThat(result.getValue(), instanceOf(MyObject.class));
        assertEquals(value, result.getValue());
        assertEquals("{}", result.getRawValue());
    }

    @Test
    public void fetchNotFoundObject()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString();
        KvFetchOperation<String> kvFetchOp = new KvFetchOperation<String>(TEST_COLLECTION, key, String.class);
        KvObject<String> result = result(kvFetchOp);

        assertNull(result);
    }

    @Test
    @org.junit.Ignore("A bug in the Orchestrate.io service.")
    public void fetchObjectNonUrlFriendlyCollection()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String collection = TEST_COLLECTION + " !";
        final String key = generateString();
        final String value = "{}";

        KvStoreOperation kvStoreOp = new KvStoreOperation(collection, key, value);
        Future<KvMetadata> future_1 = client().execute(kvStoreOp);
        KvMetadata kvMetadata = future_1.get(3, TimeUnit.SECONDS);

        KvFetchOperation<String> kvFetchOp = new KvFetchOperation<String>(collection, key, String.class);
        KvObject<String> result = result(kvFetchOp);

        assertNotNull(kvMetadata);
        assertNotNull(result);
        assertEquals(key, result.getKey());
        assertEquals(value, result.getValue());
        assertEquals(kvMetadata.getRef(), result.getRef());
    }

    @Test
    public void fetchObjectNonUrlFriendlyKey()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString() + " !";
        final String value = "{}";

        KvStoreOperation kvStoreOp = new KvStoreOperation(TEST_COLLECTION, key, value);
        Future<KvMetadata> future_1 = client().execute(kvStoreOp);
        KvMetadata kvMetadata = future_1.get(3, TimeUnit.SECONDS);

        KvFetchOperation<String> kvFetchOp = new KvFetchOperation<String>(TEST_COLLECTION, key, String.class);
        KvObject<String> result = result(kvFetchOp);

        assertNotNull(kvMetadata);
        assertNotNull(result);
        assertEquals(key, result.getKey());
        assertEquals(value, result.getValue());
        assertEquals(kvMetadata.getRef(), result.getRef());
    }

    @Test
    public void fetchEventsForObject()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString();
        final String value = "{}";
        final String eventType = generateString();

        KvStoreOperation kvStoreOp = new KvStoreOperation(TEST_COLLECTION, key, value);
        Future<KvMetadata> future_1 = client().execute(kvStoreOp);
        KvMetadata kvMetadata = future_1.get(3, TimeUnit.SECONDS);

        EventStoreOperation eventStoreOp =
                new EventStoreOperation(TEST_COLLECTION, key, eventType, value);
        Future<Boolean> future_2 = client().execute(eventStoreOp);
        Boolean success = future_2.get(3, TimeUnit.SECONDS);

        EventFetchOperation<String> eventFetchOp =
                new EventFetchOperation<String>(TEST_COLLECTION, key, eventType, String.class);
        Iterable<Event<String>> results = result(eventFetchOp);

        assertNotNull(kvMetadata);
        assertNotNull(success);
        assertTrue(success);
        assertNotNull(results);

        Iterator<Event<String>> iter = results.iterator();
        assertTrue(iter.hasNext());

        Event<String> event = iter.next();
        assertEquals(value, event.getValue());
    }

    @Test
    public void fetchNonUrlFriendlyEventTypeForObject()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString();
        final String value = "{}";
        final String eventType = generateString() + " !";

        KvStoreOperation kvStoreOp = new KvStoreOperation(TEST_COLLECTION, key, value);
        Future<KvMetadata> future_1 = client().execute(kvStoreOp);
        KvMetadata kvMetadata = future_1.get(3, TimeUnit.SECONDS);

        EventStoreOperation eventStoreOp =
                new EventStoreOperation(TEST_COLLECTION, key, eventType, value);
        Future<Boolean> future_2 = client().execute(eventStoreOp);
        Boolean success = future_2.get(3, TimeUnit.SECONDS);

        EventFetchOperation<String> eventFetchOp =
                new EventFetchOperation<String>(TEST_COLLECTION, key, eventType, String.class);
        Iterable<Event<String>> results = result(eventFetchOp);

        assertNotNull(kvMetadata);
        assertNotNull(success);
        assertTrue(success);
        assertNotNull(results);

        Iterator<Event<String>> iter = results.iterator();
        assertTrue(iter.hasNext());

        Event<String> event = iter.next();
        assertEquals(value, event.getValue());
    }

    @Test
    public void fetchEmptyEventsForObject()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString();
        final String value = "{}";
        final String eventType = generateString();

        KvStoreOperation kvStoreOp = new KvStoreOperation(TEST_COLLECTION, key, value);
        Future<KvMetadata> future_1 = client().execute(kvStoreOp);
        KvMetadata kvMetadata = future_1.get(3, TimeUnit.SECONDS);

        EventFetchOperation<String> eventFetchOp =
                new EventFetchOperation<String>(TEST_COLLECTION, key, eventType, String.class);
        Iterable<Event<String>> results = result(eventFetchOp);

        assertNotNull(kvMetadata);
        assertNotNull(results);
        assertFalse(results.iterator().hasNext());
    }

    @Test
    public void fetchEmptyEventsForNotFoundObject()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString();
        final String eventType = generateString();

        EventFetchOperation<String> eventFetchOp =
                new EventFetchOperation<String>(TEST_COLLECTION, key, eventType, String.class);
        Iterable<Event<String>> results = result(eventFetchOp);

        assertFalse(results.iterator().hasNext());
    }

}
