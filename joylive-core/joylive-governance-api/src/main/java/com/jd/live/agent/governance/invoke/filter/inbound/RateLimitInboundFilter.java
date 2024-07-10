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
package com.jd.live.agent.governance.invoke.filter.inbound;

import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.InjectLoader;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.InboundFilterChain;
import com.jd.live.agent.governance.invoke.ratelimit.RateLimiter;
import com.jd.live.agent.governance.invoke.ratelimit.RateLimiterFactory;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;

import java.util.List;
import java.util.Map;

/**
 * RateLimitInboundFilter
 */
@Injectable
@Extension(value = "LimitInboundFilter", order = InboundFilter.ORDER_INBOUND_LIMITER)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class RateLimitInboundFilter implements InboundFilter {

    @Inject
    @InjectLoader
    private Map<String, RateLimiterFactory> factories;

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    private GovernanceConfig governanceConfig;

    @Override
    public <T extends InboundRequest> void filter(InboundInvocation<T> invocation, InboundFilterChain chain) {
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        List<RateLimitPolicy> policies = servicePolicy == null ? null : servicePolicy.getRateLimitPolicies();
        if (null != policies && !policies.isEmpty()) {
            for (RateLimitPolicy policy : policies) {
                // match logic
                if (policy.match(invocation)) {
                    RateLimiter rateLimiter = getRateLimiter(policy);
                    if (null != rateLimiter && !rateLimiter.acquire()) {
                        invocation.reject(FaultType.LIMIT, "The traffic limiting policy rejects the request.");
                    }
                }
            }
        }
        chain.filter(invocation);
    }

    /**
     * Retrieves a rate limiter based on the given policy.
     * If the policy's realize type is not specified, it falls back to the default type
     * from the service configuration. If the factory for the specified type is not found,
     * it uses the first available factory.
     *
     * @param policy the rate limit policy.
     * @return the rate limiter instance based on the given policy.
     */
    private RateLimiter getRateLimiter(RateLimitPolicy policy) {
        String realizeType = policy.getRealizeType();
        if (realizeType == null || realizeType.isEmpty()) {
            realizeType = governanceConfig.getServiceConfig().getRateLimiter().getType();
        }
        RateLimiterFactory factory = realizeType != null && !realizeType.isEmpty()
                ? factories.get(realizeType)
                : factories.entrySet().iterator().next().getValue();
        return factory.get(policy);
    }

}
