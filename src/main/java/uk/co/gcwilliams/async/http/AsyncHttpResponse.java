package uk.co.gcwilliams.async.http;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * The async HTTP response
 *
 * @author : Gareth Williams
 **/
public class AsyncHttpResponse {

    private final int statusCode;

    private final Map<String, List<String>> headers;

    private final InputStream body;

    /**
     * Constructor
     *
     * @param statusCode the status code
     * @param headers the headers
     * @param body the body
     */
    private AsyncHttpResponse(int statusCode, Map<String, List<String>> headers, InputStream body) {
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }

    /**
     * Gets the status code
     *
     * @return the status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Gets the headers
     *
     * @return the headers
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Gets the body
     *
     * @return the body
     */
    public InputStream getBody() {
        return body;
    }

    /**
     * Creates the builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The builder
     *
     */
    public static class Builder {

        private Integer statusCode;

        private Map<String, List<String>> headers;

        private InputStream body;

        private Builder() {
        }

        /**
         * Sets the status code
         *
         * @param statusCode the status code
         * @return the builder
         */
        public Builder withStatusCode(Integer statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        /**
         * Sets the headers
         *
         * @param headers the headers
         * @return the builder
         */
        public Builder withHeaders(Map<String, List<String>> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Sets the body
         *
         * @param body the body
         * @return the builder
         */
        public Builder withBody(InputStream body) {
            this.body = body;
            return this;
        }

        /**
         * Builds the response
         *
         * @return the response
         */
        public AsyncHttpResponse build() {
            requireNonNull(statusCode, "no status code set");
            requireNonNull(headers, "no headers set");
            requireNonNull(body, "no body set");
            return new AsyncHttpResponse(statusCode, headers, body);
        }
    }
}
