package uk.co.gcwilliams.async.http;

import java.util.function.Consumer;

/**
 * The async HTTP client listener. Listeners should not throw exceptions when lifecycle
 * methods are being invoked.
 *
 * @author : Gareth Williams
 **/
public interface AsyncHttpClientListener {

    /**
     * Called when {@link Task#fork(Consumer, Consumer)} is invoked,
     * ideal for handling {@link ThreadLocal} variables as this is invoked on the
     * {@link Thread} that forked the task
     *
     */
    default void onPrepare() { }

    /**
     * Called before to allow the request to be modified, each listener will be called
     * in order, this would allow for example, a header to be set on every request
     *
     * @param request the request
     */
    default AsyncHttpRequest onModifyRequest(AsyncHttpRequest request) {
        return request;
    }

    /**
     * Called before a request is sent, ideal for logging requests
     *
     * @param request the request
     */
    default void onSend(AsyncHttpRequest request) { }

    /**
     * Called once the response is received, ideal for handling {@link ThreadLocal} variables
     * as this is invoked on the {@link Thread} that will resolve the {@link Task}
     *
     */
    default void onReceive() { }

    /**
     * Called once the response is received, ideal for logging
     *
     * @param request the request
     * @param response the response
     */
    default void onReceive(AsyncHttpRequest request, AsyncHttpResponse response) { }

    /**
     * Called after completion
     *
     */
    default void onComplete() { }

    /**
     * Called if an exception is raised during request processing
     *
     * @param exception the exception
     */
    default void onException(Exception exception) { }
}
