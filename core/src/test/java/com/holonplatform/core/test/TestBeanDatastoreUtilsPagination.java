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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.holonplatform.core.datastore.beans.BeanDatastore;
import com.holonplatform.core.datastore.beans.BeanDatastoreUtils;
import com.holonplatform.core.datastore.beans.BeanQuery;
import com.holonplatform.core.query.QueryFilter;
import com.holonplatform.core.query.QuerySort;
import com.holonplatform.core.test.data.Product;

/**
 * Practical examples and unit tests for the pagination methods of
 * {@link BeanDatastoreUtils}:
 *
 * <ul>
 *   <li>{@link BeanDatastoreUtils#findPage(BeanDatastore, Class, int, int)}  — page-index based</li>
 *   <li>{@link BeanDatastoreUtils#findPage(BeanDatastore, Class, QueryFilter, QuerySort, int, int)} — filtered + sorted</li>
 *   <li>{@link BeanDatastoreUtils#findSlice(BeanDatastore, Class, int, int)} — raw limit / offset</li>
 *   <li>{@link BeanDatastoreUtils#findSlice(BeanDatastore, Class, QueryFilter, QuerySort, int, int)} — filtered + sorted</li>
 * </ul>
 *
 * <h2>Arithmetic cheat-sheet</h2>
 * <pre>
 *  findPage(bds, Product.class, page=0, pageSize=5)  →  restrict(limit=5,  offset=0)
 *  findPage(bds, Product.class, page=1, pageSize=5)  →  restrict(limit=5,  offset=5)
 *  findPage(bds, Product.class, page=2, pageSize=5)  →  restrict(limit=5,  offset=10)
 *
 *  findSlice(bds, Product.class, limit=10, offset=25) →  restrict(limit=10, offset=25)
 * </pre>
 *
 * <h2>Total-pages formula</h2>
 * <pre>{@code
 *  long total      = BeanDatastoreUtils.count(bds, Product.class);
 *  int  pageSize   = 5;
 *  int  totalPages = (int) Math.ceil((double) total / pageSize);
 * }</pre>
 */
@SuppressWarnings("unchecked")
class TestBeanDatastoreUtilsPagination {

	// ---- fixture ---------------------------------------------------------

	/**
	 * In-memory catalogue: 23 products, ids 1..23, alternating categories
	 * TECH / HOME / SPORT in price-ascending order.
	 */
	private static final List<Product> CATALOGUE;

	static {
		CATALOGUE = LongStream.rangeClosed(1, 23)
				.mapToObj(id -> new Product(id,
						"Product-" + id,
						id * 9.99,
						id % 3 == 0 ? "SPORT" : id % 2 == 0 ? "HOME" : "TECH"))
				.collect(Collectors.toList());
	}

	// Mocks re-created for each test
	private BeanDatastore bds;
	private BeanQuery<Product>   productQuery;

	/**
	 * Helper: builds a fake in-memory "restrict" that slices {@link #CATALOGUE}.
	 * It stubs the mock so that {@code restrict(limit, offset)} records its args
	 * AND {@code list()} returns the correct sub-list.
	 *
	 * We capture the last restrict() call via an AtomicInteger pair so we can
	 * assert on it afterwards.
	 */
	@BeforeEach
	void setUp() {
		bds          = mock(BeanDatastore.class);
		productQuery = mock(BeanQuery.class);

		// Wire: bds.query(Product.class) → productQuery (chainable)
		when(bds.query(Product.class)).thenReturn(productQuery);

		// All filter / sort methods return the same mock (fluent chain).
		// Use doReturn to bypass the ambiguous varargs overloads on filter/sort.
		doReturn(productQuery).when(productQuery).filter(any(QueryFilter.class));
		doReturn(productQuery).when(productQuery).sort(any(QuerySort.class));

		// restrict(limit, offset) records the call AND rewires list() to return
		// the correct slice from CATALOGUE so assertions can verify real data too.
		when(productQuery.restrict(anyInt(), anyInt())).thenAnswer(call -> {
			int limit  = call.getArgument(0);
			int offset = call.getArgument(1);
			int from   = Math.min(offset, CATALOGUE.size());
			int to     = Math.min(offset + limit, CATALOGUE.size());
			List<Product> slice = CATALOGUE.subList(from, to);
			when(productQuery.list()).thenReturn(slice);
			return productQuery;          // fluent return
		});

		// count() returns the size of the full catalogue
		when(productQuery.count()).thenReturn((long) CATALOGUE.size());

		// Default list() (before restrict is called) → all
		when(productQuery.list()).thenReturn(new ArrayList<>(CATALOGUE));
	}

	// ======================================================================
	// findPage — page-index based
	// ======================================================================

