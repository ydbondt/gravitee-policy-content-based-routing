/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.policy.cbr;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.stream.WriteStream;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Map;

public class ContentBasedRoutingConnection implements ProxyConnection {
    private static final Logger logger = LoggerFactory.getLogger(ContentBasedRoutingConnection.class);
    public static final String APPLICATION_JSON = "application/json";

    private Handler<ProxyResponse> responseHandler;
    private Buffer content;
    private final ExecutionContext context;
    private final ContentBasedRoutingEndpoint endpoint;

    private final Request originalRequest;
    private final Vertx vertx;

    public ContentBasedRoutingConnection(ExecutionContext executionContext, ContentBasedRoutingEndpoint endpoint) {
        this.endpoint = endpoint;
        this.context = executionContext;
        this.originalRequest = executionContext.request();

        this.vertx = executionContext.getComponent(Vertx.class);
    }

    @Override
    public WriteStream<Buffer> write(Buffer chunk) {
        if (content == null) {
            content = Buffer.buffer();
        }
        content.appendBuffer(chunk);
        return this;
    }

    @Override
    public void end() {

        HttpMethod httpMethod = HttpMethod.valueOf(originalRequest.method().name());
        Map<String, String> originalHeaders = originalRequest.headers().toSingleValueMap();
        String messageBody = getMessageBody();

        logger.debug("Message body: " + messageBody + ", method: " + httpMethod);

        if (!StringUtils.isEmpty(messageBody) && isJsonCall()) {
            endpoint.getEndpoints(messageBody)
                    .forEach(endpoint -> callUrl(endpoint, messageBody, httpMethod, originalHeaders));
        }

        responseHandler.handle(new SuccessResponse());
    }

    private String getMessageBody() {
        if (content != null) {
            return content.toString();
        }
        return "";
    }

    private boolean isJsonCall() {
        return originalRequest.headers() != null
                && !originalRequest.headers().isEmpty()
                && originalRequest.headers().contentType() != null
                && originalRequest.headers().contentType().toLowerCase().equals(APPLICATION_JSON);
    }

    private void callUrl(String url, String messageBody, HttpMethod httpMethod, Map<String, String> originalHeaders) {
        logger.debug("Target URL: " + url);
        HttpClientOptions options = new HttpClientOptions().setConnectTimeout(2000);
        HttpClient httpClient = vertx.createHttpClient();

        HttpClientRequest clientRequest = httpClient.requestAbs(httpMethod, url, done -> {
            logger.debug("Response code: " + done.statusCode() + ", statusMessage: " + done.statusMessage() );
            done.bodyHandler(buffer -> {
                logger.debug("Body: " + buffer.getString(0, buffer.length()));
                httpClient.close();
            });
        });
        clientRequest.setTimeout(2000);

        MultiMap headers = clientRequest.headers();
        headers.setAll(originalHeaders);

        clientRequest.connectionHandler(connection -> {
            connection.exceptionHandler(ex -> {
                logger.error("Connection exception ", ex);
                httpClient.close();
            });
        });

        clientRequest.exceptionHandler(event -> {
            logger.error("Server exception", event);
            httpClient.close();
        });

        if (!messageBody.isEmpty() && (originalRequest.method().equals(io.gravitee.common.http.HttpMethod.POST)
                || originalRequest.method().equals(io.gravitee.common.http.HttpMethod.PUT)
                || originalRequest.method().equals(io.gravitee.common.http.HttpMethod.PATCH))) {
            clientRequest.write(messageBody);
        }

        clientRequest.end();

    }

    @Override
    public ProxyConnection responseHandler(Handler<ProxyResponse> responseHandler) {
        this.responseHandler = responseHandler;
        return this;
    }

}
