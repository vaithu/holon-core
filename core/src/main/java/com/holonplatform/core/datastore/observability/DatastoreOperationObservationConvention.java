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

import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationConvention;

/**
 * A Micrometer {@link ObservationConvention} contract for {@link DatastoreOperationObservationContext}, defining the
 * observation name, contextual name and key values (tags) to associate to a
 * {@link com.holonplatform.core.datastore.Datastore} operation observation.
 * <p>
 * This interface, along with {@link DatastoreOperationObservationContext} and {@link DatastoreOperationType}, forms
 * an <b>optional SPI</b> that concrete {@link com.holonplatform.core.datastore.Datastore} implementations (e.g. a
 * JDBC or JPA backed Datastore) may adopt to consistently name and tag the Micrometer {@link Observation}s they
 * create when executing an operation against the underlying data source.
 * </p>
 * <p>
 * <b>Important:</b> the {@code holon-core} artifact never creates or starts any {@link Observation} itself - it only
 * defines this naming/tagging contract, since the actual operation execution (and therefore the natural point where
 * an {@link Observation} would be started and stopped) always happens in a concrete Datastore implementation, which
 * is outside the scope of this library. A typical usage from such an implementation would be:
 * </p>
 * 
 * <pre>
 * DatastoreOperationObservationConvention convention = new DefaultDatastoreOperationObservationConvention();
 * DatastoreOperationObservationContext context = new DatastoreOperationObservationContext(
 * 		DatastoreOperationType.INSERT, target);
 * 
 * Observation.createNotStarted(convention, () -&gt; context, observationRegistry).observe(() -&gt; {
 * 	// perform the actual insert operation
 * 	return doInsert(target, propertyBox);
 * });
 * </pre>
 * 
 * @since 10.0.0
 * 
 * @see DefaultDatastoreOperationObservationConvention
 */
public interface DatastoreOperationObservationConvention
		extends ObservationConvention<DatastoreOperationObservationContext> {

	/*
	 * (non-Javadoc)
	 * @see io.micrometer.observation.ObservationConvention#supportsContext(io.micrometer.observation.Observation.
	 * Context)
	 */
	@Override
	default boolean supportsContext(Context context) {
		return context instanceof DatastoreOperationObservationContext;
	}

}
