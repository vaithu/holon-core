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

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import com.holonplatform.auth.Authenticator;
import com.holonplatform.auth.token.AccountCredentialsToken;
import com.holonplatform.core.internal.utils.ObjectUtils;
import com.holonplatform.spring.security.SpringSecurity;

/**
 * Support class to build a Spring Security {@link SecurityFilterChain} bean using the Spring Security 7 lambda DSL
 * ({@link HttpSecurity}), wiring a Holon platform {@link Authenticator} as the concrete authentication source
 * through an adapted {@link AuthenticationProvider}.
 * <p>
 * This class requires the {@code spring-security-config} and {@code spring-security-web} artifacts to be available
 * on the classpath (both provided, for example, by the {@code spring-boot-starter-security} dependency). These are
 * declared as <em>optional</em> dependencies of the {@code holon-spring-security} artifact, so this support class is
 * only usable, and only needs to be used, when such a Spring Security web/config stack is present.
 * </p>
 * <p>
 * Usage example, using a Holon {@link Authenticator} for the default {@link AccountCredentialsToken} (username /
 * password) authentication token type:
 * </p>
 * 
 * <pre>
 * {@code @Configuration
 * class SecurityConfig {
 *
 *   @Bean
 *   SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
 *     Authenticator<AccountCredentialsToken> authenticator = Account.authenticator(accountProvider);
 *     return SpringSecurityFilterChainSupport.configure(http, authenticator,
 *         authorize -> authorize.requestMatchers("/public/**").permitAll().anyRequest().authenticated());
 *   }
 *
 * }}
 * </pre>
 * 
 * @since 10.0.0
 */
public final class SpringSecurityFilterChainSupport {

	private SpringSecurityFilterChainSupport() {
	}

	/**
	 * Configure given {@link HttpSecurity} using the lambda DSL, wiring an {@link AuthenticationProvider} built from
	 * given Holon {@link Authenticator} (using {@link UsernamePasswordAuthenticationToken} / HTTP Basic
	 * authentication as concrete authentication mechanism), and requiring authentication for any request.
	 * @param http The {@link HttpSecurity} to configure (not null)
	 * @param authenticator The Holon {@link Authenticator} to use as authentication source, supporting the default
	 *        {@link AccountCredentialsToken} authentication token type (not null)
	 * @return The built {@link SecurityFilterChain}
	 * @throws Exception If an error occurred building the {@link SecurityFilterChain}
	 * @see #configure(HttpSecurity, Authenticator, Customizer)
	 */
	public static SecurityFilterChain configure(HttpSecurity http, Authenticator<AccountCredentialsToken> authenticator)
			throws Exception {
		return configure(http, authenticator, authorize -> authorize.anyRequest().authenticated());
	}

	/**
	 * Configure given {@link HttpSecurity} using the lambda DSL, wiring an {@link AuthenticationProvider} built from
	 * given Holon {@link Authenticator} (using {@link UsernamePasswordAuthenticationToken} / HTTP Basic
	 * authentication as concrete authentication mechanism).
	 * @param http The {@link HttpSecurity} to configure (not null)
	 * @param authenticator The Holon {@link Authenticator} to use as authentication source, supporting the default
	 *        {@link AccountCredentialsToken} authentication token type (not null)
	 * @param authorizeRequestsCustomizer Customizer to configure request authorization rules (not null)
	 * @return The built {@link SecurityFilterChain}
	 * @throws Exception If an error occurred building the {@link SecurityFilterChain}
	 */
	public static SecurityFilterChain configure(HttpSecurity http, Authenticator<AccountCredentialsToken> authenticator,
			Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> authorizeRequestsCustomizer)
			throws Exception {
		ObjectUtils.argumentNotNull(http, "HttpSecurity must be not null");
		ObjectUtils.argumentNotNull(authenticator, "Authenticator must be not null");
		ObjectUtils.argumentNotNull(authorizeRequestsCustomizer, "Authorize requests customizer must be not null");

		final AuthenticationProvider authenticationProvider = SpringSecurity.authenticationProvider(authenticator,
				UsernamePasswordAuthenticationToken.class,
				upt -> AccountCredentialsToken.create(String.valueOf(upt.getPrincipal()),
						String.valueOf(upt.getCredentials())));

		http.authenticationProvider(authenticationProvider).authorizeHttpRequests(authorizeRequestsCustomizer)
				.httpBasic(Customizer.withDefaults());

		return http.build();
	}

}
