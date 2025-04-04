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
package com.jd.live.agent.plugin.router.springcloud.v3.cluster.context;

import com.jd.live.agent.governance.invoke.cluster.ClusterContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import java.util.Set;

public interface CloudClusterContext extends ClusterContext {

    /**
     * Retrieves load balancing configuration properties for specified service
     *
     * @param service Target service identifier/name
     * @return Configured properties for service load balancing
     */
    LoadBalancerProperties getLoadBalancerProperties(String service);

    /**
     * Determines if raw HTTP status codes should be preserved in responses
     *
     * @param properties Active load balancing configuration
     * @return True to retain original status codes in response data
     */
    boolean isUseRawStatusCodeInResponseData(LoadBalancerProperties properties);

    /**
     * Obtains service instance provider for discovery operations
     *
     * @param service Target service identifier/name
     * @return Supplier of available service instances
     */
    ServiceInstanceListSupplier getServiceInstanceListSupplier(String service);

    /**
     * Gets lifecycle handlers for load balancing process
     *
     * @param service Target service identifier/name
     * @return Set of lifecycle processors for load balancing events
     */
    @SuppressWarnings("rawtypes")
    Set<LoadBalancerLifecycle> getLifecycleProcessors(String service);

}
