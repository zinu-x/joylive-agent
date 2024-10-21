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
package com.jd.live.agent.governance.invoke.filter.route;

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilterChain;
import com.jd.live.agent.governance.invoke.metadata.ServiceMetadata;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

import java.util.Map;

/**
 * Support service calls by configuring to filter instances of a specific group
 *
 * @since 1.4.0
 */
@Injectable
@Extension(value = "GroupFilter", order = RouteFilter.ORDER_GROUP)
public class GroupFilter implements RouteFilter {

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, RouteFilterChain chain) {
        ServiceMetadata serviceMetadata = invocation.getServiceMetadata();
        ServiceConfig serviceConfig = serviceMetadata == null ? null : serviceMetadata.getServiceConfig();
        Map<String, String> invokeMapping = serviceConfig == null ? null : serviceConfig.getInvokeGroups();
        if (invokeMapping != null && !invokeMapping.isEmpty()) {
            RouteTarget target = invocation.getRouteTarget();
            target.filter(endpoint -> endpoint.getLabel(Constants.LABEL_SERVICE_GROUP).equals(invokeMapping.get(serviceMetadata.getServiceName())));
        }
        chain.filter(invocation);
    }
}
