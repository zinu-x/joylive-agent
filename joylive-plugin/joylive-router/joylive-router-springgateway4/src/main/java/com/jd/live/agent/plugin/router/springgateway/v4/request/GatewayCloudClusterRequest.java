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
package com.jd.live.agent.plugin.router.springgateway.v4.request;

import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.plugin.router.springcloud.v4.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v4.request.AbstractCloudClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.util.UriUtils;
import com.jd.live.agent.plugin.router.springgateway.v4.cluster.context.GatewayClusterContext;
import com.jd.live.agent.plugin.router.springgateway.v4.config.GatewayConfig;
import com.jd.live.agent.plugin.router.springgateway.v4.response.GatewayClusterResponse;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.client.loadbalancer.CompletionContext.Status;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.*;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

/**
 * GatewayOutboundRequest
 *
 * @since 1.0.0
 */
@Getter
public class GatewayCloudClusterRequest extends AbstractCloudClusterRequest<ServerHttpRequest, GatewayClusterContext> {

    private final ServerWebExchange exchange;

    private final GatewayFilterChain chain;

    private final GatewayConfig gatewayConfig;

    private final RetryConfig retryConfig;

    private final int index;

    private final HttpHeaders writeableHeaders;

    public GatewayCloudClusterRequest(ServerWebExchange exchange,
                                      GatewayClusterContext context,
                                      GatewayFilterChain chain,
                                      GatewayConfig gatewayConfig,
                                      RetryConfig retryConfig,
                                      int index) {
        super(exchange.getRequest(), getURI(exchange), context);
        this.exchange = exchange;
        this.chain = chain;
        this.retryConfig = retryConfig;
        this.gatewayConfig = gatewayConfig;
        this.index = index;
        this.writeableHeaders = HttpHeaders.writableHttpHeaders(request.getHeaders());
    }

    @Override
    public HttpMethod getHttpMethod() {
        org.springframework.http.HttpMethod method = request.getMethod();
        try {
            return method == null ? null : HttpMethod.valueOf(method.name());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    @Override
    public String getCookie(String key) {
        HttpCookie cookie = key == null || key.isEmpty() ? null : request.getCookies().getFirst(key);
        return cookie == null ? null : cookie.getValue();
    }

    @Override
    public String getHeader(String key) {
        return key == null || key.isEmpty() ? null : request.getHeaders().getFirst(key);
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            writeableHeaders.set(key, value);
        }
    }

    @Override
    public String getQuery(String key) {
        return key == null || key.isEmpty() ? null : request.getQueryParams().getFirst(key);
    }

    @Override
    public boolean isInstanceSensitive() {
        return context != null && context.getLoadBalancerFactory() != null;
    }

    @Override
    protected RequestData buildRequestData() {
        // cookie is used only in RequestBasedStickySessionServiceInstanceListSupplier
        // it's disabled by live interceptor
        // so we can use null value to improve performance.
        return new RequestData(request.getMethod(), request.getURI(), request.getHeaders(), null, null);
    }

    @Override
    protected Map<String, List<String>> parseCookies() {
        return HttpUtils.parseCookie(request.getCookies(), HttpCookie::getValue);
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return writeableHeaders;
    }

    @Override
    public RetryPolicy getDefaultRetryPolicy() {
        RetryConfig retryConfig = getRetryConfig();
        if (retryConfig != null && retryConfig.getRetries() > 0) {
            List<org.springframework.http.HttpMethod> methods = retryConfig.getMethods();
            if (methods.isEmpty() || methods.contains(request.getMethod())) {
                RetryGatewayFilterFactory.BackoffConfig backoff = retryConfig.getBackoff();
                Set<String> statuses = new HashSet<>(16);
                retryConfig.getStatuses().forEach(status -> statuses.add(String.valueOf(status.value())));
                Set<HttpStatus.Series> series = new HashSet<>(retryConfig.getSeries());
                if (!series.isEmpty()) {
                    for (HttpStatus status : HttpStatus.values()) {
                        if (series.contains(status.series())) {
                            statuses.add(String.valueOf(status.value()));
                        }
                    }
                }
                Set<String> exceptions = new HashSet<>();
                retryConfig.getExceptions().forEach(e -> exceptions.add(e.getName()));

                RetryPolicy retryPolicy = new RetryPolicy();
                retryPolicy.setRetry(retryConfig.getRetries());
                retryPolicy.setInterval(backoff != null ? backoff.getFirstBackoff().toMillis() : null);
                retryPolicy.setErrorCodes(statuses);
                retryPolicy.setExceptions(exceptions);
                return retryPolicy;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onStartRequest(SpringEndpoint endpoint) {
        if (endpoint != null) {
            ServiceInstance instance = endpoint.getInstance();
            ServerWebExchange exchange = getExchange();
            Map<String, Object> attributes = exchange.getAttributes();

            URI uri = exchange.getAttributeOrDefault(GATEWAY_REQUEST_URL_ATTR, request.getURI());
            // preserve the original url
            Set<URI> urls = (Set<URI>) attributes.computeIfAbsent(GATEWAY_ORIGINAL_REQUEST_URL_ATTR, s -> new LinkedHashSet<>());
            urls.add(uri);

            // if the `lb:<scheme>` mechanism was used, use `<scheme>` as the default,
            // if the loadbalancer doesn't provide one.
            String overrideScheme = instance.isSecure() ? "https" : "http";

            String schemePrefix = (String) attributes.get(GATEWAY_SCHEME_PREFIX_ATTR);
            if (schemePrefix != null) {
                overrideScheme = request.getURI().getScheme();
            }
            URI requestUrl = UriUtils.newURI(new DelegatingServiceInstance(instance, overrideScheme), uri);

            attributes.put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
            attributes.put(GATEWAY_LOADBALANCER_RESPONSE_ATTR, endpoint.getResponse());
        }
        super.onStartRequest(endpoint);
    }

    @SuppressWarnings("unchecked")
    public void onSuccess(GatewayClusterResponse response, SpringEndpoint endpoint) {
        ResponseData responseData = new ResponseData(response.getResponse(), new RequestData(request));
        Response<ServiceInstance> resp = endpoint == null ? new DefaultResponse(null) : endpoint.getResponse();
        CompletionContext<ResponseData, ServiceInstance, ?> ctx = new CompletionContext<>(
                Status.SUCCESS,
                getLbRequest(),
                resp,
                responseData);
        lifecycles(l -> l.onComplete(ctx));

    }

    private static URI getURI(ServerWebExchange exchange) {
        return exchange.getAttributeOrDefault(GATEWAY_REQUEST_URL_ATTR, exchange.getRequest().getURI());
    }

}
