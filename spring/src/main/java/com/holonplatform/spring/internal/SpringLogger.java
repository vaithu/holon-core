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
package com.holonplatform.spring.internal;

import com.holonplatform.core.internal.Logger;
import com.holonplatform.spring.EnableBeanContext;

/**
 * JDBC module logger provider.
 * <p>
 * This logger is a thin wrapper around SLF4J (see {@link Logger#create(String)}) and does not configure or override
 * the underlying logging framework appender or output pattern in any way. As a consequence, it is fully compatible
 * with the Spring Boot structured logging support (the {@code logging.structured.format.*} configuration
 * properties): enabling structured logging on the application side requires no Holon-specific configuration.
 * </p>
 *
 * @since 5.0.0
 */
public interface SpringLogger {

	static final String NAME = EnableBeanContext.class.getPackage().getName();

	/**
	 * Get a {@link Logger} bound to {@link #NAME}.
	 * @return Logger
	 */
	static Logger create() {
		return Logger.create(NAME);
	}

}
