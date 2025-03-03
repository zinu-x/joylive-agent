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

import com.jd.live.agent.core.util.cache.CacheObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import feign.Request;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMapAdapter;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.CollectionUtils.modifiedMap;
import static com.jd.live.agent.core.util.map.MultiLinkedMap.caseInsensitive;

/**
 * Represents an outbound request made using Feign, extending the capabilities of {@link AbstractClusterRequest}
 * to handle specifics of Feign requests such as options and cookie parsing.
 * <p>
 * This class encapsulates the details of a Feign request, including HTTP method, URI, headers, and cookies,
 * and provides utilities for parsing these elements from the Feign {@link Request}. It also integrates with
 * Spring's {@link LoadBalancerClientFactory} for load balancing capabilities.
 *
 * @since 1.0.0
 */
public class FeignClusterRequest extends AbstractClusterRequest<Request> {

    private final Request.Options options;

    private CacheObject<Map<String, Collection<String>>> writeableHeaders;

    /**
     * Constructs a new {@code FeignOutboundRequest} with the specified Feign request, load balancer client factory,
     * and request options.
     *
     * @param request                   the Feign request
     * @param loadBalancerClientFactory the factory to create a load balancer client
     * @param properties                the LoadBalancerProperties object containing the configuration for the load balancer.
     * @param options                   the options for the Feign request, such as timeouts
     */
    public FeignClusterRequest(Request request,
                               ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory,
                               LoadBalancerProperties properties,
                               Request.Options options) {
        super(request, URI.create(request.url()), loadBalancerClientFactory, properties);
        this.options = options;
    }

    @Override
    public HttpMethod getHttpMethod() {
        Request.HttpMethod method = request.httpMethod();
        try {
            return method == null ? null : HttpMethod.valueOf(method.name());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    @Override
    public String getHeader(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        Collection<String> values = request.headers().get(key);
        if (values == null || values.isEmpty()) {
            return null;
        } else if (values instanceof List) {
            return ((List<String>) values).get(0);
        }
        return values.iterator().next();
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            getWriteableHeaders().computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
    }

    @Override
    protected RequestData buildRequestData() {
        // cookie is used only in RequestBasedStickySessionServiceInstanceListSupplier
        // it's disabled by live interceptor
        // so we can use null value to improve performance.
        return new RequestData(
                org.springframework.http.HttpMethod.resolve(request.httpMethod().name()), getURI(),
                new HttpHeaders(new MultiValueMapAdapter<>(getHeaders())), null, null);
    }

    public Request.Options getOptions() {
        return options;
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return caseInsensitive(request.headers(), true);
    }

    protected Map<String, Collection<String>> getWriteableHeaders() {
        if (writeableHeaders == null) {
            writeableHeaders = new CacheObject<>(modifiedMap(request.headers()));
        }
        return writeableHeaders.get();
    }

}
