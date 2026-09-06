/*
 * Copyright 2016-2017 Axioma srl.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.holonplatform.spring.boot;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

/**
 * Spring Boot auto-configuration class to expose a virtual-thread-backed {@link AsyncTaskExecutor} bean when
 * virtual threads support is enabled through the standard {@code spring.threads.virtual.enabled} configuration
 * property (and the running JVM supports virtual threads).
 * <p>
 * This auto-configuration does not change any default behavior: it only activates when virtual threads are
 * explicitly enabled by the application (see {@link ConditionalOnThreading}), and it backs off if the application
 * already defines its own bean with the same name.
 * </p>
 * <p>
 * The exposed {@link AsyncTaskExecutor} can be used by any Holon platform component or application code which
 * needs to execute tasks using Java virtual threads, without requiring any additional configuration.
 * </p>
 * <p>
 * Java's {@code ThreadLocal}-based propagation (used by Spring Security's default
 * {@code SecurityContextHolder} strategy) does <b>not</b> automatically flow from a caller thread into a task
 * submitted to a virtual thread executor: each submitted task runs on its own (virtual) thread and starts with an
 * empty {@code SecurityContext}, which silently breaks {@code Authentication}-dependent code executed
 * asynchronously. When Spring Security is present on the classpath, this auto-configuration therefore wraps the
 * exposed executor with a {@link DelegatingSecurityContextAsyncTaskExecutor}, which captures the calling thread's
 * {@link SecurityContext} at submission time and installs it for the duration of the task on the virtual thread,
 * clearing it afterwards. No additional configuration is required to benefit from this propagation: it is enough
 * to have {@code spring-security-core} on the classpath.
 * </p>
 * 
 * @since 10.0.0
 */
@AutoConfiguration
@ConditionalOnThreading(Threading.VIRTUAL)
public class VirtualThreadsAutoConfiguration {

	/**
	 * Bean name of the virtual thread {@link AsyncTaskExecutor} provided by this auto-configuration.
	 */
	public static final String HOLON_VIRTUAL_THREAD_EXECUTOR_BEAN_NAME = "holonVirtualThreadAsyncTaskExecutor";

	/**
	 * Configuration used when Spring Security is <em>not</em> available on the classpath: exposes a plain,
	 * security-context-unaware virtual thread executor.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("org.springframework.security.core.context.SecurityContext")
	static class PlainVirtualThreadExecutorConfiguration {

		@Bean(name = HOLON_VIRTUAL_THREAD_EXECUTOR_BEAN_NAME)
		@ConditionalOnMissingBean(name = HOLON_VIRTUAL_THREAD_EXECUTOR_BEAN_NAME)
		AsyncTaskExecutor holonVirtualThreadAsyncTaskExecutor() {
			return new VirtualThreadTaskExecutor("holon-vt-");
		}

	}

	/**
	 * Configuration used when Spring Security is available on the classpath: exposes a virtual thread executor
	 * which automatically propagates the calling thread's {@link SecurityContext} to submitted tasks.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(SecurityContext.class)
	static class SecurityContextPropagatingVirtualThreadExecutorConfiguration {

		@Bean(name = HOLON_VIRTUAL_THREAD_EXECUTOR_BEAN_NAME)
		@ConditionalOnMissingBean(name = HOLON_VIRTUAL_THREAD_EXECUTOR_BEAN_NAME)
		AsyncTaskExecutor holonVirtualThreadAsyncTaskExecutor() {
			return new DelegatingSecurityContextAsyncTaskExecutor(new VirtualThreadTaskExecutor("holon-vt-"));
		}

	}

}
