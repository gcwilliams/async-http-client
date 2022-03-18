package uk.co.gcwilliams.async.http.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.gcwilliams.async.http.AsyncHttpClientListener;
import uk.co.gcwilliams.async.http.AsyncHttpRequest;
import uk.co.gcwilliams.async.http.AsyncHttpResponse;

/**
 * The logging listener
 *
 * @author : Gareth Williams
 **/
public class LoggingListener implements AsyncHttpClientListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingListener.class);

    public static final AsyncHttpClientListener INSTANCE = new LoggingListener();

    private LoggingListener() {
    }

    @Override
    public void onSend(AsyncHttpRequest request) {
        LOGGER.info("Sending {} - {}", request.getMethod(), request.getUri());
    }

    @Override
    public void onReceive(AsyncHttpRequest request, AsyncHttpResponse response) {
        LOGGER.info("Received {} - {} => {}", request.getMethod(), request.getUri(), response.getStatusCode());
    }
}
