package uk.co.gcwilliams.async.http;

import java.util.List;

/**
 * The async HTTP client listener factory
 *
 * @author : Gareth Williams
 **/
@FunctionalInterface
public interface AsyncHttpClientListenerFactory {

    /**
     * Creates the listeners for a request, for any listeners which have mutable state,
     * a new instance should be provided on each invocation of this method. Others, such
     * as {@link  uk.co.gcwilliams.async.http.listeners.LoggingListener} can be the same
     * instance for each invocation
     *
     * @return the listeners
     */
    List<AsyncHttpClientListener> createListeners();
}
