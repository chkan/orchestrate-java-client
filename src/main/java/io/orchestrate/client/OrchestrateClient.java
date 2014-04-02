package io.orchestrate.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.attributes.AttributeHolder;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.HttpClientFilter;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.NIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.ssl.SSLFilter;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * The client used to read and write data to the Orchestrate.io service.
 */
@Slf4j
public class OrchestrateClient implements NewClient {

    /** The builder for this instance of the client. */
    private final Builder builder;
    /** The socket transport for HTTP messages. */
    private final NIOTransport transport;

    /**
     * Create a new {@code client} with the specified {@code apiKey} and default
     * {@code JacksonMapper}.
     *
     * <p>Equivalent to:
     * <pre>
     * {@code
     * Client client = OrchestrateClient.builder("your api key").build();
     * }
     * </pre>
     *
     * @param apiKey An API key for the Orchestrate.io service.
     */
    public OrchestrateClient(final String apiKey) {
        this(builder(apiKey));
    }

    /**
     * Create a new {@code client} with the specified {@code apiKey} and {@code
     * objectMapper}.
     *
     * @param apiKey An API key for the Orchestrate.io service.
     * @param objectMapper The Jackson JSON mapper to marshall data with.
     */
    public OrchestrateClient(final String apiKey, final ObjectMapper objectMapper) {
        this(new Builder(apiKey).mapper(objectMapper));
    }

    /**
     * Create a new {@code client} with the specified {@code apiKey} and {@code
     * mapper}.
     *
     * @param apiKey An API key for the Orchestrate.io service.
     * @param mapper The mapper to marshall data with.
     */
    public OrchestrateClient(final String apiKey, final JacksonMapper mapper) {
        this(new Builder(apiKey).mapper(mapper));
    }

    private OrchestrateClient(final Builder builder) {
        assert (builder != null);

        this.builder = builder;

        final ThreadPoolConfig poolConfig = ThreadPoolConfig.defaultConfig()
                .setPoolName("OrchestrateClientPool")
                .setCorePoolSize(builder.poolSize)
                .setMaxPoolSize(builder.maxPoolSize);
        // TODO add support for GZip compression
        final FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless()
                .add(new TransportFilter());
        if (builder.useSSL) {
            final SSLEngineConfigurator serverConfig = initializeSSL();
            final SSLEngineConfigurator clientConfig = serverConfig.copy().setClientMode(true);

            filterChainBuilder.add(new SSLFilter(serverConfig, clientConfig));
        }

        filterChainBuilder
                .add(new HttpClientFilter())
                .add(new NewClientFilter(builder.apiKey, builder.host, builder.userAgent));
        // TODO experiment with the Leader-Follower IOStrategy
        this.transport = TCPNIOTransportBuilder.newInstance()
                .setTcpNoDelay(true)
                .setKeepAlive(true)
                .setWorkerThreadPoolConfig(poolConfig)
                .setIOStrategy(WorkerThreadIOStrategy.getInstance())
                .setProcessor(filterChainBuilder.build())
                .build();
    }

    private static SSLEngineConfigurator initializeSSL() {
        final SSLContextConfigurator sslContextConfig = new SSLContextConfigurator();
        return new SSLEngineConfigurator(sslContextConfig.createSSLContext(),
                false, false, false);
    }

    private Connection connect() {
        try {
            if (transport.isStopped()) {
                transport.start();
            }

            final Future<Connection> connect =
                    transport.connect(builder.host.getHost(), builder.port);
            // TODO make the connection timeout length configurable
            return connect.get(5000, TimeUnit.MILLISECONDS);
        } catch (final Exception e) {
            throw new ClientException(e);
        }
    }

    <T> SafeFutureImpl<HttpContent> execute(
            final HttpContent request,
            final Set<ResponseCompletionHandler<T>> listeners) {
        assert (request != null);
        assert (listeners != null);

        final Connection connection = connect();

        final SafeFutureImpl<HttpContent> future = SafeFutureImpl.create();
        for (final ResponseCompletionHandler<T> listener : listeners) {
            future.addCompletionHandler(listener);
        }

        final AttributeHolder attrs = connection.getAttributes();
        attrs.setAttribute(NewClientFilter.HTTP_RESPONSE_ATTR, future);

        @SuppressWarnings("unchecked")
        final GrizzlyFuture write = connection.write(request);

        return future;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        if (transport != null && !transport.isStopped()) {
            transport.shutdownNow();
        }
    }

    /** {@inheritDoc} */
    @Override
    public KvResource kv(
            final @NonNull String collection, final @NonNull String key) {
        return new KvResource(this, builder.mapper, collection, key);
    }

