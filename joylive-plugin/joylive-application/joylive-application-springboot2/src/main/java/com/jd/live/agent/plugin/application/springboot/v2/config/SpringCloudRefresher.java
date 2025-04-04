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
package com.jd.live.agent.plugin.application.springboot.v2.config;

import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashSet;

/**
 * A refresher implementation for Spring Cloud applications.
 */
public class SpringCloudRefresher implements ConfigRefresher {

    private final ConfigurableApplicationContext context;

    public SpringCloudRefresher(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @Override
    public void refresh() {
        // refresh is slow.
        context.publishEvent(new EnvironmentChangeEvent(context, new HashSet<>()));
    }
}
