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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A helper client used for integration tests that blocks requests for results.
 */
public final class BlockingClient {

    /** The Orchestrate Client to make requests with. */
    private final Client client;
    /** The timeout period for requests. */
    private final int timeout;

    public BlockingClient(final Client client, final int timeout) {
        this.client = client;
        this.timeout = timeout;
    }

    public Boolean delete(final String collection)
            throws InterruptedException, ExecutionException, TimeoutException {
        final DeleteOperation deleteOp = new DeleteOperation(collection);
        final Future<Boolean> f = client.execute(deleteOp);
        return f.get(timeout, TimeUnit.SECONDS);
    }

    public <T> KvObject<T> kvGet(
            final String collection, final String key, final Class<T> clazz)
            throws InterruptedException, ExecutionException, TimeoutException {
        final KvFetchOperation<T> op = new KvFetchOperation<T>(collection, key, clazz);
        final Future<KvObject<T>> f = client.execute(op);
        return f.get(timeout, TimeUnit.SECONDS);
    }

    public <T> KvObject<T> kvGet(final KvMetadata kvMetadata, final Class<T> clazz)
            throws InterruptedException, ExecutionException, TimeoutException {
        final KvFetchOperation<T> op = new KvFetchOperation<T>(kvMetadata, clazz);
        final Future<KvObject<T>> f = client.execute(op);
        return f.get(timeout, TimeUnit.SECONDS);
    }

    public <T> KvObject<T> kvGetRef(
            final String collection, final String key, final String ref, final Class<T> clazz)
            throws InterruptedException, ExecutionException, TimeoutException {
        final KvFetchOperation<T> op = new KvFetchOperation<T>(collection, key, ref, clazz);
        final Future<KvObject<T>> f = client.execute(op);
        return f.get(timeout, TimeUnit.SECONDS);
    }

    public <T> KvObject<T> kvGetRef(final KvMetadata kvMetadata, final Class<T> clazz)
            throws InterruptedException, ExecutionException, TimeoutException {
        final KvFetchOperation<T> op = new KvFetchOperation<T>(kvMetadata, clazz);
        final Future<KvObject<T>> f = client.execute(op);
        return f.get(timeout, TimeUnit.SECONDS);
    }

    public <T> KvList<T> kvList(
            final String collection, final int limit, final Class<T> clazz)
            throws InterruptedException, ExecutionException, TimeoutException {
        final KvListOperation<T> op = new KvListOperation<T>(collection, limit, clazz);
        final Future<KvList<T>> f = client.execute(op);
        return f.get(timeout, TimeUnit.SECONDS);
    }

    public <T> KvList<T> kvList(
            final String collection, final String startKey, final boolean inclusive, final Class<T> clazz)
            throws InterruptedException, ExecutionException, TimeoutException {
        final KvListOperation<T> op = new KvListOperation<T>(collection, startKey, inclusive, clazz);
        final Future<KvList<T>> f = client.execute(op);
        return f.get(timeout, TimeUnit.SECONDS);
    }

    public <T> KvList<T> kvList(
            final String collection, final String startKey, final int limit, final boolean inclusive, final Class<T> clazz)
            throws InterruptedException, ExecutionException, TimeoutException {
        final KvListOperation<T> op = new KvListOperation<T>(collection, startKey, limit, inclusive, clazz);
        final Future<KvList<T>> f = client.execute(op);
        return f.get(timeout, TimeUnit.SECONDS);
    }

    public KvMetadata kvPut(final String collection, final String key, final Object value)
            throws InterruptedException, ExecutionException, TimeoutException {
        final KvStoreOperation op = new KvStoreOperation(collection, key, value);
        final Future<KvMetadata> f = client.execute(op);
        return f.get(timeout, TimeUnit.SECONDS);
    }

    public Boolean kvPurge(final String collection, final String key)
            throws InterruptedException, ExecutionException, TimeoutException {
        final KvPurgeOperation op = new KvPurgeOperation(collection, key);
        final Future<Boolean> f = client.execute(op);
        return f.get(timeout, TimeUnit.SECONDS);
    }

    public Boolean kvPurge(final KvMetadata kvMetadata)
            throws InterruptedException, ExecutionException, TimeoutException {
        final KvPurgeOperation op = new KvPurgeOperation(kvMetadata);
        final Future<Boolean> f = client.execute(op);
        return f.get(timeout, TimeUnit.SECONDS);
    }

}
