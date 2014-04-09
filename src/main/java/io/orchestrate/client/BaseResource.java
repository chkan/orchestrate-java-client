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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The base resource for features in the Orchestrate API.
 */
abstract class BaseResource {

    /** The Orchestrate client to make requests with. */
    protected final OrchestrateClient client;
    /** The object mapper used to deserialize JSON responses. */
    protected final ObjectMapper mapper;

    BaseResource(final OrchestrateClient client, final JacksonMapper mapper) {
        assert (client != null);
        assert (mapper != null);

        this.client = client;
        this.mapper = mapper.getMapper();
    }

}
