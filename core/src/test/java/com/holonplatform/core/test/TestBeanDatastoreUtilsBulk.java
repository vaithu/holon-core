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
package com.holonplatform.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.holonplatform.core.datastore.DatastoreOperations.WriteOption;
import com.holonplatform.core.datastore.DefaultWriteOption;
import com.holonplatform.core.datastore.beans.BeanBulkDelete;
import com.holonplatform.core.datastore.beans.BeanBulkInsert;
import com.holonplatform.core.datastore.beans.BeanBulkUpdate;
import com.holonplatform.core.datastore.beans.BeanDatastore;
import com.holonplatform.core.datastore.beans.BeanDatastore.BeanOperationResult;
import com.holonplatform.core.datastore.beans.BeanDatastoreUtils;
import com.holonplatform.core.query.QueryFilter;
import com.holonplatform.core.test.data.Product;

/**
 * Practical examples and tests for the bulk-operation methods of {@link BeanDatastoreUtils}.
 *
 * <h2>API summary</h2>
 * <pre>{@code
 * // ── BULK INSERT ─────────────────────��────────────────────────────────────
 * // From a Collection  (executes immediately)
 * BeanDatastoreUtils.bulkInsert(bds, Product.class, listOfProducts);
 *
 * // From varargs  (executes immediately)
 * BeanDatastoreUtils.bulkInsert(bds, Product.class, p1, p2, p3);
 *
 * // ── BULK DELETE ──────────────────────────────────────────────────────────
 * // Filter-based, executes immediately
 * BeanDatastoreUtils.bulkDelete(bds, Product.class, techFilter);
 *
 * // ── BULK UPDATE ──────────────────────────────────────────────────────────
 * // 1. Builder style — chain .set() calls, then .execute()
 * BeanDatastoreUtils.bulkUpdate(bds, Product.class, homeFilter)
 *         .set("category", "SALE")
 *         .set("price",    49.99)
 *         .execute();
 *
 * // 2. Single-property shortcut (executes immediately)
 * BeanDatastoreUtils.bulkUpdateProperty(bds, Product.class, expiredFilter, "active", false);
 *
 * // 3. Multi-property map shortcut (executes immediately)
 * Map<String, Object> updates = new LinkedHashMap<>();
 * updates.put("category", "SALE");
 * updates.put("price",    49.99);
 * BeanDatastoreUtils.bulkUpdateProperties(bds, Product.class, homeFilter, updates);
 * }</pre>
 */
@SuppressWarnings("unchecked")
class TestBeanDatastoreUtilsBulk {

	// ── fixture ──────────────────────────────────────────────────────────────

	private BeanDatastore               bds;
	private BeanBulkInsert<Product>     bulkInsert;
	private BeanBulkUpdate<Product>     bulkUpdate;
	private BeanBulkDelete<Product>     bulkDelete;

	/** Canned result: 5 rows affected. */
	private static final BeanOperationResult<Product> RESULT_5 =
			BeanOperationResult.<Product>builder().affectedCount(5).build();

	/** Canned result: 1 row affected. */
	private static final BeanOperationResult<Product> RESULT_1 =
			BeanOperationResult.<Product>builder().affectedCount(1).build();

	/** Sample products. */
	private final Product p1 = new Product(1L, "Widget",  9.99,  "TECH");
	private final Product p2 = new Product(2L, "Gadget",  19.99, "TECH");
	private final Product p3 = new Product(3L, "Thingus", 29.99, "HOME");
	private final Product p4 = new Product(4L, "Doohick", 39.99, "HOME");
	private final Product p5 = new Product(5L, "Gizmo",   49.99, "SPORT");

