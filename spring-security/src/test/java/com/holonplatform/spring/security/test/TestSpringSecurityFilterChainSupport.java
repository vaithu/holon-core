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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.holonplatform.auth.Account;
import com.holonplatform.auth.Account.AccountProvider;
import com.holonplatform.auth.Authenticator;
import com.holonplatform.auth.Credentials;
import com.holonplatform.auth.token.AccountCredentialsToken;
import com.holonplatform.spring.security.config.SpringSecurityFilterChainSupport;

@SpringJUnitWebConfig(classes = TestSpringSecurityFilterChainSupport.Config.class)
class TestSpringSecurityFilterChainSupport {

	@Configuration
	@EnableWebSecurity
	@EnableWebMvc
	protected static class Config {

		@Bean
		public AccountProvider accountProvider() {
			return id -> {
				if ("u1".equals(id)) {
					return Optional.of(Account.builder(id).credentials(Credentials.builder().secret(id).build())
							.withPermission("view").build());
				}
				return Optional.empty();
			};
		}

		@Bean
		public SecurityFilterChain securityFilterChain(HttpSecurity http, AccountProvider accountProvider)
				throws Exception {
			Authenticator<AccountCredentialsToken> authenticator = Account.authenticator(accountProvider);
			return SpringSecurityFilterChainSupport.configure(http, authenticator);
		}

		@RestController
		protected static class TestController {

			@GetMapping("/hello")
			public String hello() {
				return "hello";
			}

		}

	}

	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@BeforeEach
	void setup() {
		mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity())
				.build();
	}

	@Test
	void testUnauthenticatedRequestIsDenied() throws Exception {
		mockMvc.perform(get("/hello")).andExpect(status().isUnauthorized());
	}

	@Test
	void testAuthenticatedRequestIsAllowed() throws Exception {
		mockMvc.perform(get("/hello").with(httpBasic("u1", "u1"))).andExpect(status().isOk());
	}

	@Test
	void testInvalidCredentialsAreDenied() throws Exception {
		mockMvc.perform(get("/hello").with(httpBasic("u1", "wrong"))).andExpect(status().isUnauthorized());
	}

}
