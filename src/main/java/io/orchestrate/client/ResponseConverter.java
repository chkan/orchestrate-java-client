package io.orchestrate.client;

import org.glassfish.grizzly.http.HttpContent;

import java.io.IOException;

public interface ResponseConverter<T> {

    T from(final HttpContent response) throws IOException;

}