	@BeforeEach
	void setUp() {
		bds        = mock(BeanDatastore.class);
		bulkInsert = mock(BeanBulkInsert.class);
		bulkUpdate = mock(BeanBulkUpdate.class);
		bulkDelete = mock(BeanBulkDelete.class);

		// Wire datastore → bulk operation builders (filter-based API)
		when(bds.bulkInsert(eq(Product.class))).thenReturn(bulkInsert);
		when(bds.bulkInsert(eq(Product.class), any(WriteOption[].class))).thenReturn(bulkInsert);
		when(bds.bulkUpdate(eq(Product.class))).thenReturn(bulkUpdate);
		when(bds.bulkUpdate(eq(Product.class), any(WriteOption[].class))).thenReturn(bulkUpdate);
		when(bds.bulkDelete(eq(Product.class))).thenReturn(bulkDelete);
		when(bds.bulkDelete(eq(Product.class), any(WriteOption[].class))).thenReturn(bulkDelete);

		// Wire datastore → individual ops used by the List-accepting methods.
		// Use any(WriteOption[].class) to correctly match varargs (including empty-array calls).
		when(bds.update(any(Product.class), any(WriteOption[].class)))
				.thenReturn(RESULT_1);
		when(bds.save(any(Product.class), any(WriteOption[].class)))
				.thenReturn(RESULT_1);
		when(bds.delete(any(Product.class), any(WriteOption[].class)))
				.thenReturn(RESULT_1);

		// BulkInsert: add() is fluent
		when(bulkInsert.add(any(Product.class))).thenReturn(bulkInsert);
		when(bulkInsert.execute()).thenReturn(RESULT_5);

		// BulkUpdate: set()/setNull() are fluent; filter() uses doReturn (varargs ambiguity)
		when(bulkUpdate.set(anyString(), any())).thenReturn(bulkUpdate);
		when(bulkUpdate.setNull(anyString())).thenReturn(bulkUpdate);
		doReturn(bulkUpdate).when(bulkUpdate).filter(any(QueryFilter.class));
		when(bulkUpdate.execute()).thenReturn(RESULT_5);

		// BulkDelete: filter() uses doReturn (varargs ambiguity)
		doReturn(bulkDelete).when(bulkDelete).filter(any(QueryFilter.class));
		when(bulkDelete.execute()).thenReturn(RESULT_5);
	}

	// ── BULK INSERT ────────��────────────────────────────────────────────────

	@Nested
	@DisplayName("Bulk INSERT")
	class BulkInsertTests {

		@Test
		@DisplayName("Collection: add() called for every item, execute() called once")
		void fromCollection() {
			// ----------------------------------------------------------------
			// BeanDatastoreUtils.bulkInsert(bds, Product.class, list)
			//   → bds.bulkInsert(Product.class)
			//   → op.add(p1).add(p2).add(p3).add(p4).add(p5)
			//   → op.execute()
			// ----------------------------------------------------------------
			List<Product> batch = List.of(p1, p2, p3, p4, p5);

			BeanOperationResult<?> result =
					BeanDatastoreUtils.bulkInsert(bds, Product.class, batch);

			// Every product was added exactly once
			verify(bulkInsert).add(p1);
			verify(bulkInsert).add(p2);
			verify(bulkInsert).add(p3);
			verify(bulkInsert).add(p4);
			verify(bulkInsert).add(p5);

			// execute() called exactly once, not once per item
			verify(bulkInsert, times(1)).execute();

			assertEquals(5L, result.getAffectedCount());
		}

		@Test
		@DisplayName("Varargs: each argument forwarded to add()")
		void fromVarargs() {
			// ----------------------------------------------------------------
			// BeanDatastoreUtils.bulkInsert(bds, Product.class, p1, p2, p3)
			// ----------------------------------------------------------------
			BeanDatastoreUtils.bulkInsert(bds, Product.class, p1, p2, p3);

			verify(bulkInsert).add(p1);
			verify(bulkInsert).add(p2);
			verify(bulkInsert).add(p3);
			verify(bulkInsert, never()).add(p4);   // p4/p5 not in varargs
			verify(bulkInsert, never()).add(p5);
			verify(bulkInsert, times(1)).execute();
		}

		@Test
		@DisplayName("Insertion order is preserved (add calls happen in list order)")
		void orderPreserved() {
			// ----------------------------------------------------------------
			// Use InOrder to verify add(p1), add(p2), add(p3) happen
			// sequentially before execute().
			// ----------------------------------------------------------------
			BeanDatastoreUtils.bulkInsert(bds, Product.class, List.of(p1, p2, p3));

			InOrder order = inOrder(bulkInsert);
			order.verify(bulkInsert).add(p1);
			order.verify(bulkInsert).add(p2);
			order.verify(bulkInsert).add(p3);
			order.verify(bulkInsert).execute();
		}

