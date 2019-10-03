package io.gravitee.policy.cbr;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.unit.TestCompletion;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.TestSuite;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.function.Consumer;

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
    public void testHostHeader_shouldBeOverridden() {

        TestSuite suite = TestSuite.create("host_header");
        Buffer contentBuffer = Buffer.buffer("{ \"foo\": \"bar\" }");

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.ACCEPT, "application/json");
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
        httpHeaders.add(HttpHeaders.CONTENT_LENGTH, ""+contentBuffer.getBytes().length);
        httpHeaders.add(HttpHeaders.ORIGIN, "foobar");
        httpHeaders.add(HttpHeaders.HOST, "api.gravitee.io");

        suite.before((testContext) -> {
            Vertx vertx = testVertx(testContext, (request) -> {
                testContext.assertEquals(request.getHeader(HttpHeaders.ACCEPT), httpHeaders.get(HttpHeaders.ACCEPT).get(0));
                testContext.assertEquals(request.getHeader(HttpHeaders.CONTENT_TYPE), httpHeaders.get(HttpHeaders.CONTENT_TYPE).get(0));
                testContext.assertEquals(request.getHeader(HttpHeaders.ORIGIN), httpHeaders.get(HttpHeaders.ORIGIN).get(0));
                testContext.assertEquals(request.getHeader(HttpHeaders.HOST), "localhost");
            });

            when(executionContext.request()).thenReturn(request);
            when(request.method()).thenReturn(HttpMethod.POST);
            when(executionContext.getComponent(Vertx.class)).thenReturn(vertx);
            when(request.headers()).thenReturn(httpHeaders);
            when(endpoint.getEndpoints(any())).thenReturn(Collections.singletonList("http://localhost:16969/"));
        });

        suite.test("should_be_overridden", context -> {
            ContentBasedRoutingConnection connection = new ContentBasedRoutingConnection(executionContext, endpoint);

            connection.responseHandler((result -> {
                System.out.println(result.headers().values());
            }));
            connection.write(contentBuffer);
            connection.end();

            context.async().awaitSuccess(1000);
        });

        TestCompletion completion = suite.run();
        completion.awaitSuccess();

    }

    private Vertx testVertx(TestContext testContext, Consumer<HttpServerRequest> testFunction) {
        try {
            Vertx vertx = Vertx.vertx();
            vertx.createHttpServer()
                    .requestHandler(req -> {
                        testFunction.accept(req);
                        req.response().setStatusCode(200);
                        req.response().end();
                    })
                    .listen(16969, ar -> {
                        //testContext.async().await();
                    });


            return vertx;
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }

}
