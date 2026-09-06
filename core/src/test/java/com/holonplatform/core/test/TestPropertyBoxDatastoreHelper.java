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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.holonplatform.core.datastore.DataTarget;
import com.holonplatform.core.datastore.Datastore;
import com.holonplatform.core.datastore.Datastore.OperationResult;
import com.holonplatform.core.datastore.DatastoreOperations.WriteOption;
import com.holonplatform.core.datastore.PropertyBoxDatastoreHelper;
import com.holonplatform.core.datastore.beans.PageableQuery;
import com.holonplatform.core.datastore.bulk.BulkDelete;
import com.holonplatform.core.datastore.bulk.BulkInsert;
import com.holonplatform.core.datastore.bulk.BulkUpdate;
import com.holonplatform.core.property.PathProperty;
import com.holonplatform.core.property.PropertyBox;
import com.holonplatform.core.property.PropertySet;
import com.holonplatform.core.query.Query;
import com.holonplatform.core.query.QueryFilter;
import com.holonplatform.core.query.QuerySort;

/**
 * Tests for {@link PropertyBoxDatastoreHelper}.
 *
 * <p>Key contracts verified:</p>
 * <ul>
 *   <li>{@code findAll()} overloads return a <em>lazy {@link Stream}</em> — never a List.</li>
 *   <li>{@code findPage()} and {@code findSlice()} return an eager {@link List} because they
 *       are bounded (both call {@code stream(propertySet).collect(toList())} after {@code restrict}).</li>
 *   <li>{@code findAll(filter)} applies the filter to the query before streaming.</li>
 *   <li>{@code findAll(filter, sort)} applies both filter and sort.</li>
 *   <li>CRUD, count, exists, and bulk operations delegate correctly to the datastore.</li>
 * </ul>
 */
@SuppressWarnings("rawtypes")
class TestPropertyBoxDatastoreHelper {

	// ── Schema ───────────────────────────────────────────────────────────────

	static final PathProperty<Long>   ID    = PathProperty.create("id",       Long.class);
	static final PathProperty<String> NAME  = PathProperty.create("name",     String.class);
	static final PathProperty<Double> PRICE = PathProperty.create("price",    Double.class);
	static final PathProperty<String> CAT   = PathProperty.create("category", String.class);

	static final PropertySet<PathProperty<?>> PS = PropertySet.of(ID, NAME, PRICE, CAT);

	static final DataTarget<String> TARGET = DataTarget.named("product");

	// ── Catalogue: 10 PropertyBox rows, ids 1..10 ────────────────────────────

	static final List<PropertyBox> CATALOGUE = LongStream.rangeClosed(1, 10)
			.mapToObj(id -> PropertyBox.builder(ID, NAME, PRICE, CAT)
					.set(ID,    id)
					.set(NAME,  "P-" + id)
					.set(PRICE, id * 5.0)
					.set(CAT,   id % 2 == 0 ? "HOME" : "TECH")
					.build())
			.collect(Collectors.toList());

	// ── Fixture ──────────────────────────────────────────────────────────────

	private Datastore  datastore;
	private Query      q;
	private PropertyBoxDatastoreHelper<PathProperty<?>> helper;

	private static final OperationResult RESULT_1 =
			OperationResult.builder().type(Datastore.OperationType.INSERT).affectedCount(1).build();

	private final PropertyBox box1 = CATALOGUE.get(0);  // id=1
	private final PropertyBox box2 = CATALOGUE.get(1);  // id=2

	@BeforeEach
	void setUp() {
		datastore = mock(Datastore.class);
		q         = mock(Query.class);
		helper    = PropertyBoxDatastoreHelper.of(datastore, TARGET, PS);

		// datastore.query(TARGET) → q  (chainable)
		when(datastore.query(TARGET)).thenReturn(q);

		// Fluent chain stubs
		doReturn(q).when(q).filter(any(QueryFilter.class));
		doReturn(q).when(q).sort(any(QuerySort.class));
		doReturn(q).when(q).restrict(anyInt(), anyInt());

		// PropertyBoxDatastoreHelper calls stream(propertySet) ─ the Iterable overload.
		// Use Iterable.class to disambiguate from stream(QueryProjection).
		doAnswer(inv -> CATALOGUE.stream()).when(q).stream(any(Iterable.class));

		// findOne(propertySet) — the Iterable overload
		doReturn(Optional.of(box1)).when(q).findOne(any(Iterable.class));

		// count
		when(q.count()).thenReturn((long) CATALOGUE.size());

		// CRUD stubs
		when(datastore.insert (any(DataTarget.class), any(PropertyBox.class), any(WriteOption[].class))).thenReturn(RESULT_1);
		when(datastore.update (any(DataTarget.class), any(PropertyBox.class), any(WriteOption[].class))).thenReturn(RESULT_1);
		when(datastore.save   (any(DataTarget.class), any(PropertyBox.class), any(WriteOption[].class))).thenReturn(RESULT_1);
		when(datastore.delete (any(DataTarget.class), any(PropertyBox.class), any(WriteOption[].class))).thenReturn(RESULT_1);
		when(datastore.refresh(any(DataTarget.class), any(PropertyBox.class))).thenAnswer(inv -> inv.getArgument(1));
	}

