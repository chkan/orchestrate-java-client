package io.orchestrate.client;

import lombok.EqualsAndHashCode;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.http.HttpContent;

import java.io.IOException;

@EqualsAndHashCode
final class ResponseCompletionHandler<T> implements CompletionHandler<HttpContent> {

    private final ResponseListener<T> listener;
    private final ResponseConverter<T> converter;

    public ResponseCompletionHandler(
            final ResponseListener<T> listener,
            final ResponseConverter<T> converter) {
        assert (listener != null);
        assert (converter != null);

        this.listener = listener;
        this.converter = converter;
    }

    @Override
    public void cancelled() {
        // not used
    }

    @Override
    public void failed(final Throwable throwable) {
        listener.onFailure(throwable);
    }

    @Override
    public void completed(final HttpContent result) {
        try {
            final T obj = converter.from(result);
            listener.onSuccess(obj);   // FIXME
        } catch (final IOException e) {
            listener.onFailure(e);
        }
    }

    @Override
    public void updated(final HttpContent result) {
        // not used
    }

}
