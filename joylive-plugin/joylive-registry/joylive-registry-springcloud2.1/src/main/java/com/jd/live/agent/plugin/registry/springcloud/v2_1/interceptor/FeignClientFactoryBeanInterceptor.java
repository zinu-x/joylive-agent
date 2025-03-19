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
package com.jd.live.agent.plugin.registry.springcloud.v2_1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.registry.Registry;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * FeignClientFactoryBeanInterceptor
 */
public class FeignClientFactoryBeanInterceptor extends InterceptorAdaptor {

    private final Registry registry;

    public FeignClientFactoryBeanInterceptor(Registry registry) {
        this.registry = registry;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        // Compatible with lower versions, using reflection to get values.
        Object factoryBean = ctx.getTarget();
        UnsafeFieldAccessor accessor = getQuietly(factoryBean.getClass(), "name");
        if (accessor != null) {
            String name = (String) accessor.get(factoryBean);
            registry.subscribe(name);
        }
    }
}
