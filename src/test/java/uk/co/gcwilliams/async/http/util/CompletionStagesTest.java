package uk.co.gcwilliams.async.http.util;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import uk.co.gcwilliams.async.http.AsyncHttpClient;
import uk.co.gcwilliams.async.http.AsyncHttpRequest;
import uk.co.gcwilliams.async.http.AsyncHttpResponse;
import uk.co.gcwilliams.async.http.impl.NettyAsyncHttpClient;

import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;

/**
 * The completion stages tests
 *
 * @author : Gareth Williams
 **/
@WireMockTest
class CompletionStagesTest {

    @Test
    void toCompletionStage(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        try (AsyncHttpClient http = NettyAsyncHttpClient.builder().build()) {

            // arrange
            stubFor(get(urlEqualTo("/")).willReturn(ok()));

            AsyncHttpRequest request = AsyncHttpRequest.get(URI.create(format("http://localhost:%s", wmRuntimeInfo.getHttpPort()))).build();

            // act
            CompletionStage<AsyncHttpResponse> completionStage = CompletionStages.toCompletionStage(http.prepare(request));
            AsyncHttpResponse response = completionStage.toCompletableFuture().get(30, TimeUnit.SECONDS);

            // assert
            assertThat(response.getStatusCode(), equalTo(200));
            assertThat(response.getHeaders(), aMapWithSize(greaterThan(0)));
            assertThat(response.getBody(), instanceOf(InputStream.class));

            verify(getRequestedFor(urlEqualTo("/")));
        }
    }
}
