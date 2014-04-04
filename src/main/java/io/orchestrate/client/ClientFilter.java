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

import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.*;
import org.glassfish.grizzly.http.util.Base64Utils;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.impl.SafeFutureImpl;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import static org.glassfish.grizzly.attributes.AttributeBuilder.DEFAULT_ATTRIBUTE_BUILDER;

/**
 * A filter to handle HTTP operations and apply the Orchestrate.io
 * authentication header.
 */
@Slf4j
final class ClientFilter extends BaseFilter {

    /** The name of the filter attribute for a HTTP response. */
    public static final String HTTP_RESPONSE_ATTR = "httpResp";
    /** The value for the user agent header. */
    private static final String BASE_USER_AGENT = buildBaseUserAgent();

    /** The attribute for the HTTP response. */
    private final Attribute<SafeFutureImpl<HttpContent>> httpResponseAttr;
    /** The header value to authenticate with the Orchestrate.io service */
    private final String authHeaderValue;
    /** The header value to indicate the client and version queried with. */
    private final String userAgentValue;
    /** The hostname for the Orchestrate.io service. */
    private final String host;

    ClientFilter(
            final String apiKey,
            final URI host,
            @Nullable final String userAgent) {
        assert (apiKey != null);
        assert (host != null);

        this.httpResponseAttr =
                DEFAULT_ATTRIBUTE_BUILDER.createAttribute(HTTP_RESPONSE_ATTR);
        this.authHeaderValue =
                "Basic ".concat(Base64Utils.encodeToString(apiKey.getBytes(), true));
        this.userAgentValue = (userAgent == null)
                ? BASE_USER_AGENT
                : String.format("%s %s", BASE_USER_AGENT, userAgent);
        this.host = host.getHost();
    }

    @Override
    public void exceptionOccurred(final FilterChainContext ctx, final Throwable error) {
        final SafeFutureImpl<HttpContent> future =
                httpResponseAttr.get(ctx.getConnection().getAttributes());
        future.failure(error);
        super.exceptionOccurred(ctx, error);
    }

    @Override
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final SafeFutureImpl<HttpContent> future =
                httpResponseAttr.get(ctx.getConnection().getAttributes());
        try {
            final HttpContent content = ctx.getMessage();
            if (!content.isLast()) {
                return ctx.getStopAction(content);
            }

            final HttpHeader header = content.getHttpHeader();
            final int status = ((HttpResponsePacket) header).getStatus();

            ClientFilter.log.info("Received content: {}", header);
            switch (status) {
                case 200:   // intentional fallthrough
                case 201:   // intentional fallthrough
                case 204:   // intentional fallthrough
                case 404:   // intentional fallthrough
                    future.result(content);
                    break;
                default:
                    final String reqId = header.getHeader("x-orchestrate-req-id");
                    final String message = content.getContent().toStringContent();
                    future.failure(new RequestException(status, message, reqId));
            }
        } catch (final Throwable t) {
            future.failure(t);
        }

        return ctx.getStopAction();
    }

    @Override
    public NextAction handleWrite(final FilterChainContext ctx) throws IOException {
        final Object message = ctx.getMessage();
        if (!(message instanceof HttpPacket)) {
            return ctx.getInvokeAction();
        }

        final HttpPacket request = (HttpPacket) message;
        final HttpRequestPacket httpHeader = (HttpRequestPacket) request.getHttpHeader();

        // add version information
        final String uriWithPrefix = "/v0/".concat(httpHeader.getRequestURI());

        // adjust the HTTP request to include standard headers
        httpHeader.setProtocol(Protocol.HTTP_1_1);
        httpHeader.setHeader(Header.Host, host);
        httpHeader.setHeader(Header.UserAgent, userAgentValue);
        httpHeader.setRequestURI(uriWithPrefix);

        // add basic auth information
        httpHeader.addHeader(Header.Authorization, authHeaderValue);

        ClientFilter.log.info("Sending request: {}", httpHeader);
        ctx.write(request);

        return ctx.getStopAction();
    }

    private static String buildBaseUserAgent() {
        String version = "unknown";
        try {
            final Properties props = new Properties();
            String basePath = "/" + ClientFilter.class.getPackage().getName().replace('.', '/');
            props.load(ClientFilter.class.getResourceAsStream(basePath + "/build.properties"));
            if (props.containsKey("version")) {
                version = props.getProperty("version");
            }
        } catch (final Exception ignored) {
        }
        return String.format("OrchestrateJavaClient/%s (Java/%s; %s)",
                version, System.getProperty("java.version"), System.getProperty("java.vendor"));
    }

}
