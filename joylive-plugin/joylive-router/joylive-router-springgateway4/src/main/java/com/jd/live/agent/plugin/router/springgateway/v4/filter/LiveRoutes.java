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
package com.jd.live.agent.plugin.router.springgateway.v4.filter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * An abstract class representing the version management for live routes.
 * This class provides a global counter to track and manage the version of routes,
 * allowing for version retrieval and increment operations.
 */
public abstract class LiveRoutes {

    /**
     * A global atomic counter that tracks the current version of the routes.
     * This counter is used to ensure consistency and track changes in route configurations.
     */
    private static final AtomicLong ROUTE_VERSION = new AtomicLong(0);

    /**
     * A thread-safe map that stores live route instances, keyed by their unique identifiers.
     */
    private static final Map<String, LiveRoute> ROUTES = new ConcurrentHashMap<>();

    /**
     * Retrieves the current value of the global route version counter.
     *
     * @return the current route version as a long value
     */
    public static long getVersion() {
        return ROUTE_VERSION.get();
    }

    /**
     * Increments the global route version counter by one.
     * This method is used to signal a change in the route configuration.
     */
    public static void incVersion() {
        ROUTE_VERSION.incrementAndGet();
    }

    /**
     * Retrieves an existing live route from the map by its identifier, or creates a new one
     * using the provided function if it does not already exist.
     *
     * @param id       the unique identifier of the live route
     * @param function a function to create a new live route if it does not exist
     * @return the existing or newly created live route
     */
    public static LiveRoute getOrCreate(String id, Function<String, LiveRoute> function) {
        return ROUTES.computeIfAbsent(id, function);
    }

    /**
     * Retrieves a live route from the map by its identifier.
     *
     * @param id the unique identifier of the live route
     * @return the live route associated with the identifier, or {@code null} if not found
     */
    public static LiveRoute get(String id) {
        return ROUTES.get(id);
    }

    /**
     * Adds or updates a live route in the map with the specified identifier.
     *
     * @param id    the unique identifier of the live route
     * @param route the live route to store
     */
    public static void put(String id, LiveRoute route) {
        ROUTES.put(id, route);
    }
}
