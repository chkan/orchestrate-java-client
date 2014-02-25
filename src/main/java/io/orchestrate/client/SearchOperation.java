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
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.glassfish.grizzly.http.HttpHeader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static io.orchestrate.client.Preconditions.*;

/**
 * Search for data from the Orchestrate.io service.
 *
 * <p>Usage:
 * <pre>
 * {@code
 * SearchOperation<MyObject> searchOp = SearchOperation
 *         .builder("myCollection", MyObject.class)
 *         .query("*")  // a lucene-style query
 *         .limit(10)   // default
 *         .offset(0)   // default
 *         .build();
 * }
 * </pre>
 *
 * @see <a href="http://java.orchestrate.io/querying/#search">http://java.orchestrate.io/querying/#search</a>
 */
@ToString(callSuper=false)
@EqualsAndHashCode(callSuper=false)
public final class SearchOperation<T> extends AbstractOperation<SearchResults<T>> {

    /** The builder for this search operation. */
    private final Builder<T> builder;

    /**
     * Create a new {@code SearchOperation} on the specified {@code collection}.
     *
     * <p>Equivalent to:
     * <pre>
     * {@code
     * SearchOperation searchOp = SearchOperation.builder("myCollection").build();
     * }
     * </pre>
     *
     * @param collection The collection to search.
     * @param clazz Type information for deserializing to type {@code T} at
     *              runtime.
     * @see Builder#LUCENE_STAR_QUERY
     */
    public SearchOperation(final String collection, final Class<T> clazz) {
        this(builder(collection, clazz));
    }

    /**
     * Create a new {@code SearchOperation} on the specified {@code collection}
     * using the lucene {@code query}.
     *
     * @param collection The collection to search.
     * @param clazz Type information for deserializing to type {@code T} at
     *              runtime.
     * @param query The lucene query to run.
     */
    public SearchOperation(
            final String collection, final Class<T> clazz, final String query) {
        this(builder(collection, clazz).query(query));
    }

    /**
     * A SearchOperation configured via the {@code Builder}.
     *
     * @param builder The builder used to configure this search operation.
     */
    private SearchOperation(final Builder<T> builder) {
        assert (builder != null);

        this.builder = builder;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    SearchResults<T> fromResponse(
            final int status, final HttpHeader httpHeader, final String json, final JacksonMapper mapper)
            throws IOException {
        assert (status == 200);

        final ObjectMapper objectMapper = mapper.getMapper();
        final JsonNode jsonNode = objectMapper.readTree(json);

        final int totalCount = jsonNode.get("total_count").asInt();
        final int count = jsonNode.get("count").asInt();
        final List<Result<T>> results = new ArrayList<Result<T>>(count);

        final Iterator<JsonNode> iter = jsonNode.get("results").elements();
        while (iter.hasNext()) {
            final JsonNode result = iter.next();

            // parse result structure (e.g.):
            // {"path":{...},"value":{},"score":1.0}
            final double score = result.get("score").asDouble();
            final KvObject<T> kvObject = jsonToKvObject(objectMapper, result, builder.clazz);

            results.add(new Result<T>(kvObject, score));
        }

        return new SearchResults<T>(results, totalCount);
    }

    /**
     * Returns the collection from this operation.
     *
     * @return The collection from this operation.
     */
    public String getCollection() {
        return builder.collection;
    }

    /**
     * Returns the Lucene query from this operation.
     *
     * @return The Lucene query from this operation.
     */
    public String getQuery() {
        return builder.query;
    }

    /**
     * Returns the limit from this operation.
     *
     * @return The limit from this operation.
     */
    public int getLimit() {
        return builder.limit;
    }

    /**
     * Returns the offset from this operation.
     *
     * @return The offset from this operation.
     */
    public int getOffset() {
        return builder.offset;
    }

    /**
     * A new builder to create a {@code SearchOperation} with default settings.
     *
     * @param collection The name of the collection to search.
     * @return A new {@code Builder} with default settings.
     */
    public static <T> Builder<T> builder(final String collection, final Class<T> clazz) {
        checkNotNullOrEmpty(collection, "collection");

        return new Builder<T>(collection, clazz);
    }

    /**
     * A new builder to create a {@code SearchOperation} based on the specified
     * {@code searchOp}.
     *
     * @param searchOp The search operation to copy settings from.
     * @return A new {@code Builder} from the specified operation.
     */
    public static <T> Builder<T> builder(final SearchOperation<T> searchOp) {
        checkNotNull(searchOp, "searchOp");

        return new Builder<T>(searchOp);
    }

    /**
     * Builder used to create {@code SearchOperation}s.
     */
    @ToString(callSuper=false)
    @EqualsAndHashCode(callSuper=false)
    public static final class Builder<T> {

        /** A blank lucene query. */
        public static final String LUCENE_STAR_QUERY = "*";

        /** The name of the collection to search. */
        private final String collection;
        /** Type information for marshalling objects at runtime. */
        private final Class<T> clazz;
        /** The lucene search query. */
        private String query;
        /** The number of search results to retrieve. */
        private int limit;
        /** The offset to start search results at. */
        private int offset;

        private Builder(final String collection, final Class<T> clazz) {
            assert (collection != null);

            this.collection = collection;
            this.clazz = clazz;
            this.query = LUCENE_STAR_QUERY;
            this.limit = 10;
            this.offset = 0;
        }

        private Builder(final SearchOperation<T> searchOp) {
            assert (searchOp != null);

            this.collection = searchOp.builder.collection;
            this.clazz = searchOp.builder.clazz;
            this.query = searchOp.builder.query;
            this.limit = searchOp.builder.limit;
            this.offset = searchOp.builder.offset;
        }

        /**
         * The lucene query to make with this search request.
         *
         * @param luceneQuery The lucene query.
         * @return This builder.
         */
        public Builder<T> query(final String luceneQuery) {
            checkNotNullOrEmpty(luceneQuery, "luceneQuery");

            this.query = luceneQuery;
            return this;
        }

        /**
         * The number of search results to get in this query, this value cannot
         * exceed 100.
         *
         * @param limit The number of search results in this query.
         * @return This builder.
         */
        public Builder<T> limit(final int limit) {
            checkArgument(limit >= 0, "'limit' cannot be negative.");
            if (limit > 100) {
                final String message =
                        "'limit' cannot be greater than 100. " +
                                "Currently the Orchestrate.io service limits this value.";
                throw new IllegalArgumentException(message);
            }
            this.limit = limit;
            return this;
        }

        /**
         * The position in the results list to start retrieving results from,
         * this is useful for paginating results.
         *
         * @param offset The position to start retrieving results from.
         * @return This builder.
         */
        public Builder<T> offset(final int offset) {
            checkArgument(offset >= 0, "'offset' cannot be negative.");

            this.offset = offset;
            return this;
        }

        /**
         * Creates a new {@code SearchOperation}.
         *
         * @return A new {@link SearchOperation}.
         */
        public SearchOperation<T> build() {
            return new SearchOperation<T>(this);
        }

    }

}