		@Test
		@DisplayName("WriteOption is forwarded to bulkInsert()")
		void writeOptionForwarded() {
			// ----------------------------------------------------------------
			// BeanDatastoreUtils.bulkInsert(bds, Product.class, list, BRING_BACK...)
			// ----------------------------------------------------------------
			BeanDatastoreUtils.bulkInsert(bds, Product.class,
					List.of(p1), DefaultWriteOption.BRING_BACK_GENERATED_IDS_AND_VERSION);

			verify(bds).bulkInsert(eq(Product.class),
					eq(DefaultWriteOption.BRING_BACK_GENERATED_IDS_AND_VERSION));
		}

		@Test
		@DisplayName("Empty collection throws IllegalArgumentException — execute() not called")
		void emptyCollectionRejected() {
			assertThrows(IllegalArgumentException.class,
					() -> BeanDatastoreUtils.bulkInsert(bds, Product.class, List.of()));

			verify(bulkInsert, never()).execute();
		}
	}

	// ── BULK DELETE ─────────────────────────────────────────────────────────

	@Nested
	@DisplayName("Bulk DELETE")
	class BulkDeleteTests {

		@Test
		@DisplayName("Filter forwarded and execute() called once")
		void deleteByFilter() {
			// ----------------------------------------------------------------
			// SQL equivalent:  DELETE FROM product WHERE category = 'TECH'
			//
			// BeanDatastoreUtils.bulkDelete(bds, Product.class, techFilter)
			//   → bds.bulkDelete(Product.class)
			//   → op.filter(techFilter)
			//   → op.execute()
			// ----------------------------------------------------------------
			QueryFilter techFilter = mock(QueryFilter.class);

			BeanOperationResult<?> result =
					BeanDatastoreUtils.bulkDelete(bds, Product.class, techFilter);

			verify(bulkDelete).filter(techFilter);    // filter applied
			verify(bulkDelete, times(1)).execute();   // single execute

			assertEquals(5L, result.getAffectedCount());
		}

		@Test
		@DisplayName("Null filter throws IllegalArgumentException — no DB call made")
		void nullFilterRejected() {
			assertThrows(IllegalArgumentException.class,
					() -> BeanDatastoreUtils.bulkDelete(bds, Product.class, null));

			verify(bulkDelete, never()).execute();
		}

		@Test
		@DisplayName("WriteOption is forwarded to bulkDelete()")
		void writeOptionForwarded() {
			QueryFilter filter = mock(QueryFilter.class);

			BeanDatastoreUtils.bulkDelete(bds, Product.class, filter,
					DefaultWriteOption.BRING_BACK_GENERATED_IDS_AND_VERSION);

			verify(bds).bulkDelete(eq(Product.class),
					eq(DefaultWriteOption.BRING_BACK_GENERATED_IDS_AND_VERSION));
		}
	}

	// ── BULK UPDATE — builder style ─────────────────────────────────────────

	@Nested
	@DisplayName("Bulk UPDATE — builder style")
	class BulkUpdateBuilderTests {

		@Test
		@DisplayName("Single .set() call with filter; execute() returns result")
		void singleSet() {
			// ----------------------------------------------------------------
			// SQL equivalent:
			//   UPDATE product SET category = 'SALE' WHERE category = 'HOME'
			//
			// BeanDatastoreUtils.bulkUpdate(bds, Product.class, homeFilter)
			//         .set("category", "SALE")
			//         .execute();
			// ----------------------------------------------------------------
			QueryFilter homeFilter = mock(QueryFilter.class);
			when(bulkUpdate.execute()).thenReturn(RESULT_1);

			BeanOperationResult<?> result =
					BeanDatastoreUtils.bulkUpdate(bds, Product.class, homeFilter)
							.set("category", "SALE")
							.execute();

			InOrder order = inOrder(bulkUpdate);
			order.verify(bulkUpdate).filter(homeFilter);
			order.verify(bulkUpdate).set("category", "SALE");
			order.verify(bulkUpdate).execute();

			assertEquals(1L, result.getAffectedCount());
		}