	// ======================================================================
	// Construction
	// ======================================================================

	@Test
	@DisplayName("of(Datastore, DataTarget, PropertySet) stores all three references")
	void constructionFromPropertySet() {
		PropertyBoxDatastoreHelper<PathProperty<?>> h =
				PropertyBoxDatastoreHelper.of(datastore, TARGET, PS);
		assertSame(datastore, h.getDatastore());
		assertSame(TARGET,    h.getTarget());
		assertSame(PS,        h.getPropertySet());
	}

	// ======================================================================
	// findAll — MUST return Stream<PropertyBox>  (lazy, not List)
	// ======================================================================

	@Nested
	@DisplayName("findAll() → lazy Stream<PropertyBox>")
	class FindAllTests {

		@Test
		@DisplayName("findAll() streams the catalogue without restrict or count")
		void findAllNoFilter() {
			Stream<PropertyBox> result = helper.findAll();

			verify(q).stream(any(Iterable.class));
			verify(q, never()).restrict(anyInt(), anyInt());
			verify(q, never()).count();

			List<PropertyBox> collected = result.collect(Collectors.toList());
			assertEquals(CATALOGUE.size(), collected.size());
			assertEquals(1L, collected.get(0).getValue(ID));
		}

		@Test
		@DisplayName("findAll(filter) applies filter before streaming")
		void findAllWithFilter() {
			QueryFilter techFilter = mock(QueryFilter.class);

			Stream<PropertyBox> result = helper.findAll(techFilter);

			verify(q).filter(techFilter);
			verify(q).stream(any(Iterable.class));
			verify(q, never()).restrict(anyInt(), anyInt());
			verify(q, never()).count();

			assertNotNull(result);
		}

		@Test
		@DisplayName("findAll(filter, sort) applies both filter and sort")
		void findAllWithFilterAndSort() {
			QueryFilter filter = mock(QueryFilter.class);
			QuerySort   sort   = mock(QuerySort.class);

			Stream<PropertyBox> result = helper.findAll(filter, sort);

			verify(q).filter(filter);
			verify(q).sort(sort);
			verify(q).stream(any(Iterable.class));
			verify(q, never()).restrict(anyInt(), anyInt());
			verify(q, never()).count();

			assertNotNull(result);
		}

		@Test
		@DisplayName("findAll(null, null) skips filter and sort but still streams")
		void findAllNullFilterAndSort() {
			helper.findAll((QueryFilter) null, (QuerySort) null);

			verify(q, never()).filter(any(QueryFilter.class));
			verify(q, never()).sort(any(QuerySort.class));
			verify(q).stream(any(Iterable.class));
			verify(q, never()).count();
		}

		@Test
		@DisplayName("findAll() stream is closeable (try-with-resources)")
		void findAllStreamIsCloseable() {
			long count;
			try (Stream<PropertyBox> s = helper.findAll()) {
				count = s.count();
			}
			assertEquals(CATALOGUE.size(), count);
			verify(q, never()).count(); // Stream.count() != query.count()
		}

		@Test
		@DisplayName("findAll() vs findPage(): Stream vs List — different restrict usage")
		void streamVsListContrast() {
			// findAll() → Stream (lazy, no restrict)
			Stream<PropertyBox> stream = helper.findAll();
			assertNotNull(stream);

			// findPage(0, 3) → List (bounded via restrict)
			doAnswer(call -> {
				int limit  = call.getArgument(0);
				int offset = call.getArgument(1);
				int from   = Math.min(offset, CATALOGUE.size());
				int to     = Math.min(from + limit, CATALOGUE.size());
				List<PropertyBox> slice = CATALOGUE.subList(from, to);
				doAnswer(ignored -> slice.stream()).when(q).stream(any(Iterable.class));
				return q;
			}).when(q).restrict(anyInt(), anyInt());

			List<PropertyBox> page = helper.findPage(0, 3);
			assertNotNull(page);
			assertEquals(3, page.size());

			// count() was never called for either operation
			verify(q, never()).count();
		}
	}

