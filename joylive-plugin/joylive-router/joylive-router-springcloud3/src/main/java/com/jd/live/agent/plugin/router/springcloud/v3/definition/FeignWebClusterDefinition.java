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
package com.jd.live.agent.plugin.router.springcloud.v3.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnGovernanceEnabled;
import com.jd.live.agent.governance.annotation.ConditionalOnSpringCloudDisabled;
import com.jd.live.agent.governance.annotation.ConditionalOnRegistryEnabled;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.router.springcloud.v3.interceptor.FeignWebClusterInterceptor;

/**
 * FeignWebClusterDefinition
 */
@Extension(value = "FeignClientClusterDefinition_v5")
@ConditionalOnRegistryEnabled
@ConditionalOnGovernanceEnabled
@ConditionalOnSpringCloudDisabled
@ConditionalOnClass(FeignWebClusterDefinition.TYPE)
@Injectable
public class FeignWebClusterDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "feign.Client";

    private static final String METHOD = "execute";

    private static final String[] ARGUMENT = new String[]{
            "feign.Request",
            "feign.Request$Options"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    @Inject(Registry.COMPONENT_REGISTRY)
    private Registry registry;

    public FeignWebClusterDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(ARGUMENT)),
                        () -> new FeignWebClusterInterceptor(context, registry))
        };
    }
}
