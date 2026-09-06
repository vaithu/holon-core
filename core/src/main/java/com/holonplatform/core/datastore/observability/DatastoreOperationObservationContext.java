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

import java.util.Optional;

import com.holonplatform.core.datastore.DataTarget;
import com.holonplatform.core.internal.utils.ObjectUtils;

import io.micrometer.observation.Observation;

/**
 * A Micrometer {@link Observation.Context} implementation which carries the information about a
 * {@link com.holonplatform.core.datastore.Datastore} operation being observed: the {@link DatastoreOperationType}
 * and, when available, the operation {@link DataTarget} and the Datastore <em>data context id</em>.
 * <p>
 * This context is part of the optional Datastore operation observability SPI (see package
 * {@link com.holonplatform.core.datastore.observability}) and is meant to be created and used by concrete
 * {@link com.holonplatform.core.datastore.Datastore} implementations which actually execute operations against a
 * data source, not by this library itself.
 * </p>
 * 
 * @since 10.0.0
 * 
 * @see DatastoreOperationObservationConvention
 */
public class DatastoreOperationObservationContext extends Observation.Context {

	private final DatastoreOperationType operationType;
	private final DataTarget<?> target;
	private final String dataContextId;

	/**
	 * Constructor.
	 * @param operationType The Datastore operation type (not null)
	 * @param target The operation data target, if available
	 */
	public DatastoreOperationObservationContext(DatastoreOperationType operationType, DataTarget<?> target) {
		this(operationType, target, null);
	}

	/**
	 * Constructor.
	 * @param operationType The Datastore operation type (not null)
	 * @param target The operation data target, if available
	 * @param dataContextId The Datastore data context id, if available
	 */
	public DatastoreOperationObservationContext(DatastoreOperationType operationType, DataTarget<?> target,
			String dataContextId) {
		super();
		ObjectUtils.argumentNotNull(operationType, "The Datastore operation type must be not null");
		this.operationType = operationType;
		this.target = target;
		this.dataContextId = dataContextId;
	}

	/**
	 * Get the Datastore operation type.
	 * @return the Datastore operation type
	 */
	public DatastoreOperationType getOperationType() {
		return operationType;
	}

	/**
	 * Get the operation data target, if available.
	 * @return Optional operation data target
	 */
	public Optional<DataTarget<?>> getTarget() {
		return Optional.ofNullable(target);
	}

	/**
	 * Get the Datastore data context id, if available.
	 * @return Optional Datastore data context id
	 */
	public Optional<String> getDataContextId() {
		return Optional.ofNullable(dataContextId);
	}

}
