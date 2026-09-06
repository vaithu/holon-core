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
package com.holonplatform.spring.security.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;

import com.holonplatform.auth.Account;
import com.holonplatform.auth.Account.AccountProvider;
import com.holonplatform.auth.Authorizer;
import com.holonplatform.auth.Credentials;
import com.holonplatform.auth.Permission;
import com.holonplatform.auth.token.AccountCredentialsToken;
import com.holonplatform.spring.security.SpringSecurity;
import com.holonplatform.spring.security.config.HolonAuthorizationManager;

/**
 * Test the {@link HolonAuthorizationManager} bridge between Spring Security {@link AuthorizationResult} and the
 * Holon platform {@link Authorizer} / {@link Permission} model.
 */
class TestHolonAuthorizationManager {

	private static AccountProvider accountProvider() {
		return id -> {
			if ("u1".equals(id)) {
				return Optional.of(Account.builder(id).credentials(Credentials.builder().secret(id).build())
						.withPermission("view").build());
			}
			if ("u2".equals(id)) {
				return Optional.of(Account.builder(id).credentials(Credentials.builder().secret(id).build())
						.withPermission("view").withPermission("manage").build());
			}
			return Optional.empty();
		};
	}

	private static Authentication authenticate(String username, String password) throws Exception {
		AuthenticationManager authenticationManager = new ProviderManager(
				SpringSecurity.authenticationProvider(Account.authenticator(accountProvider()),
						UsernamePasswordAuthenticationToken.class, upt -> AccountCredentialsToken
								.create(upt.getPrincipal().toString(), upt.getCredentials().toString())));
		return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
	}

	@Test
	void testHasPermissionGranted() throws Exception {
		Authentication authentication = authenticate("u1", "u1");
		HolonAuthorizationManager<Object> manager = HolonAuthorizationManager.hasPermission(Authorizer.create(),
				"view");
		AuthorizationResult result = manager.authorize(() -> authentication, new Object());
		assertTrue(result.isGranted());
	}

	@Test
	void testHasPermissionDenied() throws Exception {
		Authentication authentication = authenticate("u1", "u1");
		HolonAuthorizationManager<Object> manager = HolonAuthorizationManager.hasPermission(Authorizer.create(),
				"manage");
		AuthorizationResult result = manager.authorize(() -> authentication, new Object());
		assertFalse(result.isGranted());
	}

	@Test
	void testHasAllPermissionsRequiresBoth() throws Exception {
		Authentication authentication = authenticate("u2", "u2");
		HolonAuthorizationManager<Object> manager = HolonAuthorizationManager.hasPermission(Authorizer.create(),
				"view", "manage");
		assertTrue(manager.authorize(() -> authentication, new Object()).isGranted());

		Authentication u1Authentication = authenticate("u1", "u1");
		assertFalse(manager.authorize(() -> u1Authentication, new Object()).isGranted());
	}

	@Test
	void testHasAnyPermission() throws Exception {
		Authentication authentication = authenticate("u1", "u1");
		HolonAuthorizationManager<Object> manager = HolonAuthorizationManager.hasAnyPermission(Authorizer.create(),
				"manage", "view");
		assertTrue(manager.authorize(() -> authentication, new Object()).isGranted());
	}

	@Test
	void testNullAuthenticationIsDenied() {
		HolonAuthorizationManager<Object> manager = HolonAuthorizationManager.hasPermission(Authorizer.create(),
				"view");
		AuthorizationResult result = manager.authorize(() -> null, new Object());
		assertFalse(result.isGranted());
	}

}
