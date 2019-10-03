package io.gravitee.policy.cbr;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.stream.ReadStream;
import io.vertx.core.http.HttpClientResponse;

import java.util.Collections;

public class ContentBasedRoutingResponse implements ProxyResponse {

    private final HttpClientResponse httpClientResponse;

    public ContentBasedRoutingResponse(HttpClientResponse httpClientResponse) {
        this.httpClientResponse = httpClientResponse;
    }

    @Override
    public int status() {
        return httpClientResponse.statusCode();
    }

    @Override
    public HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        httpClientResponse.headers().entries().forEach(entry -> headers.put(entry.getKey(), Collections.singletonList(entry.getValue())));
        return headers;
    }

    @Override
    public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
        return this;
    }

    @Override
    public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
        return this;
    }
}
