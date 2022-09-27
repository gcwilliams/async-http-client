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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.gcwilliams.async.http.impl.handler.DefaultSslHandler;
import uk.co.gcwilliams.async.http.impl.handler.HttpRequestInboundHandler;
import uk.co.gcwilliams.async.http.impl.handler.HttpRequestOutboundHandler;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
            new HttpChannelHealthChecker(host),
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

        private static final Logger LOGGER = LoggerFactory.getLogger(HttpChannelHealthChecker.class);

        private final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);

        private final String host;

        private volatile Set<String> allByName = new HashSet<>();

        private HttpChannelHealthChecker(String host) {
            this.host = host;
            schedule();
        }

        @Override
        public Future<Boolean> isHealthy(Channel channel) {
            if (!(channel.isRegistered() && channel.isActive() && channel.isOpen())) {
                return channel.eventLoop().newSucceededFuture(false);
            }
            if (!(channel.remoteAddress() instanceof InetSocketAddress)) {
                return channel.eventLoop().newSucceededFuture(true);
            }
            InetSocketAddress address = (InetSocketAddress) channel.remoteAddress();
            return channel.eventLoop().newSucceededFuture(allByName.contains(address.getAddress().getHostAddress()));
        }

        private void schedule() {
            allByName = Arrays.stream(getAllByName(host))
                .map(InetAddress::getHostAddress)
                .collect(Collectors.toUnmodifiableSet());
            EXECUTOR.schedule(this::schedule, 60, TimeUnit.SECONDS);
        }

        private static InetAddress[] getAllByName(String host) {
            try {
                return InetAddress.getAllByName(host);
            } catch (UnknownHostException ex) {
                LOGGER.warn("Unable to get InetAddresses for {}", host, ex);
                return new InetAddress[0];
            }
        }
    }
}
