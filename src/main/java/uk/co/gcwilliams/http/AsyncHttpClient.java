package uk.co.gcwilliams.http;

import uk.co.gcwilliams.http.tasks.Task;

import java.util.Map;

/**
 * The async http client interface
 *
 * Created by GWilliams on 06/08/2015.
 */
@FunctionalInterface
public interface AsyncHttpClient {

    /**
     * Gets the specified URL
     *
     * @return the response body
     */
    Task<AsyncHttpMessage> get(String url);
}
