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
package com.holonplatform.spring.security.config;

import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;

import com.holonplatform.auth.Authorizer;
import com.holonplatform.auth.Permission;
import com.holonplatform.core.internal.utils.ObjectUtils;

/**
 * Support class to build a Spring Security {@link MethodSecurityExpressionHandler} which uses a Holon platform
 * {@link Authorizer} (for example a {@link com.holonplatform.auth.Realm}) to evaluate {@code hasPermission(...)}
 * expressions in {@code @PreAuthorize} / {@code @PostAuthorize} / {@code @PreFilter} / {@code @PostFilter} method
 * security annotations, in addition to the standard, authority-based {@code hasAuthority(...)} /
 * {@code hasRole(...)} expressions which already work out-of-the-box since a Holon {@link Permission} is exposed
 * to Spring Security as a {@link org.springframework.security.core.GrantedAuthority}.
 * <p>
 * Typical usage, combined with Spring Security's {@code @EnableMethodSecurity} annotation:
 * </p>
 * 
 * <pre>
 * &#64;Configuration
 * &#64;EnableMethodSecurity
 * class MethodSecurityConfig {
 * 
 * 	&#64;Bean
 * 	MethodSecurityExpressionHandler methodSecurityExpressionHandler(Realm realm) {
 * 		return HolonMethodSecuritySupport.methodSecurityExpressionHandler(realm);
 * 	}
 * 
 * }
 * </pre>
 * 
 * <p>
 * Once configured, methods can be annotated for example as:
 * </p>
 * 
 * <pre>
 * &#64;PreAuthorize("hasPermission(#orderId, 'Order', 'view')")
 * Order findOrder(String orderId) { ... }
 * </pre>
 * 
 * @since 10.0.0
 * 
 * @see HolonPermissionEvaluator
 */
public final class HolonMethodSecuritySupport {

	private HolonMethodSecuritySupport() {
	}

	/**
	 * Build a {@link MethodSecurityExpressionHandler} which uses the given {@link Authorizer} to evaluate
	 * {@code hasPermission(...)} method security expressions.
	 * @param authorizer The {@link Authorizer} to use (not null)
	 * @return A new {@link MethodSecurityExpressionHandler} instance
	 */
	public static MethodSecurityExpressionHandler methodSecurityExpressionHandler(Authorizer<Permission> authorizer) {
		ObjectUtils.argumentNotNull(authorizer, "Authorizer must be not null");
		final DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
		handler.setPermissionEvaluator(new HolonPermissionEvaluator(authorizer));
		return handler;
	}

}
