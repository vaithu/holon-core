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

import com.holonplatform.core.Path;
import com.holonplatform.core.beans.BeanPropertySet;
import com.holonplatform.core.datastore.DatastoreOperations.WriteOption;
import com.holonplatform.core.datastore.beans.BeanDatastore;
import com.holonplatform.core.datastore.beans.BeanDatastore.BeanOperationResult;
import com.holonplatform.core.datastore.beans.BeanDatastoreHelper;
import com.holonplatform.core.datastore.beans.BeanQuery;
import com.holonplatform.core.datastore.beans.PageableQuery;
import com.holonplatform.core.query.QueryFilter;
import com.holonplatform.core.query.QuerySort;
import com.holonplatform.core.test.data.Product;

/**
 * Tests for {@link BeanDatastoreHelper}.
 *
 * <p>Key contracts verified:</p>
 * <ul>
 *   <li>{@code findAll()} overloads return a <em>lazy {@link Stream}</em> — never a List.</li>
 *   <li>{@code findPage()} and {@code findSlice()} return an eager {@link List} because they
 *       are bounded.</li>
 *   <li>{@code findAll(filter)} applies the filter to the query before streaming.</li>
 *   <li>{@code findAll(filter, sort)} applies both filter and sort.</li>
 *   <li>CRUD, count, exists, and bulk operations delegate correctly to the datastore.</li>
 * </ul>
 */
@SuppressWarnings("unchecked")
class TestBeanDatastoreHelper {

	// ── fixture ──────────────────────────────────────────────────────────────

	/** 10 sample products, ids 1..10. */
	private static final List<Product> CATALOGUE = LongStream.rangeClosed(1, 10)
			.mapToObj(id -> new Product(id, "P-" + id, id * 5.0,
					id % 2 == 0 ? "HOME" : "TECH"))
			.collect(Collectors.toList());

	private BeanDatastore         bds;
	private BeanQuery<Product>    q;
	private BeanDatastoreHelper<Product> helper;

	private static final BeanOperationResult<Product> RESULT_1 =
			BeanOperationResult.<Product>builder().affectedCount(1).build();

	private final Product p1 = CATALOGUE.get(0);
	private final Product p2 = CATALOGUE.get(1);