    /** {@inheritDoc} */
    @Override
    public void ping() throws IOException {
        // TODO
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    // TODO
    public static Builder builder(final @NonNull String apiKey) {
        return new Builder(apiKey);
    }

    /**
     * Builder used to create {@code Client} instances.
     *
     * <p>Usage:
     * <pre>
     * {@code
     * Client client = OrchestrateClient.builder("your api key")
     *         .host("https://api.orchestrate.io")  // optional
     *         .port(80)           // optional
     *         .poolSize(Runtime.getRuntime().availableProcessors()) // optional
     *         .maxPoolSize(15)    // optional
     *         .build();
     * }
     * </pre>
     */
    public static final class Builder {

        /** The default host for the Orchestrate.io service. */
        public static final String DEFAULT_HOST = "https://api.orchestrate.io";
        /** The default port for the Orchestrate.io service. */
        public static final int DEFAULT_PORT = 443;

        /** An API key for the Orchestrate.io service. */
        private final String apiKey;
        /** The host for the Orchestrate.io service. */
        private URI host;
        /** The port for the Orchestrate.io service. */
        private int port;
        /** The number of threads to use with the client. */
        private int poolSize;
        /** The maximum size of the thread pool to use with the client. */
        private int maxPoolSize;
        /** The configured JSON mapper. */
        private JacksonMapper mapper;
        /** Whether to use SSL with the connection. */
        private boolean useSSL;
        /** Value to append as the "User-Agent" in requests to Orchestrate. */
        private String userAgent;

        private Builder(final @NonNull String apiKey) {
            if (apiKey.length() < 1) {
                throw new IllegalArgumentException("'apiKey' cannot be empty.");
            }

            this.apiKey = apiKey;
            host(DEFAULT_HOST);
            port(DEFAULT_PORT);
            poolSize(Runtime.getRuntime().availableProcessors());
            maxPoolSize(Integer.MAX_VALUE);
            mapper(JacksonMapper.builder());
            useSSL(Boolean.TRUE);
        }

        /**
         * Set the hostname for the Orchestrate.io service, defaults to {@code
         * Builder.DEFAULT_HOST}.
         *
         * @param host The hostname for the Orchestrate.io service.
         * @return This builder.
         * @see ClientBuilder#DEFAULT_HOST
         */
        public Builder host(final @NonNull String host) {
            if (host.length() < 1) {
                throw new IllegalArgumentException("'host' cannot be empty.");
            }
            this.host = URI.create(host);
            return this;
        }

        /**
         * Set the port for the Orchestrate.io service, defaults to {@code
         * Builder.DEFAULT_PORT}.
         *
         * @param port The port for the Orchestrate.io service.
         * @return This builder.
         * @see ClientBuilder#DEFAULT_PORT
         */
        public Builder port(final int port) {
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("'port' must be between 1 and 65535.");
            }
            this.port = port;
            return this;
        }

        /**
         * The initial number of threads to use with the client, defaults to
         * {@link Runtime#availableProcessors()}.
         *
         * @param poolSize The size of the thread pool to start with.
         * @return This builder.
         */
        public Builder poolSize(final int poolSize) {
            if (poolSize < 0) {
                throw new IllegalArgumentException("'poolSize' cannot be negative.");
            }
            this.poolSize = poolSize;
            return this;
        }

        /**
         * The maximum number of threads to use with the client, defaults to
         * {@link Integer#MAX_VALUE}.
         *
         * @param maxPoolSize The maximum size to grow the thread pool to.
         * @return This builder.
         */
        public Builder maxPoolSize(final int maxPoolSize) {
            if (maxPoolSize < 1) {
                throw new IllegalArgumentException("'maxPoolSize' cannot be smaller than one.");
            }
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        /**
         * The Jackson JSON {@code ObjectMapper} to use when marshalling data to
         * and from the service, defaults to {@link io.orchestrate.client.JacksonMapper#builder()}.
         *
         * @param objectMapper A Jackson JSON {@code ObjectMapper}.
         * @return This builder.
         */
        public Builder mapper(final @NonNull ObjectMapper objectMapper) {
            return mapper(JacksonMapper.builder(objectMapper));
        }

        /**
         * A {@code Builder} used to build the {@code JacksonMapper} to use when
         * marshalling data to and from the service.
         *
         * @param mapperBuilder A {@code JacksonMapper.Builder}.
         * @return This builder.
         */
        public Builder mapper(final @NonNull JacksonMapper.Builder mapperBuilder) {
            return mapper(mapperBuilder.build());
        }

        /**
         * A {@code JacksonMapper} to use when marshalling data to and from the
         * service.
         *
         * @param mapper A {@code JacksonMapper}.
         * @return This builder.
         */
        public Builder mapper(final @NonNull JacksonMapper mapper) {
            this.mapper = mapper;
            return this;
        }

        /**
         * Enable or disable SSL when connecting to the service, this value defaults
         * to {@code true}.
         *
         * @param useSSL If {@code true} enable SSL when connecting to the service.
         * @return This builder.
         */
        public Builder useSSL(final boolean useSSL) {
            this.useSSL = useSSL;
            return this;
        }

        /**
         * Set a custom value to be appended to the User-Agent header being sent
         * to Orchestrate.
         *
         * <p>This is useful for custom client implementations or libraries that
         * build on top of the Orchestrate client to help identify usage of those
         * implementations and libraries.
         *
         * @param userAgent The value to append as the "user-agent" request header.
         * @return This builder.
         */
        public Builder userAgent(final @NonNull String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public OrchestrateClient build() {
            return new OrchestrateClient(this);
        }

    }

}
