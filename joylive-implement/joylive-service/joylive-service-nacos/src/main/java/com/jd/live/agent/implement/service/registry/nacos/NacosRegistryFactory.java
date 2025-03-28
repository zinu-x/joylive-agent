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
package com.jd.live.agent.implement.service.registry.nacos;

import com.alibaba.nacos.client.config.utils.SnapShotSwitch;
import com.alibaba.nacos.common.ability.discover.NacosAbilityManagerHolder;
import com.alibaba.nacos.common.remote.PayloadRegistry;
import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.config.RegistryClusterConfig;
import com.jd.live.agent.governance.registry.RegistryFactory;
import com.jd.live.agent.governance.registry.RegistryService;

/**
 * A factory implementation for creating instances of {@link NacosRegistry}.
 * This class is annotated with {@link Extension} to indicate it provides the "nacos" extension.
 */
@Extension("nacos")
public class NacosRegistryFactory implements RegistryFactory, ExtensionInitializer {

    /**
     * Creates a new instance of {@link NacosRegistry} using the provided {@link RegistryClusterConfig}.
     *
     * @param config The configuration used to initialize the {@link NacosRegistry}.
     * @return A new instance of {@link NacosRegistry}.
     */
    @Override
    public RegistryService create(RegistryClusterConfig config) {
        return new NacosRegistry(config);
    }

    @Override
    public void initialize() {
        // init payload registry in repack mode.
        SnapShotSwitch.setIsSnapShot(false);
        PayloadRegistry.init();
        NacosAbilityManagerHolder.getInstance();
    }
}
