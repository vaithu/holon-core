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
package com.holonplatform.spring.boot.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import com.holonplatform.spring.boot.VirtualThreadsAutoConfiguration;

@SpringBootTest(properties = "spring.threads.virtual.enabled=true")
class TestVirtualThreadsAutoConfiguration {

	@Configuration
	@EnableAutoConfiguration
	protected static class Config {

	}

	@Autowired(required = false)
	private AsyncTaskExecutor holonVirtualThreadAsyncTaskExecutor;

	@Test
	void testVirtualThreadExecutorBean() {
		assertNotNull(holonVirtualThreadAsyncTaskExecutor);
		// Spring Security is available on the classpath: the executor must propagate the SecurityContext
		assertTrue(holonVirtualThreadAsyncTaskExecutor instanceof DelegatingSecurityContextAsyncTaskExecutor);
	}

	@Test
	void testSecurityContextPropagation() throws InterruptedException {
		final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken("user",
				"pwd");
		SecurityContextHolder.getContext().setAuthentication(authentication);
		try {
			final AtomicReference<Object> capturedAuthentication = new AtomicReference<>();
			final CountDownLatch latch = new CountDownLatch(1);
			holonVirtualThreadAsyncTaskExecutor.execute(() -> {
				capturedAuthentication.set(SecurityContextHolder.getContext().getAuthentication());
				latch.countDown();
			});
			assertTrue(latch.await(5, TimeUnit.SECONDS));
			assertEquals(authentication, capturedAuthentication.get());
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

}
