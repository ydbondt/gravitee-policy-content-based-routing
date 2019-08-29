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
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.http.stream.TransformableRequestStreamBuilder;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.exception.TransformationException;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.annotations.OnRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@SuppressWarnings("unused")
public class ContentBasedRoutingPolicy {
    private static final Logger logger = LoggerFactory.getLogger(ContentBasedRoutingPolicy.class);

    /**
     * The associated configuration to this ContentBasedRouting Policy
     */
    private ContentBasedRoutingPolicyConfiguration configuration;

    /**
     * Create a new ContentBasedRouting Policy instance based on its associated configuration
     *
     * @param configuration the associated configuration to the new ContentBasedRouting Policy instance
     */
    public ContentBasedRoutingPolicy(ContentBasedRoutingPolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    //@OnRequestContent
    public ReadWriteStream onRequestContent(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {

        return TransformableRequestStreamBuilder
                .on(request)
                .transform(buffer -> {
                    String messageBody = buffer.toString();
                    try {

                        String extractResult = JsonPath.parse(messageBody).read(configuration.getJsonpathExpression(), String.class);
                        logger.info("Extract result: {}", extractResult);
                        Map<String, String> routingTable =  new Gson().fromJson(configuration.getRoutingTable(), Map.class);

                        if (!routingTable.containsKey(extractResult)) {
                            logger.info("No Route found for {}", extractResult);
                            return buffer;
                        }
                        String endpoint = routingTable.get(extractResult);
                        // Set final endpoint
                        executionContext.setAttribute(ExecutionContext.ATTR_REQUEST_ENDPOINT, endpoint);
                        logger.info("Route request to {}", endpoint);
                    } catch (Throwable t) {
                        throw new TransformationException("Unable to run parse content: " + t.getMessage(), t);
                    }

                    return buffer;
                })
                .build();
    }

    @OnRequest
    public void onRequest(Request request, Response response, ExecutionContext executionContext, PolicyChain policyChain) {

        executionContext.setAttribute(ExecutionContext.ATTR_INVOKER, new ContentBasedRoutingInvoker(configuration));

        // Finally continue chaining
        policyChain.doNext(request, response);
    }

}
