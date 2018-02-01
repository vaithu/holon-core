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
package com.holonplatform.core;

import java.util.Optional;

import com.holonplatform.core.internal.DefaultProvider;

/**
 * Optional object provider.
 * 
 * @param <T> Provided object type
 *
 * @since 5.1.0
 */
@FunctionalInterface
public interface Provider<T> {

	/**
	 * Get the object provided by this provider, if available.
	 * @return The optional provided object
	 */
	Optional<T> get();

	/**
	 * Create a new {@link Provider} using given value.
	 * @param <T> Provided value type
	 * @param value The value to provide (may be null)
	 * @return A new {@link Provider}
	 */
	static <T> Provider<T> create(T value) {
		return new DefaultProvider<>(value);
	}

}
