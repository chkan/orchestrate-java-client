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
package io.orchestrate.client.itest;

import io.orchestrate.client.NewClient;
import io.orchestrate.client.OrchestrateClient;
import io.orchestrate.client.ResponseAdapter;
import org.glassfish.grizzly.utils.DataStructures;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link io.orchestrate.client.OrchestrateClient#delete(String)}.
 */
public final class CollectionTest {

    /** The client to run tests on. */
    private static NewClient client;

    @BeforeClass
    public static void setUpClass() {
        final String apiKey = "e927d81d-579b-4dcb-bd30-78cc67203107";
        client = OrchestrateClient.builder(apiKey).build();
    }

    @Test
    public void deleteCollection() {
        final boolean result = client.delete("collection").execute();
        assertTrue(result);
    }

    @Test
    public void deleteCollectionAsync() throws InterruptedException {
        final BlockingQueue<Boolean> queue = DataStructures.getLTQInstance(Boolean.class);
        client.delete("collection")
              .on(new ResponseAdapter<Boolean>() {
                  @Override
                  public void onSuccess(final Boolean object) {
                      queue.add(object);
                  }

                  @Override
                  public void onFailure(final Throwable error) {
                      fail(error.getMessage());
                  }
              })
              .executeAsync();

        final Boolean result = queue.poll(5000, TimeUnit.MILLISECONDS);
        assertTrue(result);
    }

}
