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
package com.jd.live.agent.governance.invoke.fault;

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.policy.service.fault.FaultInjectionPolicy;

import java.util.Random;

/**
 * Represents an interface for fault injection mechanisms.
 * Fault injection is used to simulate or introduce failures in a system to test its resilience and behavior under adverse conditions.
 * Implementations of this interface should define how permits are acquired based on a fault injection policy.
 */
@Extensible
public interface FaultInjection {

    /**
     * Attempts to obtain a permit according to the specified fault injection policy.
     * The permit acquisition process is influenced by the provided policy and a random number generator.
     *
     * @param policy The fault injection policy to be used when attempting to acquire a permit.
     *               This policy defines the conditions under which a permit is granted or denied.
     * @param random A random number generator used to introduce variability in the permit acquisition process.
     */
    void acquire(FaultInjectionPolicy policy, Random random);
}
