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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.memory.ByteBufferWrapper;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static io.orchestrate.client.Preconditions.checkArgument;
import static io.orchestrate.client.Preconditions.checkNotNull;
import static io.orchestrate.client.Preconditions.checkNotNullOrEmpty;

/**
 * The resource for the event features in the Orchestrate API.
 */
public class EventResource extends BaseResource {

    /** The collection for the request. */
    private final String collection;
    /** The key for the request. */
    private final String key;
    /** The type of events to get. */
    private String type;
    /** The timestamp to get events from. */
    private Long start;
    /** The timestamp to get events up to. */
    private Long end;

    EventResource(final OrchestrateClient client,
                  final JacksonMapper mapper,
                  final String collection,
                  final String key) {
        super(client, mapper);
        assert (collection != null);
        assert (collection.length() > 0);
        assert (key != null);
        assert (key.length() > 0);

        this.collection = collection;
        this.key = key;
        this.start = null;
        this.end = null;
    }

    /**
     * Fetch events for a key in the Orchestrate service.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * EventList<DomainObject> events =
     *         client.event("someCollection", "someKey")
     *               .type("eventType")
     *               .get(DomainObject.class)
     *               .get();
     * }
     * </pre>
     *
     * @param clazz Type information for marshalling objects at runtime.
     * @param <T> The type to deserialize the result of the request to.
     * @return The prepared get request.
     */
    public <T> OrchestrateRequest<EventList<T>> get(final Class<T> clazz) {
        checkNotNull(clazz, "clazz");
        checkNotNull(type, "type");

        final String uri = client.uri(collection, key, "events", type);

        final HttpRequestPacket.Builder httpHeaderBuilder = HttpRequestPacket.builder()
                .method(Method.GET)
                .uri(uri);
        String query = null;
        if (start != null) {
            query += "start=" + start;
        }
        if (end != null) {
            query += "&end=" + end;
        }
        httpHeaderBuilder.query(query);

        final HttpContent packet = httpHeaderBuilder.build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<EventList<T>>(client, packet, new ResponseConverter<EventList<T>>() {
            @Override
            public EventList<T> from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                assert (status == 200);

                final JsonNode jsonNode = toJsonNode(response);

                final int count = jsonNode.get("count").asInt();
                final List<Event<T>> events = new ArrayList<Event<T>>(count);

                final Iterator<JsonNode> iter = jsonNode.get("results").elements();
                while (iter.hasNext()) {
                    final JsonNode result = iter.next();

                    final long timestamp = result.get("timestamp").asLong();

                    final JsonNode valueNode = result.get("value");
                    final String rawValue = valueNode.toString();

                    final T value = toDomainObject(rawValue, clazz);

                    events.add(new Event<T>(value, rawValue, timestamp));
                }
                return new EventList<T>(events);
            }

        });
    }

    /**
     * {@link #put(Object, Long)}.
     */
    public OrchestrateRequest<Boolean> put(final @NonNull Object value) {
        return put(value, null);
    }

    /**
     * Store an event to a key in the Orchestrate service.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * DomainObject obj = new DomainObject(); // a POJO
     * boolean result =
     *         client.event("someCollection", "someKey")
     *               .type("someType")
     *               .put(obj)
     *               .get();
     * }
     * </pre>
     *
     * @param value The object to store as the event.
     * @param timestamp The timestamp to store the event at.
     * @return The prepared put request.
     */
    public OrchestrateRequest<Boolean> put(
            final @NonNull Object value, @Nullable final Long timestamp) {
        checkNotNull(type, "type");
        checkArgument(start == null && end == null, "'start' and 'end' not allowed with PUT requests.");

        final byte[] content = toJsonBytes(value);

        final String uri = client.uri(collection, key, "events", type);

        final HttpRequestPacket.Builder httpHeaderBuilder = HttpRequestPacket.builder()
                .method(Method.PUT)
                .contentType("application/json")
                .uri(uri);
        if (timestamp != null) {
            httpHeaderBuilder.query("timestamp=" + timestamp);
        }
        httpHeaderBuilder.contentLength(content.length);

        final HttpContent packet = httpHeaderBuilder.build()
                .httpContentBuilder()
                .content(new ByteBufferWrapper(ByteBuffer.wrap(content)))
                .build();

        return new OrchestrateRequest<Boolean>(client, packet, new ResponseConverter<Boolean>() {
            @Override
            public Boolean from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                return (status == 204);
            }
        });
    }

    /**
     * The type for an event, e.g. "update" or "tweet" etc.
     *
     * @param type The type of event.
     * @return The event resource.
     */
    public EventResource type(final String type) {
        this.type = checkNotNullOrEmpty(type, "type");

        return this;
    }

    /**
     * The inclusive start of a time range to query.
     *
     * @param start The timestamp to get events from.
     * @return The event resource.
     */
    public EventResource start(final long start) {
        this.start = start;
        return this;
    }

    /**
     * The exclusive end of a time range to query.
     *
     * @param end The timestamp to get events up to.
     * @return The event resource.
     */
    public EventResource end(final long end) {
        this.end = end;
        return this;
    }

}
