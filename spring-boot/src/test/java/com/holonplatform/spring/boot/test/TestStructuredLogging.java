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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import com.holonplatform.core.internal.Logger;
import com.holonplatform.spring.internal.SpringLogger;

/**
 * Verifies that the Holon platform Spring integration does not break, and requires no special handling, when the
 * Spring Boot 4.x built-in structured logging support is enabled (see the {@code logging.structured.format.*}
 * configuration properties).
 * <p>
 * Holon components only rely on SLF4J through {@link Logger} / {@link SpringLogger} and never configure or override
 * the underlying Logback/Log4j2 appender pattern, so enabling structured logging on the application side does not
 * require, nor is affected by, any Holon-specific configuration.
 * </p>
 * <p>
 * Note: this test module ships its own {@code logback-test.xml} (as most other Holon test modules do), which - as
 * expected - takes precedence over Spring Boot's structured logging auto-configuration; this is standard Logback
 * behavior and not specific to Holon. This test therefore focuses on verifying that the
 * {@code logging.structured.format.*} properties can be set without causing any error while Holon components log
 * messages, rather than asserting a specific output format.
 * </p>
 * 
 * @since 10.0.0
 */
@SpringBootTest(properties = "logging.structured.format.console=ecs")
class TestStructuredLogging {

	@Configuration
	@EnableAutoConfiguration
	protected static class Config {

	}

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testStructuredLoggingCompatibility() {
		assertNotNull(applicationContext);
		final Logger logger = SpringLogger.create();
		assertDoesNotThrow(() -> {
			logger.info("Structured logging compatibility test - info message");
			logger.debug(() -> "Structured logging compatibility test - debug message");
			logger.warn("Structured logging compatibility test - warning message");
			logger.error("Structured logging compatibility test - error message", new RuntimeException("test"));
		});
	}

}
