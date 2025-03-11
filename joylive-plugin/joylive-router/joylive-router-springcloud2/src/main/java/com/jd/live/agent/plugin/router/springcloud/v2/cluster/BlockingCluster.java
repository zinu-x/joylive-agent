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
package com.jd.live.agent.plugin.router.springcloud.v2.cluster;

import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.plugin.router.springcloud.v2.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v2.request.BlockingClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v2.response.BlockingClusterResponse;
import com.jd.live.agent.plugin.router.springcloud.v2.response.DegradeHttpResponse;
import com.jd.live.agent.plugin.router.springcloud.v2.util.LoadBalancerUtil;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.core.util.type.ClassUtils.getValue;

/**
 * The {@code BlockingCluster} class extends {@code AbstractClientCluster} to provide a blocking
 * mechanism for handling HTTP requests, integrating load balancing and retry logic. It utilizes
 * Spring Cloud's load balancing capabilities to distribute requests across service instances and
 * supports configurable retry mechanisms for handling transient failures.
 * <p>
 * This class is designed to work within a microservices architecture, leveraging Spring Cloud's
 * infrastructure components to facilitate resilient and scalable service-to-service communication.
 *
 * @see AbstractClientCluster
 */
public class BlockingCluster extends AbstractClientCluster<BlockingClusterRequest, BlockingClusterResponse> {

    private static final Set<String> RETRY_EXCEPTIONS = new HashSet<>(Arrays.asList(
            "java.io.IOException",
            "java.util.concurrent.TimeoutException",
            "org.springframework.cloud.client.loadbalancer.RetryableStatusCodeException"
    ));

    private static final ErrorPredicate RETRY_PREDICATE = new ErrorPredicate.DefaultErrorPredicate(null, RETRY_EXCEPTIONS);

    private static final String FIELD_LOAD_BALANCER = "loadBalancer";

    private static final String FIELD_REQUEST_FACTORY = "requestFactory";

    private static final String FIELD_LB_PROPERTIES = "lbProperties";

    /**
     * An interceptor for HTTP requests, used to apply additional processing or modification
     * to requests before they are executed.
     */
    private final ClientHttpRequestInterceptor interceptor;

    /**
     * A factory for creating load-balanced {@code LoadBalancerRequest} instances, supporting
     * the dynamic selection of service instances based on load.
     */
    private final LoadBalancerRequestFactory requestFactory;

    /**
     * A factory for creating {@code ReactiveLoadBalancer} instances for service discovery
     * and load balancing.
     */
    private final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

    /**
     * Constructs a {@code BlockingCluster} with the specified HTTP request interceptor.
     * Initializes the {@code requestFactory} and {@code loadBalancerFactory} fields by
     * reflectively accessing the interceptor's fields.
     *
     * @param interceptor the HTTP request interceptor to be used by this cluster
     */
    public BlockingCluster(ClientHttpRequestInterceptor interceptor) {
        this.interceptor = interceptor;
        this.requestFactory = getValue(interceptor, FIELD_REQUEST_FACTORY);
        LoadBalancerClient client = getValue(interceptor, FIELD_LOAD_BALANCER);
        this.loadBalancerFactory = LoadBalancerUtil.getFactory(client);
        this.defaultRetryPolicy = createRetryPolicy(getValue(interceptor, FIELD_LB_PROPERTIES, v -> v instanceof LoadBalancerRetryProperties));
    }

    public ReactiveLoadBalancer.Factory<ServiceInstance> getLoadBalancerFactory() {
        return loadBalancerFactory;
    }

    @Override
    public CompletionStage<BlockingClusterResponse> invoke(BlockingClusterRequest request, SpringEndpoint endpoint) {
        try {
            LoadBalancerRequest<ClientHttpResponse> lbRequest = requestFactory.createRequest(request.getRequest(), request.getBody(), request.getExecution());
            ClientHttpResponse response = lbRequest.apply(endpoint.getInstance());
            return CompletableFuture.completedFuture(new BlockingClusterResponse(response));
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @Override
    public ErrorPredicate getRetryPredicate() {
        return RETRY_PREDICATE;
    }

    @Override
    protected BlockingClusterResponse createResponse(BlockingClusterRequest httpRequest, DegradeConfig degradeConfig) {
        return new BlockingClusterResponse(new DegradeHttpResponse(degradeConfig, httpRequest));
    }

    @Override
    protected BlockingClusterResponse createResponse(ServiceError error, ErrorPredicate predicate) {
        return new BlockingClusterResponse(error, predicate);
    }

}
