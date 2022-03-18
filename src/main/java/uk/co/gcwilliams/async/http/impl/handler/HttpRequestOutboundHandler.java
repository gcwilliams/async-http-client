package uk.co.gcwilliams.async.http.impl.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.pool.ChannelPool;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import uk.co.gcwilliams.async.http.AsyncHttpClientListener;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * The HTTP request outbound handler
 *
 * @author : Gareth Williams
 **/
public class HttpRequestOutboundHandler extends ChannelInboundHandlerAdapter {

    private final List<AsyncHttpClientListener> listeners;

    private final Consumer<Exception> reject;

    private final Executor executor;

    private final ChannelPool channelPool;

    /**
     * Constructor
     *
     * @param listeners the listeners
     * @param reject the rejection consumer
     * @param executor the executor
     * @param channelPool the channel pool
     */
    public HttpRequestOutboundHandler(
            List<AsyncHttpClientListener> listeners,
            Consumer<Exception> reject,
            Executor executor,
            ChannelPool channelPool) {
        this.listeners = listeners;
        this.reject = reject;
        this.executor = executor;
        this.channelPool = channelPool;
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
}
