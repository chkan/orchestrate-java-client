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
package io.orchestrate.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.EqualsAndHashCode;
import org.glassfish.grizzly.http.HttpHeader;

import java.io.IOException;
import java.util.*;

import static io.orchestrate.client.Preconditions.*;

/**
 * A query operation to the Orchestrate.io service.
 *
 * @param <T> The type to result the result of this operation to.
 */
@EqualsAndHashCode
abstract class AbstractOperation<T> {

    /** The list of listeners for the operation's future. */
    private final Set<OrchestrateFutureListener<T>> listeners;

    AbstractOperation() {
        listeners = new LinkedHashSet<OrchestrateFutureListener<T>>();
    }

    /**
     * Constructs the result type {@code T} from the response.
     *
     * @param status The status code from the response.
     * @param httpHeader The HTTP header from the response.
     * @param json The response content.
     * @param mapper The mapper to use when marshalling objects from JSON.
     * @return The result type of this operation.
     * @throws IOException If there was a problem processing the response.
     */
    abstract T fromResponse(
            final int status, final HttpHeader httpHeader, final String json, final JacksonMapper mapper)
            throws IOException;

    /**
     * Add the specified {@code listener} to the future for this operation.
     *
     * @param listener The listener to notify when the future for this operation
     *                 completes.
     * @return This operation.
     */
    public final AbstractOperation<T> addListener(final OrchestrateFutureListener<T> listener) {
        checkNotNull(listener, "listener");
        this.listeners.add(listener);
        return this;
    }

    /**
     * Adds the specified {@code listeners} to the future for this operation.
     *
     * @param listeners The listeners to notify when the future for this operation
     *                  completes.
     * @see io.orchestrate.client.OrchestrateFuture#addListener(OrchestrateFutureListener)
     * @return This operation.
     */
    public final AbstractOperation<T> addListener(final Iterable<OrchestrateFutureListener<T>> listeners) {
        checkNotNull(listeners, "listeners");

        for (final OrchestrateFutureListener<T> listener : listeners) {
            this.listeners.add(listener);
        }
        return this;
    }

    /**
     * Returns the list of listeners for this operation's future.
     *
     * @return The list of listeners for this operation's future.
     */
    public final Set<OrchestrateFutureListener<T>> getListeners() {
        return Collections.unmodifiableSet(listeners);
    }

    /**
     * Remove the specified {@code listener} from the future for this operation.
     *
     * @param listener The listener to remove from the future for this operation.
     * @return This operation.
     */
    public final AbstractOperation<T> removeListener(final OrchestrateFutureListener<T> listener) {
        checkNotNull(listener, "listener");
        this.listeners.remove(listener);
        return this;
    }

    /**
     * Removes the specified {@code listeners} from the future for this operation.
     *
     * @param listeners The listeners to remove from the future for this operation.
     * @see io.orchestrate.client.OrchestrateFuture#removeListener(OrchestrateFutureListener)
     * @return This operation.
     */
    public final AbstractOperation<T> removeListener(final Set<OrchestrateFutureListener<T>> listeners) {
        checkNotNull(listeners, "listeners");

        for (final OrchestrateFutureListener<T> listener : listeners) {
            this.listeners.remove(listener);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    static <T> KvObject<T> jsonToKvObject(
            final ObjectMapper objectMapper, final JsonNode jsonNode, final Class<T> clazz)
            throws IOException {
        // parse the PATH structure (e.g.):
        // {"collection":"coll","key":"aKey","ref":"someRef"}
        final JsonNode path = jsonNode.get("path");
        final String collection = path.get("collection").asText();
        final String key = path.get("key").asText();
        final String ref = path.get("ref").asText();
        final KvMetadata metadata = new KvMetadata(collection, key, ref);

        // parse result structure (e.g.):
        // {"path":{...},"value":{}}
        final JsonNode valueNode = jsonNode.get("value");

        //TODO Can this be removed? Is there value in always having the raw string value available on the KvObject?
        final String rawValue = objectMapper.writeValueAsString(valueNode);

        final T value;
        if (clazz == String.class) {
            // don't deserialize JSON data
            value = (T) rawValue;
        } else {
            value = valueNode.traverse(objectMapper).readValueAs(clazz);
        }

        return new KvObject<T>(metadata, value, rawValue);
    }

}
