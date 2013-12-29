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
package io.orchestrate.client.integration;

import io.orchestrate.client.OrchestrateFuture;
import io.orchestrate.client.SearchOperation;
import io.orchestrate.client.SearchResults;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

/**
 * {@link io.orchestrate.client.SearchOperation}.
 */
public final class SearchTest extends OperationTest {

    private <T> SearchResults<T> result(final SearchOperation<T> searchOp)
            throws InterruptedException, ExecutionException, TimeoutException {
        OrchestrateFuture<SearchResults<T>> future = client().execute(searchOp);
        return future.get(3, TimeUnit.SECONDS);
    }

    @Test
    public void basicSearch()
            throws InterruptedException, ExecutionException, TimeoutException {
        SearchOperation<String> searchOp = SearchOperation
                .builder(TEST_COLLECTION, String.class)
                .build();
        SearchResults<String> results = result(searchOp);

        assertNotNull(results);
    }

}
