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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.glassfish.grizzly.http.*;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.http.util.UEncoder;
import org.glassfish.grizzly.memory.ByteBufferWrapper;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.ByteBuffer;

public class KvResource {

    /** The Orchestrate client to make requests with. */
    private final OrchestrateClient client;
    /** The object mapper used to deserialize JSON responses. */
    private final ObjectMapper mapper;
    /** The collection for the request. */
    private final String collection;
    /** The key for the request. */
    private final String key;
    // TODO
    private boolean ifAbsent;
    private String objectRef;

    KvResource(final OrchestrateClient client,
               final JacksonMapper mapper,
               final @NonNull String collection,
               final @NonNull String key) {
        assert (client != null);
        assert (mapper != null);

        this.client = client;
        this.mapper = mapper.getMapper();
        this.collection = collection;
        this.key = key;
        this.ifAbsent = false;
        this.objectRef = null;
    }

    public OrchestrateRequest<Boolean> delete() {
        return delete(Boolean.FALSE);
    }

    public OrchestrateRequest<Boolean> delete(final boolean purge) {
        if (ifAbsent) {
            throw new IllegalStateException("'ifAbsent' cannot be used in a DELETE request.");
        }

        final UEncoder urlEncoder = new UEncoder();
        final String uri = urlEncoder.encodeURL(collection)
                .concat("/")
                .concat(urlEncoder.encodeURL(key));

        final HttpRequestPacket.Builder builder = HttpRequestPacket.builder()
                .method(Method.DELETE)
                .uri(uri);
        if (purge) {
            builder.query("purge=true");
        }
        if (objectRef != null) {
            builder.header(Header.IfMatch, "\"".concat(objectRef).concat("\""));
        }

        final HttpContent packet = builder.build().httpContentBuilder().build();
        return new OrchestrateRequest<Boolean>(client, packet, new ResponseConverter<Boolean>() {
            @Override
            public Boolean from(final HttpContent response) throws IOException {
                final int status = ((HttpResponsePacket) response.getHttpHeader()).getStatus();
                return (status == HttpStatus.NO_CONTENT_204.getStatusCode());
            }
        });
    }

    public <T> OrchestrateRequest<KvObject<T>> get(final @NonNull Class<T> clazz) {
        return get(clazz, null);
    }

    public <T> OrchestrateRequest<KvObject<T>> get(final @NonNull Class<T> clazz, @Nullable final String ref) {
        final UEncoder urlEncoder = new UEncoder();
        String uri = urlEncoder.encodeURL(collection)
                .concat("/")
                .concat(urlEncoder.encodeURL(key));
        if (ref != null) {
            uri = uri.concat("/refs/").concat(ref);
        }

        final HttpContent packet = HttpRequestPacket.builder()
                .method(Method.GET)
                .uri(uri)
                .build()
                .httpContentBuilder()
                .build();

        return new OrchestrateRequest<KvObject<T>>(client, packet, new ResponseConverter<KvObject<T>>() {
            @Override
            public KvObject<T> from(final HttpContent response) throws IOException {
                final HttpHeader header = response.getHttpHeader();
                final int status = ((HttpResponsePacket) header).getStatus();

                if (status == 404) {
                    // maybe one day we can return an optional type
                    return null;
                }

                final String json = response.getContent().toStringContent();
                final String ref  = header.getHeader(Header.ETag)
                        .replace("\"", "")
                        .replaceFirst("-gzip$", "");

                @SuppressWarnings("unchecked")
                final T value = (clazz == String.class)
                        ? (T) json
                        : mapper.readValue(json, clazz);

                return new KvObject<T>(collection, key, ref, value, json);
            }
        });
    }

    public KvResource ifAbsent() {
        return ifAbsent(Boolean.TRUE);
    }

    public KvResource ifAbsent(final boolean ifAbsent) {
        if (objectRef != null) {
            throw new IllegalStateException("'ifMatch' and 'ifAbsent' cannot be used together.");
        }
        this.ifAbsent = ifAbsent;
        return this;
    }

    public KvResource ifMatch(final @NonNull String objectRef) {
        if (ifAbsent) {
            throw new IllegalStateException("'ifMatch' and 'ifAbsent' cannot be used together.");
        }
        this.objectRef = objectRef;
        return this;
    }

    public OrchestrateRequest<KvMetadata> put(final Object value) {
        if (value == null) {
            throw new IllegalArgumentException("'value' cannot be null.");
        }

        final byte[] content;
        try {
            content = (value instanceof String)
                    ? ((String) value).getBytes()
                    : mapper.writeValueAsBytes(value);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e); // FIXME
        }

        final UEncoder urlEncoder = new UEncoder();
        final String uri = urlEncoder.encodeURL(collection)
                .concat("/")
                .concat(urlEncoder.encodeURL(key));

        final HttpRequestPacket.Builder httpHeaderBuilder = HttpRequestPacket.builder()
                .method(Method.PUT)
                .contentType("application/json")
                .uri(uri);
        if (objectRef != null) {
            httpHeaderBuilder.header(Header.IfMatch, "\"".concat(objectRef).concat("\""));
        } else if (ifAbsent) {
            httpHeaderBuilder.header(Header.IfNoneMatch, "\"*\"");
        }
        httpHeaderBuilder.contentLength(content.length);

        final HttpContent packet = httpHeaderBuilder.build()
                .httpContentBuilder()
                .content(new ByteBufferWrapper(ByteBuffer.wrap(content)))
                .build();
        return new OrchestrateRequest<KvMetadata>(client, packet, new ResponseConverter<KvMetadata>() {
            @Override
            public KvMetadata from(final HttpContent response) throws IOException {
                final HttpHeader header = response.getHttpHeader();
                final int status = ((HttpResponsePacket) header).getStatus();

                if (status == 201) {
                    final String ref = header.getHeader(Header.ETag)
                            .replace("\"", "")
                            .replace("-gzip", "");
                    return new KvMetadata(collection, key, ref);
                }
                return null;
            }
        });
    }

}
