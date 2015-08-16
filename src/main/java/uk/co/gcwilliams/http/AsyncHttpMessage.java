package uk.co.gcwilliams.http;

import java.util.Map;

/**
 * The async http message
 *
 */
public interface AsyncHttpMessage {

    /**
     * Gets the status code
     *
     * @return the status code
     */
    int getStatusCode();

    /**
     * Gets the headers of the message
     *
     * @return the headers
     */
    Iterable<Map.Entry<String, String>> getHeaders();

    /**
     * Gets the content of the message
     *
     * @return the content
     */
    byte[] getContent();
}