	// ======================================================================
	// findOne — Optional<PropertyBox>
	// ======================================================================

	@Test
	@DisplayName("findOne(filter) returns Optional backed by query.findOne()")
	void findOne() {
		QueryFilter filter = mock(QueryFilter.class);

		Optional<PropertyBox> result = helper.findOne(filter);

		verify(q).filter(filter);
		assertTrue(result.isPresent());
		assertSame(box1, result.get());
	}

	// ======================================================================
	// findFirst — Optional<PropertyBox>, LIMIT 1 OFFSET 0
	// ======================================================================

	@Nested
	@DisplayName("findFirst() → Optional<PropertyBox>  (LIMIT 1 OFFSET 0)")
	class FindFirstTests {

		@BeforeEach
		void stubRestrict1() {
			doAnswer(call -> {
				int limit  = call.getArgument(0);
				int offset = call.getArgument(1);
				int from   = Math.min(offset, CATALOGUE.size());
				int to     = Math.min(from + limit, CATALOGUE.size());
				List<PropertyBox> slice = CATALOGUE.subList(from, to);
				doAnswer(ignored -> slice.stream()).when(q).stream(any(Iterable.class));
				return q;
			}).when(q).restrict(anyInt(), anyInt());
		}

		@Test
		@DisplayName("findFirst() — no filter/sort — calls restrict(1, 0)")
		void findFirstNoArgs() {
			Optional<PropertyBox> result = helper.findFirst();

			verify(q).restrict(1, 0);
			assertTrue(result.isPresent());
			assertEquals(1L, result.get().getValue(ID));
		}

		@Test
		@DisplayName("findFirst(filter) — applies filter then restrict(1, 0)")
		void findFirstWithFilter() {
			QueryFilter filter = mock(QueryFilter.class);

			Optional<PropertyBox> result = helper.findFirst(filter);

			verify(q).filter(filter);
			verify(q).restrict(1, 0);
			assertTrue(result.isPresent());
		}

		@Test
		@DisplayName("findFirst(filter, sort) — applies filter + sort then restrict(1, 0)")
		void findFirstWithFilterAndSort() {
			QueryFilter filter = mock(QueryFilter.class);
			QuerySort   sort   = mock(QuerySort.class);

			Optional<PropertyBox> result = helper.findFirst(filter, sort);

			verify(q).filter(filter);
			verify(q).sort(sort);
			verify(q).restrict(1, 0);
			assertTrue(result.isPresent());
		}

		@Test
		@DisplayName("findFirst() on empty target returns Optional.empty()")
		void findFirstEmptyTarget() {
			doReturn(q).when(q).restrict(anyInt(), anyInt());
			doAnswer(ignored -> Stream.empty()).when(q).stream(any(Iterable.class));

			Optional<PropertyBox> result = helper.findFirst();
			assertFalse(result.isPresent());
		}
	}

	// ======================================================================
	// findTop — List<PropertyBox>, LIMIT n OFFSET 0
	// ======================================================================

	@Nested
	@DisplayName("findTop(n, …) → List<PropertyBox>  (LIMIT n OFFSET 0)")
	class FindTopTests {

		@BeforeEach
		void stubRestrict() {
			doAnswer(call -> {
				int limit  = call.getArgument(0);
				int offset = call.getArgument(1);
				int from   = Math.min(offset, CATALOGUE.size());
				int to     = Math.min(from + limit, CATALOGUE.size());
				List<PropertyBox> slice = CATALOGUE.subList(from, to);
				doAnswer(ignored -> slice.stream()).when(q).stream(any(Iterable.class));
				return q;
			}).when(q).restrict(anyInt(), anyInt());
		}

		@Test
		@DisplayName("findTop(3) — no filter/sort — calls restrict(3, 0)")
		void findTop3NoArgs() {
			List<PropertyBox> top = helper.findTop(3);

			verify(q).restrict(3, 0);
			verify(q, never()).filter(any(QueryFilter.class));
			verify(q, never()).sort(any(QuerySort.class));
			assertEquals(3, top.size());
			assertEquals(1L, top.get(0).getValue(ID));
			assertEquals(3L, top.get(2).getValue(ID));
		}

