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

import java.util.function.Supplier;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;

import com.holonplatform.auth.Authentication;
import com.holonplatform.auth.Authorizer;
import com.holonplatform.auth.Permission;
import com.holonplatform.auth.Realm;
import com.holonplatform.core.internal.utils.ObjectUtils;
import com.holonplatform.spring.security.SpringSecurity;

/**
 * A Spring Security {@link AuthorizationManager} implementation which delegates the actual authorization decision to
 * a Holon platform {@link Authorizer} (for example a {@link Realm}, which extends {@link Authorizer}), allowing the
 * Holon {@link Permission} model to drive Spring Security authorization rules, instead of having to duplicate the
 * permission set using Spring Security's native {@code hasAuthority(...)} / {@code hasRole(...)} / SpEL expressions.
 * <p>
 * Since this class ignores the generic authorization {@code context} parameter and only relies on the current Spring
 * Security {@link org.springframework.security.core.Authentication}, a single instance can be used both for HTTP
 * request authorization (i.e. as a {@link org.springframework.security.web.access.intercept.RequestAuthorizationContext}
 * typed {@link AuthorizationManager}, using {@code .access(...)} within {@code authorizeHttpRequests(...)}) and for
 * method security (i.e. as a {@code MethodInvocation} typed {@link AuthorizationManager}, when using
 * {@code @EnableMethodSecurity} custom authorization managers).
 * </p>
 * <p>
 * Usage example, checking that the current user has the <code>"admin"</code> permission for a set of HTTP requests:
 * </p>
 * 
 * <pre>
 * {@code http.authorizeHttpRequests(authorize -> authorize
 *     .requestMatchers("/admin/**").access(HolonAuthorizationManager.hasPermission(realm, "admin"))
 *     .anyRequest().authenticated());}
 * </pre>
 * 
 * @param <T> Authorization context type
 * 
 * @since 10.0.0
 * 
 * @see SpringSecurityFilterChainSupport
 */
public final class HolonAuthorizationManager<T> implements AuthorizationManager<T> {

	/**
	 * Permission matching strategy.
	 */
	public enum PermissionMatch {

		/**
		 * All the specified permissions are required to be granted.
		 */
		ALL,

		/**
		 * At least one of the specified permissions is required to be granted.
		 */
		ANY;

	}

	private final Authorizer<Permission> authorizer;
	private final PermissionMatch match;
	private final String[] permissions;

	private HolonAuthorizationManager(Authorizer<Permission> authorizer, PermissionMatch match,
			String... permissions) {
		super();
		ObjectUtils.argumentNotNull(authorizer, "Authorizer must be not null");
		ObjectUtils.argumentNotNull(match, "Permission match strategy must be not null");
		ObjectUtils.argumentNotNull(permissions, "Permissions must be not null");
		if (permissions.length == 0) {
			throw new IllegalArgumentException("At least one permission must be provided");
		}
		this.authorizer = authorizer;
		this.match = match;
		this.permissions = permissions;
	}

	/**
	 * Create a {@link HolonAuthorizationManager} which grants access only if the current {@link Authentication} has
	 * <em>all</em> the given permissions, according to given {@link Authorizer}.
	 * @param <T> Authorization context type
	 * @param authorizer The {@link Authorizer} to use to check the permissions (not null), for example a
	 *        {@link Realm}
	 * @param permissions The permission/s to check (not null, at least one required)
	 * @return A new {@link HolonAuthorizationManager}
	 */
	public static <T> HolonAuthorizationManager<T> hasPermission(Authorizer<Permission> authorizer,
			String... permissions) {
		return new HolonAuthorizationManager<>(authorizer, PermissionMatch.ALL, permissions);
	}

	/**
	 * Create a {@link HolonAuthorizationManager} which grants access if the current {@link Authentication} has
	 * <em>any</em> of the given permissions, according to given {@link Authorizer}.
	 * @param <T> Authorization context type
	 * @param authorizer The {@link Authorizer} to use to check the permissions (not null), for example a
	 *        {@link Realm}
	 * @param permissions The permission/s to check (not null, at least one required)
	 * @return A new {@link HolonAuthorizationManager}
	 */
	public static <T> HolonAuthorizationManager<T> hasAnyPermission(Authorizer<Permission> authorizer,
			String... permissions) {
		return new HolonAuthorizationManager<>(authorizer, PermissionMatch.ANY, permissions);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.security.authorization.AuthorizationManager#authorize(java.util.function.Supplier,
	 * java.lang.Object)
	 */
	@Override
	public AuthorizationResult authorize(Supplier<? extends org.springframework.security.core.Authentication> authentication,
			T context) {
		return new AuthorizationDecision(isGranted(authentication.get()));
	}

	/**
	 * Check whether given Spring Security {@link org.springframework.security.core.Authentication} is granted the
	 * required permission/s, according to the configured {@link Authorizer} and {@link PermissionMatch} strategy.
	 * @param springAuthentication The Spring Security Authentication to check
	 * @return <code>true</code> if the required permission/s are granted, <code>false</code> otherwise (including
	 *         when given Authentication is <code>null</code> or not authenticated)
	 */
	private boolean isGranted(org.springframework.security.core.Authentication springAuthentication) {
		if (springAuthentication == null || !springAuthentication.isAuthenticated()) {
			return false;
		}
		final Authentication holonAuthentication = SpringSecurity.asAuthentication(springAuthentication);
		return (match == PermissionMatch.ALL) ? authorizer.isPermitted(holonAuthentication, permissions)
				: authorizer.isPermittedAny(holonAuthentication, permissions);
	}

}
