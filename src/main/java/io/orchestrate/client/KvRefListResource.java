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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static io.orchestrate.client.Preconditions.checkNotNegative;

/**
 * The resource for the KV ref list feature in the Orchestrate API.
 */
public class KvRefListResource extends BaseResource {

    /** The collection for the request. */
    private final String collection;
    /** The key for the request. */
    private final String key;
    /** The number of KV objects to retrieve. */
    private int limit;
    /** The starting position of the results. */
    private int offset;
    /** Whether to return the value for each ref in the history. */
    private boolean withValues;

    KvRefListResource(final OrchestrateClient client,
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
        this.limit = 10;
        this.offset = 0;
        this.withValues = false;
    }

    /**
     * Fetches the ref history for an item in a collection.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * KvList<String> objects =
     *         client.listRefs("someCollection", "someKey")
     *               .limit(10)
     *               .get(String.class)
     *               .get();
     * }
     * </pre>
     *
     * @param clazz Type information for marshalling objects at runtime.
     * @param <T> The type to deserialize the result of the request to.
     * @return The prepared get request.
     */
    public <T> OrchestrateRequest<KvList<T>> get(final @NonNull Class<T> clazz) {
        final String uri = client.uri(collection, key);
        final String query = "limit=".concat(limit + "")
                .concat("&offset=")
                .concat(offset + "")
                .concat("&values=")
                .concat(withValues + "");

        final HttpContent packet = HttpRequestPacket.builder()
                .method(Method.GET)
                .uri(uri)
                .query(query)
                .build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<KvList<T>>(client, packet, new ResponseConverter<KvList<T>>() {
            @Override
            public KvList<T> from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                if (status == 404) {
                    return new KvList<T>(new ArrayList<KvObject<T>>(), 0, null);
                }
                assert (status == 200);

                final JsonNode jsonNode = toJsonNode(response);

                final OrchestrateRequest<KvList<T>> next;
                if (jsonNode.has("next")) {
                    final String page = jsonNode.get("next").asText();
                    final URI url = URI.create(page);
                    final HttpContent packet = HttpRequestPacket.builder()
                            .method(Method.GET)
                            .uri(uri)
                            .query(url.getQuery())
                            .build()
                            .httpContentBuilder()
                            .build();
                    next = new OrchestrateRequest<KvList<T>>(client, packet, this, false);
                } else {
                    next = null;
                }
                final int count = jsonNode.get("count").asInt();
                final List<KvObject<T>> results = new ArrayList<KvObject<T>>(count);

                final Iterator<JsonNode> iter = jsonNode.get("results").elements();
                while (iter.hasNext()) {
                    results.add(toKvObject(iter.next(), clazz));
                }

                return new KvList<T>(results, count, next);
            }
        });
    }

    /**
     * The number of results to return.
     *
     * @param limit The number of KV objects to retrieve.
     * @return The KV ref list resource.
     */
    public KvRefListResource limit(final int limit) {
        this.limit = checkNotNegative(limit, "limit");
        return this;
    }

    /**
     * The position in the results list to start retrieving results from,
     * this is useful for paginating results.
     *
     * @param offset The position to start retrieving results from.
     * @return This KV ref list resource.
     */
    public KvRefListResource offset(final int offset) {
        this.offset = checkNotNegative(offset, "offset");
        return this;
    }

    /**
     * If {@code withValues} is {@code true} then the KV objects being listed
     * will be retrieved with their values. Defaults to {@code false}.
     *
     * @param withValues The setting for whether to retrieve KV values.
     * @return The KV ref list resource.
     */
    public KvRefListResource withValues(final boolean withValues) {
        this.withValues = withValues;
        return this;
    }

}
