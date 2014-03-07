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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.glassfish.grizzly.http.HttpHeader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static io.orchestrate.client.Preconditions.*;

/**
 * Fetch objects related to a key in the Orchestrate.io service.
 *
 * <p>Usage:
 * <pre>
 * {@code
 * RelationFetchOperation relationFetchOp =
 *         new RelationFetchOperation("myCollection", "someKey", "relationName");
 * Future<Iterable<KvObject<String>>> futureResult = client.execute(relationFetchOp);
 * Iterable<KvObject<String>> results = futureResult.get();
 * for (KvObject<String> result : results)
 *     System.out.println(result.getValue());
 * }
 * </pre>
 *
 * @see <a href="http://java.orchestrate.io/querying/#fetch-relations">http://java.orchestrate.io/querying/#fetch-relations</a>
 */
@ToString(callSuper=false)
@EqualsAndHashCode(callSuper=false)
public final class RelationFetchOperation extends AbstractOperation<Iterable<KvObject<String>>> {

    /** The collection containing the key. */
    private final String collection;
    /** The key to query for kinds. */
    private final String key;
    /** The kinds to traverse in the query. */
    private final String[] kinds;

    /**
     * Create a new {@code RelationFetchOperation} to get all objects that have
     * the sequence of {@code kinds} from the {@code key} in the
     * {@code collection}.
     *
     * @param collection The collection containing the key.
     * @param key The key to fetch related objects from.
     * @param kinds The name of the relationships to traverse to the related
     *                  objects.
     */
    public RelationFetchOperation(
            final String collection, final String key, final String... kinds) {
        checkArgument(kinds.length > 0, "'kinds' cannot be empty.");
        for (final String kind : kinds) {
            checkNotNullOrEmpty(kind, "kind");
        }
        this.collection = checkNotNullOrEmpty(collection, "collection");
        this.key = checkNotNullOrEmpty(key, "key");
        this.kinds = kinds;
    }

    /** {@inheritDoc} */
    @Override
    Iterable<KvObject<String>> fromResponse(
            final int status, final HttpHeader httpHeader, final String json, final JacksonMapper mapper)
            throws IOException {
        assert (status == 200);
        final ObjectMapper objectMapper = mapper.getMapper();
        final JsonNode jsonNode = parseJson(json, objectMapper);

        final int count = jsonNode.path("count").asInt();
        final List<KvObject<String>> relatedObjects = new ArrayList<KvObject<String>>(count);

        for (JsonNode node : jsonNode.path("results")) {
            relatedObjects.add(jsonToKvObject(objectMapper, node, String.class));
        }

        return relatedObjects;
    }

    private static JsonNode parseJson(final String json, final ObjectMapper mapper)
            throws IOException, JsonProcessingException {
        try {
            if(json != null) {
                return mapper.readTree(json);
            }
        }
        catch(JsonMappingException invalidJson) { }
        return MissingNode.getInstance();
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
    public String getKey() {
        return key;
    }

    /**
     * Returns the kinds from this operation.
     *
     * @return The kinds from this operation.
     */
    public List<String> getKinds() {
        return Arrays.asList(kinds);
    }

}
