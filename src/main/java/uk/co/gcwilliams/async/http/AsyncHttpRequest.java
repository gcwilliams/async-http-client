package uk.co.gcwilliams.async.http;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * The async HTTP client request
 *
 * @author : Gareth Williams
 */
public class AsyncHttpRequest {

    private final HttpMethod method;

    private final URI uri;

    private final Map<String, List<String>> headers;

    private final byte[] body;

    /**
     * Constructor
     *
     * @param method the method
     * @param uri the URI
     * @param headers the headers
     * @param body the body
     */
    private AsyncHttpRequest(
            HttpMethod method,
            URI uri,
            Map<String, List<String>> headers,
            byte[] body) {
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.body = body;
    }

    /**
     * Gets the HTTP method
     *
     * @return the HTTP method
     */
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * Gets the URI
     *
     * @return the URI
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Gets the HTTP headers
     *
     * @return the HTTP headers
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Gets the body
     *
     * @return the body
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * The HTTP method
     *
     */
    public enum HttpMethod {

        GET,

        POST,

        PUT,

        DELETE,

        HEAD,

        PATCH,

        OPTIONS
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
     * Creates a builder with for a GET
     *
     * @return the builder
     */
    public static Builder get() {
        return new Builder().withMethod(HttpMethod.GET);
    }

    /**
     * Creates a builder with for a GET
     *
     * @param uri the URI
     * @return the builder
     */
    public static Builder get(URI uri) {
        return new Builder().withMethod(HttpMethod.GET).withURI(uri);
    }

    /**
     * Creates a builder with for a POST
     *
     * @return the builder
     */
    public static Builder post() {
        return new Builder().withMethod(HttpMethod.POST);
    }

    /**
     * Creates a builder with for a POST
     *
     * @param uri the URI
     * @return the builder
     */
    public static Builder post(URI uri) {
        return new Builder().withMethod(HttpMethod.POST).withURI(uri);
    }

    /**
     * Creates a builder with for a PUT
     *
     * @return the builder
     */
    public static Builder put() {
        return new Builder().withMethod(HttpMethod.PUT);
    }

    /**
     * Creates a builder with for a PUT
     *
     * @param uri the URI
     * @return the builder
     */
    public static Builder put(URI uri) {
        return new Builder().withMethod(HttpMethod.PUT).withURI(uri);
    }

    /**
     * Creates a builder with for a DELETE
     *
     * @return the builder
     */
    public static Builder delete() {
        return new Builder().withMethod(HttpMethod.DELETE);
    }

    /**
     * Creates a builder with for a DELETE
     *
     * @param uri the URI
     * @return the builder
     */
    public static Builder delete(URI uri) {
        return new Builder().withMethod(HttpMethod.DELETE).withURI(uri);
    }

    /**
     * The async HTTP request builder
     *
     */
    public static class Builder {

        private HttpMethod method;

        private URI uri;

        private Map<String, List<String>> headers = new HashMap<>();

        private byte[] body;

        private Builder() {
        }

        /**
         * Sets the method
         *
         * @param method the method
         * @return the builder
         */
        public Builder withMethod(HttpMethod method) {
            this.method = method;
            return this;
        }

        /**
         * Sets the URI
         *
         * @param uri the URI
         * @return the builder
         */
        public Builder withURI(URI uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Sets the headers
         *
         * @param headers the headers
         * @return the builder
         */
        public Builder withHeaders(Map<String, List<String>> headers) {
            this.headers = headers != null ? headers : new HashMap<>();
            return this;
        }

        /**
         * Sets the header
         *
         * @param header the header name
         * @param value the value
         * @param values the additional values
         * @return the builder
         */
        public Builder withHeader(String header, String value, String... values) {
            this.headers.put(header, Stream.concat(Stream.of(value), stream(values)).collect(toList()));
            return this;
        }

        /**
         * Sets the body
         *
         * @param body the body
         * @return the builder
         */
        public Builder withBody(byte[] body) {
            this.body = body;
            return this;
        }

        /**
         * Sets the body
         *
         * @param body the body
         * @return the builder
         */
        public Builder withBody(String body) {
            return withBody(body, StandardCharsets.UTF_8);
        }

        /**
         * Sets the body
         *
         * @param body the body
         * @param charset the charset
         * @return the builder
         */
        public Builder withBody(String body, Charset charset) {
            this.body = body.getBytes(charset);
            return this;
        }

        /**
         * Builds the request
         *
         * @return the request
         */
        public AsyncHttpRequest build() {
            requireNonNull(method, "The HTTP method should not be null");
            requireNonNull(uri, "The URI should not be null");
            if (!uri.isAbsolute() || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
                throw new IllegalStateException("The URI should be absolute and should be either HTTP or HTTPS");
            }
            requireNonNull(headers, "The HTTP headers should not be null");
            if (method == HttpMethod.GET || method == HttpMethod.HEAD || method == HttpMethod.OPTIONS) {
                throw new IllegalStateException("GET, HEAD, or OPTIONS should not have request bodies");
            }
            return new AsyncHttpRequest(method, uri, headers, body);
        }
    }
}
