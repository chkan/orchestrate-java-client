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
package io.orchestrate.client;

import lombok.NonNull;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.http.HttpContent;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class OrchestrateRequest<T> {

    /** The default timeout length for a HTTP request to Orchestrate. */
    public static final int DEFAULT_TIMEOUT = 2500;

    private final OrchestrateClient client;
    private final HttpContent httpRequest;
    private final ResponseConverter<T> converter;
    /** The listeners for this request. */
    private final Set<ResponseCompletionHandler<T>> listeners;

    OrchestrateRequest(
            final OrchestrateClient client,
            final HttpContent httpRequest,
            final ResponseConverter<T> converter) {
        assert (client != null);
        assert (httpRequest != null);
        assert (converter != null);

        this.client = client;
        this.httpRequest = httpRequest;
        this.converter = converter;
        this.listeners = new LinkedHashSet<ResponseCompletionHandler<T>>();
    }

    public T execute() {
        return execute(DEFAULT_TIMEOUT);
    }

    public T execute(final int timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("'timeout' cannot be negative.");
        }

        final GrizzlyFuture<HttpContent> future = client.execute(httpRequest, listeners);

        try {
            final HttpContent response = future.get(timeout, TimeUnit.MILLISECONDS);
            return converter.from(response);
        } catch (final Throwable t) {
            throw new RuntimeException(t); // FIXME
        }
    }

    public void executeAsync(final @NonNull ResponseListener<T> listener) {
        // ensure that this listener is triggered first
        final Set<ResponseCompletionHandler<T>> listeners =
                new LinkedHashSet<ResponseCompletionHandler<T>>();
        listeners.add(new ResponseCompletionHandler<T>(listener, converter));
        listeners.addAll(this.listeners);

        client.execute(httpRequest, listeners);
    }

    public OrchestrateRequest<T> on(final @NonNull ResponseListener<T> listener) {
        listeners.add(new ResponseCompletionHandler<T>(listener, converter));
        return this;
    }

}
