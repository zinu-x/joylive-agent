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
package com.jd.live.agent.plugin.router.springcloud.v3.request;

import com.jd.live.agent.core.util.cache.CacheObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.map.MultiLinkedMap;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpOutboundRequest;
import feign.Request;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.CollectionUtils.modifiedMap;

/**
 * FeignClientClusterRequest
 */
public class FeignWebClusterRequest extends AbstractHttpOutboundRequest<Request> implements FeignOutboundRequest {

    private final String service;

    private final List<ServiceEndpoint> instances;

    private final FeignExecution execution;

    private CacheObject<Map<String, Collection<String>>> writeableHeaders;

    public FeignWebClusterRequest(Request request,
                                  String service,
                                  URI uri,
                                  List<ServiceEndpoint> instances,
                                  FeignExecution execution) {
        super(request);
        this.service = service;
        this.uri = uri;
        this.instances = instances;
        this.execution = execution;
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public HttpMethod getHttpMethod() {
        try {
            return HttpMethod.valueOf(request.httpMethod().name());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    @Override
    public String getHeader(String key) {
        Collection<String> values = request.headers().get(key);
        return values == null || values.isEmpty() ? null : values.iterator().next();
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            getWriteableHeaders().computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return MultiLinkedMap.caseInsensitive(request.headers(), true);
    }

    public List<ServiceEndpoint> getInstances() {
        return instances;
    }

    public feign.Response execute(Endpoint endpoint) throws IOException {
        return execution.execute(endpoint);
    }

    protected Map<String, Collection<String>> getWriteableHeaders() {
        if (writeableHeaders == null) {
            writeableHeaders = new CacheObject<>(modifiedMap(request.headers()));
        }
        return writeableHeaders.get();
    }

    @FunctionalInterface
    public interface FeignExecution {

        feign.Response execute(Endpoint endpoint) throws IOException;

    }

}
