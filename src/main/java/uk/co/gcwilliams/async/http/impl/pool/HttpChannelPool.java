package uk.co.gcwilliams.async.http.impl.pool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.Future;
import uk.co.gcwilliams.async.http.impl.handler.DefaultSslHandler;
import uk.co.gcwilliams.async.http.impl.handler.HttpRequestInboundHandler;
import uk.co.gcwilliams.async.http.impl.handler.HttpRequestOutboundHandler;

import java.time.Duration;

/**
 * The HTTP channel pool
 *
 * @author : Gareth Williams
 **/
public class HttpChannelPool extends FixedChannelPool {

    private final String host;

    private final int port;

    /**
     * Constructor
     *
     * @param bootstrap the bootstrap
     * @param scheme the scheme
     * @param host the host
     * @param port the port
     * @param acquireTimeout the acquire timeout
     * @param maxConnections the maximum number of connections
     * @param maxPendingAcquires the maximum number of pending acquires
     * @param configuration the configuration
     */
    public HttpChannelPool(
            Bootstrap bootstrap,
            String scheme,
            String host,
            int port,
            Duration acquireTimeout,
            int maxConnections,
            int maxPendingAcquires,
            HttpChannelPoolConfiguration configuration) {
        super(
            bootstrap,
            new HttpChannelPoolHandler(
                    scheme,
                    host,
                    port,
                    configuration),
            new HttpChannelHealthChecker(),
            AcquireTimeoutAction.NEW,
            acquireTimeout.toMillis(),
            maxConnections,
            maxPendingAcquires,
            true,
            true);
        this.host = host;
        this.port = port;
    }

    @Override
    protected ChannelFuture connectChannel(Bootstrap bootstrap) {
        return bootstrap.remoteAddress(host, port).connect();
    }

    /**
     * The HTTP channel pool handler
     *
     */
    private static class HttpChannelPoolHandler extends AbstractChannelPoolHandler {

        private final String scheme;

        private final String host;

        private final int port;

        private final HttpChannelPoolConfiguration configuration;

        /**
         * Constructor
         *
         * @param scheme the scheme
         * @param host the host
         * @param port the port
         * @param configuration the configuration
         */
        private HttpChannelPoolHandler(
                String scheme,
                String host,
                int port,
                HttpChannelPoolConfiguration configuration) {
            this.scheme = scheme;
            this.host = host;
            this.port = port;
            this.configuration = configuration;
        }

        @Override
        public void channelCreated(Channel channel) throws Exception {
            ChannelPipeline pipeline = channel.pipeline();
            if ("https".equals(scheme)) {
                DefaultSslHandler defaultSslHandler = configuration.isEnableSni()
                    ? new DefaultSslHandler(host, port)
                    : new DefaultSslHandler();
                pipeline.addLast(DefaultSslHandler.class.getName(), defaultSslHandler);
            }
            HttpClientCodec codec = new HttpClientCodec(
                configuration.getMaxInitialLineLength(),
                configuration.getMaxHeaderSize(),
                configuration.getMaxChunkSize(),
                false,
                true,
                configuration.getInitialBufferSize());
            pipeline.addLast(HttpClientCodec.class.getName(), codec);
            pipeline.addLast(HttpObjectAggregator.class.getName(), new HttpObjectAggregator(configuration.getMaxContentLength()));
        }

        @Override
        public void channelReleased(Channel channel) throws Exception {
            ChannelPipeline pipeline = channel.pipeline();
            pipeline.remove(WriteTimeoutHandler.class.getName());
            pipeline.remove(ReadTimeoutHandler.class.getName());
            pipeline.remove(HttpRequestInboundHandler.class.getName());
            pipeline.remove(HttpRequestOutboundHandler.class.getName());
        }
    }

    /**
     * The HTTP channel health checker
     *
     */
    private static class HttpChannelHealthChecker implements ChannelHealthChecker {

        @Override
        public Future<Boolean> isHealthy(Channel channel) {

            if (!(channel.isRegistered() && channel.isActive() && channel.isOpen())) {
                return channel.eventLoop().newSucceededFuture(false);
            }

            // TODO: DNS check
            return channel.eventLoop().newSucceededFuture(true);
        }
    }
}
