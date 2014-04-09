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

import com.pholser.junit.quickcheck.ForAll;
import io.orchestrate.client.ResponseAdapter;
import org.glassfish.grizzly.utils.DataStructures;
import org.junit.contrib.theories.Theories;
import org.junit.contrib.theories.Theory;
import org.junit.runner.RunWith;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

/**
 * {@link io.orchestrate.client.OrchestrateClient#event(String, String)}.
 */
@RunWith(Theories.class)
public final class EventTest extends BaseClientTest {

    @Theory
    public void putEvent(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final Boolean result =
                client.event(collection(), "key")
                      .type(type)
                      .put("{}")
                      .get();

        assertTrue(result);
    }

    @Theory
    public void putEventAsync(@ForAll(sampleSize=10) final String type)
            throws InterruptedException {
        assumeThat(type, not(isEmptyString()));

        final BlockingQueue<Boolean> queue = DataStructures.getLTQInstance(Boolean.class);
        client.event(collection(), "key")
              .type(type)
              .put("{}")
              .on(new ResponseAdapter<Boolean>() {
                  @Override
                  public void onFailure(final Throwable error) {
                      fail(error.getMessage());
                  }

                  @Override
                  public void onSuccess(final Boolean object) {
                      queue.add(object);
                  }
              });

        final Boolean result = queue.poll(5000, TimeUnit.MILLISECONDS);
        assertTrue(result);
    }

    @Theory
    public void putEventWithTimestamp(@ForAll(sampleSize=10) final String type) {
        assumeThat(type, not(isEmptyString()));

        final Boolean result =
                client.event(collection(), "key")
                      .type(type)
                      .put("{}", System.currentTimeMillis())
                      .get();

        assertTrue(result);
    }

    @Theory
    public void putEventWithTimestampAsync(@ForAll(sampleSize=10) final String type)
            throws InterruptedException {
        assumeThat(type, not(isEmptyString()));

        final BlockingQueue<Boolean> queue = DataStructures.getLTQInstance(Boolean.class);
        client.event(collection(), "key")
              .type(type)
              .put("{}", System.currentTimeMillis())
              .on(new ResponseAdapter<Boolean>() {
                  @Override
                  public void onFailure(final Throwable error) {
                      fail(error.getMessage());
                  }

                  @Override
                  public void onSuccess(final Boolean object) {
                      queue.add(object);
                  }
              });

        final Boolean result = queue.poll(5000, TimeUnit.MILLISECONDS);
        assertTrue(result);
    }

}
