/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.deploy;

import com.google.inject.Binder;
import com.google.inject.Module;
import io.micrometer.core.instrument.Tags;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.opentracing.Tracer;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.che.api.deploy.jsonrpc.CheMajorWebSocketEndpointConfiguration;
import org.eclipse.che.api.deploy.jsonrpc.CheMajorWebSocketEndpointExecutorServiceProvider;
import org.eclipse.che.api.deploy.jsonrpc.CheMinorWebSocketEndpointConfiguration;
import org.eclipse.che.api.deploy.jsonrpc.CheMinorWebSocketEndpointExecutorServiceProvider;
import org.eclipse.che.api.workspace.server.WorkspaceSharedPool;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.commons.lang.concurrent.ThreadLocalPropagateContext;
import org.eclipse.che.core.metrics.ExecutorServiceMetrics;
import org.eclipse.che.workspace.infrastructure.kubernetes.util.KubernetesSharedPool;

/**
 * {@link Module} that provides metered implementation for different classes. Metrics will be
 * published to {@link PrometheusMeterRegistry}.
 */
public class MetricsOverrideBinding implements Module {
  @Override
  public void configure(Binder binder) {
    binder
        .bind(CheMajorWebSocketEndpointExecutorServiceProvider.class)
        .to(MeteredCheMajorWebSocketEndpointExecutorServiceProvider.class);

    binder
        .bind(CheMinorWebSocketEndpointExecutorServiceProvider.class)
        .to(MeteredCheMinorWebSocketEndpointExecutorServiceProvider.class);
    System.out.println("11111");
    binder.bind(KubernetesSharedPool.class).to(MeteredKubernetesSharedPool.class);
    binder.bind(WorkspaceSharedPool.class).to(MeteredWorkspaceSharedPool.class);
    System.out.println("2222");
  }

  public static class MeteredWorkspaceSharedPool extends WorkspaceSharedPool {
    private final PrometheusMeterRegistry meterRegistry;

    private ExecutorService executorService;

    @Inject
    public MeteredWorkspaceSharedPool(
        @Named("che.workspace.pool.type") String poolType,
        @Named("che.workspace.pool.exact_size") @Nullable String exactSizeProp,
        @Named("che.workspace.pool.cores_multiplier") @Nullable String coresMultiplierProp,
        Tracer tracer,
        PrometheusMeterRegistry meterRegistry) {
      super(poolType, exactSizeProp, coresMultiplierProp, tracer);
      this.meterRegistry = meterRegistry;
    }

    @Override
    public ExecutorService getExecutor() {
      if (executorService == null) {
        executorService =
            ExecutorServiceMetrics.monitor(
                meterRegistry, super.getExecutor(), "WorkspaceSharedPool", Tags.empty());
      }
      return executorService;
    }

    @Override
    public void execute(Runnable runnable) {
      super.execute(runnable);
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
      return getExecutor().submit(callable);
    }

    @Override
    public CompletableFuture<Void> runAsync(Runnable runnable) {
      return CompletableFuture.runAsync(ThreadLocalPropagateContext.wrap(runnable), getExecutor());
    }
  }

  public static class MeteredKubernetesSharedPool extends KubernetesSharedPool {
    private final PrometheusMeterRegistry meterRegistry;

    private ExecutorService executorService;

    @Inject
    public MeteredKubernetesSharedPool(PrometheusMeterRegistry meterRegistry) {
      super();
      this.meterRegistry = meterRegistry;
    }

    @Override
    public ExecutorService getExecutor() {
      if (executorService == null) {
        executorService =
            ExecutorServiceMetrics.monitor(
                meterRegistry, super.getExecutor(), "KubernetesMachineSharedPool", Tags.empty());
      }
      return executorService;
    }
  }

  @Singleton
  public static class MeteredCheMajorWebSocketEndpointExecutorServiceProvider
      extends CheMajorWebSocketEndpointExecutorServiceProvider {

    private final PrometheusMeterRegistry meterRegistry;

    private ExecutorService executorService;

    @Inject
    public MeteredCheMajorWebSocketEndpointExecutorServiceProvider(
        @Named(JSON_RPC_MAJOR_CORE_POOL_SIZE_PARAMETER_NAME) int corePoolSize,
        @Named(JSON_RPC_MAJOR_MAX_POOL_SIZE_PARAMETER_NAME) int maxPoolSize,
        @Named(JSON_RPC_MAJOR_QUEUE_CAPACITY_PARAMETER_NAME) int queueCapacity,
        PrometheusMeterRegistry meterRegistry) {
      super(corePoolSize, maxPoolSize, queueCapacity);
      this.meterRegistry = meterRegistry;
    }

    @Override
    public synchronized ExecutorService get() {
      if (executorService == null) {
        executorService =
            ExecutorServiceMetrics.monitor(
                meterRegistry,
                super.get(),
                CheMajorWebSocketEndpointConfiguration.EXECUTOR_NAME,
                Tags.empty());
      }
      return executorService;
    }
  }

  @Singleton
  public static class MeteredCheMinorWebSocketEndpointExecutorServiceProvider
      extends CheMinorWebSocketEndpointExecutorServiceProvider {
    private final PrometheusMeterRegistry meterRegistry;

    private ExecutorService executorService;

    @Inject
    public MeteredCheMinorWebSocketEndpointExecutorServiceProvider(
        @Named(JSON_RPC_MINOR_CORE_POOL_SIZE_PARAMETER_NAME) int corePoolSize,
        @Named(JSON_RPC_MINOR_MAX_POOL_SIZE_PARAMETER_NAME) int maxPoolSize,
        @Named(JSON_RPC_MINOR_QUEUE_CAPACITY_PARAMETER_NAME) int queueCapacity,
        PrometheusMeterRegistry meterRegistry) {
      super(corePoolSize, maxPoolSize, queueCapacity);
      this.meterRegistry = meterRegistry;
    }

    @Override
    public synchronized ExecutorService get() {
      if (executorService == null) {

        executorService =
            ExecutorServiceMetrics.monitor(
                meterRegistry,
                super.get(),
                CheMinorWebSocketEndpointConfiguration.EXECUTOR_NAME,
                Tags.empty());
      }
      return executorService;
    }
  }
}
