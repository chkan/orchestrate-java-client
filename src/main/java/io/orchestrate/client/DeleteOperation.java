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

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.glassfish.grizzly.http.HttpHeader;

import java.io.IOException;

import static io.orchestrate.client.Preconditions.*;

/**
 * Delete all KV objects from a collection in the Orchestrate.io service.
 *
 * <p>Usage:
 * <pre>
 * {@code
 * DeleteOperation deleteOp = new DeleteOperation("myCollection");
 * Future<Boolean> futureResult = client.execute(deleteOp);
 * Boolean result = futureResult.get();
 * if (result)
 *     System.out.println("Successfully deleted 'someKey'.");
 * }
 * </pre>
 */
@ToString(callSuper=false)
@EqualsAndHashCode(callSuper=false)
public final class DeleteOperation extends AbstractOperation<Boolean> {

    /** The collection to delete. */
    private final String collection;

    /**
     * Create a new {@code DeleteOperation} to remove all objects from the
     * specified {@code collection}.
     *
     * @param collection The name of collection to delete.
     */
    public DeleteOperation(final String collection) {
        this.collection = checkNotNullOrEmpty(collection, "collection");
    }

    /** {@inheritDoc} */
    @Override
    Boolean fromResponse(final int status, final HttpHeader httpHeader, final String json, final JacksonMapper mapper)
            throws IOException {
        return (status == 204);
    }

    /**
     * Returns the collection from this operation.
     *
     * @return The collection from this operation.
     */
    public String getCollection() {
        return collection;
    }

}
