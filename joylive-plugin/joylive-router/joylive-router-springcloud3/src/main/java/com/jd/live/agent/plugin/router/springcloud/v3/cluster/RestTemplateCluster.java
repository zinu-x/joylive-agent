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
package com.jd.live.agent.plugin.router.springcloud.v3.cluster;

import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ErrorPredicate.DefaultErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.AbstractLiveCluster;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v3.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v3.request.RestTemplateClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.response.BlockingClusterResponse;
import com.jd.live.agent.plugin.router.springcloud.v3.response.DegradeHttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A cluster implementation for Feign clients that manages a group of servers and provides load balancing and failover capabilities.
 *
 * @see AbstractClientCluster
 */
public class RestTemplateCluster extends AbstractLiveCluster<RestTemplateClusterRequest, BlockingClusterResponse, ServiceEndpoint> {

    private static final Set<String> RETRY_EXCEPTIONS = new HashSet<>(Arrays.asList(
            "java.io.IOException",
            "java.util.concurrent.TimeoutException"
    ));

    private static final ErrorPredicate RETRY_PREDICATE = new DefaultErrorPredicate(null, RETRY_EXCEPTIONS);

    protected final SpringOutboundThrower<RestTemplateClusterRequest> thrower = new SpringOutboundThrower<>();

    public static final RestTemplateCluster INSTANCE = new RestTemplateCluster();

    public RestTemplateCluster() {
    }

    @Override
    public ErrorPredicate getRetryPredicate() {
        return RETRY_PREDICATE;
    }

    @Override
    public CompletionStage<BlockingClusterResponse> invoke(RestTemplateClusterRequest request, ServiceEndpoint endpoint) {
        try {
            ClientHttpResponse response = request.getRequest().execute(endpoint);
            return CompletableFuture.completedFuture(new BlockingClusterResponse(response));
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @Override
    protected BlockingClusterResponse createResponse(RestTemplateClusterRequest request) {
        return createResponse(request, DegradeConfig.builder().responseCode(HttpStatus.OK.value()).responseBody("").build());
    }

    @Override
    public CompletionStage<List<ServiceEndpoint>> route(RestTemplateClusterRequest request) {
        return CompletableFuture.completedFuture(request.getInstances());
    }

    @Override
    protected BlockingClusterResponse createResponse(RestTemplateClusterRequest request, DegradeConfig degradeConfig) {
        return new BlockingClusterResponse(new DegradeHttpResponse(degradeConfig, request));
    }

    @Override
    protected BlockingClusterResponse createResponse(ServiceError error, ErrorPredicate predicate) {
        return new BlockingClusterResponse(error, predicate);
    }

    @Override
    public Throwable createException(Throwable throwable, RestTemplateClusterRequest request) {
        return thrower.createException(throwable, request);
    }

    @Override
    public Throwable createException(Throwable throwable, RestTemplateClusterRequest request, ServiceEndpoint endpoint) {
        return thrower.createException(throwable, request, endpoint);
    }

    @Override
    public Throwable createException(Throwable throwable, OutboundInvocation<RestTemplateClusterRequest> invocation) {
        return thrower.createException(throwable, invocation);
    }

}
