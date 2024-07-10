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
package com.jd.live.agent.implement.flowcontrol.resilience4j.circuitbreak;

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.invoke.circuitbreak.AbstractCircuitBreaker;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerStateListener;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakerPolicy;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.util.concurrent.TimeUnit;

/**
 * Resilience4jCircuitBreaker
 *
 * @since 1.1.0
 */
public class Resilience4jCircuitBreaker extends AbstractCircuitBreaker {

    private final io.github.resilience4j.circuitbreaker.CircuitBreaker delegate;

    private final Resilience4jCircuitBreakerEventConsumer eventConsumer;

    private long lastAcquireTime;

    public Resilience4jCircuitBreaker(CircuitBreakerPolicy policy, URI uri, CircuitBreaker delegate) {
        super(policy, uri);
        this.delegate = delegate;
        this.eventConsumer = new Resilience4jCircuitBreakerEventConsumer();
        this.delegate.getEventPublisher().onStateTransition(eventConsumer);
    }

    @Override
    public boolean acquire() {
        lastAcquireTime = System.currentTimeMillis();
        return delegate.tryAcquirePermission();
    }

    @Override
    public long getLastAcquireTime() {
        return lastAcquireTime;
    }

    @Override
    public void release() {
        delegate.releasePermission();
    }

    @Override
    public void onSuccess(long durationInMs) {
        delegate.onSuccess(durationInMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onError(long durationInMs, Throwable throwable) {
        delegate.onError(durationInMs, TimeUnit.MILLISECONDS, throwable);
    }

    @Override
    public void addListener(CircuitBreakerStateListener listener) {
        eventConsumer.addListener(listener);
    }

}
