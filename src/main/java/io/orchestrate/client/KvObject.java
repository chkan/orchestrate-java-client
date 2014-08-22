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

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A container for a KV object.
 *
 * @param <T> The deserializable type for the value of this KV object.
 */
@ToString(callSuper=true)
@EqualsAndHashCode(callSuper=true)
public class KvObject<T> extends KvMetadata {

    /** The value for this KV object. */
    private final T value;
    /** The raw JSON value for this KV object. */
    private final String rawValue;
    /** Marks whether this KV object is a tombstone. */
    private final boolean tombstone;

    KvObject(final String collection, final String key, final String ref, final T value, final String rawValue) {
        this(collection, key, ref, value, rawValue, false);
    }

    KvObject(final String collection, final String key, final String ref, final T value, final String rawValue, final boolean tombstone) {
        super(collection, key, ref);
        assert (value != null);
        assert (rawValue != null);

        this.value = value;
        this.rawValue = rawValue;
        this.tombstone = tombstone;
    }

    KvObject(final KvMetadata metadata, final T value, final String rawValue) {
        this(metadata.getCollection(), metadata.getKey(), metadata.getRef(), value, rawValue);
    }

    KvObject(final KvMetadata metadata, final T value, final String rawValue, final boolean tombstone) {
        this(metadata.getCollection(), metadata.getKey(), metadata.getRef(), value, rawValue, tombstone);
    }

    KvObject(final KvObject<T> kvObject) {
        this(kvObject.getCollection(), kvObject.getKey(), kvObject.getRef(), kvObject.value, kvObject.rawValue);
    }

    /**
     * Returns the value of this KV object.
     *
     * @return The value of the KV object.
     */
    public final T getValue() {
        return value;
    }

    /**
     * Returns the raw JSON value of this KV object.
     *
     * @return The raw JSON value of this KV object.
     */
    public final String getRawValue() {
        return rawValue;
    }

    /**
     *
     *
     * @return {@code true} if this KV object is a tombstone.
     */
    public final boolean isTombstone() {
        return tombstone;
    }

}
