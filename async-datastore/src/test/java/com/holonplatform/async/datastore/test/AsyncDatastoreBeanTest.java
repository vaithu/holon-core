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
package com.holonplatform.async.datastore.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.holonplatform.async.datastore.AsyncDatastore;
import com.holonplatform.async.datastore.operation.AsyncDelete;
import com.holonplatform.async.datastore.operation.AsyncInsert;
import com.holonplatform.async.datastore.operation.AsyncRefresh;
import com.holonplatform.async.datastore.operation.AsyncSave;
import com.holonplatform.async.datastore.operation.AsyncUpdate;
import com.holonplatform.core.beans.BeanIntrospector;
import com.holonplatform.core.beans.BeanPropertySet;
import com.holonplatform.core.datastore.DataTarget;
import com.holonplatform.core.datastore.Datastore.OperationResult;
import com.holonplatform.core.datastore.DatastoreOperations.WriteOption;
import com.holonplatform.core.property.PathProperty;
import com.holonplatform.core.property.PropertyBox;
import com.holonplatform.core.property.PropertySet;

/**
 * Tests demonstrating how AsyncDatastore works with Java beans via
 * BeanPropertySet and PropertyBox conversion.
 */
class AsyncDatastoreBeanTest {

	// --- Test bean ---

	public static class Product {
		private Long id;
		private String name;
		private Double price;

		public Product() {
		}

		public Product(Long id, String name, Double price) {
			this.id = id;
			this.name = name;
			this.price = price;
		}

		public Long getId() { return id; }
		public void setId(Long id) { this.id = id; }
		public String getName() { return name; }
		public void setName(String name) { this.name = name; }
		public Double getPrice() { return price; }
		public void setPrice(Double price) { this.price = price; }
	}

	// --- Manual property model ---

	static final PathProperty<Long> ID = PathProperty.create("id", Long.class);
	static final PathProperty<String> NAME = PathProperty.create("name", String.class);
	static final PathProperty<Double> PRICE = PathProperty.create("price", Double.class);
	static final PropertySet<?> PROPERTIES = PropertySet.of(ID, NAME, PRICE);
	static final DataTarget<String> TARGET = DataTarget.named("products");

	private AsyncDatastore datastore;

	@BeforeEach
	void setUp() {
		datastore = mock(AsyncDatastore.class, Mockito.CALLS_REAL_METHODS);
	}

	// --- BeanPropertySet read/write ---

	@Test
	void beanPropertySet_shouldReadBeanIntoPropertyBox() {
		BeanPropertySet<Product> bps = BeanPropertySet.create(Product.class);

		Product product = new Product(1L, "Widget", 9.99);
		PropertyBox box = bps.read(product);

		assertNotNull(box);
		assertEquals(Long.valueOf(1L), box.getValue(bps.property("id")));
		assertEquals("Widget", box.getValue(bps.property("name")));
		assertEquals(Double.valueOf(9.99), box.getValue(bps.property("price")));
	}

	@Test
	void beanPropertySet_shouldWritePropertyBoxToBean() {
		BeanPropertySet<Product> bps = BeanPropertySet.create(Product.class);

		PropertyBox box = bps.read(new Product(42L, "Gadget", 19.99));
		Product result = bps.write(box, new Product());

		assertEquals(Long.valueOf(42L), result.getId());
		assertEquals("Gadget", result.getName());
		assertEquals(Double.valueOf(19.99), result.getPrice());
	}

	@Test
	void beanIntrospector_shouldReadBeanIntoManualPropertyBox() {
		Product product = new Product(5L, "Gizmo", 4.50);

		PropertyBox box = BeanIntrospector.get().read(PropertyBox.create(PROPERTIES), product);

		assertEquals(Long.valueOf(5L), box.getValue(ID));
		assertEquals("Gizmo", box.getValue(NAME));
		assertEquals(Double.valueOf(4.50), box.getValue(PRICE));
	}

	@Test
	void beanIntrospector_shouldWriteManualPropertyBoxToBean() {
		PropertyBox box = PropertyBox.builder(PROPERTIES)
				.set(ID, 10L)
				.set(NAME, "Doohickey")
				.set(PRICE, 2.25)
				.build();

		Product product = BeanIntrospector.get().write(box, new Product());

		assertEquals(Long.valueOf(10L), product.getId());
		assertEquals("Doohickey", product.getName());
		assertEquals(Double.valueOf(2.25), product.getPrice());
	}

	// --- Bean → PropertyBox → AsyncDatastore insert ---

