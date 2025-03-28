/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.plugin.router.springcloud.v3.request;

import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpOutboundRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * A specialized HTTP outbound request class for handling cluster requests.
 * This class extends {@link AbstractHttpOutboundRequest} and provides methods for interacting with service instances,
 * managing headers, cookies, and executing HTTP requests.
 */
public class ReactiveWebClusterRequest extends AbstractHttpOutboundRequest<ClientRequest> {

    private final String service;

    private final Registry registry;

    private final ExchangeFunction next;

    private final HttpHeaders writeableHeaders;

    public ReactiveWebClusterRequest(ClientRequest request, String service, Registry registry, ExchangeFunction next) {
        super(request);
        this.service = service;
        this.registry = registry;
        this.next = next;
        this.uri = request.url();
        this.writeableHeaders = HttpHeaders.writableHttpHeaders(request.headers());
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public HttpMethod getHttpMethod() {
        try {
            return HttpMethod.valueOf(request.method().name());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    @Override
    public String getCookie(String key) {
        return key == null || key.isEmpty() ? null : request.cookies().getFirst(key);
    }

    @Override
    public String getHeader(String key) {
        return key == null || key.isEmpty() ? null : request.headers().getFirst(key);
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            writeableHeaders.set(key, value);
        }
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return writeableHeaders;
    }

    @Override
    protected Map<String, List<String>> parseCookies() {
        return request.cookies();
    }

    /**
     * Retrieves the list of service endpoints for the associated service.
     *
     * @return a list of {@link ServiceEndpoint} instances
     */
    public List<ServiceEndpoint> getInstances() {
        return registry.getEndpoints(service);
    }

    /**
     * Executes the HTTP request for a specific endpoint and returns a reactive {@link ClientResponse}.
     *
     * @param endpoint the {@link Endpoint} to which the request is directed
     * @return a {@link Mono} emitting the {@link ClientResponse} containing the response data
     */
    public Mono<ClientResponse> exchange(Endpoint endpoint) {
        return next.exchange(create(request, endpoint));
    }

    /**
     * Executes the HTTP request and returns a reactive {@link ClientResponse}.
     *
     * @param request the {@link ClientRequest} to execute
     * @return a {@link Mono} emitting the {@link ClientResponse} containing the response data
     */
    public Mono<ClientResponse> exchange(ClientRequest request) {
        return next.exchange(request);
    }

    /**
     * Creates a new {@link ClientRequest} based on the original request and the specified endpoint.
     *
     * @param request  the original {@link ClientRequest}
     * @param endpoint the {@link Endpoint} to which the request is directed
     * @return a new {@link ClientRequest}
     */
    public static ClientRequest create(ClientRequest request, Endpoint endpoint) {
        return ClientRequest.from(request).url(HttpUtils.newURI(request.url(), endpoint.getHost(), endpoint.getPort())).build();
    }
}
