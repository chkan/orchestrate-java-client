package io.orchestrate.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.util.UEncoder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static io.orchestrate.client.ResponseConverterUtil.jsonToKvObject;

public class ListResource {

    /** The Orchestrate client to make requests with. */
    private final OrchestrateClient client;
    /** The object mapper used to deserialize JSON responses. */
    private final ObjectMapper mapper;
    /** The collection for the request. */
    private final String collection;
    /** The start of the key range to paginate from. */
    private @Nullable String startKey;
    /** Include the specified "startKey" if it exists. */
    private boolean inclusive;
    /** The number of KV objects to retrieve. */
    private int limit = 10;

    ListResource(final OrchestrateClient client,
               final JacksonMapper mapper,
               final String collection) {
        assert (client != null);
        assert (mapper != null);
        assert (collection != null);
        assert (collection.length() > 0);

        this.client = client;
        this.mapper = mapper.getMapper();
        this.collection = collection;
        this.inclusive = false;
        this.limit = 10;
    }

    public <T> OrchestrateRequest<KvList<T>> get(final @NonNull Class<T> clazz) {
        if (startKey != null && inclusive) {
            throw new IllegalStateException("'inclusive' requires 'startKey' for request.");
        }

        final UEncoder urlEncoder = new UEncoder();
        final String uri = urlEncoder.encodeURL(collection);
        String query = "limit=".concat(limit + "");
        if (startKey != null) {
            final String keyName = (inclusive) ? "startKey" : "afterKey";
            query = query
                    .concat('&' + keyName + '=')
                    .concat(urlEncoder.encodeURL(startKey));
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

    public ListResource inclusive() {
        return inclusive(true);
    }

    public ListResource inclusive(final boolean inclusive) {
        this.inclusive = inclusive;
        return this;
    }

    public ListResource limit(final int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("'limit' cannot be negative.");
        }

        this.limit = limit;
        return this;
    }

    public ListResource startKey(final @NonNull String startKey) {
        if (startKey.length() < 1) {
            throw new IllegalArgumentException("'startKey' cannot be empty.");
        }

        this.startKey = startKey;
        return this;
    }

}
