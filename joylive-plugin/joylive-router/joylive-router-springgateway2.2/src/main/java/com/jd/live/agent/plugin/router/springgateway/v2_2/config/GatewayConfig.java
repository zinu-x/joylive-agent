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
package com.jd.live.agent.plugin.router.springgateway.v2_2.config;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class GatewayConfig {

    public static final String CONFIG_SPRING_GATEWAY_PREFIX = "agent.governance.router.springgateway";

    public static final String TYPE_REWRITE_PATH_FILTER = "org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory$1";

    public static final String TYPE_STRIP_PREFIX = "org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory$1";

    public static final String KEY_HOST_EXPRESSION = "hostExpression";

    protected static final String DEFAULT_HOST_EXPRESSION = "${unit}-${host}";

    private String hostExpression;

    private Set<String> pathFilters = new HashSet<>(Arrays.asList(TYPE_REWRITE_PATH_FILTER, TYPE_STRIP_PREFIX));

    /**
     * Checks if the given name is a path filter.
     *
     * @param filter The filter class name.
     * @return true if the filter is a path filter, false otherwise.
     */
    public boolean isPathFilter(String filter) {
        return pathFilters != null && filter != null && pathFilters.contains(filter);
    }

}
