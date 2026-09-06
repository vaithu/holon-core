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
package com.holonplatform.core.datastore.beans;

/**
 * A generic interface representing a pageable query with limit and offset.
 * <p>
 * This interface is compatible with Vaadin's {@code Query<T, F>} which provides
 * {@code getLimit()} and {@code getOffset()} methods, allowing it to be passed
 * directly to methods that accept this interface.
 * </p>
 *
 * @since 10.0.0
 */
@FunctionalInterface
public interface PageableQuery {

	/**
	 * Get the maximum number of items to fetch.
	 *
	 * @return The limit (number of items to fetch)
	 */
	int getLimit();

	/**
	 * Get the zero-based offset of the first item to fetch.
	 *
	 * @return The offset (default is 0)
	 */
	default int getOffset() {
		return 0;
	}

}
