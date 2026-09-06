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
package com.holonplatform.core.datastore.observability.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.holonplatform.core.datastore.DataTarget;
import com.holonplatform.core.datastore.observability.DatastoreOperationObservationContext;
import com.holonplatform.core.datastore.observability.DatastoreOperationType;
import com.holonplatform.core.datastore.observability.DefaultDatastoreOperationObservationConvention;

import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;

/**
 * Test the {@link DatastoreOperationObservationContext} / {@link DefaultDatastoreOperationObservationConvention}
 * optional Datastore observability SPI.
 * <p>
 * These tests simulate how a downstream {@code Datastore} implementation would use the SPI to wrap an operation
 * execution with a Micrometer {@link Observation} - {@code holon-core} itself never creates any Observation.
 * </p>
 */
public class TestDatastoreOperationObservability {

	@Test
	public void testConventionSupportsContext() {
		DefaultDatastoreOperationObservationConvention convention = new DefaultDatastoreOperationObservationConvention();
		DatastoreOperationObservationContext context = new DatastoreOperationObservationContext(
				DatastoreOperationType.INSERT, DataTarget.named("test"));
		assertTrue(convention.supportsContext(context));
		assertFalse(convention.supportsContext(new Context()));
	}

	@Test
	public void testConventionNamingAndTags() {
		DefaultDatastoreOperationObservationConvention convention = new DefaultDatastoreOperationObservationConvention();
		DatastoreOperationObservationContext context = new DatastoreOperationObservationContext(
				DatastoreOperationType.QUERY, DataTarget.named("test1"), "ctx1");

		assertEquals("holon.datastore.operation", convention.getName());
		assertEquals("datastore query", convention.getContextualName(context));
		assertEquals("query",
				convention.getLowCardinalityKeyValues(context).stream()
						.filter(kv -> kv.getKey().equals("holon.datastore.operation")).findFirst().get().getValue());
		assertEquals("test1",
				convention.getLowCardinalityKeyValues(context).stream()
						.filter(kv -> kv.getKey().equals("holon.datastore.target")).findFirst().get().getValue());
		assertEquals("ctx1",
				convention.getLowCardinalityKeyValues(context).stream()
						.filter(kv -> kv.getKey().equals("holon.datastore.context")).findFirst().get().getValue());
	}

	@Test
	public void testContextWithoutTargetAndDataContextId() {
		DefaultDatastoreOperationObservationConvention convention = new DefaultDatastoreOperationObservationConvention();
		DatastoreOperationObservationContext context = new DatastoreOperationObservationContext(
				DatastoreOperationType.DELETE, null);
		assertEquals("none",
				convention.getLowCardinalityKeyValues(context).stream()
						.filter(kv -> kv.getKey().equals("holon.datastore.target")).findFirst().get().getValue());
		assertEquals("default",
				convention.getLowCardinalityKeyValues(context).stream()
						.filter(kv -> kv.getKey().equals("holon.datastore.context")).findFirst().get().getValue());
	}

	@Test
	public void testObservationLifecycle() {
		TestObservationRegistry registry = TestObservationRegistry.create();

		DefaultDatastoreOperationObservationConvention convention = new DefaultDatastoreOperationObservationConvention();
		DatastoreOperationObservationContext context = new DatastoreOperationObservationContext(
				DatastoreOperationType.INSERT, DataTarget.named("targetEntity"));

		AtomicBoolean executed = new AtomicBoolean(false);

		Observation.createNotStarted(convention, () -> context, (ObservationRegistry) registry).observe(() -> {
			executed.set(true);
		});

		assertTrue(executed.get());

		TestObservationRegistryAssert.assertThat(registry).hasObservationWithNameEqualTo("holon.datastore.operation")
				.that().hasContextualNameEqualTo("datastore insert")
				.hasLowCardinalityKeyValue("holon.datastore.operation", "insert")
				.hasLowCardinalityKeyValue("holon.datastore.target", "targetEntity");
	}

}
