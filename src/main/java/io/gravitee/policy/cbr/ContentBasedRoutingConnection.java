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

import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
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
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ContentBasedRoutingConnection implements ProxyConnection {
    private static final Logger logger = LoggerFactory.getLogger(ContentBasedRoutingConnection.class);
    public static final String APPLICATION_JSON = "application/json";

    private Handler<ProxyResponse> responseHandler;
    private Buffer content;
    private final ContentBasedRoutingPolicyConfiguration configuration;
    private final ExecutionContext context;

    private final HttpClient httpClient;
    private final Request originalRequest;

    public ContentBasedRoutingConnection(ExecutionContext executionContext, ContentBasedRoutingPolicyConfiguration configuration) {
        this.configuration = configuration;
        this.context = executionContext;

        originalRequest = executionContext.request();
        Vertx vertx = executionContext.getComponent(Vertx.class);
        this.httpClient = vertx.createHttpClient();
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
        String messageBody = (content != null) ? content.toString() : "";
        HttpMethod httpMethod = HttpMethod.valueOf(originalRequest.method().name());
        logger.debug("Message body: " + messageBody + ", method: " + httpMethod);
        try {
            boolean hasJsonContentTypeHeader = false;

            if (originalRequest.headers() != null && !originalRequest.headers().isEmpty()) {
                String ctHeader = originalRequest.headers().contentType();
                hasJsonContentTypeHeader = ctHeader != null && ctHeader.toLowerCase().equals(APPLICATION_JSON);
            }

            List<String> urls = null;

            if (hasJsonContentTypeHeader) {
                String extractResult = JsonPath.parse(messageBody).read(configuration.getJsonpathExpression(), String.class);
                logger.info("Extract result: {}", extractResult);
                Map<String, List<String>> routingTable = new Gson().fromJson(configuration.getRoutingTable(), Map.class);

                if (routingTable.containsKey(extractResult)) {
                    urls = routingTable.get(extractResult);
                } else {
                    logger.info("No Route found for {}", extractResult);
                    urls = Collections.singletonList(configuration.getFallbackUrl());
                }
            }

            if (urls != null) {
                final Map<String, String> originalHeaders = originalRequest.headers().toSingleValueMap();
                urls.forEach(url -> callUrl(url, messageBody, httpMethod, originalHeaders));
            }

        } catch (Exception e) {
            logger.error("General exception: ", e);
        }

        responseHandler.handle(new SuccessResponse());
    }

    private void callUrl(String url, String messageBody, HttpMethod httpMethod, Map<String, String> originalHeaders) {
        logger.info("Target URL: " + url);
        try {
            URL urlObject = new URL(url);

            HttpClientRequest clientRequest = httpClient.requestAbs(httpMethod, url, done -> {
                logger.info("Response code: " + done.statusCode() + ", statusMessage: " + done.statusMessage() );
                done.bodyHandler(buffer -> {
                    logger.debug("Body: " + buffer.getString(0, buffer.length()));
                });
            });

            MultiMap headers = clientRequest.headers();
            headers.setAll(originalHeaders);

            clientRequest.connectionHandler(connection -> {
                connection.exceptionHandler(ex -> {
                    logger.error("Connection exception ", ex);
                });
            });

            clientRequest.exceptionHandler(event -> {
                logger.error("Server exception", event);
            });

            if (!messageBody.isEmpty() && (originalRequest.method().equals(io.gravitee.common.http.HttpMethod.POST)
                    || originalRequest.method().equals(io.gravitee.common.http.HttpMethod.PUT)
                    || originalRequest.method().equals(io.gravitee.common.http.HttpMethod.PATCH))) {
                clientRequest.write(messageBody);
            }
            clientRequest.end();
        } catch (MalformedURLException e) {
            logger.debug("Invalid URL: " + url);
        }
    }

    @Override
    public ProxyConnection responseHandler(Handler<ProxyResponse> responseHandler) {
        this.responseHandler = responseHandler;
        return this;
    }

}
