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
package com.holonplatform.spring.security.observability.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import com.holonplatform.spring.security.observability.ObservedAuthenticationProvider;

import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;

/**
 * Test the {@link ObservedAuthenticationProvider} authentication observability decorator.
 */
class TestObservedAuthenticationProvider {

	private static AuthenticationProvider provider(boolean succeed) {
		return new AuthenticationProvider() {

			@Override
			public Authentication authenticate(Authentication authentication) throws AuthenticationException {
				if (!succeed) {
					throw new BadCredentialsException("Invalid credentials");
				}
				return new UsernamePasswordAuthenticationToken(authentication.getName(), null);
			}

			@Override
			public boolean supports(Class<?> authentication) {
				return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
			}
		};
	}

	@Test
	void testSuccessfulAuthenticationIsObserved() {
		TestObservationRegistry registry = TestObservationRegistry.create();
		ObservedAuthenticationProvider observed = new ObservedAuthenticationProvider(provider(true), registry);

		Authentication result = observed.authenticate(new UsernamePasswordAuthenticationToken("u1", "pwd"));
		assertEquals("u1", result.getName());

		TestObservationRegistryAssert.assertThat(registry)
				.hasObservationWithNameEqualTo("holon.security.authentication").that()
				.hasLowCardinalityKeyValue("holon.security.authentication.outcome", "success")
				.hasLowCardinalityKeyValue("holon.security.authentication.failure", "none");
	}

	@Test
	void testFailedAuthenticationIsObserved() {
		TestObservationRegistry registry = TestObservationRegistry.create();
		ObservedAuthenticationProvider observed = new ObservedAuthenticationProvider(provider(false), registry);

		assertThrows(BadCredentialsException.class,
				() -> observed.authenticate(new UsernamePasswordAuthenticationToken("u1", "wrong")));

		TestObservationRegistryAssert.assertThat(registry)
				.hasObservationWithNameEqualTo("holon.security.authentication").that()
				.hasLowCardinalityKeyValue("holon.security.authentication.outcome", "failure")
				.hasLowCardinalityKeyValue("holon.security.authentication.failure", "BadCredentialsException");
	}

	@Test
	void testSupportsDelegation() {
		TestObservationRegistry registry = TestObservationRegistry.create();
		ObservedAuthenticationProvider observed = new ObservedAuthenticationProvider(provider(true), registry);
		assertTrue(observed.supports(UsernamePasswordAuthenticationToken.class));
	}

}