		@Test
		@DisplayName("Multiple chained .set() calls in one bulk update")
		void multipleChainedSets() {
			// ----------------------------------------------------------------
			// SQL equivalent:
			//   UPDATE product SET category = 'SALE', price = 49.99
			//   WHERE category = 'HOME'
			//
			// BeanDatastoreUtils.bulkUpdate(bds, Product.class, homeFilter)
			//         .set("category", "SALE")
			//         .set("price",    49.99)
			//         .execute();
			// ----------------------------------------------------------------
			QueryFilter homeFilter = mock(QueryFilter.class);

			BeanDatastoreUtils.bulkUpdate(bds, Product.class, homeFilter)
					.set("category", "SALE")
					.set("price",    49.99)
					.execute();

			verify(bulkUpdate).filter(homeFilter);
			verify(bulkUpdate).set("category", "SALE");
			verify(bulkUpdate).set("price",    49.99);
			verify(bulkUpdate, times(1)).execute();
		}

		@Test
		@DisplayName("setNull() clears a property value")
		void setNull() {
			// ----------------------------------------------------------------
			// SQL equivalent:
			//   UPDATE product SET category = NULL WHERE id = 99
			// ----------------------------------------------------------------
			QueryFilter idFilter = mock(QueryFilter.class);

			BeanDatastoreUtils.bulkUpdate(bds, Product.class, idFilter)
					.setNull("category")
					.execute();

			verify(bulkUpdate).setNull("category");
			verify(bulkUpdate, times(1)).execute();
		}

		@Test
		@DisplayName("execute() is NOT called if caller omits it (builder returned)")
		void executeNotCalledAutomatically() {
			// ----------------------------------------------------------------
			// bulkUpdate() returns a builder — the caller must call execute().
			// This test documents that if the caller forgets, nothing is sent
			// to the datastore.
			// ----------------------------------------------------------------
			QueryFilter anyFilter = mock(QueryFilter.class);

			// Caller forgets to call .execute()
			BeanDatastoreUtils.bulkUpdate(bds, Product.class, anyFilter)
					.set("category", "SALE");
			//   ↑ Missing .execute() → the UPDATE is never issued

			verify(bulkUpdate, never()).execute();
		}
	}

	// ── BULK UPDATE — single-property shortcut ──────────────────────────────

	@Nested
	@DisplayName("Bulk UPDATE — single-property shortcut")
	class BulkUpdatePropertyTests {

		@Test
		@DisplayName("bulkUpdateProperty sets one field and executes immediately")
		void singlePropertyUpdate() {
			// ----------------------------------------------------------------
			// SQL equivalent:
			//   UPDATE product SET active = false WHERE expiredAt < NOW()
			//
			// BeanDatastoreUtils.bulkUpdateProperty(
			//         bds, Product.class, expiredFilter, "active", false);
			// ----------------------------------------------------------------
			QueryFilter expiredFilter = mock(QueryFilter.class);

			BeanDatastoreUtils.bulkUpdateProperty(
					bds, Product.class, expiredFilter, "active", false);

			InOrder order = inOrder(bulkUpdate);
			order.verify(bulkUpdate).filter(expiredFilter);
			order.verify(bulkUpdate).set("active", false);
			order.verify(bulkUpdate).execute();
		}

		@Test
		@DisplayName("Null propertyName throws IllegalArgumentException")
		void nullPropertyNameRejected() {
			QueryFilter filter = mock(QueryFilter.class);
			assertThrows(IllegalArgumentException.class,
					() -> BeanDatastoreUtils.bulkUpdateProperty(
							bds, Product.class, filter, null, "SALE"));
			verify(bulkUpdate, never()).execute();
		}
	}

	// ── BULK UPDATE — multi-property map shortcut ───────────────────────────

	@Nested
	@DisplayName("Bulk UPDATE — multi-property map shortcut")
	class BulkUpdatePropertiesTests {

		@Test
		@DisplayName("All map entries forwarded as set() calls")
		void allEntriesForwarded() {
			// ----------------------------------------------------------------
			// SQL equivalent:
			//   UPDATE product SET category = 'SALE', price = 49.99
			//   WHERE category = 'HOME'
			//
			// Map<String, Object> updates = new LinkedHashMap<>();
			// updates.put("category", "SALE");
			// updates.put("price",    49.99);
			// BeanDatastoreUtils.bulkUpdateProperties(bds, Product.class, homeFilter, updates);
			// ----------------------------------------------------------------
			QueryFilter homeFilter = mock(QueryFilter.class);

			Map<String, Object> updates = new LinkedHashMap<>();
			updates.put("category", "SALE");
			updates.put("price",    49.99);

			BeanDatastoreUtils.bulkUpdateProperties(bds, Product.class, homeFilter, updates);

			verify(bulkUpdate).filter(homeFilter);
			verify(bulkUpdate).set("category", "SALE");
			verify(bulkUpdate).set("price",    49.99);
			verify(bulkUpdate, times(1)).execute();
		}

