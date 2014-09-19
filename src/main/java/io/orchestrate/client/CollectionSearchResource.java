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
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Method;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static io.orchestrate.client.Preconditions.*;

/**
 * The resource for the collection search features in the Orchestrate API.
 */
public class CollectionSearchResource extends BaseResource {

    /** The name of the collection to search. */
    private final String collection;
    /** The number of search results to retrieve. */
    private int limit;
    /** The offset to start search results at. */
    private int offset;
    /** Whether to retrieve the values for the list of search results. */
    private boolean withValues;

    CollectionSearchResource(
            final OrchestrateClient client,
            final JacksonMapper mapper,
            final String collection) {
        super(client, mapper);
        assert (collection != null);
        assert (collection.length() > 0);

        this.collection = collection;
        this.limit = 10;
        this.offset = 0;
        this.withValues = true;
    }

    /**
     * Retrieve data from the Orchestrate service.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * String luceneQuery = "*";
     * SearchResults<String> results1 =
     *         client.searchCollection("someCollection")
     *               .limit(10)
     *               .offset(0)
     *               .get(String.class, luceneQuery)
     *               .get();
     * }
     * </pre>
     *
     * @param clazz Type information for marshalling objects at runtime.
     * @param luceneQuery The lucene search query.
     * @param <T> The type to
     * @return The prepared search request.
     */
    public <T> OrchestrateRequest<SearchResults<T>> get(
            final Class<T> clazz, final String luceneQuery) {
        checkNotNull(clazz, "clazz");
        checkNotNullOrEmpty(luceneQuery, "luceneQuery");

        final String query = "query=".concat(client.encode(luceneQuery))
                .concat("&limit=").concat(limit + "")
                .concat("&offset=").concat(offset + "")
                .concat("&values=").concat(Boolean.toString(withValues));

        final HttpContent packet = HttpRequestPacket.builder()
                .method(Method.GET)
                .uri(client.uri(collection))
                .query(query)
                .build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<SearchResults<T>>(client, packet, new ResponseConverter<SearchResults<T>>() {
            @Override
            public SearchResults<T> from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                assert (status == 200);

                final JsonNode jsonNode = toJsonNode(response);

                final int totalCount = jsonNode.get("total_count").asInt();
                final int count = jsonNode.get("count").asInt();
                final List<Result<T>> results = new ArrayList<Result<T>>(count);

                final Iterator<JsonNode> iter = jsonNode.get("results").elements();
                while (iter.hasNext()) {
                    final JsonNode result = iter.next();

                    // parse result structure (e.g.):
                    // {"path":{...},"value":{},"score":1.0}
                    final double score = result.get("score").asDouble();
                    final KvObject<T> kvObject = toKvObject(result, clazz);

                    results.add(new Result<T>(kvObject, score));
                }

                return new SearchResults<T>(results, totalCount);
            }
        });
    }

    /**
     * The number of search results to get in this query, this value cannot
     * exceed 100.
     *
     * @param limit The number of search results in this query.
     * @return This request.
     */
    public CollectionSearchResource limit(final int limit) {
        this.limit = checkNotNegative(limit, "limit");

        return this;
    }

    /**
     * The position in the results list to start retrieving results from,
     * this is useful for paginating results.
     *
     * @param offset The position to start retrieving results from.
     * @return This request.
     */
    public CollectionSearchResource offset(final int offset) {
        this.offset = checkNotNegative(offset, "offset");

        return this;
    }

    /**
     * If {@code withValues} is {@code true} then the KV objects in the search
     * results will be retrieved with their values. Defaults to {@code true}.
     *
     * @param withValues The setting for whether to retrieve KV values.
     * @return This request.
     */
    public CollectionSearchResource withValues(final boolean withValues) {
        this.withValues = withValues;
        return this;
    }

}
