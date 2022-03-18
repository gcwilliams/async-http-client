package uk.co.gcwilliams.async.http.impl.pool;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * The HTTP channel pools
 *
 * @author : Gareth Williams
 **/
public class HttpChannelPools extends AbstractChannelPoolMap<HttpChannelPools.Key, ChannelPool> {

    private final Bootstrap bootstrap;

    private final Duration acquireTimeout;

    private final int maxConnections;

    private final int maxPendingAcquires;

    private final HttpChannelPoolConfiguration configuration;

    /**
     * Constructor
     *
     * @param group the event loop group
     * @param connectTimeout the connection timeout
     * @param acquireTimeout the acquire timeout
     * @param maxConnections the maximum number of connections
     * @param maxPendingAcquires the maximum number of pending acquires
     * @param configuration the configuration
     */
    public HttpChannelPools(
            EventLoopGroup group,
            Duration connectTimeout,
            Duration acquireTimeout,
            int maxConnections,
            int maxPendingAcquires,
            HttpChannelPoolConfiguration configuration) {
        this.bootstrap = new Bootstrap();
        this.bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        this.bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int)connectTimeout.toMillis());
        this.bootstrap.group(group);
        this.bootstrap.channel(NioSocketChannel.class);
        this.acquireTimeout = acquireTimeout;
        this.maxConnections = maxConnections;
        this.maxPendingAcquires = maxPendingAcquires;
        this.configuration = configuration;
    }

    @Override
    protected ChannelPool newPool(Key key) {
        return new HttpChannelPool(
            bootstrap.clone(),
            key.scheme,
            key.host,
            key.port,
            acquireTimeout,
            maxConnections,
            maxPendingAcquires,
            configuration);
    }

    /**
     * Shuts down the event loop group
     *
     */
    public void shutdown() {
        bootstrap.config().group().shutdownGracefully();
    }

    /**
     * Creates a new key
     *
     * @param uri the URI
     * @return the key
     */
    public static Key createKey(URI uri) {
        return new Key(uri.getScheme(), uri.getHost(), uri.getPort());
    }

    /**
     * The key
     *
     */
    public static class Key {

        final String scheme;

        final String host;

        final int port;

        /**
         * Constructor
         *
         * @param scheme the scheme
         * @param host the host
         * @param port the port
         */
        private Key(String scheme, String host, int port) {
            this.scheme = scheme;
            this.host = host;
            this.port = port > -1 ? port : "https".equals(scheme) ? 443 : 80;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return port == key.port && scheme.equals(key.scheme) && host.equals(key.host);
        }

        @Override
        public int hashCode() {
            return Objects.hash(scheme, host, port);
        }
    }
}
