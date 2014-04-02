package io.orchestrate.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

final class ResponseConverterUtil {

    static <T> KvObject<T> jsonToKvObject(
            final ObjectMapper mapper, final JsonNode jsonNode, final Class<T> clazz)
            throws IOException {
        assert (mapper != null);
        assert (jsonNode != null);
        assert (clazz != null);

        // parse the PATH structure (e.g.):
        // {"collection":"coll","key":"aKey","ref":"someRef"}
        final JsonNode path = jsonNode.get("path");
        final String collection = path.get("collection").asText();
        final String key = path.get("key").asText();
        final String ref = path.get("ref").asText();
        final KvMetadata metadata = new KvMetadata(collection, key, ref);

        // parse result structure (e.g.):
        // {"path":{...},"value":{}}
        final JsonNode valueNode = jsonNode.get("value");

        // TODO Is there value in always having the raw string value available on the KvObject?
        final String rawValue = mapper.writeValueAsString(valueNode);

        @SuppressWarnings("unchecked")
        final T value = (clazz == String.class)
                ? (T) rawValue
                : valueNode.traverse(mapper).readValueAs(clazz);

        return new KvObject<T>(metadata, value, rawValue);
    }

}
