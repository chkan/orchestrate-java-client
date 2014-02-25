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
 * {@link io.orchestrate.client.RelationPurgeOperation}.
 */
public final class PurgeTest extends OperationTest {

    private KvMetadata storeKvObject()
            throws InterruptedException, ExecutionException, TimeoutException {
        final String key = generateString();
        final String value = "{}";

        final KvStoreOperation kvStoreOp = new KvStoreOperation(TEST_COLLECTION, key, value);
        return client().execute(kvStoreOp).get(3, TimeUnit.SECONDS);
    }

    private Boolean storeRelation(final KvMetadata source, final String kind, final KvMetadata sink)
            throws InterruptedException, ExecutionException, TimeoutException {
        final RelationStoreOperation relationStoreOp =
                new RelationStoreOperation(source, kind, sink);
        return client().execute(relationStoreOp).get(3, TimeUnit.SECONDS);
    }

    private Boolean purgeRelation(final KvMetadata source, final String kind, final KvMetadata sink)
            throws InterruptedException, ExecutionException, TimeoutException {
        final RelationPurgeOperation relationPurgeOp =
                new RelationPurgeOperation(source, kind, sink);
        return client().execute(relationPurgeOp).get(3, TimeUnit.SECONDS);
    }

    private Iterable<KvObject<String>> fetchRelation(final KvMetadata source, final String kind)
            throws InterruptedException, ExecutionException, TimeoutException {
        final RelationFetchOperation relationFetchOp =
                new RelationFetchOperation(source.getCollection(), source.getKey(), kind);
        return client().execute(relationFetchOp).get(3, TimeUnit.SECONDS);
    }

    @Test
    public void purgeRelation()
            throws InterruptedException, ExecutionException, TimeoutException {
        // create source object
        final KvMetadata kvMetadata_1 = storeKvObject();
        // create sink object
        final KvMetadata kvMetadata_2 = storeKvObject();
        // create relation
        final String kind = generateString();
        final Boolean storeResult = storeRelation(kvMetadata_1, kind, kvMetadata_2);
        // purge relation
        final Boolean purgeResult = purgeRelation(kvMetadata_1, kind, kvMetadata_2);
        // check relation no longer exists
        final Iterable<KvObject<String>> kvObjects = fetchRelation(kvMetadata_1, kind);

        assertNotNull(kvMetadata_1);
        assertNotNull(kvMetadata_2);
        assertTrue(storeResult);
        assertTrue(purgeResult);
        assertFalse(kvObjects.iterator().hasNext());
    }

    @Test
    public void purgeKvObject()
            throws InterruptedException, ExecutionException, TimeoutException {
        final BlockingClient blockingClient = new BlockingClient(client(), 3);

        final String key = generateString();
        final KvMetadata kvMetadata = blockingClient.kvPut(TEST_COLLECTION, key, "{}");
        final Boolean result = blockingClient.kvPurge(kvMetadata);
        final KvObject<MyEmptyObject> kvObject = blockingClient.kvGetRef(kvMetadata, MyEmptyObject.class);

        assertNotNull(kvMetadata);
        assertTrue(result);
        assertNull(kvObject);
    }

}
