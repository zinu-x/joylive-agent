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
package com.jd.live.agent.governance.rule;

import com.jd.live.agent.core.parser.json.JsonAlias;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The OpType enum represents different types of operations that can be performed
 * for condition matching within a conditional interface such as {@link Conditional}.
 * Each operation type has a corresponding code, a description, and a method to determine
 * if a given argument matches the criteria defined by a list of values.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Getter
public enum OpType {

    /**
     * Represents an equality operation.
     */
    @JsonAlias("eq")
    EQUAL("eq", "equal") {
        @Override
        public boolean isMatch(List<String> values, List<String> args) {
            if (values == null || values.isEmpty() || args == null || args.isEmpty()) {
                return false;
            }
            if (values.size() != args.size()) {
                return false;
            }
            for (String arg : args) {
                if (!values.contains(arg)) {
                    return false;
                }
            }
            return true;
        }
    },

    /**
     * Represents a not-equal operation.
     */
    @JsonAlias("ne")
    NOT_EQUAL("ne", "not equal") {
        @Override
        public boolean isMatch(List<String> values, List<String> args) {
            if (values == null || values.isEmpty()) {
                return args != null && !args.isEmpty();
            }
            if (args == null || values.size() != args.size()) {
                return true;
            }
            for (String arg : args) {
                if (!values.contains(arg)) {
                    return true;
                }
            }
            return false;
        }
    },

    /**
     * Represents an operation to check if a value is not in a list.
     */
    @JsonAlias("nin")
    NOT_IN("nin", "not in") {
        @Override
        public boolean isMatch(List<String> values, List<String> args) {
            if (values == null || values.isEmpty() || args == null || args.isEmpty()) {
                return true;
            }
            for (String value : values) {
                if (args.contains(value)) {
                    return false;
                }
            }
            return true;
        }
    },

    /**
     * Represents an operation to check if a value is in a list.
     */
    @JsonAlias("in")
    IN("in", "in") {
        @Override
        public boolean isMatch(List<String> values, List<String> args) {
            if (values == null || values.isEmpty() || args == null || args.isEmpty()) {
                return false;
            }
            for (String value : values) {
                if (args.contains(value)) {
                    return true;
                }
            }
            return false;
        }
    },

    /**
     * Represents a regular expression match operation.
     */
    @JsonAlias("regular")
    REGULAR("regular", "regular") {
        @Override
        public boolean isMatch(List<String> values, List<String> args) {
            if (values == null || values.isEmpty() || args == null || args.isEmpty()) {
                return false;
            }
            for (String arg : args) {
                boolean matchFound = false;
                for (String value : values) {
                    Pattern pattern = PATTERNS.computeIfAbsent(value, Pattern::compile);
                    if (pattern.matcher(arg).matches()) {
                        matchFound = true;
                        break;
                    }
                }
                if (!matchFound) {
                    return false;
                }
            }
            return true;
        }
    },

    /**
     * Represents a prefix match operation.
     */
    @JsonAlias("prefix")
    PREFIX("prefix", "prefix") {
        @Override
        public boolean isMatch(List<String> values, List<String> args) {
            if (values == null || values.isEmpty() || args == null || args.isEmpty()) {
                return false;
            }
            for (String arg : args) {
                boolean matchFound = false;
                for (String value : values) {
                    if (arg.startsWith(value)) {
                        matchFound = true;
                        break;
                    }
                }
                if (!matchFound) {
                    return false;
                }
            }
            return true;
        }
    };

    private static final Map<String, OpType> TYPES = Arrays.stream(values()).collect(Collectors.toMap(OpType::getCode, o -> o));

    private static final Map<String, Pattern> PATTERNS = new ConcurrentHashMap<>();

    /**
     * The code associated with this operation type.
     */
    private final String code;

    /**
     * The description of this operation type.
     */
    private final String description;

    /**
     * Constructs an OpType with the specified code and description.
     *
     * @param code        the code for the operation type
     * @param description the human-readable description of the operation type
     */
    OpType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Determines if the specified argument matches the criteria defined by the list of values
     * for this operation type.
     *
     * @param values the list of values to match against
     * @param args   the list of arguments to test for a match
     * @return true if the argument matches the criteria, false otherwise
     */
    public boolean isMatch(List<String> values, List<String> args) {
        return false;
    }

    /**
     * Returns the OpType associated with the given code.
     *
     * @param code the code to lookup
     * @return the corresponding OpType, or null if not found
     */
    public static OpType codeOf(String code) {
        return code == null ? null : TYPES.get(code);
    }
}

