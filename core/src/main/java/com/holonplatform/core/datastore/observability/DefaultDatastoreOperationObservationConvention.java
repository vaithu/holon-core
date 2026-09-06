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
package com.holonplatform.core.datastore.observability;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

/**
 * Default {@link DatastoreOperationObservationConvention} implementation.
 * <p>
 * Uses {@code holon.datastore.operation} as the (low-cardinality) observation name, and provides the following
 * low-cardinality key values:
 * </p>
 * <ul>
 * <li>{@code holon.datastore.operation} - the {@link DatastoreOperationType} name, lower case (e.g.
 * <code>insert</code>, <code>query</code>)</li>
 * <li>{@code holon.datastore.target} - the operation {@link com.holonplatform.core.datastore.DataTarget} name, if
 * available, or <code>none</code> otherwise</li>
 * <li>{@code holon.datastore.context} - the Datastore data context id, if available, or <code>default</code>
 * otherwise</li>
 * </ul>
 * 
 * @since 10.0.0
 */
public class DefaultDatastoreOperationObservationConvention implements DatastoreOperationObservationConvention {

	/**
	 * Default observation name.
	 */
	public static final String OBSERVATION_NAME = "holon.datastore.operation";

	/*
	 * (non-Javadoc)
	 * @see io.micrometer.observation.ObservationConvention#getName()
	 */
	@Override
	public String getName() {
		return OBSERVATION_NAME;
	}

	/*
	 * (non-Javadoc)
	 * @see io.micrometer.observation.ObservationConvention#getContextualName(io.micrometer.observation.Observation.
	 * Context)
	 */
	@Override
	public String getContextualName(DatastoreOperationObservationContext context) {
		return "datastore " + context.getOperationType().name().toLowerCase();
	}

	/*
	 * (non-Javadoc)
	 * @see io.micrometer.observation.ObservationConvention#getLowCardinalityKeyValues(io.micrometer.observation.
	 * Observation.Context)
	 */
	@Override
	public KeyValues getLowCardinalityKeyValues(DatastoreOperationObservationContext context) {
		return KeyValues.of(
				KeyValue.of("holon.datastore.operation", context.getOperationType().name().toLowerCase()),
				KeyValue.of("holon.datastore.target", context.getTarget().map(t -> t.getName()).orElse("none")),
				KeyValue.of("holon.datastore.context", context.getDataContextId().orElse("default")));
	}

}
