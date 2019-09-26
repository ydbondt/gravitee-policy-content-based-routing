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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;


public abstract class ContentBasedRoutingEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(ContentBasedRoutingConnection.class);

    public static List<String> getEndpoints(String messageBody, ContentBasedRoutingPolicyConfiguration configuration) {
        String extractResult = JsonPath.parse(messageBody).read(configuration.getJsonpathExpression(), String.class);
        Map<String, List<String>> routingTable = new Gson().fromJson(configuration.getRoutingTable(), Map.class);

        List<String> endpoints = null;
        if (routingTable.containsKey(extractResult)) {
            endpoints = routingTable.get(extractResult);
        } else {
            logger.debug("No Route found for {}", extractResult);
            endpoints = Collections.singletonList(configuration.getFallbackUrl());
        }

        logger.info("{} -> {}", extractResult, endpoints);
        return endpoints;
    }
}
