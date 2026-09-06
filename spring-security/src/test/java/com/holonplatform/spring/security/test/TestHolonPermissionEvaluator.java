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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.holonplatform.auth.Account;
import com.holonplatform.auth.Account.AccountProvider;
import com.holonplatform.auth.Authorizer;
import com.holonplatform.auth.Credentials;
import com.holonplatform.auth.Permission;
import com.holonplatform.auth.token.AccountCredentialsToken;
import com.holonplatform.spring.security.SpringSecurity;
import com.holonplatform.spring.security.config.HolonMethodSecuritySupport;
import com.holonplatform.spring.security.config.HolonPermissionEvaluator;

/**
 * Test the {@link HolonPermissionEvaluator} / {@link HolonMethodSecuritySupport} method security integration.
 */
class TestHolonPermissionEvaluator {

	private static AccountProvider accountProvider() {
		return id -> {
			if ("u1".equals(id)) {
				return Optional.of(Account.builder(id).credentials(Credentials.builder().secret(id).build())
						.withPermission("view").withPermission("Order:view").build());
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
	void testHasPermissionWithoutTargetType() throws Exception {
		Authentication authentication = authenticate("u1", "u1");
		HolonPermissionEvaluator evaluator = new HolonPermissionEvaluator(Authorizer.create());

		assertTrue(evaluator.hasPermission(authentication, null, "view"));
		assertFalse(evaluator.hasPermission(authentication, null, "manage"));
	}

	@Test
	void testHasPermissionWithTargetTypeConvention() throws Exception {
		Authentication authentication = authenticate("u1", "u1");
		HolonPermissionEvaluator evaluator = new HolonPermissionEvaluator(Authorizer.create());

		// 4-args overload: target id + target type
		assertTrue(evaluator.hasPermission(authentication, "1", "Order", "view"));
		// 3-args overload: target domain object simple class name is used as target type
		assertTrue(evaluator.hasPermission(authentication, new Order(), "view"));
		assertFalse(evaluator.hasPermission(authentication, "1", "Invoice", "view"));
	}

	@Test
	void testNullAuthenticationOrPermissionIsDenied() {
		HolonPermissionEvaluator evaluator = new HolonPermissionEvaluator(Authorizer.create());
		assertFalse(evaluator.hasPermission(null, "target", "view"));
		assertFalse(evaluator.hasPermission(null, "1", "Order", "view"));
	}

	@Test
	void testMethodSecurityExpressionHandler() {
		MethodSecurityExpressionHandler handler = HolonMethodSecuritySupport
				.methodSecurityExpressionHandler(Authorizer.create());
		assertNotNull(handler);
	}

	static class Order {
	}

}
