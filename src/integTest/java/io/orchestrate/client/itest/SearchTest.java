/*
 * Copyright 2014 the original author or authors.
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
package io.orchestrate.client.itest;

import io.orchestrate.client.*;
import org.glassfish.grizzly.utils.DataStructures;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * {@link io.orchestrate.client.OrchestrateClient#searchCollection(String)}.
 */
public final class SearchTest extends BaseClientTest {

    @Test
    public void getSearchCollectionEmpty() {
        final SearchResults<String> results =
                client.searchCollection(collection())
                      .get(String.class, "*")
                      .get();

        assertNotNull(results);
        assertFalse(results.iterator().hasNext());
    }

    @Test
    public void getSearchCollection() throws InterruptedException {
        final KvMetadata kvMetadata = client.kv(collection(), "key").put("{}").get();
        // give time for the write to hit the search index
        Thread.sleep(1000);

        final SearchResults<String> results =
                client.searchCollection(kvMetadata.getCollection())
                      .get(String.class, "*")
                      .get();

        assertNotNull(kvMetadata);
        assertNotNull(results);
        assertTrue(results.iterator().hasNext());

        final Result<String> result = results.iterator().next();
        assertNotNull(result);

        final KvObject<String> kvObject = result.getKvObject();
        assertNotNull(kvObject);
        assertEquals(kvMetadata.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata.getKey(), kvObject.getKey());
        assertEquals(kvMetadata.getRef(), kvObject.getRef());
        assertEquals("{}", kvObject.getValue());
    }

    @Test
    public void getSearchCollectionAsync() throws InterruptedException {
        final KvMetadata kvMetadata = client.kv(collection(), "key").put("{}").get();
        // give time for the write to hit the search index
        Thread.sleep(1000);

        final BlockingQueue<SearchResults> queue = DataStructures.getLTQInstance(SearchResults.class);
        client.searchCollection(kvMetadata.getCollection())
              .get(String.class, "*")
              .on(new ResponseAdapter<SearchResults<String>>() {
                  @Override
                  public void onFailure(final Throwable error) {
                      fail(error.getMessage());
                  }

                  @Override
                  public void onSuccess(final SearchResults<String> object) {
                      queue.add(object);
                  }
              });

        @SuppressWarnings("unchecked")
        final SearchResults<String> results = queue.poll(5000, TimeUnit.MILLISECONDS);

        assertNotNull(kvMetadata);
        assertNotNull(results);
        assertTrue(results.iterator().hasNext());

        final Result<String> result = results.iterator().next();
        assertNotNull(result);

        final KvObject<String> kvObject = result.getKvObject();
        assertNotNull(kvObject);
        assertEquals(kvMetadata.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata.getKey(), kvObject.getKey());
        assertEquals(kvMetadata.getRef(), kvObject.getRef());
        assertEquals("{}", kvObject.getValue());
    }

    @Test
    public void getSearchCollectionAndPaginate() throws InterruptedException {
        final String collection = collection();
        final KvMetadata kvMetadata1 = client.kv(collection, "key1").put("{}").get();
        final KvMetadata kvMetadata2 = client.kv(collection, "key2").put("{}").get();
        // give time for the write to hit the search index
        Thread.sleep(3000);

        final SearchResults<String> results1 =
                client.searchCollection(kvMetadata1.getCollection())
                      .limit(10)
                      .offset(0)
                      .get(String.class, "*")
                      .get();

        final SearchResults<String> results2 =
                client.searchCollection(kvMetadata1.getCollection())
                      .offset(1)
                      .get(String.class, "*")
                      .get();

        assertNotNull(kvMetadata1);
        assertNotNull(kvMetadata2);
        assertNotNull(results1);
        assertNotNull(results2);
        assertTrue(results1.iterator().hasNext());
        assertTrue(results2.iterator().hasNext());

        final Result<String> result1 = results1.iterator().next();
        assertNotNull(result1);

        final KvObject<String> kvObject1 = result1.getKvObject();
        assertNotNull(kvObject1);
        assertEquals(kvMetadata1.getCollection(), kvObject1.getCollection());
        assertEquals(kvMetadata1.getKey(), kvObject1.getKey());
        assertEquals(kvMetadata1.getRef(), kvObject1.getRef());
        assertEquals("{}", kvObject1.getValue());

        final Result<String> result2 = results2.iterator().next();
        assertNotNull(result2);

        final KvObject<String> kvObject2 = result2.getKvObject();
        assertNotNull(kvObject2);
        assertEquals(kvMetadata2.getCollection(), kvObject2.getCollection());
        assertEquals(kvMetadata2.getKey(), kvObject2.getKey());
        assertEquals(kvMetadata2.getRef(), kvObject2.getRef());
        assertEquals("{}", kvObject2.getValue());
    }

