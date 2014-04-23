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
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.impl.SafeFutureImpl;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.orchestrate.client.Preconditions.checkNotNegative;

public final class OrchestrateRequest<T> implements Future<T> {

    /** The default timeout length for a HTTP request to Orchestrate. */
    public static final int DEFAULT_TIMEOUT = 2500;

    private final SafeFutureImpl<HttpContent> rawResponseFuture;
    private final SafeFutureImpl<T> convertedResponseFuture;
    private GrizzlyFuture activeRequest;

    OrchestrateRequest(
            final OrchestrateClient client,
            final HttpContent httpRequest,
            final ResponseConverter<T> converter) {
        assert (client != null);
        assert (httpRequest != null);
        assert (converter != null);

        rawResponseFuture = SafeFutureImpl.create();
        convertedResponseFuture = SafeFutureImpl.create();
        rawResponseFuture.addCompletionHandler(new CompletionHandler<HttpContent>() {
            @Override
            public void cancelled() {
                convertedResponseFuture.cancel(false);
            }

            @Override
            public void failed(Throwable throwable) {
                convertedResponseFuture.failure(throwable);
            }

            @Override
            public void completed(HttpContent result) {
                try {
                    convertedResponseFuture.result(converter.from(result));
                } catch (Exception e) {
                    failed(e);
                }
            }

            @Override
            public void updated(HttpContent result) {
            }
        });
        client.execute(httpRequest, new ConnectionCompletionHandler(this, httpRequest));
    }

    public OrchestrateRequest<T> on(final @NonNull Iterable<ResponseListener<T>> listeners) {
        for(ResponseListener<T> listener : listeners) {
            convertedResponseFuture.addCompletionHandler(new ResponseCompletionHandler<T>(listener));
        }
        return this;
    }

    public OrchestrateRequest<T> on(final @NonNull ResponseListener<T> listener) {
        convertedResponseFuture.addCompletionHandler(new ResponseCompletionHandler<T>(listener));
        return this;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if(activeRequest != null) {
            activeRequest.cancel(mayInterruptIfRunning);
        }
        return convertedResponseFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return convertedResponseFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return convertedResponseFuture.isDone();
    }

    @Override
    public T get() {
        return get(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    @Override
    public T get(long timeout, TimeUnit unit) {
        checkNotNegative(timeout, "timeout");

        try {
            return convertedResponseFuture.get(timeout, unit);
        } catch (final ClientException ex) {
            throw ex;
        } catch (final ExecutionException ex) {
            if (ex.getCause() instanceof ClientException) {
                throw (ClientException) ex.getCause();
            }
            throw new ClientException(ex.getCause());
        } catch (final Exception e) {
            throw new ClientException(e);
        }
    }

    void failed(Throwable t){
        convertedResponseFuture.failure(t);
    }

    void setActiveRequest(GrizzlyFuture activeRequest) {
        this.activeRequest = activeRequest;
    }

    SafeFutureImpl<HttpContent> getRawResponseFuture() {
        return rawResponseFuture;
    }

}
