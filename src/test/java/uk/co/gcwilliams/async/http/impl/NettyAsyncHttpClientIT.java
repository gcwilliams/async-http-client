package uk.co.gcwilliams.async.http.impl;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.co.gcwilliams.async.http.AsyncHttpClient;
import uk.co.gcwilliams.async.http.AsyncHttpRequest;
import uk.co.gcwilliams.async.http.AsyncHttpResponse;
import uk.co.gcwilliams.async.http.Tasks;
import uk.co.gcwilliams.async.http.listeners.LoggingListener;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;

/**
 * The netty async HTTP client integration tests
 *
 * @author : Gareth Williams
 **/
class NettyAsyncHttpClientIT {

    static Stream<Arguments> urls() {
        return Stream.of(
            Arguments.of("https://www.google.co.uk"),
            Arguments.of("https://www.bbc.co.uk"),
            Arguments.of("https://github.com"),
            Arguments.of("https://www.virginholidays.co.uk")
        );
    }

    @ParameterizedTest @MethodSource("urls") void request(String url) throws Exception {

        try (AsyncHttpClient http = NettyAsyncHttpClient
                .builder()
                .withListenerFactory(() -> List.of(LoggingListener.INSTANCE))
                .build()) {

            // arrange
            AsyncHttpRequest request = AsyncHttpRequest
                .get(URI.create(url))
                .withWriteTimeout(Duration.ofSeconds(30))
                .withReadTimeout(Duration.ofSeconds(60))
                .build();

            // act
            AsyncHttpResponse response = Tasks.get(http.prepare(request), Duration.ofMinutes(1));

            // assert
            assertThat(response.getStatusCode(), equalTo(200));
            assertThat(response.getHeaders(), aMapWithSize(greaterThan(0)));
            assertThat(response.getBody(), instanceOf(InputStream.class));
            assertThat(response.getBody().readAllBytes().length, greaterThan(0));
        }
    }
}
