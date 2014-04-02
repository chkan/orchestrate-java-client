package io.orchestrate.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.glassfish.grizzly.http.*;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.UEncoder;

import javax.annotation.Nullable;
import java.io.IOException;

public class KvResource {

    /** The Orchestrate client to make requests with. */
    private final OrchestrateClient client;
    /** The object mapper used to deserialize JSON responses. */
    private final ObjectMapper mapper;
    /** The collection for the request. */
    private final String collection;
    /** The key for the request. */
    private final String key;

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

}