	@Test
	@DisplayName("findPage(page=0, pageSize=5) → first 5 products, restrict(5, 0)")
	void findPage_firstPage() {
		// -----------------------------------------------------------------------
		// page=0, pageSize=5  →  offset = page * pageSize = 0 * 5 = 0
		//                        limit  = pageSize = 5
		// SQL: LIMIT 5 OFFSET 0
		// -----------------------------------------------------------------------
		List<Product> page0 = BeanDatastoreUtils.findPage(bds, Product.class, 0, 5);

		// The underlying query must receive restrict(limit=5, offset=0)
		verify(productQuery).restrict(5, 0);

		// Data: products 1..5
		assertEquals(5, page0.size());
		assertEquals(1L, page0.get(0).getId());
		assertEquals(5L, page0.get(4).getId());
	}

	@Test
	@DisplayName("findPage(page=1, pageSize=5) → products 6-10, restrict(5, 5)")
	void findPage_secondPage() {
		// -----------------------------------------------------------------------
		// page=1, pageSize=5  →  offset = 1 * 5 = 5
		//                        limit  = 5
		// SQL: LIMIT 5 OFFSET 5
		// -----------------------------------------------------------------------
		List<Product> page1 = BeanDatastoreUtils.findPage(bds, Product.class, 1, 5);

		verify(productQuery).restrict(5, 5);

		assertEquals(5, page1.size());
		assertEquals(6L, page1.get(0).getId());
		assertEquals(10L, page1.get(4).getId());
	}

	@Test
	@DisplayName("findPage(page=2, pageSize=5) → products 11-15, restrict(5, 10)")
	void findPage_thirdPage() {
		// -----------------------------------------------------------------------
		// page=2, pageSize=5  →  offset = 2 * 5 = 10
		//                        limit  = 5
		// SQL: LIMIT 5 OFFSET 10
		// -----------------------------------------------------------------------
		List<Product> page2 = BeanDatastoreUtils.findPage(bds, Product.class, 2, 5);

		verify(productQuery).restrict(5, 10);

		assertEquals(5, page2.size());
		assertEquals(11L, page2.get(0).getId());
		assertEquals(15L, page2.get(4).getId());
	}

	@Test
	@DisplayName("findPage on last partial page → fewer items than pageSize")
	void findPage_lastPartialPage() {
		// -----------------------------------------------------------------------
		// 23 products, pageSize=10:
		//   page 0 → items  1..10  (10)
		//   page 1 → items 11..20  (10)
		//   page 2 → items 21..23  (3)  ← partial
		//
		// page=2, pageSize=10  →  offset=20, limit=10
		// SQL: LIMIT 10 OFFSET 20
		// -----------------------------------------------------------------------
		List<Product> lastPage = BeanDatastoreUtils.findPage(bds, Product.class, 2, 10);

		verify(productQuery).restrict(10, 20);

		assertEquals(3, lastPage.size(),
				"Last page should contain only the 3 remaining products");
		assertEquals(21L, lastPage.get(0).getId());
		assertEquals(23L, lastPage.get(2).getId());
	}

	// ======================================================================
	// findPage — with filter + sort
	// ======================================================================

	@Test
	@DisplayName("findPage with filter + sort passes them to the query chain")
	void findPage_withFilterAndSort() {
		// -----------------------------------------------------------------------
		// Suppose we filter by category and sort by price descending.
		// We only verify that filter/sort were forwarded; data verification is
		// covered by the plain findPage tests above.
		// SQL: SELECT * FROM product WHERE category='TECH' ORDER BY price DESC
		//      LIMIT 5 OFFSET 10
		// -----------------------------------------------------------------------
		QueryFilter filter = mock(QueryFilter.class);
		QuerySort   sort   = mock(QuerySort.class);

		BeanDatastoreUtils.findPage(bds, Product.class, filter, sort, 2, 5);

		verify(productQuery).filter(filter);
		verify(productQuery).sort(sort);
		verify(productQuery).restrict(5, 10);   // page=2, pageSize=5 → offset=10
	}

	// ======================================================================
	// findSlice — raw limit + offset
	// ======================================================================

	@Test
	@DisplayName("findSlice(limit=10, offset=25) → restrict(10, 25)")
	void findSlice_rawLimitOffset() {
		// -----------------------------------------------------------------------
		// Direct limit/offset: no page-index arithmetic, values go straight to
		// the underlying restrict() call.
		// SQL: LIMIT 10 OFFSET 25
		// -----------------------------------------------------------------------
		// Our catalogue has 23 items so offset=25 → empty result
		List<Product> slice = BeanDatastoreUtils.findSlice(bds, Product.class, 10, 25);

		verify(productQuery).restrict(10, 25);

		assertTrue(slice.isEmpty(), "Offset beyond catalogue size → empty slice");
	}

	@Test
	@DisplayName("findSlice(limit=5, offset=0) → same as first page")
	void findSlice_firstSliceMatchesFirstPage() {
		// -----------------------------------------------------------------------
		// findSlice(limit=5, offset=0) is exactly findPage(page=0, pageSize=5)
		// SQL: LIMIT 5 OFFSET 0
		// -----------------------------------------------------------------------
		List<Product> slice = BeanDatastoreUtils.findSlice(bds, Product.class, 5, 0);

		verify(productQuery).restrict(5, 0);

		assertEquals(5, slice.size());
		assertEquals(1L, slice.get(0).getId());
	}

