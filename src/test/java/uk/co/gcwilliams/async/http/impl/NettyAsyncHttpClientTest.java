package uk.co.gcwilliams.async.http.impl;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.co.gcwilliams.async.http.AsyncHttpClient;
import uk.co.gcwilliams.async.http.AsyncHttpClientListener;
import uk.co.gcwilliams.async.http.AsyncHttpRequest;
import uk.co.gcwilliams.async.http.AsyncHttpResponse;
import uk.co.gcwilliams.async.http.Task;
import uk.co.gcwilliams.async.http.Tasks;
import uk.co.gcwilliams.async.http.listeners.DefaultHeadersListener;
import uk.co.gcwilliams.async.http.listeners.LoggingListener;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.request;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.newRequestPattern;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static uk.co.gcwilliams.async.http.AsyncHttpRequest.HttpMethod.GET;
import static uk.co.gcwilliams.async.http.AsyncHttpRequest.HttpMethod.HEAD;
import static uk.co.gcwilliams.async.http.AsyncHttpRequest.HttpMethod.OPTIONS;

/**
 * The netty async HTTP client tests
 *
 * @author : Gareth Williams
 **/
@WireMockTest
class NettyAsyncHttpClientTest {

    private static AsyncHttpClient HTTP;

    @BeforeAll static void beforeEach() {
        AsyncHttpClientListener userAgentListener = new DefaultHeadersListener(
            "User-Agent",
            "Async-Http-Client");
        HTTP = NettyAsyncHttpClient.builder()
            .withConnectTimeout(Duration.ofSeconds(20))
            .withMaxConnections(100)
            .withMaxPendingAcquires(Integer.MAX_VALUE)
            .withAcquireTimeout(Duration.ofMinutes(2))
            .withListenerFactory(() -> List.of(userAgentListener))
            .build();
    }

    @AfterAll static void afterAll() throws Exception {
        HTTP.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "DELETE", "HEAD", "PATCH", "OPTIONS"})
    void methods(String method, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        // arrange
        stubFor(request(method, urlEqualTo("/")).willReturn(ok().withBody(randomBytes(200))));

        AsyncHttpRequest.HttpMethod httpMethod = AsyncHttpRequest.HttpMethod.valueOf(method);

        boolean hasRequestBody = !EnumSet.of(GET, HEAD, OPTIONS).contains(httpMethod);
        byte[] requestBody = hasRequestBody ? randomBytes(100) : new byte[0];

        AsyncHttpRequest request = AsyncHttpRequest
            .builder()
            .withMethod(httpMethod)
            .withURI(URI.create(format("http://localhost:%s", wmRuntimeInfo.getHttpPort())))
            .withHeader("Content-Type", "application/octet-stream")
            .withBody(requestBody)
            .withListener(LoggingListener.INSTANCE)
            .build();

        // act
        AsyncHttpResponse response = Tasks.get(HTTP.prepare(request), Duration.ofMinutes(1));

        // assert
        assertThat(response.getStatusCode(), equalTo(200));
        assertThat(response.getHeaders(), aMapWithSize(greaterThan(0)));
        assertThat(response.getBody(), instanceOf(InputStream.class));
        assertThat(response.getBody().readAllBytes().length, equalTo(httpMethod == HEAD ? 0 : 200));

        RequestPatternBuilder patternBuilder = newRequestPattern(RequestMethod.fromString(method), urlEqualTo("/"))
            .withHeader("Host", WireMock.equalTo("localhost"))
            .withHeader("Content-Length", WireMock.equalTo(String.valueOf(requestBody.length)))
            .withHeader("User-Agent", WireMock.equalTo("Async-Http-Client"));
        if (hasRequestBody) {
            patternBuilder.withRequestBody(WireMock.equalTo(new String(requestBody, StandardCharsets.UTF_8)));
        }
        verify(patternBuilder);
    }

    @Test void concurrent(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        // arrange
        stubFor(get("/").willReturn(ok().withBody(randomBytes())));

        AsyncHttpRequest request = AsyncHttpRequest
            .get(URI.create(format("http://localhost:%s", wmRuntimeInfo.getHttpPort())))
            .withWriteTimeout(Duration.ofSeconds(30))
            .withReadTimeout(Duration.ofSeconds(60))
            .build();

        // act
        List<Task<AsyncHttpResponse>> tasks = range(0, 250000)
            .mapToObj(__ -> HTTP.prepare(request))
            .collect(toList());
        List<AsyncHttpResponse> responses = Tasks.get(Tasks.traverseP(tasks), Duration.ofMinutes(2));

        // assert
        assertThat(responses.size(), equalTo(250000));
        for (AsyncHttpResponse response : responses) {
            assertThat(response.getStatusCode(), equalTo(200));
            assertThat(response.getHeaders(), aMapWithSize(greaterThan(0)));
            assertThat(response.getBody(), instanceOf(InputStream.class));
            assertThat(response.getBody().readAllBytes().length, greaterThan(1));
        }
    }

    /**
     * Gets a random number of random bytes
     *
     * @return the bytes
     */
    private static byte[] randomBytes() {
        return randomBytes(ThreadLocalRandom.current().nextInt(4999) + 1);
    }

    /**
     * Gets the specified number of random bytes
     *
     * @param size the size
     * @return the bytes
     */
    private static byte[] randomBytes(int size) {
        byte[] randomBytes = new byte[size];
        ThreadLocalRandom.current().nextBytes(randomBytes);
        return randomBytes;
    }
}
