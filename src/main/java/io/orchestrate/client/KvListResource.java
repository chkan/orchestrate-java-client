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

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static io.orchestrate.client.Preconditions.checkArgument;
import static io.orchestrate.client.Preconditions.checkNotNegative;
import static io.orchestrate.client.Preconditions.checkNotNullOrEmpty;
import static io.orchestrate.client.ResponseConverterUtil.jsonToKvObject;

/**
 * The resource for the KV list features in the Orchestrate API.
 */
public class KvListResource extends BaseResource {

    /** The collection for the request. */
    private final String collection;
    /** The start of the key range to paginate from. */
    private @Nullable String startKey;
    /** Include the specified "startKey" if it exists. */
    private boolean inclusive;
    /** The number of KV objects to retrieve. */
    private int limit;

    KvListResource(final OrchestrateClient client,
                   final JacksonMapper mapper,
                   final String collection) {
        super(client, mapper);
        assert (collection != null);
        assert (collection.length() > 0);

        this.collection = collection;
        this.inclusive = false;
        this.limit = 10;
    }

    /**
     * Fetch a paginated, lexicographically ordered list of items contained in a
     * collection.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * KvList<String> objects =
     *         client.listCollection("someCollection")
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
        checkArgument(!inclusive || startKey != null, "'inclusive' requires 'startKey' for request.");

        final String uri = client.uri(collection);
        String query = "limit=".concat(limit + "");
        if (startKey != null) {
            final String keyName = (inclusive) ? "startKey" : "afterKey";
            query = query
                    .concat('&' + keyName + '=')
                    .concat(client.encode(startKey));
        }

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
                assert (status == 200);

                final String json = response.getContent().toStringContent(Charset.forName("UTF-8"));
                final JsonNode jsonNode = mapper.readTree(json);

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
                    next = new OrchestrateRequest<KvList<T>>(client, packet, this);
                } else {
                    next = null;
                }
                final int count = jsonNode.get("count").asInt();
                final List<KvObject<T>> results = new ArrayList<KvObject<T>>(count);

                final Iterator<JsonNode> iter = jsonNode.get("results").elements();
                while (iter.hasNext()) {
                    results.add(jsonToKvObject(mapper, iter.next(), clazz));
                }

                return new KvList<T>(results, count, next);
            }
        });
    }

    /**
     * Add the 'startKey' to the result set, equivalent to:
     *
     * <pre>
     * {@code
     * this.inclusive(Boolean.TRUE);
     * }
     * </pre>
     *
     * @return The KV list resource.
     * @see #inclusive(boolean)
     */
    public KvListResource inclusive() {
        return inclusive(Boolean.TRUE);
    }

    /**
     * If {@code inclusive} is {@code true} the 'startKey' will be included in
     * the result set.
     *
     * @param inclusive Whether to include the 'startKey' in the result set.
     * @return The KV list resource.
     */
    public KvListResource inclusive(final boolean inclusive) {
        this.inclusive = inclusive;

        return this;
    }

    /**
     * The number of results to return.
     *
     * @param limit The number of KV objects to retrieve.
     * @return The KV list resource.
     */
    public KvListResource limit(final int limit) {
        this.limit = checkNotNegative(limit, "limit");

        return this;
    }

    /**
     * The start of the key range to paginate from including the specified value
     * if it exists.
     *
     * @param startKey The start of the key range to paginate from.
     * @return The KV list resource.
     */
    public KvListResource startKey(final String startKey) {
        this.startKey = checkNotNullOrEmpty(startKey, "startKey");

        return this;
    }

}