	@Test
	void insert_shouldWorkWithBeanConvertedToPropertyBox() throws Exception {
		AsyncInsert insertOp = mock(AsyncInsert.class);
		OperationResult opResult = mock(OperationResult.class);
		when(opResult.getAffectedCount()).thenReturn(1L);

		when(datastore.create(AsyncInsert.class)).thenReturn(insertOp);
		when(insertOp.target(any())).thenReturn(insertOp);
		when(insertOp.value(any(PropertyBox.class))).thenReturn(insertOp);
		when(insertOp.withWriteOptions(any(WriteOption[].class))).thenReturn(insertOp);
		when(insertOp.execute()).thenReturn(CompletableFuture.completedFuture(opResult));

		// Convert bean to PropertyBox
		Product product = new Product(1L, "Widget", 9.99);
		PropertyBox box = BeanIntrospector.get().read(PropertyBox.create(PROPERTIES), product);

		// Insert via AsyncDatastore
		CompletionStage<OperationResult> result = datastore.insert(TARGET, box);

		assertNotNull(result);
		assertEquals(Long.valueOf(1L), result.toCompletableFuture().get().getAffectedCount());
		verify(insertOp).value(box);
	}

	// --- Bean → PropertyBox → AsyncDatastore save ---

	@Test
	void save_shouldWorkWithBeanConvertedToPropertyBox() throws Exception {
		AsyncSave saveOp = mock(AsyncSave.class);
		OperationResult opResult = mock(OperationResult.class);

		when(datastore.create(AsyncSave.class)).thenReturn(saveOp);
		when(saveOp.target(any())).thenReturn(saveOp);
		when(saveOp.value(any(PropertyBox.class))).thenReturn(saveOp);
		when(saveOp.withWriteOptions(any(WriteOption[].class))).thenReturn(saveOp);
		when(saveOp.execute()).thenReturn(CompletableFuture.completedFuture(opResult));

		Product product = new Product(2L, "Gadget", 19.99);
		PropertyBox box = BeanIntrospector.get().read(PropertyBox.create(PROPERTIES), product);

		CompletionStage<OperationResult> result = datastore.save(TARGET, box);

		assertNotNull(result.toCompletableFuture().get());
		verify(saveOp).value(box);
	}

	// --- Bean → PropertyBox → AsyncDatastore update ---

	@Test
	void update_shouldWorkWithBeanConvertedToPropertyBox() throws Exception {
		AsyncUpdate updateOp = mock(AsyncUpdate.class);
		OperationResult opResult = mock(OperationResult.class);

		when(datastore.create(AsyncUpdate.class)).thenReturn(updateOp);
		when(updateOp.target(any())).thenReturn(updateOp);
		when(updateOp.value(any(PropertyBox.class))).thenReturn(updateOp);
		when(updateOp.withWriteOptions(any(WriteOption[].class))).thenReturn(updateOp);
		when(updateOp.execute()).thenReturn(CompletableFuture.completedFuture(opResult));

		Product product = new Product(3L, "Updated", 29.99);
		PropertyBox box = BeanIntrospector.get().read(PropertyBox.create(PROPERTIES), product);

		CompletionStage<OperationResult> result = datastore.update(TARGET, box);

		assertNotNull(result.toCompletableFuture().get());
		verify(updateOp).value(box);
	}

	// --- Bean → PropertyBox → AsyncDatastore delete ---

	@Test
	void delete_shouldWorkWithBeanConvertedToPropertyBox() throws Exception {
		AsyncDelete deleteOp = mock(AsyncDelete.class);
		OperationResult opResult = mock(OperationResult.class);

		when(datastore.create(AsyncDelete.class)).thenReturn(deleteOp);
		when(deleteOp.target(any())).thenReturn(deleteOp);
		when(deleteOp.value(any(PropertyBox.class))).thenReturn(deleteOp);
		when(deleteOp.withWriteOptions(any(WriteOption[].class))).thenReturn(deleteOp);
		when(deleteOp.execute()).thenReturn(CompletableFuture.completedFuture(opResult));

		Product product = new Product(4L, "ToDelete", 0.0);
		PropertyBox box = BeanIntrospector.get().read(PropertyBox.create(PROPERTIES), product);

		CompletionStage<OperationResult> result = datastore.delete(TARGET, box);

		assertNotNull(result.toCompletableFuture().get());
		verify(deleteOp).value(box);
	}

	// --- AsyncDatastore refresh → PropertyBox → Bean ---