		@Test
		@DisplayName("Empty map throws IllegalArgumentException — execute() not called")
		void emptyMapRejected() {
			QueryFilter filter = mock(QueryFilter.class);
			assertThrows(IllegalArgumentException.class,
					() -> BeanDatastoreUtils.bulkUpdateProperties(
							bds, Product.class, filter, Map.of()));
			verify(bulkUpdate, never()).execute();
		}
	}

	// ── Combined: insert catalogue, update sale items, delete expired ────────

	@Nested
	@DisplayName("Combined scenario: insert → update → delete")
	class CombinedScenarioTest {

		@Test
		@DisplayName("Full catalogue management cycle in three bulk calls")
		void catalogueManagementCycle() {
			// ----------------------------------------------------------------
			// Step 1 — Import a fresh batch of 5 products in one DB round-trip
			// ─────────────────────────────────────────────────────────────────
			// SQL: INSERT INTO product (id, name, price, category) VALUES (...)
			//                                                              × 5
			// ----------------------------------------------------------------
			BeanDatastoreUtils.bulkInsert(bds, Product.class,
					List.of(p1, p2, p3, p4, p5));

			verify(bulkInsert, times(5)).add(any(Product.class));
			verify(bulkInsert, times(1)).execute();

			// ----------------------------------------------------------------
			// Step 2 — Mark all HOME products as "SALE" in one UPDATE
			// ─────────────────────────────────────────────────────────────────
			// SQL: UPDATE product SET category = 'SALE' WHERE category = 'HOME'
			// ----------------------------------------------------------------
			QueryFilter homeFilter = mock(QueryFilter.class);

			BeanDatastoreUtils.bulkUpdateProperty(
					bds, Product.class, homeFilter, "category", "SALE");

			verify(bulkUpdate).filter(homeFilter);
			verify(bulkUpdate).set("category", "SALE");
			verify(bulkUpdate, times(1)).execute();

			// ----------------------------------------------------------------
			// Step 3 — Purge all TECH products in one DELETE
			// ─────────────────────────────────────────────────────────────────
			// SQL: DELETE FROM product WHERE category = 'TECH'
			// ----------------------------------------------------------------
			QueryFilter techFilter = mock(QueryFilter.class);

			BeanDatastoreUtils.bulkDelete(bds, Product.class, techFilter);

			verify(bulkDelete).filter(techFilter);
			verify(bulkDelete, times(1)).execute();
		}
	}

	// ── BULK UPDATE — List<T> ────────────────────────────────────────────────

	@Nested
	@DisplayName("Bulk UPDATE from List<T>  (individual UPDATE per bean)")
	class BulkUpdateListTests {

		@Test
		@DisplayName("update() called once per bean; total affected count returned")
		void updatesEachBean() {
			// ----------------------------------------------------------------
			// BeanDatastoreUtils.bulkUpdate(bds, List.of(p1, p2, p3))
			//   → bds.update(p1)   ← 1 row
			//   → bds.update(p2)   ← 1 row
			//   → bds.update(p3)   ← 1 row
			//   returns 3  (= sum of affectedCount for each call)
			// ----------------------------------------------------------------
			long affected = BeanDatastoreUtils.bulkUpdate(bds, List.of(p1, p2, p3));

			verify(bds).update(eq(p1), any(WriteOption[].class));
			verify(bds).update(eq(p2), any(WriteOption[].class));
			verify(bds).update(eq(p3), any(WriteOption[].class));
			verify(bds, never()).update(eq(p4), any(WriteOption[].class));
			assertEquals(3L, affected);
		}

		@Test
		@DisplayName("WriteOption forwarded to every individual update() call")
		void writeOptionForwarded() {
			BeanDatastoreUtils.bulkUpdate(bds, List.of(p1, p2),
					DefaultWriteOption.BRING_BACK_GENERATED_IDS_AND_VERSION);

			verify(bds, times(2)).update(any(Product.class),
					eq(DefaultWriteOption.BRING_BACK_GENERATED_IDS_AND_VERSION));
		}

