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

import io.orchestrate.client.ClientException;
import io.orchestrate.client.OrchestrateClient;
import io.orchestrate.client.RequestException;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link io.orchestrate.client.OrchestrateClient#ping(String)}.
 */
public final class ClientTest extends BaseClientTest {

    @Test
    public void pingCheck() throws IOException {
        client.ping();
    }

    @Test
    public void pingWithInvalidKey() throws IOException {
        String badKey = "12345678-1234-1234-1234-1234567890123";
        client = OrchestrateClient.builder(badKey).build();
        try {
            client.ping();
        } catch (ClientException ex) {
            ExecutionException exex = (ExecutionException)ex.getCause();
            RequestException rex = (RequestException) exex.getCause();
            assertEquals(401, rex.getStatusCode());
        }
    }

}