	@Test
	void refresh_shouldReturnPropertyBoxConvertibleToBean() throws Exception {
		AsyncRefresh refreshOp = mock(AsyncRefresh.class);

		// Simulate a refreshed PropertyBox coming back from the datastore
		PropertyBox refreshedBox = PropertyBox.builder(PROPERTIES)
				.set(ID, 1L)
				.set(NAME, "RefreshedWidget")
				.set(PRICE, 12.50)
				.build();

		when(datastore.create(AsyncRefresh.class)).thenReturn(refreshOp);
		when(refreshOp.target(any())).thenReturn(refreshOp);
		when(refreshOp.value(any(PropertyBox.class))).thenReturn(refreshOp);
		when(refreshOp.execute()).thenReturn(CompletableFuture.completedFuture(refreshedBox));

		PropertyBox inputBox = BeanIntrospector.get().read(PropertyBox.create(PROPERTIES),
				new Product(1L, "Widget", 9.99));

		CompletionStage<PropertyBox> result = datastore.refresh(TARGET, inputBox);

		PropertyBox resultBox = result.toCompletableFuture().get();

		// Convert back to bean
		Product refreshedProduct = BeanIntrospector.get().write(resultBox, new Product());

		assertEquals(Long.valueOf(1L), refreshedProduct.getId());
		assertEquals("RefreshedWidget", refreshedProduct.getName());
		assertEquals(Double.valueOf(12.50), refreshedProduct.getPrice());
	}

	// --- Round-trip: Bean → PropertyBox → insert → refresh → Bean ---

	@Test
	void roundTrip_beanToDatastoreAndBack() throws Exception {
		// Setup insert
		AsyncInsert insertOp = mock(AsyncInsert.class);
		OperationResult insertResult = mock(OperationResult.class);
		when(insertResult.getAffectedCount()).thenReturn(1L);
		when(datastore.create(AsyncInsert.class)).thenReturn(insertOp);
		when(insertOp.target(any())).thenReturn(insertOp);
		when(insertOp.value(any(PropertyBox.class))).thenReturn(insertOp);
		when(insertOp.withWriteOptions(any(WriteOption[].class))).thenReturn(insertOp);
		when(insertOp.execute()).thenReturn(CompletableFuture.completedFuture(insertResult));

		// Setup refresh - returns updated data
		AsyncRefresh refreshOp = mock(AsyncRefresh.class);
		PropertyBox refreshedBox = PropertyBox.builder(PROPERTIES)
				.set(ID, 100L)
				.set(NAME, "ServerName")
				.set(PRICE, 55.00)
				.build();
		when(datastore.create(AsyncRefresh.class)).thenReturn(refreshOp);
		when(refreshOp.target(any())).thenReturn(refreshOp);
		when(refreshOp.value(any(PropertyBox.class))).thenReturn(refreshOp);
		when(refreshOp.execute()).thenReturn(CompletableFuture.completedFuture(refreshedBox));

		// Step 1: Bean → PropertyBox
		Product original = new Product(100L, "ClientName", 50.00);
		PropertyBox box = BeanIntrospector.get().read(PropertyBox.create(PROPERTIES), original);

		// Step 2: Insert
		OperationResult ir = datastore.insert(TARGET, box).toCompletableFuture().get();
		assertEquals(1L, ir.getAffectedCount());

		// Step 3: Refresh
		PropertyBox fromDb = datastore.refresh(TARGET, box).toCompletableFuture().get();

		// Step 4: PropertyBox → Bean
		Product refreshed = BeanIntrospector.get().write(fromDb, new Product());
		assertEquals(Long.valueOf(100L), refreshed.getId());
		assertEquals("ServerName", refreshed.getName());
		assertEquals(Double.valueOf(55.00), refreshed.getPrice());
	}

	// --- PropertyBox built manually ---

	@Test
	void insert_shouldWorkWithManuallyBuiltPropertyBox() throws Exception {
		AsyncInsert insertOp = mock(AsyncInsert.class);
		OperationResult opResult = mock(OperationResult.class);

		when(datastore.create(AsyncInsert.class)).thenReturn(insertOp);
		when(insertOp.target(any())).thenReturn(insertOp);
		when(insertOp.value(any(PropertyBox.class))).thenReturn(insertOp);
		when(insertOp.withWriteOptions(any(WriteOption[].class))).thenReturn(insertOp);
		when(insertOp.execute()).thenReturn(CompletableFuture.completedFuture(opResult));

		// Build PropertyBox manually (no bean involved)
		PropertyBox box = PropertyBox.builder(PROPERTIES)
				.set(ID, 99L)
				.set(NAME, "Manual")
				.set(PRICE, 1.00)
				.build();

		CompletionStage<OperationResult> result = datastore.insert(TARGET, box);

		assertNotNull(result.toCompletableFuture().get());

		// The manually built PropertyBox can still be written to a bean
		Product product = BeanIntrospector.get().write(box, new Product());
		assertEquals(Long.valueOf(99L), product.getId());
		assertEquals("Manual", product.getName());
		assertEquals(Double.valueOf(1.00), product.getPrice());
	}
}

