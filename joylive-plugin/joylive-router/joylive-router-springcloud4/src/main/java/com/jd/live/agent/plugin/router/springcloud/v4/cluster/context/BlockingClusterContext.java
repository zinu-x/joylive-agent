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
package com.jd.live.agent.plugin.router.springcloud.v4.cluster.context;

import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * A concrete implementation of cluster context that provides blocking behavior
 * for cluster operations, extending {@link AbstractCloudClusterContext}
 */
public class BlockingClusterContext extends AbstractCloudClusterContext {

    private static final String FIELD_REQUEST_FACTORY = "requestFactory";

    private static final String FIELD_LOAD_BALANCER_CLIENT_FACTORY = "loadBalancer.loadBalancerClientFactory";

    private static final String FIELD_PROPERTIES = "properties";

    private final ClientHttpRequestInterceptor interceptor;

    private final LoadBalancerRequestFactory requestFactory;

    public BlockingClusterContext(ClientHttpRequestInterceptor interceptor) {
        this.interceptor = interceptor;
        this.requestFactory = getQuietly(interceptor, FIELD_REQUEST_FACTORY);
        this.loadBalancerFactory = getQuietly(interceptor, new String[]{FIELD_LOAD_BALANCER_CLIENT_FACTORY}, v -> v instanceof ReactiveLoadBalancer.Factory);
        this.loadBalancerProperties = getQuietly(loadBalancerFactory, FIELD_PROPERTIES, v -> v instanceof LoadBalancerProperties);
    }

    public LoadBalancerRequestFactory getRequestFactory() {
        return requestFactory;
    }

    @Override
    public boolean isRetryable() {
        return interceptor instanceof RetryLoadBalancerInterceptor;
    }
}
