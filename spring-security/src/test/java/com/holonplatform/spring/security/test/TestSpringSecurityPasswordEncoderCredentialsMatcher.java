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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.holonplatform.auth.CredentialsContainer;
import com.holonplatform.auth.exceptions.UnexpectedCredentialsException;
import com.holonplatform.spring.security.config.SpringSecurityPasswordEncoderCredentialsMatcher;

/**
 * Test the {@link SpringSecurityPasswordEncoderCredentialsMatcher} bridge between Spring Security's
 * {@link PasswordEncoder} abstraction and the Holon {@link CredentialsContainer.CredentialsMatcher} contract.
 */
class TestSpringSecurityPasswordEncoderCredentialsMatcher {

	private static CredentialsContainer container(Object credentials) {
		return () -> credentials;
	}

	@Test
	void testMatch() {
		PasswordEncoder encoder = new BCryptPasswordEncoder();
		String encoded = encoder.encode("s3cr3t");

		SpringSecurityPasswordEncoderCredentialsMatcher matcher = new SpringSecurityPasswordEncoderCredentialsMatcher(
				encoder);

		assertTrue(matcher.credentialsMatch(container("s3cr3t"), container(encoded)));
		assertFalse(matcher.credentialsMatch(container("wrong"), container(encoded)));
	}

	@Test
	void testMatchWithCharArrayAndBytes() {
		PasswordEncoder encoder = new BCryptPasswordEncoder();
		String encoded = encoder.encode("s3cr3t");

		SpringSecurityPasswordEncoderCredentialsMatcher matcher = new SpringSecurityPasswordEncoderCredentialsMatcher(
				encoder);

		assertTrue(matcher.credentialsMatch(container("s3cr3t".toCharArray()), container(encoded)));
		assertTrue(matcher.credentialsMatch(container("s3cr3t".getBytes()), container(encoded.getBytes())));
	}

	@Test
	void testNullCredentialsThrows() {
		PasswordEncoder encoder = new BCryptPasswordEncoder();
		SpringSecurityPasswordEncoderCredentialsMatcher matcher = new SpringSecurityPasswordEncoderCredentialsMatcher(
				encoder);
		assertThrows(UnexpectedCredentialsException.class,
				() -> matcher.credentialsMatch(container(null), container(encoder.encode("s3cr3t"))));
	}

	@Test
	void testUnsupportedCredentialsTypeThrows() {
		PasswordEncoder encoder = new BCryptPasswordEncoder();
		SpringSecurityPasswordEncoderCredentialsMatcher matcher = new SpringSecurityPasswordEncoderCredentialsMatcher(
				encoder);
		assertThrows(UnexpectedCredentialsException.class,
				() -> matcher.credentialsMatch(container(42), container(encoder.encode("s3cr3t"))));
	}

}
