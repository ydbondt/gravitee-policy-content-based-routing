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

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.vertx.core.Vertx;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContentBasedRoutingPolicyTest {

    @Mock
    private ContentBasedRoutingPolicyConfiguration configuration;

    @Test
    public void testThreadLock() throws Exception {

        ExecutionContext executionContext = mock(ExecutionContext.class);t
        Request request = mock(Request.class);
        when(request.method()).thenReturn(HttpMethod.POST);
        Response response = mock(Response.class);

        when(executionContext.getComponent(Vertx.class)).thenReturn(Vertx.vertx());
        when(executionContext.request()).thenReturn(request);
        when(executionContext.response()).thenReturn(response);

        ContentBasedRoutingConnection connection = new ContentBasedRoutingConnection(executionContext, configuration);

        connection.end();
    }

    @Test
    public void testOnResponse() throws Exception {

    }

}