		@Test
		@DisplayName("findTop(5, sort) — applies sort then restrict(5, 0)")
		void findTopWithSort() {
			QuerySort sort = mock(QuerySort.class);

			List<PropertyBox> top = helper.findTop(5, sort);

			verify(q).sort(sort);
			verify(q, never()).filter(any(QueryFilter.class));
			verify(q).restrict(5, 0);
			assertEquals(5, top.size());
		}

		@Test
		@DisplayName("findTop(4, filter) — applies filter then restrict(4, 0)")
		void findTopWithFilter() {
			QueryFilter filter = mock(QueryFilter.class);

			List<PropertyBox> top = helper.findTop(4, filter);

			verify(q).filter(filter);
			verify(q, never()).sort(any(QuerySort.class));
			verify(q).restrict(4, 0);
			assertEquals(4, top.size());
		}

		@Test
		@DisplayName("findTop(3, filter, sort) — applies both then restrict(3, 0)")
		void findTopWithFilterAndSort() {
			QueryFilter filter = mock(QueryFilter.class);
			QuerySort   sort   = mock(QuerySort.class);

			List<PropertyBox> top = helper.findTop(3, filter, sort);

			verify(q).filter(filter);
			verify(q).sort(sort);
			verify(q).restrict(3, 0);
			assertEquals(3, top.size());
		}

		@Test
		@DisplayName("findTop(n) when n > total rows returns all rows (no error)")
		void findTopBeyondSize() {
			List<PropertyBox> top = helper.findTop(100);

			verify(q).restrict(100, 0);
			assertEquals(CATALOGUE.size(), top.size());
		}

		@Test
		@DisplayName("findTop(0) throws IllegalArgumentException")
		void findTopZeroRejected() {
			assertThrows(IllegalArgumentException.class, () -> helper.findTop(0));
		}

		@Test
		@DisplayName("findTop vs findPage: same LIMIT, different OFFSET")
		void findTopVsFindPage() {
			helper.findTop(3);
			helper.findPage(2, 3);

			verify(q).restrict(3, 0);
			verify(q).restrict(3, 6);
		}
	}

	// ======================================================================
	// Pagination — findPage / findSlice return List<PropertyBox>
	// ======================================================================

	@Nested
	@DisplayName("findPage() and findSlice() return eager List (bounded by restrict)")
	class PaginationTests {

		@BeforeEach
		void stubRestrict() {
			doAnswer(call -> {
				int limit  = call.getArgument(0);
				int offset = call.getArgument(1);
				int from   = Math.min(offset, CATALOGUE.size());
				int to     = Math.min(offset + limit, CATALOGUE.size());
				List<PropertyBox> slice = CATALOGUE.subList(from, to);
				doAnswer(ignored -> slice.stream()).when(q).stream(any(Iterable.class));
				return q;
			}).when(q).restrict(anyInt(), anyInt());
		}

		@Test
		@DisplayName("findPage(0, 5) → List of 5 items; calls restrict(5, 0)")
		void findPageFirstPage() {
			List<PropertyBox> page = helper.findPage(0, 5);

			verify(q).restrict(5, 0);
			verify(q, never()).count();
			assertEquals(5, page.size());
			assertEquals(1L, page.get(0).getValue(ID));
		}

		@Test
		@DisplayName("findPage(1, 5) → List of 5 items; calls restrict(5, 5)")
		void findPageSecondPage() {
			List<PropertyBox> page = helper.findPage(1, 5);

			verify(q).restrict(5, 5);
			assertEquals(5, page.size());
			assertEquals(6L, page.get(0).getValue(ID));
		}

		@Test
		@DisplayName("findPage(filter, sort, 0, 5) applies filter+sort then restrict(5, 0)")
		void findPageWithFilterAndSort() {
			QueryFilter filter = mock(QueryFilter.class);
			QuerySort   sort   = mock(QuerySort.class);

			List<PropertyBox> page = helper.findPage(filter, sort, 0, 5);

			verify(q).filter(filter);
			verify(q).sort(sort);
			verify(q).restrict(5, 0);
			assertEquals(5, page.size());
		}

		@Test
		@DisplayName("findSlice(3, 4) → List of 3 items; calls restrict(3, 4)")
		void findSlice() {
			List<PropertyBox> slice = helper.findSlice(3, 4);

			verify(q).restrict(3, 4);
			verify(q, never()).count();
			assertEquals(3, slice.size());
			assertEquals(5L, slice.get(0).getValue(ID)); // offset=4 → id 5
		}

