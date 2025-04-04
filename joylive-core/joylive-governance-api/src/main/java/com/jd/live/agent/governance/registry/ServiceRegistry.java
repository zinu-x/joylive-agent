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
package com.jd.live.agent.governance.registry;

import java.util.List;

/**
 * An interface representing a service registry that provides access to service endpoints and metadata.
 * Implementations of this interface are responsible for managing and retrieving information about
 * registered services, including their endpoints, service names, and group names.
 */
public interface ServiceRegistry {

    /**
     * Retrieves a list of {@link ServiceEndpoint} objects representing the available endpoints
     * for the registered service.
     *
     * @return a list of {@link ServiceEndpoint} objects
     */
    List<ServiceEndpoint> getEndpoints();

    /**
     * Retrieves the name of the registered service.
     *
     * @return the name of the service
     */
    String getService();

}

