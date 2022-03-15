package uk.co.gcwilliams.async.http;

import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.Map;

/**
 * The async HTTP response
 *
 * @author : Gareth Williams
 **/
public class AsyncHttpResponse {

    private final int statusCode;

    private final Map<String, List<String>> headers;

    private AsyncHttpResponse(int statusCode, Map<String, List<String>> headers) {
        this.statusCode = statusCode;
        this.headers = headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, List<String>> getHeaders() {


        ByteBuf buf;



        return headers;
    }
}
