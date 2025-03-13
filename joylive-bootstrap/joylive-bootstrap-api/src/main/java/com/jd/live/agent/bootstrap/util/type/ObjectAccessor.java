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
package com.jd.live.agent.bootstrap.util.type;

import java.util.function.Predicate;

/**
 * An interface that combines the functionality of both {@link ObjectGetter} and {@link ObjectSetter}.
 * It allows for both retrieving and setting values on a target object.
 */
public interface ObjectAccessor extends ObjectGetter, ObjectSetter {
    /**
     * Retrieves a value from the specified target object.
     *
     * @param target    the target object
     * @param predicate an optional predicate to filter the result
     * @return The value retrieved from the target object.
     */
    default Object get(Object target, Predicate<Object> predicate) {
        Object result = get(target);
        return result != null && (predicate == null || predicate.test(result)) ? result : null;
    }
}

