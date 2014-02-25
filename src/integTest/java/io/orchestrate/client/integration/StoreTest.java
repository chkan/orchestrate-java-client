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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * {@link io.orchestrate.client.KvStoreOperation},
 * {@link io.orchestrate.client.EventStoreOperation},
 * {@link io.orchestrate.client.RelationStoreOperation}.
 */
public final class StoreTest extends OperationTest {

    private KvMetadata result(final KvStoreOperation kvStoreOp)
            throws InterruptedException, ExecutionException, TimeoutException {
        OrchestrateFuture<KvMetadata> future = client().execute(kvStoreOp);
        return future.get(3, TimeUnit.SECONDS);
    }

    private Boolean result(final EventStoreOperation eventStoreOp)
            throws InterruptedException, ExecutionException, TimeoutException {
        OrchestrateFuture<Boolean> future = client().execute(eventStoreOp);
        return future.get(3, TimeUnit.SECONDS);
    }

    @Test
    public void storeObject()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString();
        final String value = "{}";

        KvStoreOperation kvStoreOp =
                new KvStoreOperation(TEST_COLLECTION, key, value);
        KvMetadata result = result(kvStoreOp);

        assertNotNull(result);
        assertEquals(TEST_COLLECTION, result.getCollection());
        assertEquals(key, result.getKey());
        assertEquals("cbb48f9464612f20", result.getRef()); // should match '{}'
    }

    @Test
    public void storeEventNotFoundObject()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString();
        final String event = "eventType";
        final Object value = "{}";

        EventStoreOperation eventStoreOp =
                new EventStoreOperation(TEST_COLLECTION, key, event, value);
        Boolean result = result(eventStoreOp);

        assertNotNull(result);
        assertTrue(result);
    }

}
