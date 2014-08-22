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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * A utility class with helper methods for converting JSON response data from
 * Orchestrate.
 */
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
        final boolean tombstone = path.has("tombstone");

        // parse result structure (e.g.):
        // {"path":{...},"value":{}}
        final JsonNode valueNode = jsonNode.get("value");

        return jsonToKvObject(mapper, valueNode, clazz, collection, key, ref, tombstone);
    }

    public static <T> KvObject<T> jsonToKvObject(ObjectMapper mapper, JsonNode valueNode, Class<T> clazz,
                                                 String collection, String key, String ref, boolean tombstone) throws IOException {
        assert (mapper != null);
        assert (valueNode != null);
        assert (clazz != null);

        final KvMetadata metadata = new KvMetadata(collection, key, ref);

        final String rawValue = mapper.writeValueAsString(valueNode);

        @SuppressWarnings("unchecked")
        final T value = (clazz == String.class)
                ? (T) rawValue
                : valueNode.traverse(mapper).readValueAs(clazz);

        // TODO Is there value in always having the raw string value available ON the KvObject?
        return new KvObject<T>(metadata, value, rawValue, tombstone);
    }
}