	@Test
	@DisplayName("findSlice with filter + sort forwards all arguments")
	void findSlice_withFilterAndSort() {
		// -----------------------------------------------------------------------
		// SQL: SELECT * FROM product WHERE category='HOME' ORDER BY name ASC
		//      LIMIT 7 OFFSET 14
		// -----------------------------------------------------------------------
		QueryFilter filter = mock(QueryFilter.class);
		QuerySort   sort   = mock(QuerySort.class);

		BeanDatastoreUtils.findSlice(bds, Product.class, filter, sort, 7, 14);

		verify(productQuery).filter(filter);
		verify(productQuery).sort(sort);
		verify(productQuery).restrict(7, 14);
	}

	// ======================================================================
	// Total-pages calculation
	// ======================================================================

	@Test
	@DisplayName("Total pages = ceil(count / pageSize)")
	void totalPageCalculation() {
		// -----------------------------------------------------------------------
		// count=23, pageSize=5  → ceil(23/5) = ceil(4.6) = 5 pages
		// count=23, pageSize=10 → ceil(23/10)= ceil(2.3) = 3 pages
		// count=20, pageSize=5  → ceil(20/5) = ceil(4.0) = 4 pages
		// -----------------------------------------------------------------------
		long count = BeanDatastoreUtils.count(bds, Product.class); // returns 23

		assertEquals(23L, count);

		assertEquals(5, totalPages(count, 5));
		assertEquals(3, totalPages(count, 10));
		assertEquals(1, totalPages(count, 100)); // fewer items than one page

		// Exact multiple: 20 items, pageSize 5 → exactly 4 pages, no partial
		assertEquals(4, totalPages(20, 5));
	}

	// ======================================================================
	// Walk all pages
	// ======================================================================

	@Test
	@DisplayName("Iterating over all pages retrieves every product exactly once")
	void walkAllPages() {
		// -----------------------------------------------------------------------
		// Simulates the typical "browse all" loop:
		//
		//   long total      = count(bds, Product.class);   // 23
		//   int  pageSize   = 5;
		//   int  totalPages = totalPages(total, pageSize);  // 5
		//
		//   for (int p = 0; p < totalPages; p++) {
		//       List<Product> page = findPage(bds, Product.class, p, pageSize);
		//       process(page);
		//   }
		// -----------------------------------------------------------------------
		final int PAGE_SIZE = 5;

		long total      = BeanDatastoreUtils.count(bds, Product.class);
		int  totalPages = totalPages(total, PAGE_SIZE);

		assertEquals(5, totalPages);   // ceil(23 / 5) = 5

		List<Product> allCollected = new ArrayList<>();

		for (int page = 0; page < totalPages; page++) {
			List<Product> pageData = BeanDatastoreUtils.findPage(bds, Product.class, page, PAGE_SIZE);
			allCollected.addAll(pageData);
		}

		// Every product id should appear exactly once
		assertEquals(CATALOGUE.size(), allCollected.size());
		List<Long> ids = allCollected.stream()
				.map(Product::getId)
				.sorted()
				.toList();

		for (int i = 0; i < CATALOGUE.size(); i++) {
			assertEquals(Long.valueOf(i + 1), ids.get(i),
					"Missing or duplicated product at position " + i);
		}
	}

	// ======================================================================
	// Validation guards
	// ======================================================================

	@Test
	@DisplayName("findPage rejects negative page index")
	void findPage_rejectsNegativePage() {
		assertThrows(IllegalArgumentException.class,
				() -> BeanDatastoreUtils.findPage(bds, Product.class, -1, 10));
	}

	@Test
	@DisplayName("findPage rejects zero pageSize")
	void findPage_rejectsZeroPageSize() {
		assertThrows(IllegalArgumentException.class,
				() -> BeanDatastoreUtils.findPage(bds, Product.class, 0, 0));
	}

	@Test
	@DisplayName("findSlice rejects zero limit")
	void findSlice_rejectsZeroLimit() {
		assertThrows(IllegalArgumentException.class,
				() -> BeanDatastoreUtils.findSlice(bds, Product.class, 0, 5));
	}

	@Test
	@DisplayName("findSlice rejects negative offset")
	void findSlice_rejectsNegativeOffset() {
		assertThrows(IllegalArgumentException.class,
				() -> BeanDatastoreUtils.findSlice(bds, Product.class, 10, -1));
	}

	// ======================================================================
	// Helper
	// ======================================================================

	/**
	 * Compute total number of pages.
	 *
	 * @param totalItems total record count
	 * @param pageSize   items per page
	 * @return number of pages (always at least 1 when totalItems &gt; 0)
	 */
	private static int totalPages(long totalItems, int pageSize) {
		return (int) Math.ceil((double) totalItems / pageSize);
	}
}



