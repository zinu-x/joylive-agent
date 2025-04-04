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
package com.jd.live.agent.plugin.router.springcloud.v3.condition;

import com.jd.live.agent.core.extension.annotation.ConditionalComposite;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnMissingClass;
import com.jd.live.agent.governance.annotation.ConditionalOnGovernanceEnabled;
import com.jd.live.agent.governance.annotation.ConditionalOnSpringCloudDisabled;
import com.jd.live.agent.governance.annotation.ConditionalOnRegistryEnabled;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnRegistryEnabled
@ConditionalOnGovernanceEnabled
@ConditionalOnSpringCloudDisabled
@ConditionalOnClass(ConditionalOnSpringWeb5RegistryEnabled.TYPE_CLIENT_HTTP_REQUEST)
@ConditionalOnMissingClass(ConditionalOnSpringWeb5RegistryEnabled.TYPE_ERROR_RESPONSE)
@ConditionalComposite
public @interface ConditionalOnSpringWeb5RegistryEnabled {

    // spring web 6
    String TYPE_ERROR_RESPONSE = "org.springframework.web.ErrorResponse";

    String TYPE_CLIENT_HTTP_REQUEST = "org.springframework.http.client.ClientHttpRequest";

}
