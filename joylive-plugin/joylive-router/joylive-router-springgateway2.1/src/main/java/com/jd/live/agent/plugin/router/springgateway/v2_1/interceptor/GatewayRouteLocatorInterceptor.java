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
package com.jd.live.agent.plugin.router.springgateway.v2_1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.router.springgateway.v2_1.filter.LiveRoutePredicate;
import com.jd.live.agent.plugin.router.springgateway.v2_1.filter.LiveRoutes;
import org.springframework.cloud.gateway.handler.AsyncPredicate;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.server.ServerWebExchange;

/**
 * GatewayRouteConstructorInterceptor
 *
 * @since 1.7.0
 */
public class GatewayRouteLocatorInterceptor extends InterceptorAdaptor {

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        RouteDefinition routeDefinition = mc.getArgument(0);
        AsyncPredicate<ServerWebExchange> predicate = mc.getResult();
        mc.setResult(new LiveRoutePredicate(predicate, routeDefinition, LiveRoutes.getVersion()));
    }
}
