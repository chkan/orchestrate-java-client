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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.http.util.UEncoder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static io.orchestrate.client.Preconditions.*;
import static io.orchestrate.client.ResponseConverterUtil.jsonToKvObject;

/**
 * The resource for the relation features in the Orchestrate API.
 */
public class RelationResource extends BaseResource {

    /** The collection containing the source key. */
    private String sourceCollection;
    /** The source key to add the relation to. */
    private String sourceKey;
    /** The collection containing the destination key. */
    private String destCollection;
    /** The destination key to add the relation to. */
    private String destKey;
    /** Whether to swap the "source" and "destination" objects. */
    private boolean invert;

    RelationResource(final OrchestrateClient client,
            final JacksonMapper mapper,
            final String sourceCollection,
            final String sourceKey) {
        super(client, mapper);
        assert (sourceCollection != null);
        assert (sourceKey != null);

        this.sourceCollection = sourceCollection;
        this.sourceKey = sourceKey;
    }

    /**
     * Fetch objects related to a key in the Orchestrate service.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * RelationList<String> relatedObjects =
     *         client.relation("someCollection", "someKey")
     *               .get(String.class, "someKind")
     *               .get();
     * }
     * </pre>
     *
     * @param clazz Type information for deserializing to type {@code T} at
     *              runtime.
     * @param kinds The name of the relationships to traverse to the related
     *              objects.
     * @param <T> The type to deserialize the response from the request to.
     * @return A prepared get request.
     */
    public <T> OrchestrateRequest<RelationList<T>> get(final Class<T> clazz, final String... kinds) {
        checkNotNull(clazz, "clazz");
        checkArgument(destCollection == null && destKey == null,
                "'destCollection' and 'destKey' not valid in GET query.");
        checkNoneEmpty(kinds, "kinds", "kind");

        final UEncoder urlEncoder = new UEncoder();
        String uri = urlEncoder.encodeURL(sourceCollection)
                .concat("/")
                .concat(urlEncoder.encodeURL(sourceKey))
                .concat("/relations");
        for (final String kind : kinds) {
            uri = uri.concat("/").concat(urlEncoder.encodeURL(kind));
        }

        final HttpContent packet = HttpRequestPacket.builder()
                .method(Method.GET)
                .uri(uri)
                .build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<RelationList<T>>(client, packet, new ResponseConverter<RelationList<T>>() {
            @Override
            public RelationList<T> from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                assert (status == 200 || status == 404);

                if (status == 404) {
                    return null;
                }

                final String json = response.getContent().toStringContent(Charset.forName("UTF-8"));
                final JsonNode jsonNode = parseJson(json, mapper);

                final int count = jsonNode.path("count").asInt();
                final List<KvObject<T>> relatedObjects = new ArrayList<KvObject<T>>(count);

                for (JsonNode node : jsonNode.path("results")) {
                    relatedObjects.add(jsonToKvObject(mapper, node, clazz));
                }

                return new RelationList<T>(relatedObjects);
            }
        });
    }

    /**
     * Store a relationship between two objects in the Orchestrate service.
     *
     * <p>Usage:</p>
     * <pre>
     * {@code
     * boolean result =
     *         client.relation("someCollection", "someKey")
     *               .to("anotherCollection", "anotherKey")
     *               .put(kind)
     *               .get();
     * }
     * </pre>
     *
     * @param kind The name of the relationship to create.
     * @return A prepared put request.
     */
    public OrchestrateRequest<Boolean> put(final String kind) {
        checkNotNullOrEmpty(kind, "kind");
        checkArgument(destCollection != null && destKey != null,
                "'destCollection' and 'destKey' required for PUT query.");

        String localSourceCollection = invert ? destCollection : sourceCollection;
        String localSourceKey = invert ? destKey : sourceKey;
        String localDestCollection = invert ? sourceCollection : destCollection;
        String localDestKey = invert ? sourceKey : destKey;

        final UEncoder urlEncoder = new UEncoder();
        final String uri = urlEncoder.encodeURL(localSourceCollection)
                .concat("/")
                .concat(urlEncoder.encodeURL(localSourceKey))
                .concat("/relation/")
                .concat(urlEncoder.encodeURL(kind))
                .concat("/")
                .concat(urlEncoder.encodeURL(localDestCollection))
                .concat("/")
                .concat(urlEncoder.encodeURL(localDestKey));

        final HttpContent packet = HttpRequestPacket.builder()
                .method(Method.PUT)
                .uri(uri)
                .build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<Boolean>(client, packet, new ResponseConverter<Boolean>() {
            @Override
            public Boolean from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                return (status == HttpStatus.NO_CONTENT_204.getStatusCode());
            }
        });
    }

    /**
     * Remove a relationship between two objects in the Orchestrate service.
     *
     * <pre>
     * {@code
     * boolean result =
     *         client.relation("someCollection", "someKey")
     *               .to("anotherCollection", "anotherKey")
     *               .purge(kind)
     *               .get();
     * }
     * </pre>
     *
     * @param kind The name of the relationship to delete.
     * @return A prepared delete request.
     */
    public OrchestrateRequest<Boolean> purge(final String kind) {
        checkNotNullOrEmpty(kind, "kind");
        checkArgument(destCollection != null && destKey != null,
                "'destCollection' and 'destKey' required for DELETE query.");

        String localSourceCollection = invert ? destCollection : sourceCollection;
        String localSourceKey = invert ? destKey : sourceKey;
        String localDestCollection = invert ? sourceCollection : destCollection;
        String localDestKey = invert ? sourceKey : destKey;

        final UEncoder urlEncoder = new UEncoder();
        final String uri = urlEncoder.encodeURL(localSourceCollection)
                .concat("/")
                .concat(urlEncoder.encodeURL(localSourceKey))
                .concat("/relation/")
                .concat(urlEncoder.encodeURL(kind))
                .concat("/")
                .concat(urlEncoder.encodeURL(localDestCollection))
                .concat("/")
                .concat(urlEncoder.encodeURL(localDestKey));

        final HttpContent packet = HttpRequestPacket.builder()
                .method(Method.DELETE)
                .uri(uri)
                .query("purge=true")
                .build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<Boolean>(client, packet, new ResponseConverter<Boolean>() {
            @Override
            public Boolean from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                return (status == HttpStatus.NO_CONTENT_204.getStatusCode());
            }
        });
    }

    /**
     * The "destination" object to point the relationship to.
     *
     * @param collection The collection containing the destination key.
     * @param key The destination key to add the relation to.
     * @return This relation resource.
     */
    public RelationResource to(
            final String collection, final String key) {
        this.destCollection = checkNotNullOrEmpty(collection, "collection");
        this.destKey = checkNotNullOrEmpty(key, "key");

        return this;
    }

    /**
     * Swap the "source" and "destination" in the request, equivalent to:
     *
     * <pre>
     * {@code
     * this.invert(Boolean.TRUE);
     * }
     * </pre>
     *
     * @return This relation resource.
     * @see #invert(boolean)
     */
    public RelationResource invert() {
        return invert(Boolean.TRUE);
    }

    /**
     * If {@code invert} is {@code true} the "source" and "destination" objects
     * will be swapped in the request. This is useful for designing a
     * bi-directional relationship.
     *
     * @param invert Whether to invert the request.
     * @return This relation resource.
     */
    public RelationResource invert(final boolean invert) {
        this.invert = invert;
        return this;
    }

    private static JsonNode parseJson(final String json, final ObjectMapper mapper)
            throws IOException {
        assert (mapper != null);

        try {
            if(json != null) {
                return mapper.readTree(json);
            }
        } catch(final JsonMappingException ignored) {
        }
        return MissingNode.getInstance();
    }

}
