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
package com.jd.live.agent.core.inject.jbind.converter.fundamental;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.jbind.Conversion;
import com.jd.live.agent.core.inject.jbind.Converter;

import java.io.File;

@Extension("CharSequence2FileConverter")
public class CharSequence2FileConverter implements Converter.FundamentalConverter {
    @Override
    public Object convert(Conversion conversion) {
        Object source = conversion.getSource();
        String value = source == null ? null : source.toString();
        return value == null || value.isEmpty() ? null : new File(value);
    }

    @Override
    public Class<?> getSourceType() {
        return CharSequence.class;
    }

    @Override
    public Class<?> getTargetType() {
        return File.class;
    }
}