	@BeforeEach
	void setUp() {
		bds    = mock(BeanDatastore.class);
		q      = mock(BeanQuery.class);
		helper = BeanDatastoreHelper.of(bds, Product.class);

		// bds.query(Product.class) → q (chainable)
		when(bds.query(Product.class)).thenReturn(q);

		// Fluent chain stubs (doReturn avoids varargs ambiguity on filter/sort)
		doReturn(q).when(q).filter(any(QueryFilter.class));
		doReturn(q).when(q).sort(any(QuerySort.class));

		// restrict(limit, offset) → returns q; list() will be re-stubbed per test
		when(q.restrict(anyInt(), anyInt())).thenReturn(q);
		when(q.list()).thenReturn(CATALOGUE);

		// stream() → lazy stream backed by the catalogue
		when(q.stream()).thenAnswer(inv -> CATALOGUE.stream());

		// count
		when(q.count()).thenReturn((long) CATALOGUE.size());

		// findOne → Optional of first item
		when(q.findOne()).thenReturn(Optional.of(p1));

		// individual CRUD results — use doReturn to bypass raw-type generic mismatch
		doReturn(RESULT_1).when(bds).insert(any(Product.class), any(WriteOption[].class));
		doReturn(RESULT_1).when(bds).update(any(Product.class), any(WriteOption[].class));
		doReturn(RESULT_1).when(bds).save  (any(Product.class), any(WriteOption[].class));
		doReturn(RESULT_1).when(bds).delete(any(Product.class), any(WriteOption[].class));
		when(bds.refresh(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	// ======================================================================
	// Construction
	// ======================================================================

	@Test
	@DisplayName("of(BeanDatastore, Class) stores both references")
	void constructionFromBeanDatastore() {
		BeanDatastoreHelper<Product> h = BeanDatastoreHelper.of(bds, Product.class);
		assertSame(bds,          h.getDatastore());
		assertSame(Product.class, h.getBeanClass());
	}

	// ======================================================================
	// findAll — MUST return Stream<T>  (lazy, not List)
	// ======================================================================

	@Nested
	@DisplayName("findAll() → lazy Stream")
	class FindAllTests {

		@Test
		@DisplayName("findAll() returns Stream backed by query.stream() — not a List")
		void findAllNoFilter() {
			// ----------------------------------------------------------------
			// Contract: findAll() calls datastore.query(beanClass).stream()
			// The result is a Stream, NOT a materialised List.
			// count() must NEVER be called in advance (no eager size probe).
			// ----------------------------------------------------------------
			Stream<Product> result = helper.findAll();

			// Verify the query was opened lazily via stream(), not list() or count()
			verify(q).stream();
			verify(q, never()).list();
			verify(q, never()).count();

			// Consume the stream and check data
			List<Product> collected = result.collect(Collectors.toList());
			assertEquals(CATALOGUE.size(), collected.size());
			assertEquals(1L, collected.get(0).getId());
		}

		@Test
		@DisplayName("findAll(filter) applies filter before streaming")
		void findAllWithFilter() {
			// ----------------------------------------------------------------
			// Contract: findAll(filter) chains .filter(f).stream()
			// count() must NEVER be called in advance (no eager size probe).
			// ----------------------------------------------------------------
			QueryFilter techFilter = mock(QueryFilter.class);

			Stream<Product> result = helper.findAll(techFilter);

			verify(q).filter(techFilter);   // filter was applied
			verify(q).stream();             // result is a stream, not a list
			verify(q, never()).list();
			verify(q, never()).count();

			assertNotNull(result);
		}

		@Test
		@DisplayName("findAll(filter, sort) applies both filter and sort before streaming")
		void findAllWithFilterAndSort() {
			// ----------------------------------------------------------------
			// Contract: findAll(filter, sort) chains .filter(f).sort(s).stream()
			// count() must NEVER be called in advance (no eager size probe).
			// ----------------------------------------------------------------
			QueryFilter filter = mock(QueryFilter.class);
			QuerySort   sort   = mock(QuerySort.class);

			Stream<Product> result = helper.findAll(filter, sort);

			verify(q).filter(filter);
			verify(q).sort(sort);
			verify(q).stream();
			verify(q, never()).list();
			verify(q, never()).count();

			assertNotNull(result);
		}

		@Test
		@DisplayName("findAll(null, null) skips filter and sort but still streams")
		void findAllNullFilterAndSort() {
			// Both null → no filter/sort applied, just stream; count() never called.
			helper.findAll((QueryFilter) null, (QuerySort) null);

			verify(q, never()).filter(any(QueryFilter.class));
			verify(q, never()).sort(any(QuerySort.class));
			verify(q).stream();
			verify(q, never()).count();
		}

		@Test
		@DisplayName("findAll() stream is closeable (try-with-resources)")
		void findAllStreamIsCloseable() {
			// Streams returned by findAll() must be usable with try-with-resources.
			// count() must NOT be called to size the stream ahead of time.
			long count;
			try (Stream<Product> s = helper.findAll()) {
				count = s.count();   // terminal op on the stream, NOT a pre-probe
			}
			assertEquals(CATALOGUE.size(), count);
			verify(q, never()).count();   // datastore count() was never consulted
		}

		@Test
		@DisplayName("findAll() vs findPage(): both return Stream — different restrict semantics")
		void streamVsListContrast() {
			// findAll() → Stream (lazy, unbounded) — must not probe count in advance
			Stream<Product> stream = helper.findAll();
			assertNotNull(stream);

			// findPage() → Stream (bounded via restrict)
			doAnswer(call -> {
				doAnswer(inv -> CATALOGUE.subList(0, 3).stream()).when(q).stream();
				return q;
			}).when(q).restrict(anyInt(), anyInt());
			Stream<Product> page = helper.findPage(0, 3);
			assertNotNull(page);
			assertEquals(3, page.count());

			// stream() was called for both findAll and findPage
			verify(q, times(2)).stream();
			verify(q, never()).list();
			// count() must never have been called for either operation
			verify(q, never()).count();
		}
	}

	// ======================================================================
	// findOne — Optional<T>
	// ======================================================================

	@Test
	@DisplayName("findOne(filter) returns Optional backed by query.findOne()")
	void findOne() {
		QueryFilter filter = mock(QueryFilter.class);

		Optional<Product> result = helper.findOne(filter);

		verify(q).filter(filter);
		assertTrue(result.isPresent());
		assertSame(p1, result.get());
	}

	// ======================================================================
	// findFirst — Optional<T>, LIMIT 1 OFFSET 0
	// ======================================================================

	@Nested
	@DisplayName("findFirst() → Optional<T>  (LIMIT 1 OFFSET 0)")
	class FindFirstTests {

		/**
		 * Wire restrict(limit, offset) so that list() returns the correct sub-slice.
		 * Uses doAnswer + doReturn to avoid Mockito's "nested when() inside thenAnswer"
		 * anti-pattern (which leaves the state-machine pointing at the wrong invocation).
		 */
		@BeforeEach
		void stubRestrict1() {
			doAnswer(call -> {
				int limit  = call.getArgument(0);
				int offset = call.getArgument(1);
				int from   = Math.min(offset, CATALOGUE.size());
				int to     = Math.min(from + limit, CATALOGUE.size());
				doReturn(CATALOGUE.subList(from, to)).when(q).list();
				return q;
			}).when(q).restrict(anyInt(), anyInt());
		}

		@Test
		@DisplayName("findFirst() — no filter/sort — calls restrict(1, 0)")
		void findFirstNoArgs() {
			Optional<Product> result = helper.findFirst();

			verify(q).restrict(1, 0);
			verify(q, never()).stream();
			assertTrue(result.isPresent());
			assertEquals(1L, result.get().getId());
		}

		@Test
		@DisplayName("findFirst(filter) — applies filter then restrict(1, 0)")
		void findFirstWithFilter() {
			QueryFilter filter = mock(QueryFilter.class);

			Optional<Product> result = helper.findFirst(filter);

			verify(q).filter(filter);
			verify(q).restrict(1, 0);
			assertTrue(result.isPresent());
		}

		@Test
		@DisplayName("findFirst(filter, sort) — applies filter + sort then restrict(1, 0)")
		void findFirstWithFilterAndSort() {
			QueryFilter filter = mock(QueryFilter.class);
			QuerySort   sort   = mock(QuerySort.class);

			Optional<Product> result = helper.findFirst(filter, sort);

			verify(q).filter(filter);
			verify(q).sort(sort);
			verify(q).restrict(1, 0);
			assertTrue(result.isPresent());
		}

		@Test
		@DisplayName("findFirst() on empty table returns Optional.empty()")
		void findFirstEmptyTable() {
			// Override restrict to just return q (without re-stubbing list),
			// then set list to return nothing.
			doReturn(q).when(q).restrict(anyInt(), anyInt());
			doReturn(List.of()).when(q).list();

			Optional<Product> result = helper.findFirst();
			assertFalse(result.isPresent());
		}
	}

	// ======================================================================
	// findTop — Stream<T>, LIMIT n OFFSET 0
	// ======================================================================

	@Nested
	@DisplayName("findTop(n, …) → Stream<T>  (LIMIT n OFFSET 0)")
	class FindTopTests {

		@BeforeEach
		void stubRestrict() {
			doAnswer(call -> {
				int limit  = call.getArgument(0);
				int offset = call.getArgument(1);
				int from   = Math.min(offset, CATALOGUE.size());
				int to     = Math.min(from + limit, CATALOGUE.size());
				List<Product> subList = CATALOGUE.subList(from, to);
				doAnswer(inv -> subList.stream()).when(q).stream();
				return q;
			}).when(q).restrict(anyInt(), anyInt());
		}

		@Test
		@DisplayName("findTop(3) — no filter/sort — calls restrict(3, 0)")
		void findTop3NoArgs() {
			Stream<Product> top = helper.findTop(3);

			verify(q).restrict(3, 0);
			verify(q).stream();

			List<Product> collected = top.collect(Collectors.toList());
			assertEquals(3, collected.size());
			assertEquals(1L, collected.get(0).getId());
			assertEquals(3L, collected.get(2).getId());
		}

		@Test
		@DisplayName("findTop(5, sort) — applies sort then restrict(5, 0)")
		void findTopWithSort() {
			QuerySort sort = mock(QuerySort.class);

			Stream<Product> top = helper.findTop(5, sort);

			verify(q).sort(sort);
			verify(q, never()).filter(any(QueryFilter.class));
			verify(q).restrict(5, 0);
			verify(q).stream();
			assertEquals(5, top.count());
		}

		@Test
		@DisplayName("findTop(4, filter) — applies filter then restrict(4, 0)")
		void findTopWithFilter() {
			QueryFilter filter = mock(QueryFilter.class);

			Stream<Product> top = helper.findTop(4, filter);

			verify(q).filter(filter);
			verify(q, never()).sort(any(QuerySort.class));
			verify(q).restrict(4, 0);
			verify(q).stream();
			assertEquals(4, top.count());
		}

		@Test
		@DisplayName("findTop(3, filter, sort) — applies both then restrict(3, 0)")
		void findTopWithFilterAndSort() {
			QueryFilter filter = mock(QueryFilter.class);
			QuerySort   sort   = mock(QuerySort.class);

			Stream<Product> top = helper.findTop(3, filter, sort);

			verify(q).filter(filter);
			verify(q).sort(sort);
			verify(q).restrict(3, 0);
			verify(q).stream();
			assertEquals(3, top.count());
		}

		@Test
		@DisplayName("findTop(n) when n > total rows returns all rows (no error)")
		void findTopBeyondSize() {
			Stream<Product> top = helper.findTop(100);

			verify(q).restrict(100, 0);
			verify(q).stream();
			// CATALOGUE has 10 items; subList clips at size
			assertEquals(CATALOGUE.size(), top.count());
		}

		@Test
		@DisplayName("findTop(0) throws IllegalArgumentException")
		void findTopZeroRejected() {
			assertThrows(IllegalArgumentException.class, () -> helper.findTop(0));
		}

		@Test
		@DisplayName("findTop vs findPage: same LIMIT, different OFFSET")
		void findTopVsFindPage() {
			// findTop(3)      → restrict(3, 0)  — always starts at offset 0
			// findPage(2, 3)  → restrict(3, 6)  — page 2, size 3 → offset 6
			helper.findTop(3);
			helper.findPage(2, 3);

			verify(q).restrict(3, 0);
			verify(q).restrict(3, 6);
		}
	}

	// ======================================================================
	// Pagination — findPage / findSlice return Stream<T>
	// ======================================================================

	@Nested
	@DisplayName("findPage() and findSlice() return Stream (bounded via restrict)")
	class PaginationTests {

		@BeforeEach
		void stubRestrict() {
			// Make restrict() slice the catalogue so stream() returns real data.
			// Uses doAnswer to avoid the Mockito nested-when anti-pattern.
			doAnswer(call -> {
				int limit  = call.getArgument(0);
				int offset = call.getArgument(1);
				int from   = Math.min(offset, CATALOGUE.size());
				int to     = Math.min(offset + limit, CATALOGUE.size());
				List<Product> subList = CATALOGUE.subList(from, to);
				doAnswer(inv -> subList.stream()).when(q).stream();
				return q;
			}).when(q).restrict(anyInt(), anyInt());
		}

		@Test
		@DisplayName("findPage(0, 5) → Stream of 5 items; calls restrict(5, 0)")
		void findPageFirstPage() {
			Stream<Product> page = helper.findPage(0, 5);

			verify(q).restrict(5, 0);
			verify(q).stream();
			verify(q, never()).list();

			List<Product> collected = page.collect(Collectors.toList());
			assertEquals(5, collected.size());
			assertEquals(1L, collected.get(0).getId());
		}

		@Test
		@DisplayName("findPage(1, 5) → Stream of 5 items; calls restrict(5, 5)")
		void findPageSecondPage() {
			Stream<Product> page = helper.findPage(1, 5);

			verify(q).restrict(5, 5);
			verify(q).stream();

			List<Product> collected = page.collect(Collectors.toList());
			assertEquals(5, collected.size());
			assertEquals(6L, collected.get(0).getId());
		}

		@Test
		@DisplayName("findSlice(3, 4) → Stream of 3 items; calls restrict(3, 4)")
		void findSlice() {
			Stream<Product> slice = helper.findSlice(3, 4);

			verify(q).restrict(3, 4);
			verify(q).stream();
			verify(q, never()).list();

			List<Product> collected = slice.collect(Collectors.toList());
			assertEquals(3, collected.size());
			assertEquals(5L, collected.get(0).getId()); // offset=4 → starts at id=5
		}
	}

	// ======================================================================
	// Count / exists
	// ======================================================================

	@Test
	@DisplayName("count() returns total record count")
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
	@DisplayName("Single-bean CRUD")
	class CrudTests {

		@Test
		void insert() {
			BeanOperationResult<Product> r = helper.insert(p1);
			assertEquals(1L, r.getAffectedCount());
			verify(bds).insert(eq(p1), any(WriteOption[].class));
		}

		@Test
		void update() {
			BeanOperationResult<Product> r = helper.update(p1);
			assertEquals(1L, r.getAffectedCount());
			verify(bds).update(eq(p1), any(WriteOption[].class));
		}

		@Test
		void save() {
			BeanOperationResult<Product> r = helper.save(p1);
			assertEquals(1L, r.getAffectedCount());
			verify(bds).save(eq(p1), any(WriteOption[].class));
		}

		@Test
		void delete() {
			BeanOperationResult<Product> r = helper.delete(p1);
			assertEquals(1L, r.getAffectedCount());
			verify(bds).delete(eq(p1), any(WriteOption[].class));
		}

		@Test
		void refresh() {
			Product refreshed = helper.refresh(p2);
			assertSame(p2, refreshed);
			verify(bds).refresh(p2);
		}
	}

	// ======================================================================
	// Column-projection overloads  findAll / findOne / findFirst / findTop /
	//                               findPage / findSlice  (Path<?>... selection)
	// ======================================================================

	/**
	 * Tests for the {@code Path<?>... selection} overloads added to
	 * {@link BeanDatastoreHelper}.  These delegate to
	 * {@link BeanQuery#stream(Class, Path[])} rather than the plain
	 * {@link BeanQuery#stream()} or {@link BeanQuery#list()} methods.
	 */
	@Nested
	@DisplayName("Projection overloads — findAll / findOne / findTop / findPage / findSlice with Path selection")
	class ProjectionOverloadTests {

		/** Two sample property paths pulled from the Product bean. */
		private final BeanPropertySet<Product> PS   = BeanPropertySet.create(Product.class);
		private final Path<?>                  pId  = PS.property("id");
		private final Path<?>                  pName = PS.property("name");

		@BeforeEach
		void stubProjectionStream() {
			// BeanQuery.stream(Class, Path[]...) — stub using individual per-element
			// matchers so Mockito's varargs handling is unambiguous.
			// For these delegation tests we return the full catalogue; restrict(n,0)
			// is the contract under test, not the slice size.
			doAnswer(inv -> CATALOGUE.stream()).when(q).stream(any(Class.class), any(Path.class), any(Path.class));
			doAnswer(inv -> CATALOGUE.stream()).when(q).stream(any(Class.class), any(Path.class));
			// Also cover the zero-selection edge case
			doAnswer(inv -> CATALOGUE.stream()).when(q).stream(any(Class.class));
		}

		// ── findAll overloads ──────────────────────────────────────────────

		@Test
		@DisplayName("findAll(selection) calls stream(beanClass, paths) — not plain stream()")
		void findAllWithSelection() {
			Stream<Product> result = helper.findAll(pId, pName);

			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
			verify(q, never()).stream();   // the no-arg stream must NOT be called
			assertNotNull(result);
		}

		@Test
		@DisplayName("findAll(filter, selection) applies filter before projection stream")
		void findAllFilterSelection() {
			QueryFilter filter = mock(QueryFilter.class);

			helper.findAll(filter, pId, pName);

			verify(q).filter(filter);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
			verify(q, never()).stream();
		}

		@Test
		@DisplayName("findAll(filter, sort, selection) applies filter + sort before projection stream")
		void findAllFilterSortSelection() {
			QueryFilter filter = mock(QueryFilter.class);
			QuerySort   sort   = mock(QuerySort.class);

			helper.findAll(filter, sort, pId, pName);

			verify(q).filter(filter);
			verify(q).sort(sort);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
		}

		@Test
		@DisplayName("findAll(null, null, selection) skips filter and sort")
		void findAllNullFilterNullSortSelection() {
			helper.findAll((QueryFilter) null, (QuerySort) null, pId);

			verify(q, never()).filter(any(QueryFilter.class));
			verify(q, never()).sort(any(QuerySort.class));
			verify(q).stream(eq(Product.class), eq(pId));
		}

		// ── findOne overload ───────────────────────────────────────────────

		@Test
		@DisplayName("findOne(filter, selection) applies filter and uses projection stream")
		void findOneWithSelection() {
			QueryFilter filter = mock(QueryFilter.class);
			// Override to return exactly one item so findOne doesn't throw QueryNonUniqueResult
			doReturn(Stream.of(p1)).when(q).stream(eq(Product.class), eq(pId), eq(pName));

			Optional<Product> result = helper.findOne(filter, pId, pName);

			verify(q).filter(filter);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
			assertTrue(result.isPresent());
			assertSame(p1, result.get());
		}

		// ── findFirst overloads ────────────────────────────────────────────

		@Test
		@DisplayName("findFirst(sort, selection) applies sort + restrict(1,0) then projection stream")
		void findFirstSortSelection() {
			QuerySort sort = mock(QuerySort.class);

			Optional<Product> result = helper.findFirst(sort, pId, pName);

			verify(q).sort(sort);
			verify(q).restrict(1, 0);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
			assertNotNull(result);
		}

		@Test
		@DisplayName("findFirst(filter, selection) applies filter + restrict(1,0) then projection stream")
		void findFirstFilterSelection() {
			QueryFilter filter = mock(QueryFilter.class);

			Optional<Product> result = helper.findFirst(filter, pId, pName);

			verify(q).filter(filter);
			verify(q).restrict(1, 0);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
			assertNotNull(result);
		}

		// ── findTop overloads ──────────────────────────────────────────────

		@Test
		@DisplayName("findTop(3, sort, selection) applies sort + restrict(3,0) then projection stream")
		void findTopSortSelection() {
			QuerySort sort = mock(QuerySort.class);

			Stream<Product> top = helper.findTop(3, sort, pId, pName);

			verify(q).sort(sort);
			verify(q).restrict(3, 0);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
			assertNotNull(top);
		}

		@Test
		@DisplayName("findTop(3, filter, sort, selection) applies both + restrict(3,0)")
		void findTopFilterSortSelection() {
			QueryFilter filter = mock(QueryFilter.class);
			QuerySort   sort   = mock(QuerySort.class);

			Stream<Product> top = helper.findTop(3, filter, sort, pId, pName);

			verify(q).filter(filter);
			verify(q).sort(sort);
			verify(q).restrict(3, 0);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
			assertNotNull(top);
		}

		// ── findPage overloads ─────────────────────────────────────────────

		@Test
		@DisplayName("findPage(0, 5, selection) calls restrict(5, 0) then projection stream")
		void findPageSelection() {
			Stream<Product> page = helper.findPage(0, 5, pId, pName);

			verify(q).restrict(5, 0);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
		}

		@Test
		@DisplayName("findPage(filter, sort, 1, 5, selection) calls restrict(5, 5) then projection stream")
		void findPageFilterSortSelection() {
			QueryFilter filter = mock(QueryFilter.class);
			QuerySort   sort   = mock(QuerySort.class);

			Stream<Product> page = helper.findPage( 1, 5,filter, sort, pId, pName);

			verify(q).filter(filter);
			verify(q).sort(sort);
			verify(q).restrict(5, 5);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
		}

		// ── findSlice overloads ────────────────────────────────────────────

		@Test
		@DisplayName("findSlice(3, 4, selection) calls restrict(3, 4) then projection stream")
		void findSliceSelection() {
			Stream<Product> slice = helper.findSlice(3, 4, pId, pName);

			verify(q).restrict(3, 4);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
		}

		@Test
		@DisplayName("findSlice(filter, sort, 3, 4, selection) applies filter+sort then restrict")
		void findSliceFilterSortSelection() {
			QueryFilter filter = mock(QueryFilter.class);
			QuerySort   sort   = mock(QuerySort.class);

			Stream<Product> slice = helper.findSlice( 3, 4,filter, sort, pId, pName);

			verify(q).filter(filter);
			verify(q).sort(sort);
			verify(q).restrict(3, 4);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
		}
	}

	// ======================================================================
	// PageableQuery-based overloads
	// ======================================================================

	@Nested
	@DisplayName("PageableQuery-based findSlice overloads")
	class PageableQueryTests {

		@BeforeEach
		void stubRestrict() {
			doAnswer(call -> {
				int limit  = call.getArgument(0);
				int offset = call.getArgument(1);
				int from   = Math.min(offset, CATALOGUE.size());
				int to     = Math.min(from + limit, CATALOGUE.size());
				List<Product> subList = CATALOGUE.subList(from, to);
				doAnswer(inv -> subList.stream()).when(q).stream();
				return q;
			}).when(q).restrict(anyInt(), anyInt());
		}

		@Test
		@DisplayName("findSlice(PageableQuery) delegates to restrict(limit, offset)")
		void findSlicePageableQuery() {
			PageableQuery pq = new PageableQuery() {
				@Override public int getLimit()  { return 3; }
				@Override public int getOffset() { return 4; }
			};

			Stream<Product> result = helper.findSlice(pq);

			verify(q).restrict(3, 4);
			verify(q).stream();
			assertEquals(3, result.count());
		}

		@Test
		@DisplayName("findSlice(PageableQuery, filter) applies filter")
		void findSlicePageableQueryWithFilter() {
			PageableQuery pq = new PageableQuery() {
				@Override public int getLimit()  { return 5; }
				@Override public int getOffset() { return 0; }
			};
			QueryFilter filter = mock(QueryFilter.class);

			Stream<Product> result = helper.findSlice(pq, filter);

			verify(q).filter(filter);
			verify(q).restrict(5, 0);
			verify(q).stream();
			assertNotNull(result);
		}

		@Test
		@DisplayName("findSlice(PageableQuery, sort) applies sort")
		void findSlicePageableQueryWithSort() {
			PageableQuery pq = new PageableQuery() {
				@Override public int getLimit()  { return 5; }
				@Override public int getOffset() { return 2; }
			};
			QuerySort sort = mock(QuerySort.class);

			Stream<Product> result = helper.findSlice(pq, sort);

			verify(q).sort(sort);
			verify(q, never()).filter(any(QueryFilter.class));
			verify(q).restrict(5, 2);
			verify(q).stream();
			assertNotNull(result);
		}

		@Test
		@DisplayName("findSlice(PageableQuery, filter, sort) applies both")
		void findSlicePageableQueryWithFilterAndSort() {
			PageableQuery pq = new PageableQuery() {
				@Override public int getLimit()  { return 4; }
				@Override public int getOffset() { return 6; }
			};
			QueryFilter filter = mock(QueryFilter.class);
			QuerySort   sort   = mock(QuerySort.class);

			Stream<Product> result = helper.findSlice(pq, filter, sort);

			verify(q).filter(filter);
			verify(q).sort(sort);
			verify(q).restrict(4, 6);
			verify(q).stream();
			assertNotNull(result);
		}

		@Test
		@DisplayName("PageableQuery with default offset (0)")
		void findSlicePageableQueryDefaultOffset() {
			PageableQuery pq = () -> 7; // only getLimit(), offset defaults to 0

			Stream<Product> result = helper.findSlice(pq);

			verify(q).restrict(7, 0);
			verify(q).stream();
			assertNotNull(result);
		}

		@Test
		@DisplayName("findSlice(null PageableQuery) throws NullPointerException/IllegalArgumentException")
		void findSliceNullPageableQuery() {
			assertThrows(IllegalArgumentException.class, () -> helper.findSlice((PageableQuery) null));
		}
	}

	// ======================================================================
	// List<String> columns overloads (resolved via BeanPropertySet)
	// ======================================================================

	@Nested
	@DisplayName("findSlice with List<String> columns")
	class ColumnsOverloadTests {

		private final BeanPropertySet<Product> PS    = BeanPropertySet.create(Product.class);
		private final Path<?>                  pId   = PS.property("id");
		private final Path<?>                  pName = PS.property("name");

		@BeforeEach
		void stubProjectionStream() {
			doAnswer(inv -> CATALOGUE.stream()).when(q).stream(any(Class.class), any(Path.class), any(Path.class));
			doAnswer(inv -> CATALOGUE.stream()).when(q).stream(any(Class.class), any(Path.class));
		}

		@Test
		@DisplayName("findSlice(limit, offset, columns) resolves columns to Paths and calls stream(beanClass, paths)")
		void findSliceWithColumns() {
			Stream<Product> result = helper.findSlice(3, 4, List.of("id", "name"));

			verify(q).restrict(3, 4);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
			assertNotNull(result);
		}

		@Test
		@DisplayName("findSlice(limit, offset, filter, columns) applies filter then projection")
		void findSliceWithFilterAndColumns() {
			QueryFilter filter = mock(QueryFilter.class);

			Stream<Product> result = helper.findSlice(3, 0, filter, List.of("id", "name"));

			verify(q).filter(filter);
			verify(q).restrict(3, 0);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
			assertNotNull(result);
		}

		@Test
		@DisplayName("findSlice(limit, offset, sort, columns) applies sort then projection")
		void findSliceWithSortAndColumns() {
			QuerySort sort = mock(QuerySort.class);

			Stream<Product> result = helper.findSlice(5, 2, sort, List.of("id", "name"));

			verify(q).sort(sort);
			verify(q, never()).filter(any(QueryFilter.class));
			verify(q).restrict(5, 2);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
			assertNotNull(result);
		}

		@Test
		@DisplayName("findSlice(limit, offset, filter, sort, columns) applies both then projection")
		void findSliceWithFilterSortAndColumns() {
			QueryFilter filter = mock(QueryFilter.class);
			QuerySort   sort   = mock(QuerySort.class);

			Stream<Product> result = helper.findSlice(4, 6, filter, sort, List.of("id", "name"));

			verify(q).filter(filter);
			verify(q).sort(sort);
			verify(q).restrict(4, 6);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
			assertNotNull(result);
		}

		@Test
		@DisplayName("findSlice(PageableQuery, columns) uses query limit/offset with column projection")
		void findSlicePageableQueryWithColumns() {
			PageableQuery pq = new PageableQuery() {
				@Override public int getLimit()  { return 3; }
				@Override public int getOffset() { return 4; }
			};

			Stream<Product> result = helper.findSlice(pq, List.of("id", "name"));

			verify(q).restrict(3, 4);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
			assertNotNull(result);
		}

		@Test
		@DisplayName("findSlice(PageableQuery, filter, sort, columns) full combination")
		void findSlicePageableQueryFilterSortColumns() {
			PageableQuery pq = new PageableQuery() {
				@Override public int getLimit()  { return 5; }
				@Override public int getOffset() { return 10; }
			};
			QueryFilter filter = mock(QueryFilter.class);
			QuerySort   sort   = mock(QuerySort.class);

			Stream<Product> result = helper.findSlice(pq, filter, sort, List.of("id", "name"));

			verify(q).filter(filter);
			verify(q).sort(sort);
			verify(q).restrict(5, 10);
			verify(q).stream(eq(Product.class), eq(pId), eq(pName));
			assertNotNull(result);
		}

		@Test
		@DisplayName("findSlice with empty columns list throws IllegalArgumentException")
		void findSliceEmptyColumnsRejected() {
			assertThrows(IllegalArgumentException.class, () -> helper.findSlice(3, 0, List.of()));
		}

		@Test
		@DisplayName("findSlice with null columns list throws IllegalArgumentException")
		void findSliceNullColumnsRejected() {
			assertThrows(IllegalArgumentException.class,
					() -> helper.findSlice(3, 0, (List<String>) null));
		}

		@Test
		@DisplayName("findSlice with single column resolves correctly")
		void findSliceSingleColumn() {
			Stream<Product> result = helper.findSlice(2, 0, List.of("id"));

			verify(q).restrict(2, 0);
			verify(q).stream(eq(Product.class), eq(pId));
			assertNotNull(result);
		}
	}
}



