		@Test
		@DisplayName("findSlice(filter, sort, limit, offset) applies filter+sort then restrict")
		void findSliceWithFilterAndSort() {
			QueryFilter filter = mock(QueryFilter.class);
			QuerySort   sort   = mock(QuerySort.class);

			List<PropertyBox> slice = helper.findSlice(filter, sort, 3, 4);

			verify(q).filter(filter);
			verify(q).sort(sort);
			verify(q).restrict(3, 4);
			assertEquals(3, slice.size());
		}
	}

	// ======================================================================
	// PageableQuery-based overloads
	// ======================================================================

	@Nested
	@DisplayName("findSlice(PageableQuery, …) → List<PropertyBox> via PageableQuery")
	class PageableQueryTests {

		@BeforeEach
		void stubRestrict() {
			doAnswer(call -> {
				int limit  = call.getArgument(0);
				int offset = call.getArgument(1);
				int from   = Math.min(offset, CATALOGUE.size());
				int to     = Math.min(from + limit, CATALOGUE.size());
				List<PropertyBox> slice = CATALOGUE.subList(from, to);
				doAnswer(ignored -> slice.stream()).when(q).stream(any(Iterable.class));
				return q;
			}).when(q).restrict(anyInt(), anyInt());
		}

		@Test
		@DisplayName("findSlice(PageableQuery) delegates to findSlice(limit, offset)")
		void findSlicePageableQuery() {
			PageableQuery pq = () -> 5;

			List<PropertyBox> result = helper.findSlice(pq);

			verify(q).restrict(5, 0);
			assertEquals(5, result.size());
			assertEquals(1L, result.get(0).getValue(ID));
		}

		@Test
		@DisplayName("findSlice(PageableQuery) with custom offset")
		void findSlicePageableQueryWithOffset() {
			PageableQuery pq = new PageableQuery() {
				@Override public int getLimit()  { return 3; }
				@Override public int getOffset() { return 4; }
			};

			List<PropertyBox> result = helper.findSlice(pq);

			verify(q).restrict(3, 4);
			assertEquals(3, result.size());
			assertEquals(5L, result.get(0).getValue(ID));
		}

		@Test
		@DisplayName("findSlice(PageableQuery, filter) applies filter")
		void findSlicePageableQueryWithFilter() {
			QueryFilter filter = mock(QueryFilter.class);
			PageableQuery pq = () -> 5;

			List<PropertyBox> result = helper.findSlice(pq, filter);

			verify(q).filter(filter);
			verify(q).restrict(5, 0);
			assertEquals(5, result.size());
		}

		@Test
		@DisplayName("findSlice(PageableQuery, sort) applies sort")
		void findSlicePageableQueryWithSort() {
			QuerySort sort = mock(QuerySort.class);
			PageableQuery pq = () -> 5;

			List<PropertyBox> result = helper.findSlice(pq, sort);

			verify(q).sort(sort);
			verify(q).restrict(5, 0);
			assertEquals(5, result.size());
		}

		@Test
		@DisplayName("findSlice(PageableQuery, filter, sort) applies both")
		void findSlicePageableQueryWithFilterAndSort() {
			QueryFilter filter = mock(QueryFilter.class);
			QuerySort   sort   = mock(QuerySort.class);
			PageableQuery pq = () -> 5;

			List<PropertyBox> result = helper.findSlice(pq, filter, sort);

			verify(q).filter(filter);
			verify(q).sort(sort);
			verify(q).restrict(5, 0);
			assertEquals(5, result.size());
		}
	}

	// ======================================================================
	// Count / exists
	// ======================================================================

	@Test
	@DisplayName("count() returns total row count from query.count()")
	void countAll() {
		assertEquals(CATALOGUE.size(), helper.count());
		verify(q).count();
	}

	@Test
	@DisplayName("count(filter) applies filter then counts")
	void countWithFilter() {
		QueryFilter filter = mock(QueryFilter.class);
		when(q.count()).thenReturn(3L);

		assertEquals(3L, helper.count(filter));

		verify(q).filter(filter);
		verify(q).count();
	}

	@Test
	@DisplayName("exists(filter) returns true when count > 0")
	void existsTrue() {
		QueryFilter filter = mock(QueryFilter.class);
		when(q.count()).thenReturn(1L);

		assertTrue(helper.exists(filter));
	}

