package uk.co.gcwilliams.async.http.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import uk.co.gcwilliams.async.http.AsyncHttpClient;
import uk.co.gcwilliams.async.http.AsyncHttpClientListener;
import uk.co.gcwilliams.async.http.AsyncHttpClientListenerFactory;
import uk.co.gcwilliams.async.http.AsyncHttpRequest;
import uk.co.gcwilliams.async.http.AsyncHttpResponse;
import uk.co.gcwilliams.async.http.Task;
import uk.co.gcwilliams.async.http.impl.handler.HttpRequestInboundHandler;
import uk.co.gcwilliams.async.http.impl.handler.HttpRequestOutboundHandler;
import uk.co.gcwilliams.async.http.impl.pool.HttpChannelPoolConfiguration;
import uk.co.gcwilliams.async.http.impl.pool.HttpChannelPools;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * The netty async HTTP client
 *
 * @author : Gareth Williams
 **/
public class NettyAsyncHttpClient implements AsyncHttpClient {

    private final HttpChannelPools httpChannelPools;

    private final AsyncHttpClientListenerFactory listenerFactory;

    private final Executor executor;

    /**
     * Constructor
     *
     * @param httpChannelPools the HTTP channel pools
     * @param listenerFactory the listener factory
     * @param executor the executor
     */
    private NettyAsyncHttpClient(
            HttpChannelPools httpChannelPools,
            AsyncHttpClientListenerFactory listenerFactory,
            Executor executor) {
        this.httpChannelPools = httpChannelPools;
        this.listenerFactory = listenerFactory;
        this.executor = executor;
    }

    @Override
    public Task<AsyncHttpResponse> prepare(AsyncHttpRequest request) {

        return Task.of((resolve, reject) -> {

            List<AsyncHttpClientListener> listeners = listenerFactory.createListeners();

            listeners.forEach(AsyncHttpClientListener::onPrepare); // TODO: handle exceptions

            AsyncHttpRequest modified = onModifyRequest(request, listeners);

            ChannelPool channelPool = httpChannelPools.get(HttpChannelPools.createKey(request.getUri()));

            channelPool.acquire().addListener((GenericFutureListener<Future<Channel>>) future -> {

                if (future.isSuccess()) {

                    Channel channel = future.getNow();

                    ChannelPipeline pipeline = channel.pipeline();

                    WriteTimeoutHandler writeTimeoutHandler = new WriteTimeoutHandler(request.getWriteTimeout().toMillis(), TimeUnit.MILLISECONDS);
                    pipeline.addBefore(HttpClientCodec.class.getName(), WriteTimeoutHandler.class.getName(), writeTimeoutHandler);

                    ReadTimeoutHandler readTimeoutHandler = new ReadTimeoutHandler(request.getReadTimeout().toMillis(), TimeUnit.MILLISECONDS);
                    pipeline.addAfter(WriteTimeoutHandler.class.getName(), ReadTimeoutHandler.class.getName(), readTimeoutHandler);

                    pipeline.addLast(HttpRequestInboundHandler.class.getName(), new HttpRequestInboundHandler(listeners, request, resolve, reject, executor, channelPool));
                    pipeline.addLast(HttpRequestOutboundHandler.class.getName(), new HttpRequestOutboundHandler(listeners, reject, executor, channelPool));

                    channel.writeAndFlush(createFullHttpRequest(modified));

                    executor.execute(() -> listeners.forEach(listener -> listener.onSend(modified)));

                } else {
                    executor.execute(() -> {
                        Throwable cause = future.cause();
                        Exception exception = cause instanceof Exception ? (Exception) cause : new Exception(cause);
                        listeners.forEach(listener -> listener.onException(exception));
                        reject.accept(exception);
                    });
                }
            });
        });
    }

    @Override
    public void close() throws Exception {
        httpChannelPools.shutdown();
    }

    /**
     * Calls on modify request
     *
     * @param request the request
     * @param listeners the listeners
     * @return the modified
     */
    private AsyncHttpRequest onModifyRequest(AsyncHttpRequest request, List<AsyncHttpClientListener> listeners) {
        for (AsyncHttpClientListener listener : listeners) {
            request = listener.onModifyRequest(request);
        }
        return request;
    }

    /**
     * Creates the netty HTTP request
     *
     * @param request the request
     * @return the netty request
     */
    private static FullHttpRequest createFullHttpRequest(AsyncHttpRequest request) {

        DefaultFullHttpRequest fullHttpRequest = new DefaultFullHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.valueOf(request.getMethod().name()),
            request.getUri().toString(),
            createBody(request));

        request.getHeaders().forEach(fullHttpRequest.headers()::add);

        fullHttpRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        fullHttpRequest.headers().set(HttpHeaderNames.HOST, request.getUri().getHost());
        fullHttpRequest.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.getBody().length);

        return fullHttpRequest;
    }

    /**
     * Creates the body
     *
     * @param request the request body
     * @return the byte buffer
     */
    private static ByteBuf createBody(AsyncHttpRequest request) {
        if (request.getBody().length == 0) {
            return Unpooled.buffer(0);
        }
        ByteBuf buffer = Unpooled.buffer(request.getBody().length);
        buffer.writeBytes(request.getBody());
        return buffer;
    }

    /**
     * Creates a builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The builder
     *
     */
    public static class Builder {

        private Integer threads;

        private Duration connectTimeout = Duration.ofSeconds(1);

        private Duration acquireTimeout = Duration.ofSeconds(10);

        private int maxConnections = 100;

        private int maxPendingAcquires = 100;

        private int maxInitialLineLength = 4096;

        private int maxHeaderSize = 8192;

        private int maxChunkSize = 8192;

        private int initialBufferSize = 128;

        private int maxContentLength = Integer.MAX_VALUE;

        private boolean enableSni = true;

        private Executor executor = Executors.newCachedThreadPool();

        private AsyncHttpClientListenerFactory listenerFactory = List::of;

        private Builder() {
        }

        /**
         * Sets the number of threads for the event loop group, see {@link io.netty.channel.nio.NioEventLoopGroup}
         *
         * @param threads the number of threads
         * @return the builder
         */
        public Builder withThreads(Integer threads) {
            this.threads = threads;
            return this;
        }

        /**
         * Sets the connection timeout
         *
         * @param connectTimeout the connection timeout
         * @return the builder
         */
        public Builder withConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * Sets the acquire timeout, the maximum timeout waiting for a connection to
         * be provided from the connection pool
         *
         * @param acquireTimeout the acquire timeout
         * @return the builder
         */
        public Builder withAcquireTimeout(Duration acquireTimeout) {
            this.acquireTimeout = acquireTimeout;
            return this;
        }

        /**
         * Sets the maximum number of connections per protocol, host, port
         *
         * @param maxConnections the maximum number of connections per protocol, host, port
         * @return the builder
         */
        public Builder withMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        /**
         * Sets the maximum pending acquires allowed, e.g. the number of requests waiting
         * for a connection to be provided from the connection pool
         *
         * @param maxPendingAcquires the maximum number of acquires that can be waiting for a connection
         * @return the builder
         */
        public Builder withMaxPendingAcquires(int maxPendingAcquires) {
            this.maxPendingAcquires = maxPendingAcquires;
            return this;
        }

        /**
         * Sets the max initial line length
         *
         * @param maxInitialLineLength the max initial line length
         * @return the builder
         */
        public Builder setMaxInitialLineLength(int maxInitialLineLength) {
            this.maxInitialLineLength = maxInitialLineLength;
            return this;
        }

        /**
         * Sets the max header size
         *
         * @param maxHeaderSize the max header size
         * @return the builder
         */
        public Builder setMaxHeaderSize(int maxHeaderSize) {
            this.maxHeaderSize = maxHeaderSize;
            return this;
        }

        /**
         * Sets the max chunk size
         *
         * @param maxChunkSize the max chunk size
         * @return the builder
         */
        public Builder setMaxChunkSize(int maxChunkSize) {
            this.maxChunkSize = maxChunkSize;
            return this;
        }

        /**
         * Sets the initial buffer size
         *
         * @param initialBufferSize the initial buffer size
         * @return the builder
         */
        public Builder setInitialBufferSize(int initialBufferSize) {
            this.initialBufferSize = initialBufferSize;
            return this;
        }

        /**
         * Sets the max content length
         *
         * @param maxContentLength the max content length
         * @return the builder
         */
        public Builder setMaxContentLength(int maxContentLength) {
            this.maxContentLength = maxContentLength;
            return this;
        }

        /**
         * Sets whether SNI (Server Name Identification) is enabled or not
         *
         * @param enableSni true if SNI is enabled, false otherwise
         * @return the builder
         */
        public Builder setEnableSni(boolean enableSni) {
            this.enableSni = enableSni;
            return this;
        }

        /**
         * Sets the executor
         *
         * @param executor the executor
         * @return the builder
         */
        public Builder withExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Sets the listener factory
         *
         * @param listenerFactory the listener factory
         */
        public Builder withListenerFactory(AsyncHttpClientListenerFactory listenerFactory) {
            this.listenerFactory = listenerFactory;
            return this;
        }

        /**
         * Builds the async HTTP client
         *
         * @return the async HTTP client
         */
        public AsyncHttpClient build() {
            requireNonNull(connectTimeout, "the connection timeout should be provided");
            requireNonNull(acquireTimeout, "the acquire timeout should be provided");
            checkState(maxConnections > 0, "the maximum number of connections should be positive");
            checkState(maxPendingAcquires > 0, "the maximum number of acquires should be positive");
            checkState(maxInitialLineLength > 0, "the maximum initial line length should be positive");
            checkState(maxHeaderSize > 0, "the maximum header size should be positive");
            checkState(maxChunkSize > 0, "the maximum chunk size should be positive");
            checkState(initialBufferSize > 0, "the initial buffer size should be positive");
            checkState(maxContentLength > 0, "the max content length should be positive");
            requireNonNull(listenerFactory, "the listener factory should be provided");
            HttpChannelPools httpChannelPools = new HttpChannelPools(
                threads != null ? new NioEventLoopGroup(threads) : new NioEventLoopGroup(),
                connectTimeout,
                acquireTimeout,
                maxConnections,
                maxPendingAcquires,
                new HttpChannelPoolConfiguration(
                    maxInitialLineLength,
                    maxHeaderSize,
                    maxChunkSize,
                    initialBufferSize,
                    maxContentLength,
                    enableSni));
            return new NettyAsyncHttpClient(httpChannelPools, listenerFactory, executor);
        }

        /**
         * Checks the state
         *
         * @param state the state
         * @param message the message
         */
        private static void checkState(boolean state, String message) {
            if (!state) {
                throw new IllegalStateException(message);
            }
        }
    }
}
