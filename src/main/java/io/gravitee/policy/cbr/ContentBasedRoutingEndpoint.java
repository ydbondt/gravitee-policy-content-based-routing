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
        logger.info("Extract result: {}", extractResult);
        Map<String, List<String>> routingTable = new Gson().fromJson(configuration.getRoutingTable(), Map.class);

        if (routingTable.containsKey(extractResult)) {
            return routingTable.get(extractResult);
        } else {
            logger.info("No Route found for {}", extractResult);
            return Collections.singletonList(configuration.getFallbackUrl());
        }
    }
}
