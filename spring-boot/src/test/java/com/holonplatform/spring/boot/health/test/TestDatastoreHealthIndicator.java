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
package com.holonplatform.spring.boot.health.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import com.holonplatform.core.datastore.DataTarget;
import com.holonplatform.core.datastore.Datastore;
import com.holonplatform.core.query.Query;
import com.holonplatform.spring.boot.health.DatastoreHealthIndicator;

class TestDatastoreHealthIndicator {

	private static final DataTarget<?> TARGET = DataTarget.named("test");

	@Test
	void testUp() {
		final Datastore datastore = mock(Datastore.class);
		final Query query = mock(Query.class);
		when(datastore.query(TARGET)).thenReturn(query);
		when(query.count()).thenReturn(7L);

		final DatastoreHealthIndicator indicator = new DatastoreHealthIndicator(datastore, TARGET);
		final Health health = indicator.health();

		assertEquals(Status.UP, health.getStatus());
		assertEquals("test", health.getDetails().get("target"));
		assertEquals(7L, health.getDetails().get("count"));
	}

	@Test
	void testDown() {
		final Datastore datastore = mock(Datastore.class);
		final Query query = mock(Query.class);
		when(datastore.query(TARGET)).thenReturn(query);
		when(query.count()).thenThrow(new RuntimeException("test failure"));

		final DatastoreHealthIndicator indicator = new DatastoreHealthIndicator(datastore, TARGET);
		final Health health = indicator.health();

		assertEquals(Status.DOWN, health.getStatus());
		assertEquals("test", health.getDetails().get("target"));
	}

	@Test
	void testNullDatastore() {
		assertThrows(IllegalArgumentException.class, () -> new DatastoreHealthIndicator(null, TARGET));
	}

	@Test
	void testNullTarget() {
		final Datastore datastore = mock(Datastore.class);
		assertThrows(IllegalArgumentException.class, () -> new DatastoreHealthIndicator(datastore, null));
	}

}
