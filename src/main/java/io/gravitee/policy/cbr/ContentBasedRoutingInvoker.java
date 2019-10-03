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
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.stream.ReadStream;

public class ContentBasedRoutingInvoker implements Invoker {
    final ContentBasedRoutingEndpoint endpoint;

    public ContentBasedRoutingInvoker(ContentBasedRoutingPolicyConfiguration configuration) {
        this.endpoint = new ContentBasedRoutingEndpoint(configuration);
    }

    @Override
    public void invoke(ExecutionContext executionContext, ReadStream<Buffer> readStream, Handler<ProxyConnection> connectionHandler) {
        final ContentBasedRoutingConnection connection = new ContentBasedRoutingConnection(executionContext, endpoint);

        // Return connection to backend
        connectionHandler.handle(connection);

        // Plug underlying stream to connection stream
        readStream
                .bodyHandler(connection::write)
                .endHandler(aVoid -> connection.end());

        // Resume the incoming request to handle content and end
        executionContext.request().resume();
    }
}
