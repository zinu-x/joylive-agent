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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.core.bootstrap.AppListener;
import com.jd.live.agent.core.bootstrap.AppListenerSupplier;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.InjectSource;
import com.jd.live.agent.core.inject.InjectSourceSupplier;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.AppService;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.service.AbstractService;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.RegistryClusterConfig;
import com.jd.live.agent.governance.config.RegistryConfig;
import com.jd.live.agent.governance.config.RegistryMode;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.exception.RegistryException;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.registry.RegistryService.AbstractRegistryService;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * {@code LiveRegistry} is an implementation of {@link Registry} that manages the registration and unregistration
 * of service instances.
 *
 * @see AbstractService
 * @see Registry
 * @see InjectSourceSupplier
 */
@Extension("LiveRegistry")
@Injectable
public class LiveRegistry extends AbstractService implements RegistrySupervisor, InjectSourceSupplier, AppListenerSupplier {

    private static final Logger logger = LoggerFactory.getLogger(LiveRegistry.class);

    private static final String FRAMEWORK = "framework";

    @Inject(RegistryConfig.COMPONENT_REGISTRY_CONFIG)
    private RegistryConfig registryConfig;

    @Inject(RegistryConfig.COMPONENT_REGISTRY_CONFIG)
    private ServiceConfig serviceConfig;

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    @Inject
    private Map<String, RegistryFactory> factories;

    private volatile List<RegistryService> registries = null;

    private final Map<String, Registration> registrations = new ConcurrentHashMap<>();

    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

    private final AtomicBoolean ready = new AtomicBoolean(false);

