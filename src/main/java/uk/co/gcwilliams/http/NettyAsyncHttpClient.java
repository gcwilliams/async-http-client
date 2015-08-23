package uk.co.gcwilliams.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.ReferenceCountUtil;
import uk.co.gcwilliams.http.tasks.Task;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The netty http client
 *
 * Created by GWilliams on 06/08/2015.
 */
public class NettyAsyncHttpClient implements AsyncHttpClient {

    private static final CharSequence HOST = HttpHeaders.newEntity("Host");

    private static final CharSequence ACCEPT = HttpHeaders.newEntity("Accept");

    private static final CharSequence JSON = HttpHeaders.newEntity("application/json");

    private final Bootstrap bootstrap = new Bootstrap();

    /**
     * Default constructor
     *
     * @param workers the worker
     */
    public NettyAsyncHttpClient(EventLoopGroup workers) {
        this(workers, 1000, 5, 5);
    }

    /**
     * Constructor taking timeout settings
     *
     * @param workers the workers
     * @param connectTimeoutMillis the connect timeout in milliseconds
     * @param writeTimeoutSeconds the write timeout in seconds
     * @param readTimeoutSeconds the read timeout in seconds
     */
    public NettyAsyncHttpClient(EventLoopGroup workers, int connectTimeoutMillis, int writeTimeoutSeconds, int readTimeoutSeconds) {
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis);
        bootstrap.group(workers);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ch.pipeline().addLast(new WriteTimeoutHandler(writeTimeoutSeconds));
                ch.pipeline().addLast(new ReadTimeoutHandler(readTimeoutSeconds));
                ch.pipeline().addLast(new HttpClientCodec());
                ch.pipeline().addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
            }
        });
    }

    @Override
    public Task<AsyncHttpMessage, Exception> get(String url) {
        return new Task<>((resolve, reject) -> {

            URI uri = URI.create(url);

            FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                HttpMethod.GET,
                uri.getRawPath() + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "")
            );
            request.headers().set(HOST, uri.getHost());
            request.headers().set(ACCEPT, JSON);

            ChannelFuture channelFuture = bootstrap.connect(uri.getHost(), uri.getPort() > -1 ? uri.getPort() : 80);
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    Channel channel = future.channel();
                    channel.pipeline().addLast(new HttpRequestHandler(reject));
                    if (future.isSuccess()) {
                        channel.writeAndFlush(request);
                        channel.pipeline().addLast(new HttpResponseHandler(resolve, reject));
                    } else {
                        future.channel().pipeline().fireExceptionCaught(future.cause());
                    }
                }
            });
        });
    }

    /**
     * A http response handler, that invokes the resolve or reject callback
     *
     */
    private static final class HttpResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

        private final Consumer<AsyncHttpMessage> resolve;

        private final Consumer<Exception> reject;

        /**
         * Default constructor
         *
         * @param resolve the resolve consumer
         * @param reject the reject consumer
         */
        private HttpResponseHandler(Consumer<AsyncHttpMessage> resolve, Consumer<Exception> reject) {
            this.resolve = resolve;
            this.reject = reject;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
            try {
                ByteBuf buffer = msg.content();
                byte[] contents = new byte[buffer.readableBytes()];
                msg.content().getBytes(buffer.readerIndex(), contents);
                resolve.accept(new AsyncHttpMessageImpl(
                    msg.getStatus().code(),
                    msg.headers(),
                    contents
                ));
            } finally {
                ctx.channel().closeFuture();
                ctx.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            try {
                reject.accept(cause instanceof Exception ? (Exception)cause : new RuntimeException(cause));
            } finally {
                ctx.channel().closeFuture();
                ctx.close();
                ReferenceCountUtil.release(cause);
            }
        }
    }

    /**
     * A http request handler
     *
     */
    private static final class HttpRequestHandler extends ChannelOutboundHandlerAdapter {

        private final Consumer<Exception> reject;

        /**
         * Default constructor
         *
         * @param reject the reject consumer
         */
        private HttpRequestHandler(Consumer<Exception> reject) {
            this.reject = reject;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            try {
                reject.accept(cause instanceof Exception ? (Exception) cause : new RuntimeException(cause));
            } finally {
                ctx.channel().closeFuture();
                ctx.close();
                ReferenceCountUtil.release(cause);
            }
        }
    }

    /**
     * The async http message implementation
     *
     */
    private static final class AsyncHttpMessageImpl implements AsyncHttpMessage {

        private final int statusCode;

        private final Iterable<Map.Entry<String, String>> headers;

        private final byte[] content;

        /**
         * Default constructor
         *
         * @param statusCode the status code
         * @param headers the headers
         * @param content the content
         */
        private AsyncHttpMessageImpl(int statusCode, Iterable<Map.Entry<String, String>> headers, byte[] content) {
            this.statusCode = statusCode;
            this.headers = headers;
            this.content = content;
        }

        @Override
        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public Iterable<Map.Entry<String, String>> getHeaders() {
            return headers;
        }

        @Override
        public byte[] getContent() {
            return content;
        }
    }
}
