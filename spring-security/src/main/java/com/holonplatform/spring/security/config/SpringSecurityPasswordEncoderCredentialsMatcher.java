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

import java.nio.charset.StandardCharsets;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.holonplatform.auth.Credentials;
import com.holonplatform.auth.CredentialsContainer;
import com.holonplatform.auth.exceptions.AuthenticationException;
import com.holonplatform.auth.exceptions.UnexpectedCredentialsException;
import com.holonplatform.core.internal.utils.ObjectUtils;

/**
 * A Holon platform {@link CredentialsContainer.CredentialsMatcher} implementation which delegates credentials
 * validation to a Spring Security {@link PasswordEncoder}, allowing account passwords to be encoded and verified
 * using any standard Spring Security password encoding strategy (for example {@code BCryptPasswordEncoder},
 * {@code Argon2PasswordEncoder} or a {@code DelegatingPasswordEncoder} supporting the {@code {id}} prefix
 * convention), instead of (or in addition to) the built-in {@link Credentials.Encoder} hash-based encoding.
 * <p>
 * The provided (raw, clear-text) credentials and the stored (encoded) credentials are both converted to a
 * {@link String} representation before being passed to {@link PasswordEncoder#matches(CharSequence, String)}:
 * </p>
 * <ul>
 * <li>a {@link String} is used as-is;</li>
 * <li>a {@code char[]} is converted using {@link String#valueOf(char[])};</li>
 * <li>a {@code byte[]} is decoded as UTF-8;</li>
 * <li>a {@link Credentials} instance uses its {@link Credentials#getSecret()} value, decoded as UTF-8 (any hash
 * algorithm, salt or iterations configured on the {@link Credentials} instance is ignored, since encoding is
 * entirely delegated to the {@link PasswordEncoder}).</li>
 * </ul>
 * <p>
 * Typical usage:
 * </p>
 * 
 * <pre>
 * Authenticator&lt;AccountCredentialsToken&gt; authenticator = Account.authenticator(accountProvider,
 * 		new SpringSecurityPasswordEncoderCredentialsMatcher(new BCryptPasswordEncoder()));
 * </pre>
 * 
 * @since 10.0.0
 */
public class SpringSecurityPasswordEncoderCredentialsMatcher implements CredentialsContainer.CredentialsMatcher {

	private final PasswordEncoder passwordEncoder;

	/**
	 * Constructor.
	 * @param passwordEncoder The {@link PasswordEncoder} to use to validate credentials (not null)
	 */
	public SpringSecurityPasswordEncoderCredentialsMatcher(PasswordEncoder passwordEncoder) {
		super();
		ObjectUtils.argumentNotNull(passwordEncoder, "PasswordEncoder must be not null");
		this.passwordEncoder = passwordEncoder;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.auth.CredentialsContainer.CredentialsMatcher#credentialsMatch(com.holonplatform.auth.
	 * CredentialsContainer, com.holonplatform.auth.CredentialsContainer)
	 */
	@Override
	public boolean credentialsMatch(CredentialsContainer provided, CredentialsContainer stored)
			throws AuthenticationException {
		final String rawPassword = asString(provided, "provided");
		final String encodedPassword = asString(stored, "stored");
		return passwordEncoder.matches(rawPassword, encodedPassword);
	}

	/**
	 * Convert the given credentials container's credentials data into a {@link String} representation.
	 * @param container Credentials container
	 * @param role Container role (provided/stored), used for error reporting only
	 * @return String representation of the credentials data
	 * @throws UnexpectedCredentialsException If credentials data is <code>null</code> or of an unsupported type
	 */
	private static String asString(CredentialsContainer container, String role) throws UnexpectedCredentialsException {
		final Object credentials = (container != null) ? container.getCredentials() : null;
		if (credentials == null) {
			throw new UnexpectedCredentialsException("Null " + role + " credentials");
		}
		if (credentials instanceof String string) {
			return string;
		}
		if (credentials instanceof char[] chars) {
			return String.valueOf(chars);
		}
		if (credentials instanceof byte[] bytes) {
			return new String(bytes, StandardCharsets.UTF_8);
		}
		if (credentials instanceof Credentials creds) {
			final byte[] secret = creds.getSecret();
			if (secret == null) {
				throw new UnexpectedCredentialsException("Null secret in " + role + " Credentials");
			}
			return new String(secret, StandardCharsets.UTF_8);
		}
		throw new UnexpectedCredentialsException(
				"Unsupported " + role + " credentials type: " + credentials.getClass().getName());
	}

}
