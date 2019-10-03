package io.gravitee.policy.cbr;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ContentBasedRoutingConnectionTest {

    private static final Logger log = LoggerFactory.getLogger(ContentBasedRoutingConnectionTest.class);

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private ContentBasedRoutingPolicyConfiguration configuration;

    @Mock
    private ContentBasedRoutingEndpoint endpoint;

    @Mock
    private Request request;

    @Mock
    private Handler<ProxyResponse> responseHandler;

    @Test
    public void testHostHeader_shouldNotBeOverridden() throws InterruptedException, Throwable {

        when(executionContext.request()).thenReturn(request);
        when(request.method()).thenReturn(HttpMethod.POST);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.ACCEPT, "application/json");
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
        httpHeaders.add(HttpHeaders.ORIGIN, "foobar");
        httpHeaders.add(HttpHeaders.HOST, "api.gravitee.io");

        VertxTestContext testContext = new VertxTestContext();
        when(executionContext.getComponent(Vertx.class)).thenReturn(testVertx(testContext, (request) -> {
            assertThat(request.getHeader(HttpHeaders.ACCEPT)).isEqualTo(httpHeaders.get(HttpHeaders.ACCEPT));
            assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(httpHeaders.get(HttpHeaders.CONTENT_TYPE));
            assertThat(request.getHeader(HttpHeaders.ORIGIN)).isEqualTo(httpHeaders.get(HttpHeaders.ORIGIN));
            assertThat(request.getHeader(HttpHeaders.HOST)).isEqualTo("localhost");
        }));

        when(request.headers()).thenReturn(httpHeaders);
        when(endpoint.getEndpoints(any())).thenReturn(Collections.singletonList("http://localhost:16969/"));
        ContentBasedRoutingConnection connection = new ContentBasedRoutingConnection(executionContext, endpoint);
        connection.responseHandler(responseHandler);


        connection.write(Buffer.buffer("{ \"foo\": \"bar\" }"));
        connection.end();




    }

    private Vertx testVertx(VertxTestContext testContext, Consumer<HttpServerRequest> testFunction) throws InterruptedException, Throwable {
        Vertx vertx = Vertx.vertx();
        HttpServer server = vertx.createHttpServer()
                .requestHandler(req -> {
                    testFunction.accept(req);
                    req.response().end();
                    testContext.completeNow();
                })
                .listen(16969, testContext.completing());
        assertThat(testContext.awaitCompletion(100, TimeUnit.SECONDS)).isTrue();

        if (testContext.failed()) {
            throw testContext.causeOfFailure();
        } else {
            log.info("Service started at port {}", server.actualPort());
        }

        return vertx;
    }

}