    @Test
    public void getSearchResultsWithoutValues() throws InterruptedException {
        final String collection = collection();

        final KvMetadata kvMetadata = client.kv(collection, "key1").put("{}").get();
        // give time for the write to hit the search index
        Thread.sleep(3000);

        final SearchResults<String> results =
                client.searchCollection(kvMetadata.getCollection())
                        .withValues(false)
                        .get(String.class, "*")
                        .get();

        assertNotNull(kvMetadata);
        assertNotNull(results);
        assertTrue(results.iterator().hasNext());

        final Result<String> result = results.iterator().next();
        assertNotNull(result);

        final KvObject<String> kvObject = result.getKvObject();
        assertNotNull(kvObject);
        assertEquals(kvMetadata.getCollection(), kvObject.getCollection());
        assertEquals(kvMetadata.getKey(), kvObject.getKey());
        assertEquals(kvMetadata.getRef(), kvObject.getRef());
        assertNull(kvObject.getValue());
    }

    @Test
    public void getSearchResultsSorted() throws InterruptedException {
        final String collection = collection();

        final KvMetadata kvMetadata1 = client.kv(collection, "key1")
                .put("{'name': {'first': 'Jacob', 'last': 'Grimm'}}".replace('\'', '"'))
                .get();
        final KvMetadata kvMetadata2 = client.kv(collection, "key2")
                .put("{'name': {'first': 'Wilhelm', 'last': 'Grimm'}}".replace('\'', '"'))
                .get();
        // give time for the write to hit the search index
        Thread.sleep(3000);

        final SearchResults<String> results =
                client.searchCollection(kvMetadata1.getCollection())
                        .sort("value.name.last:asc,value.name.first:asc")
                        .get(String.class, "value.name.last: Grimm")
                        .get();

        assertNotNull(kvMetadata1);
        assertNotNull(kvMetadata2);
        assertNotNull(results);
        assertTrue(results.iterator().hasNext());

        final Result<String> result = results.iterator().next();
        assertNotNull(result);

        final KvObject<String> kvObject = result.getKvObject();
        assertNotNull(kvObject);
        assertTrue(kvObject.getValue().contains("Jacob"));
    }

    @Test
    public void getSearchResultsFromGeoQuery() throws InterruptedException {
        final String collection = collection();

        final KvMetadata kvMetadata = client.kv(collection, "key1")
                .put("{'location': {'lat': 1, 'lon': 1}}".replace('\'', '"'))
                .get();
        // give time for the write to hit the search index
        Thread.sleep(3000);

        final SearchResults<String> results =
                client.searchCollection(kvMetadata.getCollection())
                        .sort("value.location:distance:asc")
                        .get(String.class, "value.location:NEAR:{lat:1 lon:1 dist:1km}")
                        .get();

        assertNotNull(kvMetadata);
        assertNotNull(results);
        assertTrue(results.iterator().hasNext());

        final Result<String> result = results.iterator().next();
        assertNotNull(result);
        final KvObject<String> kvObject = result.getKvObject();
        assertNotNull(kvObject);
        assertTrue(kvObject.getValue().contains("distance"));
    }

}
