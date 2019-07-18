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

import io.gravitee.policy.api.PolicyConfiguration;
import io.gravitee.common.http.HttpMethod;

@SuppressWarnings("unused")
public class ContentBasedRoutingPolicyConfiguration implements PolicyConfiguration {

    /**
     * Expression for extracting a value from JSON Request Body.
     */
    private String jsonpathExpression;

    private String routingTable;

    private String fallbackUrl;

    /**
     * Expression for extracting a value from JSON Request Body
     *
     * @return the String parameter
     */
    public String getJsonpathExpression() {
        return jsonpathExpression;
    }

    public void setJsonpathExpression(String jsonpathExpression) {
        this.jsonpathExpression = jsonpathExpression;
    }

    public String getRoutingTable() {
        return routingTable;
    }

    public void setRoutingTable(String routingTable) {
        this.routingTable = routingTable;
    }

    public String getFallbackUrl() {
        return fallbackUrl;
    }

    public void setFallbackUrl(String fallbackUrl) {
        this.fallbackUrl = fallbackUrl;
    }
}