    @Override
    protected CompletableFuture<Void> doStart() {
        if (!registryConfig.isEnabled()) {
            return CompletableFuture.completedFuture(null);
        }
        // start registries
        List<RegistryClusterConfig> clusters = registryConfig.getClusters();
        List<RegistryService> registries = new ArrayList<>();
        try {
            if (clusters != null) {
                for (RegistryClusterConfig cluster : clusters) {
                    if (cluster.validate()) {
                        RegistryFactory factory = factories.get(cluster.getType());
                        if (factory == null) {
                            throw new RegistryException("registry type " + cluster.getType() + " is not supported");
                        }
                        registries.add(factory.create(cluster));
                    }
                }
            }
            if (registries.isEmpty()) {
                throw new RegistryException("No registry config found");
            } else {
                for (RegistryService registry : registries) {
                    startCluster(registry);
                }
            }
        } catch (Throwable e) {
            return Futures.future(e);
        }
        this.registries = registries;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        ready.set(false);
        onApplicationStop();
        // stop registries
        Close.instance().close(registries);
        registries = null;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public AppListener getAppListener() {
        return new AppListener.AppListenerAdapter() {

            @Override
            public void onReady(AppContext context) {
                onApplicationReady();
            }

            public void onStop(AppContext context) {
                onApplicationStop();
            }
        };
    }

    @Override
    public void register(ServiceInstance instance, Callable<Void> doRegister) {
        if (instance == null) {
            return;
        }
        if (instance.getGroup() == null) {
            instance.setGroup(application.getService().getGroup());
        }
        String name = getName(instance.getService(), instance.getGroup());
        Registration registration = registrations.computeIfAbsent(name, n -> createRegistration(n, instance, doRegister));
        if (ready.get()) {
            registration.register();
        } else {
            // delay register
            logger.info("Delay registering instance {}:{} to {} until application is ready",
                    instance.getHost(), instance.getPort(),
                    instance.getService());
        }
    }

    @Override
    public void unregister(ServiceInstance instance) {
        if (instance == null) {
            return;
        }
        if (instance.getGroup() == null) {
            instance.setGroup(application.getService().getGroup());
        }
        String name = getName(instance.getService(), instance.getGroup());
        Registration registration = registrations.remove(name);
        if (registration != null) {
            registration.unregister();
        }
    }

    @Override
    public CompletableFuture<Void> register(String service, String group) {
        AppService appService = application.getService();
        service = service == null ? appService.getName() : service;
        group = group == null ? appService.getGroup() : group;
        return policySupplier.subscribe(service, group);
    }

    @Override
    public CompletableFuture<Void> subscribe(String service, String group) {
        group = group == null ? serviceConfig.getGroup(service) : group;
        // subscribe instance
        subscribe(service, group, (Consumer<InstanceEvent>) null);
        // subscribe govern policy
        return policySupplier.subscribe(service, group);
    }

    @Override
    public void subscribe(String service, String group, Consumer<InstanceEvent> consumer) {
        if (service == null || service.isEmpty()) {
            return;
        }
        String targetGroup = group == null ? serviceConfig.getGroup(service) : group;
        String name = getName(service, targetGroup);
        Subscription subscription = subscriptions.computeIfAbsent(name, s -> createSubscription(s, targetGroup));
        subscription.addConsumer(consumer);
        subscription.subscribe();
    }

    @Override
    public ServiceRegistry getServiceRegistry(String service, String group) {
        if (service == null || service.isEmpty()) {
            return null;
        }
        String targetGroup = group == null ? serviceConfig.getGroup(service) : group;
        String name = getName(service, targetGroup);
        return subscriptions.get(name);
    }

    @Override
    public void update(String service, List<ServiceEndpoint> instances) {
        if (service != null && !service.isEmpty()) {
            String name = getName(service, serviceConfig.getGroup(service));
            Subscription subscription = subscriptions.get(name);
            if (subscription != null) {
                subscription.update(FRAMEWORK, null, new InstanceEvent(service, instances));
            }
        }
    }

    @Override
    public boolean isSubscribed(String service) {
        return service != null && !service.isEmpty() && subscriptions.containsKey(service);
    }

    @Override
    public void apply(InjectSource source) {
        source.add(Registry.COMPONENT_REGISTRY, this);
    }

    private static String getName(String service, String group) {
        if (group == null || group.isEmpty()) {
            return service;
        }
        return service + "@" + group;
    }

    private void startCluster(RegistryService registry) throws Exception {
        try {
            registry.start();
            logger.info("Success starting registry: {}", registry.getName());
        } catch (Exception e) {
            logger.error("Failed to start registry: {}", registry.getName(), e);
            throw e;
        }
    }

    /**
     * Called when the application is ready to start. This method iterates through all registered services and calls their register method.
     */
    private void onApplicationReady() {
        ready.set(true);
        for (Registration registration : registrations.values()) {
            registration.register();
        }
    }

    /**
     * Called when the application is stopping. This method iterates through all registered services and calls their stop method.
     */
    private void onApplicationStop() {
        ready.set(false);

        for (Registration registration : registrations.values()) {
            registration.stop();
        }
        registrations.clear();

        for (Subscription subscription : subscriptions.values()) {
            subscription.stop();
        }
        subscriptions.clear();
    }

    /**
     * Creates a new {@link Registration} object based on the provided service instance and registration action.
     *
     * @param name       The name of the registration.
     * @param instance   The service instance to be registered.
     * @param doRegister A {@link Callable} representing the registration action. If null, the existing registries are used.
     * @return A new {@link Registration} object containing the service instance, registries, publisher, registry configuration, and timer.
     */
    private Registration createRegistration(String name, ServiceInstance instance, Callable<Void> doRegister) {
        // violate
        List<RegistryService> clusters = registries;
        List<ClusterRegistration> values = new ArrayList<>(clusters == null ? 1 : clusters.size() + 1);
        if (doRegister != null) {
            values.add(new ClusterRegistration(new FrameworkRegistryService(doRegister)));
        }
        if (clusters != null) {
            for (RegistryService cluster : clusters) {
                values.add(new ClusterRegistration(cluster));
            }
        }
        return new Registration(name, instance, values, timer);
    }

    /**
     * Creates a subscription for the specified service and group by initializing a list of {@link ClusterOperation} instances
     * from the available {@link RegistryService} clusters. The subscription is then constructed with the service, group,
     * cluster registries, and a timer.
     *
     * @param service the name of the service to subscribe to.
     * @param group   the group to which the service belongs.
     * @return a new {@link Subscription} instance containing the service, group, cluster registries, and timer.
     */
    private Subscription createSubscription(String service, String group) {
        // violate
        List<RegistryService> clusters = registries;
        List<ClusterSubscription> values = new ArrayList<>(clusters == null ? 0 : clusters.size());
        if (clusters != null) {
            for (RegistryService cluster : clusters) {
                values.add(new ClusterSubscription(cluster));
            }
        }
        return new Subscription(service, group, values, timer);
    }

    /**
     * A private static class that represents a registration of a service instance with the registry.
     */
    private static class Registration {

        /**
         * The name of the registration.
         */
        private final String name;

        /**
         * The service instance being registered.
         */
        private final ServiceInstance instance;

        private final List<ClusterRegistration> clusters;

        /**
         * A timer used to schedule heartbeat and registration delays.
         */
        private final Timer timer;

        /**
         * An atomic boolean indicating whether the registration has been started.
         */
        private final AtomicBoolean started = new AtomicBoolean(true);

        /**
         * An atomic boolean indicating whether the registration has been completed.
         */
        private final AtomicBoolean registered = new AtomicBoolean(false);

        Registration(String name,
                     ServiceInstance instance,
                     List<ClusterRegistration> clusters,
                     Timer timer) {
            this.name = name;
            this.instance = instance;
            this.clusters = clusters;
            this.timer = timer;
        }

        /**
         * Registers the service instance with the registry.
         */
        public void register() {
            if (registered.compareAndSet(false, true)) {
                if (clusters != null && !clusters.isEmpty()) {
                    doRegister();
                } else {
                    throw new RegistryException("Registry center is not configured");
                }
            }
        }

        /**
         * Unregisters the service instance from the registry.
         */
        public void unregister() {
            if (registered.compareAndSet(true, false)) {
                doUnregister();
            }
        }

        /**
         * Stops the registration process.
         */
        public void stop() {
            started.set(false);
            unregister();
        }

        /**
         * Delays the register process by a random amount of time.
         */
        private void delayRegister() {
            long delay = 1000 + (long) (Math.random() * 2000.0);
            timer.delay("register-" + name, delay, this::doRegister);
        }

        /**
         * Performs the actual register of the service instance.
         */
        private void doRegister() {
            if (!started.get()) {
                return;
            }
            int counter = 0;
            int dones = 0;
            for (ClusterRegistration cluster : clusters) {
                if (cluster.getConfig().getMode().isRegister()) {
                    counter++;
                    if (!cluster.isDone()) {
                        String group = cluster.getGroup(instance.getGroup());
                        String name = getName(instance.getService(), group);
                        try {
                            cluster.register(instance.getService(), group, instance);
                            logger.info("Success registering instance {}:{} to {} at {}",
                                    instance.getHost(), instance.getPort(), name, cluster.getName());
                            dones++;
                        } catch (Exception e) {
                            logger.error("Failed to register instance {}:{} to {} at {}, caused by {}",
                                    instance.getHost(), instance.getPort(), name, cluster.getName(), e.getMessage(), e);
                        }
                    } else {
                        dones++;
                    }
                }
            }
            if (dones != counter) {
                delayRegister();
            }
        }

        /**
         * Performs the actual unregister of the service instance.
         */
        private void doUnregister() {
            for (ClusterRegistration cluster : clusters) {
                if (cluster.getConfig().getMode().isRegister() && cluster.isDone()) {
                    String group = cluster.getGroup(instance.getGroup());
                    String name = getName(instance.getService(), group);
                    try {
                        cluster.unregister(instance.getService(), group, instance);
                        logger.info("Success unregistering instance {}:{} to {} at {}",
                                instance.getHost(), instance.getPort(), name, cluster.getName());
                    } catch (Exception e) {
                        logger.error("Failed to unregister instance {}:{} to {} at {}, caused by {}",
                                instance.getHost(), instance.getPort(), name, cluster.getName(), e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * A private static class that represents a subscription to endpoint events for a specific service group.
     */
    private static class Subscription implements ServiceRegistry {

        /**
         * The service group that this subscription is for.
         */
        @Getter
        private final String service;

        @Getter
        private final String group;

        private final List<ClusterSubscription> clusters;

        /**
         * A timer used to schedule heartbeat and registration delays.
         */
        private final Timer timer;

        /**
         * An atomic boolean indicating whether the registration has been started.
         */
        private final AtomicBoolean started = new AtomicBoolean(true);

        /**
         * An atomic boolean indicating whether the registration has been completed.
         */
        private final AtomicBoolean subscribed = new AtomicBoolean(false);

        /**
         * The consumer that will receive endpoint events.
         */
        private final List<Consumer<InstanceEvent>> consumers = new CopyOnWriteArrayList<>();

        private final Map<String, List<ServiceEndpoint>> clustersEndpoints = new ConcurrentHashMap<>();

        /**
         * A map of endpoints for the service group, keyed by their addresses.
         */
        @Getter
        private volatile List<ServiceEndpoint> endpoints;

        private final Object mutex = new Object();

        Subscription(String service, String group, List<ClusterSubscription> clusters, Timer timer) {
            this.service = service;
            this.group = group;
            this.clusters = clusters;
            this.timer = timer;
        }

        /**
         * Adds a new consumer to the list of consumers that will receive endpoint events.
         *
         * @param consumer the consumer to add
         */
        public void addConsumer(Consumer<InstanceEvent> consumer) {
            if (consumer != null) {
                synchronized (mutex) {
                    if (!consumers.contains(consumer)) {
                        consumers.add(consumer);
                    }
                }
            }
        }

        /**
         * Subscribes to the service if it has not already been subscribed.
         */
        public void subscribe() {
            if (subscribed.compareAndSet(false, true)) {
                // allow clusters is empty.
                if (clusters != null && !clusters.isEmpty()) {
                    doSubscribe();
                }
            }
        }

        /**
         * Stops the registration process.
         */
        public void stop() {
            started.set(false);
            doUnsubscribe();
        }

        /**
         * Updates the list of endpoints for a specific service instance based on the provided {@link InstanceEvent}.
         *
         * @param clusterName  The name of the cluster or service instance being updated.
         * @param event The {@link InstanceEvent} containing the updated endpoints and their size.
         *              If the size is 0, the endpoints for the specified name are removed; otherwise, they are updated.
         */
        public void update(String clusterName, String serviceGroup, InstanceEvent event) {
            if (!started.get() || event == null) {
                return;
            }
            int size = event.size();
            String name = serviceGroup == null ? getName(service, group) : serviceGroup;
            logger.info("Service instance count is changed to {}, {} at {}", size, name, clusterName);
            synchronized (mutex) {
                if (!started.get()) {
                    return;
                }
                List<ServiceEndpoint> ce = size == 0 ? clustersEndpoints.remove(clusterName) : clustersEndpoints.put(clusterName, event.getInstances());
                int capacity = endpoints == null ? 0 : endpoints.size();
                capacity = capacity + size - (ce == null ? 0 : ce.size());
                Map<String, ServiceEndpoint> merged = new HashMap<>(capacity);
                clustersEndpoints.forEach((cluster, endpoints) -> {
                    if (endpoints != null) {
                        endpoints.forEach(endpoint -> merged.put(endpoint.getAddress(), endpoint));
                    }
                });
                List<ServiceEndpoint> newEndpoints = new ArrayList<>(merged.values());
                this.endpoints = newEndpoints;
                for (Consumer<InstanceEvent> consumer : consumers) {
                    consumer.accept(new InstanceEvent(service, newEndpoints));
                }
            }
        }

        /**
         * Delays the register process by a random amount of time.
         */
        private void delaySubscribe() {
            long delay = 1000 + (long) (Math.random() * 2000.0);
            timer.delay("subscribe-" + service, delay, this::doSubscribe);
        }

        /**
         * Performs the actual subscription of the service.
         */
        private void doSubscribe() {
            if (!started.get()) {
                return;
            }
            int counter = 0;
            int dones = 0;
            for (ClusterSubscription cluster : clusters) {
                if (cluster.getConfig().getMode().isSubscribe()) {
                    counter++;
                    if (!cluster.isDone()) {
                        String group = cluster.getGroup(this.group);
                        String name = getName(service, group);
                        try {
                            cluster.subscribe(service, group, e -> update(cluster.getName(), name, e));
                            logger.info("Success subscribing {} at {}", name, cluster.getName());
                            dones++;
                        } catch (Exception e) {
                            logger.error("Failed to subscribe {} at {}, caused by {}", name, cluster.getName(), e.getMessage(), e);
                        }
                    } else {
                        dones++;
                    }
                }
            }
            if (dones != counter) {
                delaySubscribe();
            }
        }

        /**
         * Performs the actual unsubscription of the service.
         */
        private void doUnsubscribe() {
            for (ClusterSubscription cluster : clusters) {
                if (cluster.getConfig().getMode().isSubscribe() && cluster.isDone()) {
                    String group = cluster.getGroup(this.group);
                    String name = getName(service, group);
                    try {
                        cluster.unsubscribe(service, group);
                        logger.info("Success unsubscribing {} at {}", name, cluster.getName());
                    } catch (Exception e) {
                        logger.error("Failed to unsubscribe {} at {}, caused by {}", name, cluster.getName(), e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * Abstract base class for cluster operations. Provides common functionality for managing
     * operations on a {@link RegistryService} cluster, including retry logic and completion status.
     */
    private abstract static class ClusterOperation {

        @Getter
        protected final RegistryService cluster;

        protected final AtomicBoolean done = new AtomicBoolean(false);

        protected final AtomicLong retry = new AtomicLong(0);

        ClusterOperation(RegistryService cluster) {
            this.cluster = cluster;
        }

        public String getName() {
            return cluster.getName();
        }

        public String getGroup(String defaultGroup) {
            RegistryClusterConfig config = cluster.getConfig();
            return config == null ? defaultGroup : config.getGroup(defaultGroup);
        }

        public boolean isDone() {
            return done.get();
        }

        public void setDone(boolean done) {
            this.done.set(done);
        }

        public long getRetry() {
            return retry.get();
        }

        public void addRetry() {
            retry.incrementAndGet();
        }

        public RegistryClusterConfig getConfig() {
            return cluster.getConfig();
        }

    }

    /**
     * Handles registration and unregistration of service instances with a {@link RegistryService} cluster.
     * Extends {@link ClusterOperation} to manage the lifecycle of service instances.
     */
    private static class ClusterRegistration extends ClusterOperation {

        ClusterRegistration(RegistryService cluster) {
            super(cluster);
        }

        /**
         * Registers a service instance with the registry.
         *
         * @param instance The service instance to be registered.
         */
        public void register(String service, String group, ServiceInstance instance) throws Exception {
            cluster.register(service, group, instance);
            done.set(true);
        }

        /**
         * Unregisters a service instance from the registry.
         *
         * @param instance The service instance to be unregistered.
         */
        public void unregister(String service, String group, ServiceInstance instance) throws Exception {
            cluster.unregister(service, group, instance);
            done.set(false);
        }
    }

    /**
     * Handles subscription and unsubscription to endpoint events for a {@link RegistryService} cluster.
     * Extends {@link ClusterOperation} to manage event listening for specific services.
     */
    private static class ClusterSubscription extends ClusterOperation {

        ClusterSubscription(RegistryService cluster) {
            super(cluster);
        }

        /**
         * Subscribes to endpoint events for a specific service and group.
         *
         * @param service  The service name to subscribe to.
         * @param group    The group associated with the service.
         * @param consumer The consumer to handle endpoint events.
         * @throws Exception if the subscription fails.
         */
        public void subscribe(String service, String group, Consumer<InstanceEvent> consumer) throws Exception {
            cluster.subscribe(service, group, consumer);
            done.set(true);
        }

        /**
         * Unsubscribes from endpoint events for a specific service and group.
         *
         * @param service The service name to unsubscribe from.
         * @param group   The group associated with the service.
         * @throws Exception if the unsubscription fails.
         */
        public void unsubscribe(String service, String group) throws Exception {
            cluster.unsubscribe(service, group);
            done.set(false);
        }
    }

    /**
     * A specialized implementation of {@link AbstractRegistryService} that performs registration
     * using a provided callback. This class is designed to handle framework-specific registration logic.
     */
    private static class FrameworkRegistryService extends AbstractRegistryService {

        private final Callable<Void> callback;

        private final RegistryClusterConfig config = new RegistryClusterConfig();

        FrameworkRegistryService(Callable<Void> callback) {
            this.callback = callback;
            config.setMode(RegistryMode.FULL);
        }

        @Override
        public String getName() {
            return FRAMEWORK;
        }

        @Override
        public void register(String service, String group, ServiceInstance instance) throws Exception {
            callback.call();
        }

        @Override
        public RegistryClusterConfig getConfig() {
            return config;
        }
    }
}

