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

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static io.orchestrate.client.Preconditions.*;

/**
 * Fetch a paginated, lexicographically ordered list of items contained in a
 * collection.
 *
 * <p>Usage:
 * <pre>
 * {@code
 * KvListOperation<MyObject> kvListOp =
 *         new KvListOperation<MyObject>("myCollection", 20, MyObject.class);
 * Future<Iterable<KvObject<MyObject>>> futureResult = client.execute(kvFetchOp);
 * Iterable<KvObject<MyObject>> results = futureResult.get();
 * for (KvObject<MyObject> kvObject : results)
 *     System.out.println(kvObject.getValue());
 * }
 * </pre>
 */
@ToString(callSuper=false)
@EqualsAndHashCode(callSuper=false)
public final class KvListOperation<T> extends AbstractOperation<KvList<T>> {

    /** The collection to fetch the KV objects from. */
    private final String collection;
    /** The number of KV objects to retrieve. */
    private final int limit;
    /** The start of the key range to paginate from. */
    private final String startKey;
    /** Include the specified "startKey" if it exists. */
    private final boolean inclusive;
    /** Type information for marshalling objects at runtime. */
    private final Class<T> clazz;

    public KvListOperation(final String collection, final Class<T> clazz) {
        this(collection, 10, clazz);
    }

    public KvListOperation(final String collection, final int limit, final Class<T> clazz) {
        this(collection, null, limit, clazz);
    }

    public KvListOperation(final String collection, final String startKey, final Class<T> clazz) {
        this(collection, startKey, 10, clazz);
    }

    public KvListOperation(
            final String collection, final String startKey, final int limit, final Class<T> clazz) {
        this(collection, startKey, limit, false, clazz);
    }

    public KvListOperation(
            final String collection, final String startKey, final boolean inclusive, final Class<T> clazz) {
        this(collection, startKey, 10, inclusive, clazz);
    }

    public KvListOperation(
            final String collection, @Nullable final String startKey, final int limit, final boolean inclusive, final Class<T> clazz) {
        checkArgument(limit >= 0, "'limit' cannot be negative.");

        this.collection = checkNotNullOrEmpty(collection, "collection");
        this.startKey = startKey;
        this.limit = limit;
        this.inclusive = inclusive;
        this.clazz = checkNotNull(clazz, "clazz");
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    KvList<T> fromResponse(
            final int status, final HttpHeader httpHeader, final String json, final JacksonMapper mapper)
            throws IOException {
        assert (status == 200);

        final ObjectMapper objectMapper = mapper.getMapper();
        final JsonNode jsonNode = objectMapper.readTree(json);

        final String next = (jsonNode.has("next")) ? jsonNode.get("next").asText() : null;
        final int count = jsonNode.get("count").asInt();
        final List<KvObject<T>> results = new ArrayList<KvObject<T>>(count);

        final Iterator<JsonNode> iter = jsonNode.get("results").elements();
        while (iter.hasNext()) {
            results.add(jsonToKvObject(objectMapper, iter.next(), clazz));
        }

        return new KvList<T>(results, count, next);
    }

    /**
     * Returns the collection from this operation.
     *
     * @return The collection from this operation.
     */
    public String getCollection() {
        return collection;
    }

    /**
     * Returns the key from this operation.
     *
     * @return The key from this operation.
     */
    @Nullable
    public String getStartKey() {
        return startKey;
    }

    /**
     * Returns whether a startKey was supplied to this operation.
     *
     * @return {@code true} if a startKey was supplied to this operation.
     */
    public boolean hasStartKey() {
        return (startKey != null);
    }

    /**
     * Returns the limit from this operation.
     *
     * @return The limit from this operation.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Returns {@code true} if the operation is inclusive of the startKey.
     *
     * @return {@code true} to include the startKey if it exists.
     */
    public boolean isInclusive() {
        return inclusive;
    }

}
