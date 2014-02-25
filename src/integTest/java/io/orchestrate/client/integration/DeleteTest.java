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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

/**
 * {@link io.orchestrate.client.KvDeleteOperation}.
 */
public final class DeleteTest extends OperationTest {

    private Boolean result(final KvDeleteOperation deleteOp)
            throws InterruptedException, ExecutionException, TimeoutException {
        OrchestrateFuture<Boolean> future = client().execute(deleteOp);
        return future.get(3, TimeUnit.SECONDS);
    }

    private Boolean result(final DeleteOperation deleteOp)
            throws InterruptedException, ExecutionException, TimeoutException {
        OrchestrateFuture<Boolean> future = client().execute(deleteOp);
        return future.get(3, TimeUnit.SECONDS);
    }

    @Test
    public void deleteCollection()
            throws InterruptedException, ExecutionException, TimeoutException {
        DeleteOperation deleteOp = new DeleteOperation(TEST_COLLECTION);
        Boolean result = result(deleteOp);

        assertNotNull(result);
        assertTrue(result);
    }

    @Test
    public void deleteNotFoundCollection()
            throws InterruptedException, ExecutionException, TimeoutException {
        String collection = generateString();
        DeleteOperation deleteOp = new DeleteOperation(collection);
        Boolean result = result(deleteOp);

        assertNotNull(result);
        assertTrue(result);
    }

    @Test
    public void deleteNonUrlFriendlyCollection()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String collection = TEST_COLLECTION + " !";
        DeleteOperation deleteOp = new DeleteOperation(collection);
        Boolean result = result(deleteOp);

        assertNotNull(result);
        assertTrue(result);
    }

    @Test
    public void deleteObject()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString();
        final String value = "{}";
        KvStoreOperation kvStoreOp = new KvStoreOperation(TEST_COLLECTION, key, value);
        Future<KvMetadata> future_1 = client().execute(kvStoreOp);
        KvMetadata kvMetadata = future_1.get(3, TimeUnit.SECONDS);

        KvDeleteOperation deleteOp = new KvDeleteOperation(kvMetadata);
        Boolean result = result(deleteOp);

        KvFetchOperation<String> kvFetchOp = new KvFetchOperation<String>(TEST_COLLECTION, key, String.class);
        Future<KvObject<String>> future_2 = client().execute(kvFetchOp);
        KvObject<String> kvObject = future_2.get(3, TimeUnit.SECONDS);

        assertNotNull(kvMetadata);
        assertEquals(TEST_COLLECTION, kvMetadata.getCollection());
        assertEquals(key, kvMetadata.getKey());
        assertNotNull(result);
        assertTrue(result);
        assertNull(kvObject);
    }

    @Test
    public void deleteNotFoundObject()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString();
        KvDeleteOperation deleteOp = new KvDeleteOperation(TEST_COLLECTION, key);
        Boolean result = result(deleteOp);

        assertNotNull(result);
        assertTrue(result);
    }

    @Test
    public void deleteObjectNonUrlFriendlyKey()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString() + " !";
        KvDeleteOperation deleteOp = new KvDeleteOperation(TEST_COLLECTION, key);
        Boolean result = result(deleteOp);

        assertNotNull(result);
        assertTrue(result);
    }

    @Test
    public void deleteObjectConditionalRefMatch()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString();
        KvStoreOperation kvStoreOp = new KvStoreOperation(TEST_COLLECTION, key, "{}");
        Future<KvMetadata> future = client().execute(kvStoreOp);
        KvMetadata kvMetadata = future.get(3, TimeUnit.SECONDS);

        KvDeleteOperation deleteOp = new KvDeleteOperation(TEST_COLLECTION, key, kvMetadata);
        Boolean result = result(deleteOp);

        assertNotNull(kvMetadata);
        assertEquals(key, kvMetadata.getKey());
        assertNotNull(result);
        assertTrue(result);
    }

    @Test
    public void deleteObjectConditionalInvalidRef()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString();
        final String ref = "invalidRef";
        KvDeleteOperation deleteOp = new KvDeleteOperation(TEST_COLLECTION, key, ref);

        Throwable t = null;
        try {
            result(deleteOp);
        } catch (final ExecutionException e) {
            t = e;
        }

        assertNotNull(t);
        assertThat(t.getCause(), instanceOf(RequestException.class));
    }

    @Test
    public void deleteObjectConditionalRefNonMatch()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString();
        final String value = "{}";
        KvStoreOperation kvStoreOp = new KvStoreOperation(TEST_COLLECTION, key, value);
        Future<KvMetadata> future_1 = client().execute(kvStoreOp);
        KvMetadata kvMetadata = future_1.get(3, TimeUnit.SECONDS);

        final String ref = "0208f332c68016df";
        KvDeleteOperation deleteOp = new KvDeleteOperation(TEST_COLLECTION, key, ref);
        Boolean result = result(deleteOp);

        KvFetchOperation<String> kvFetchOp = new KvFetchOperation<String>(TEST_COLLECTION, key, String.class);
        Future<KvObject<String>> future_2 = client().execute(kvFetchOp);
        KvObject<String> kvObject = future_2.get(3, TimeUnit.SECONDS);

        assertNotNull(kvMetadata);
        assertEquals(key, kvMetadata.getKey());
        assertNotNull(result);
        assertFalse(result);
        assertNotNull(kvObject);
        assertEquals(value, kvObject.getValue());
    }

    @Test
    public void deleteNotFoundObjectConditionalRefNonMatch()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString();
        final String ref = "0208f332c68016df";
        KvDeleteOperation deleteOp = new KvDeleteOperation(TEST_COLLECTION, key, ref);
        Boolean result = result(deleteOp);

        assertNotNull(result);
        assertFalse(result);
    }

}
