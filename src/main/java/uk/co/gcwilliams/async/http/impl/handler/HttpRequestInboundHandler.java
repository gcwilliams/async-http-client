package uk.co.gcwilliams.async.http.impl.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import uk.co.gcwilliams.async.http.AsyncHttpClientListener;
import uk.co.gcwilliams.async.http.AsyncHttpRequest;
import uk.co.gcwilliams.async.http.AsyncHttpResponse;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * The HTTP request inbound handler
 *
 * @author : Gareth Williams
 **/
public class HttpRequestInboundHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final List<AsyncHttpClientListener> listeners;

    private final AsyncHttpRequest request;

    private final Consumer<AsyncHttpResponse> resolve;

    private final Consumer<Exception> reject;

    private final Executor executor;

    private final ChannelPool channelPool;

    /**
     * Constructor
     *
     * @param listeners the listeners
     * @param request the request
     * @param resolve the resolve consumer
     * @param reject the reject consumer
     * @param executor the executor
     * @param channelPool the channel pool
     */
    public HttpRequestInboundHandler(
            List<AsyncHttpClientListener> listeners,
            AsyncHttpRequest request,
            Consumer<AsyncHttpResponse> resolve,
            Consumer<Exception> reject,
            Executor executor,
            ChannelPool channelPool) {
        this.listeners = listeners;
        this.request = request;
        this.resolve = resolve;
        this.reject = reject;
        this.executor = executor;
        this.channelPool = channelPool;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        AsyncHttpResponse response = AsyncHttpResponse.builder()
            .withStatusCode(msg.status().code())
            .withHeaders(msg.headers()
                .names()
                .stream()
                .collect(toMap(Function.identity(), name -> msg.headers().getAll(name))))
            .withBody(new ByteArrayInputStream(readBody(msg.content()))) // TODO: should only read if requested, otherwise drop bytes, or close channel and open new
            .build();

        executor.execute(() -> {
            listeners.forEach(AsyncHttpClientListener::onReceive);
            listeners.forEach(listener -> listener.onReceive(request, response));
            resolve.accept(response);
            listeners.forEach(AsyncHttpClientListener::onComplete);
        });

        channelPool.release(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Exception exception = cause instanceof Exception ? (Exception) cause : new Exception(cause);
        executor.execute(() -> {
            listeners.forEach(AsyncHttpClientListener::onReceive);
            listeners.forEach(listener -> listener.onException(exception));
            reject.accept(exception);
        });
        channelPool.release(ctx.channel());
        ctx.fireExceptionCaught(cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof SslHandshakeCompletionEvent) {
            SslHandshakeCompletionEvent sslHandshakeCompletionEvent = (SslHandshakeCompletionEvent) evt;
            if (!sslHandshakeCompletionEvent.isSuccess()) {
                Throwable cause = sslHandshakeCompletionEvent.cause();
                Exception exception = cause instanceof Exception ? (Exception) cause : new Exception(cause);
                executor.execute(() -> {
                    listeners.forEach(AsyncHttpClientListener::onReceive);
                    listeners.forEach(listener -> listener.onException(exception));
                    reject.accept(exception);
                });
                channelPool.release(ctx.channel());
            }
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    /**
     * Reads the body
     *
     * @param buffer the buffer
     * @return the body
     */
    private static byte[] readBody(ByteBuf buffer) {
        if (buffer.hasArray()) {
            return buffer.array();
        }
        byte[] contents = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), contents);
        return contents;
    }
}