		@Test
		@DisplayName("Empty list throws IllegalArgumentException — no update() called")
		void emptyListRejected() {
			assertThrows(IllegalArgumentException.class,
					() -> BeanDatastoreUtils.bulkUpdate(bds, List.of()));
			verify(bds, never()).update(any(), any(WriteOption[].class));
		}
	}

	// ── BULK SAVE — List<T> ──────────────────────────────────────────────────

	@Nested
	@DisplayName("Bulk SAVE from List<T>  (individual SAVE/upsert per bean)")
	class BulkSaveListTests {

		@Test
		@DisplayName("save() called once per bean; total affected count returned")
		void savesEachBean() {
			// ----------------------------------------------------------------
			// BeanDatastoreUtils.bulkSave(bds, List.of(p1, p2, p3, p4, p5))
			//   → bds.save(p1..p5)  each returning affectedCount=1
			//   returns 5
			// ----------------------------------------------------------------
			long affected = BeanDatastoreUtils.bulkSave(bds, List.of(p1, p2, p3, p4, p5));

			verify(bds).save(eq(p1), any(WriteOption[].class));
			verify(bds).save(eq(p2), any(WriteOption[].class));
			verify(bds).save(eq(p3), any(WriteOption[].class));
			verify(bds).save(eq(p4), any(WriteOption[].class));
			verify(bds).save(eq(p5), any(WriteOption[].class));
			assertEquals(5L, affected);
		}

		@Test
		@DisplayName("Empty list throws IllegalArgumentException — no save() called")
		void emptyListRejected() {
			assertThrows(IllegalArgumentException.class,
					() -> BeanDatastoreUtils.bulkSave(bds, List.of()));
			verify(bds, never()).save(any(), any(WriteOption[].class));
		}
	}

	// ── BULK DELETE — List<T> ────────────────────────────────────────────────

	@Nested
	@DisplayName("Bulk DELETE from List<T>  (individual DELETE per bean)")
	class BulkDeleteListTests {

		@Test
		@DisplayName("delete() called once per bean; total affected count returned")
		void deletesEachBean() {
			// ----------------------------------------------------------------
			// BeanDatastoreUtils.bulkDelete(bds, List.of(p1, p2))
			//   → bds.delete(p1)   ← 1 row
			//   → bds.delete(p2)   ← 1 row
			//   returns 2
			// ----------------------------------------------------------------
			long affected = BeanDatastoreUtils.bulkDelete(bds, List.of(p1, p2));

			verify(bds).delete(eq(p1), any(WriteOption[].class));
			verify(bds).delete(eq(p2), any(WriteOption[].class));
			verify(bds, never()).delete(eq(p3), any(WriteOption[].class));
			assertEquals(2L, affected);
		}

		@Test
		@DisplayName("WriteOption forwarded to every individual delete() call")
		void writeOptionForwarded() {
			BeanDatastoreUtils.bulkDelete(bds, List.of(p1, p2, p3),
					DefaultWriteOption.BRING_BACK_GENERATED_IDS_AND_VERSION);

			verify(bds, times(3)).delete(any(Product.class),
					eq(DefaultWriteOption.BRING_BACK_GENERATED_IDS_AND_VERSION));
		}

		@Test
		@DisplayName("Empty list throws IllegalArgumentException — no delete() called")
		void emptyListRejected() {
			assertThrows(IllegalArgumentException.class,
					() -> BeanDatastoreUtils.bulkDelete(bds, List.of()));
			verify(bds, never()).delete(any(), any(WriteOption[].class));
		}

		@Test
		@DisplayName("List-based delete is independent of filter-based bulkDelete")
		void listVsFilterAreIndependent() {
			// ----------------------------------------------------------------
			// The List<T> overload delegates to individual delete(bean) calls.
			// The QueryFilter overload delegates to bulkDelete(Class, filter).
			// Both share the same bds mock but never interfere with each other.
			// ----------------------------------------------------------------
			QueryFilter filter = mock(QueryFilter.class);

			// List-based
			BeanDatastoreUtils.bulkDelete(bds, List.of(p1));

			// Filter-based (uses the underlying BulkDelete builder)
			BeanDatastoreUtils.bulkDelete(bds, Product.class, filter);

			// Each path exercised exactly once
			verify(bds, times(1)).delete(eq(p1), any(WriteOption[].class));
			verify(bulkDelete, times(1)).execute();
		}
	}
}

