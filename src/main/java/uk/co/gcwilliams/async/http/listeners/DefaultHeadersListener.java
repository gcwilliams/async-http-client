package uk.co.gcwilliams.async.http.listeners;

import uk.co.gcwilliams.async.http.AsyncHttpClientListener;
import uk.co.gcwilliams.async.http.AsyncHttpRequest;

import java.util.List;
import java.util.Map;

import static java.util.function.Predicate.not;

/**
 * The default headers listener, adds headers if they do not exist in the request
 *
 * @author : Gareth Williams
 **/
public class DefaultHeadersListener implements AsyncHttpClientListener {

    private final Map<String, List<String>> defaultHeaders;

    /**
     * Constructor
     *
     * @param name the name
     * @param value the value
     */
    public DefaultHeadersListener(String name, String value) {
        this(name, List.of(value));
    }

    /**
     * Constructor
     *
     * @param name1 the name
     * @param value1 the value
     * @param name2 the name
     * @param value2 the value
     */
    public DefaultHeadersListener(String name1, String value1, String name2, String value2) {
        this(Map.of(name1, List.of(value1), name2, List.of(value2)));
    }

    /**
     * Constructor
     *
     * @param name1 the name
     * @param value1 the value
     * @param name2 the name
     * @param value2 the value
     * @param name3 the name
     * @param value3 the value
     */
    public DefaultHeadersListener(String name1, String value1, String name2, String value2, String name3, String value3) {
        this(Map.of(name1, List.of(value1), name2, List.of(value2), name3, List.of(value3)));
    }

    /**
     * Constructor
     *
     * @param name the name
     * @param values the values
     */
    public DefaultHeadersListener(String name, List<String> values) {
        this(Map.of(name, values));
    }

    /**
     * Constructor
     *
     * @param name1 the name
     * @param values1 the values
     * @param name2 the name
     * @param values2 the values
     */
    public DefaultHeadersListener(String name1, List<String> values1, String name2, List<String> values2) {
        this(Map.of(name1, values1, name2, values2));
    }

    /**
     * Constructor
     *
     * @param name1 the name
     * @param values1 the values
     * @param name2 the name
     * @param values2 the values
     * @param name3 the name
     * @param values3 the values
     */
    public DefaultHeadersListener(String name1, List<String> values1, String name2, List<String> values2, String name3, List<String> values3) {
        this(Map.of(name1, values1, name2, values2, name3, values3));
    }

    /**
     * Constructor
     *
     * @param defaultHeaders the default headers
     */
    public DefaultHeadersListener(Map<String, List<String>> defaultHeaders) {
        this.defaultHeaders = defaultHeaders;
    }

    @Override
    public AsyncHttpRequest onModifyRequest(AsyncHttpRequest request) {
        AsyncHttpRequest.Builder builder = AsyncHttpRequest.builder(request);
        defaultHeaders.entrySet()
            .stream()
            .filter(not(header -> request.getHeaders().containsKey(header.getKey())))
            .forEach(header -> builder.withHeader(header.getKey(), header.getValue()));
        return builder.build();
    }
}
