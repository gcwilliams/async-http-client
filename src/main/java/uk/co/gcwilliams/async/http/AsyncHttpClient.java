package uk.co.gcwilliams.async.http;

/**
 * The async HTTP client
 *
 * @author : Gareth Williams
 **/
public interface AsyncHttpClient extends AutoCloseable {

    /**
     * Prepares the request
     *
     * @param request the request
     * @return the task to get the response
     */
    Task<AsyncHttpResponse> prepare(AsyncHttpRequest request);
}