	@Test
	@DisplayName("exists(filter) returns false when count == 0")
	void existsFalse() {
		QueryFilter filter = mock(QueryFilter.class);
		when(q.count()).thenReturn(0L);

		assertFalse(helper.exists(filter));
	}

	// ======================================================================
	// CRUD
	// ======================================================================

	@Nested
	@DisplayName("Single-box CRUD operations")
	class CrudTests {

		@Test
		void insert() {
			OperationResult r = helper.insert(box1);
			assertEquals(1L, r.getAffectedCount());
			verify(datastore).insert(eq(TARGET), eq(box1), any(WriteOption[].class));
		}

		@Test
		void update() {
			OperationResult r = helper.update(box1);
			assertEquals(1L, r.getAffectedCount());
			verify(datastore).update(eq(TARGET), eq(box1), any(WriteOption[].class));
		}

		@Test
		void save() {
			OperationResult r = helper.save(box1);
			assertEquals(1L, r.getAffectedCount());
			verify(datastore).save(eq(TARGET), eq(box1), any(WriteOption[].class));
		}

		@Test
		void delete() {
			OperationResult r = helper.delete(box1);
			assertEquals(1L, r.getAffectedCount());
			verify(datastore).delete(eq(TARGET), eq(box1), any(WriteOption[].class));
		}

		@Test
		void refresh() {
			PropertyBox refreshed = helper.refresh(box2);
			assertSame(box2, refreshed);
			verify(datastore).refresh(eq(TARGET), eq(box2));
		}
	}

	// ======================================================================
	// Bulk operations
	// ======================================================================

	@Nested
	@DisplayName("Bulk operations")
	class BulkTests {

		private BulkInsert bi;
		private BulkUpdate bu;
		private BulkDelete bd;

		@BeforeEach
		void stubBulk() {
			OperationResult bulkResult =
					OperationResult.builder().type(Datastore.OperationType.INSERT).affectedCount(2).build();

			bi = mock(BulkInsert.class);
			doReturn(bi).when(bi).add(any(PropertyBox.class));
			when(bi.execute()).thenReturn(bulkResult);
			when(datastore.bulkInsert(any(DataTarget.class), any(), any(WriteOption[].class))).thenReturn(bi);

			bu = mock(BulkUpdate.class);
			doReturn(bu).when(bu).filter(any(QueryFilter.class));
			when(datastore.bulkUpdate(any(DataTarget.class), any(WriteOption[].class))).thenReturn(bu);

			bd = mock(BulkDelete.class);
			doReturn(bd).when(bd).filter(any(QueryFilter.class));
			when(bd.execute()).thenReturn(bulkResult);
			when(datastore.bulkDelete(any(DataTarget.class), any(WriteOption[].class))).thenReturn(bd);
		}

		@Test
		@DisplayName("bulkInsert(collection) adds each box then executes")
		void bulkInsert() {
			List<PropertyBox> toInsert = List.of(box1, box2);

			OperationResult r = helper.bulkInsert(toInsert);

			verify(datastore).bulkInsert(eq(TARGET), eq(PS), any(WriteOption[].class));
			verify(bi, times(2)).add(any(PropertyBox.class));
			verify(bi).execute();
			assertEquals(2L, r.getAffectedCount());
		}

		@Test
		@DisplayName("bulkUpdate() returns a BulkUpdate builder pre-set to TARGET")
		void bulkUpdateNoFilter() {
			BulkUpdate result = helper.bulkUpdate();

			verify(datastore).bulkUpdate(eq(TARGET), any(WriteOption[].class));
			assertSame(bu, result);
		}

		@Test
		@DisplayName("bulkUpdate(filter) returns a BulkUpdate builder pre-filtered")
		void bulkUpdateWithFilter() {
			QueryFilter filter = mock(QueryFilter.class);

			BulkUpdate result = helper.bulkUpdate(filter);

			verify(datastore).bulkUpdate(eq(TARGET), any(WriteOption[].class));
			verify(bu).filter(filter);
			assertSame(bu, result);
		}

		@Test
		@DisplayName("bulkDelete(filter) executes a filtered bulk delete")
		void bulkDelete() {
			QueryFilter filter = mock(QueryFilter.class);

			OperationResult r = helper.bulkDelete(filter);

			verify(datastore).bulkDelete(eq(TARGET), any(WriteOption[].class));
			verify(bd).filter(filter);
			verify(bd).execute();
			assertEquals(2L, r.getAffectedCount());
		}
	}
}
