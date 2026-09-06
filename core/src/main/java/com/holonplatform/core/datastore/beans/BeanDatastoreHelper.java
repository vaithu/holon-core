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
package com.holonplatform.core.datastore.beans;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.holonplatform.core.Path;
import com.holonplatform.core.beans.BeanPropertySet;

import com.holonplatform.core.datastore.Datastore;
import com.holonplatform.core.datastore.DatastoreOperations.WriteOption;
import com.holonplatform.core.datastore.beans.BeanDatastore.BeanOperationResult;
import com.holonplatform.core.datastore.beans.BeanDatastoreUtils.BeanTransactionalOperation;
import com.holonplatform.core.exceptions.DataAccessException;
import com.holonplatform.core.internal.utils.ObjectUtils;
import com.holonplatform.core.query.QueryFilter;
import com.holonplatform.core.query.QueryResults.QueryNonUniqueResultException;
import com.holonplatform.core.query.QuerySort;

/**
 * Typed, instance-based façade over {@link BeanDatastoreUtils} that holds both a
 * {@link BeanDatastore} and a bean {@link Class} so neither ever needs to be repeated on a call.
 *
 * <h3>Construction</h3>
 * <pre>{@code
 * // From a raw Datastore
 * BeanDatastoreHelper<Product> products = BeanDatastoreHelper.of(rawDatastore, Product.class);
 *
 * // From an existing BeanDatastore
 * BeanDatastoreHelper<Product> products = BeanDatastoreHelper.of(beanDatastore, Product.class);
 * }</pre>
 *
 * <h3>Return-type convention</h3>
 * <ul>
 *   <li>All {@code find…(…)} methods that return multiple results return a <em>lazy</em>
 *       {@link Stream}. The datastore fetches rows on demand as the stream is consumed.
 *       The caller <strong>must close</strong> the stream (use try-with-resources or a
 *       terminal operation that closes it automatically).</li>
 *   <li>{@code findOne(…)} and {@code findFirst(…)} return {@link Optional}.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // single-bean writes
 * products.insert(p);
 * products.update(p);
 * products.save(p);
 * products.delete(p);
 *
 * // lazy streams — close after use
 * try (Stream<Product> all = products.findAll()) {
 *     all.forEach(this::process);
 * }
 * try (Stream<Product> tech = products.findAll(techFilter)) {
 *     tech.map(Product::getName).forEach(System.out::println);
 * }
 *
 * // bounded streams — close after use
 * Optional<Product> one  = products.findOne(QueryFilter.eq(ID, 42L));
 * try (Stream<Product> page  = products.findPage(0, 20)) { ... }
 * try (Stream<Product> slice = products.findSlice(20, 40)) { ... }
 *
 * // count / exists
 * long total  = products.count();
 * boolean any = products.exists(techFilter);
 *
 * // bulk
 * products.bulkInsert(List.of(p1, p2, p3));
 * products.bulkDelete(techFilter);
 * products.bulkUpdateProperty(homeFilter, "category", "SALE");
 * products.bulkUpdate(List.of(p1, p2));
 *
 * // transaction
 * products.withTransaction((ds, tx) -> { ds.insert(p); tx.commit(); return null; });
 * }</pre>
 *
 * @param <T> Bean type managed by this helper
 * @since 10.0.0
 */
public final class BeanDatastoreHelper<T> {

	private final BeanDatastore datastore;
	private final Class<T>      beanClass;

	private BeanDatastoreHelper(BeanDatastore datastore, Class<T> beanClass) {
		ObjectUtils.argumentNotNull(datastore,  "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass,  "Bean class must be not null");
		this.datastore = datastore;
		this.beanClass = beanClass;
	}

	// -------------------------------------------------------------------------
	// Factory
	// -------------------------------------------------------------------------

	/**
	 * Create a helper wrapping the given {@link BeanDatastore} for the given bean type.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @return A new typed {@link BeanDatastoreHelper}
	 */
	public static <T> BeanDatastoreHelper<T> of(BeanDatastore datastore, Class<T> beanClass) {
		return new BeanDatastoreHelper<>(datastore, beanClass);
	}

	/**
	 * Create a helper from a raw {@link Datastore} for the given bean type.
	 *
	 * @param <T>       Bean type
	 * @param datastore The concrete {@link Datastore} (not null)
	 * @param beanClass The bean class (not null)
	 * @return A new typed {@link BeanDatastoreHelper}
	 */
	public static <T> BeanDatastoreHelper<T> of(Datastore datastore, Class<T> beanClass) {
		return new BeanDatastoreHelper<>(BeanDatastore.of(datastore), beanClass);
	}

	/** Return the underlying {@link BeanDatastore}. */
	public BeanDatastore getDatastore() { return datastore; }

	/** Return the bean class managed by this helper. */
	public Class<T> getBeanClass()      { return beanClass; }

	// -------------------------------------------------------------------------
	// Single-bean write operations
	// -------------------------------------------------------------------------

	/**
	 * Insert a bean instance.
	 *
	 * @param bean    The bean to insert (not null)
	 * @param options Optional write options
	 * @return The operation result
	 * @throws DataAccessException if an error occurs
	 */
	public BeanOperationResult<T> insert(T bean, WriteOption... options) {
		return BeanDatastoreUtils.insert(datastore, bean, options);
	}

	/**
	 * Update a bean instance.
	 *
	 * @param bean    The bean to update (not null)
	 * @param options Optional write options
	 * @return The operation result
	 * @throws DataAccessException if an error occurs
	 */
	public BeanOperationResult<T> update(T bean, WriteOption... options) {
		return BeanDatastoreUtils.update(datastore, bean, options);
	}

	/**
	 * Save (upsert) a bean instance — inserts if absent, updates otherwise.
	 *
	 * @param bean    The bean to save (not null)
	 * @param options Optional write options
	 * @return The operation result
	 * @throws DataAccessException if an error occurs
	 */
	public BeanOperationResult<T> save(T bean, WriteOption... options) {
		return BeanDatastoreUtils.save(datastore, bean, options);
	}

	/**
	 * Delete a bean instance.
	 *
	 * @param bean    The bean to delete (not null)
	 * @param options Optional write options
	 * @return The operation result
	 * @throws DataAccessException if an error occurs
	 */
	public BeanOperationResult<T> delete(T bean, WriteOption... options) {
		return BeanDatastoreUtils.delete(datastore, bean, options);
	}

	/**
	 * Refresh a bean instance, reloading its properties from the datastore.
	 *
	 * @param bean The bean to refresh (not null)
	 * @return The refreshed bean instance
	 * @throws DataAccessException if an error occurs
	 */
	public T refresh(T bean) {
		return BeanDatastoreUtils.refresh(datastore, bean);
	}

	// -------------------------------------------------------------------------
	// Query / Read
	// -------------------------------------------------------------------------

	/**
	 * Return a <em>lazy</em> {@link Stream} over <strong>all</strong> persisted instances of this bean type.
	 * <p>
	 * Rows are fetched from the datastore on demand as the stream is consumed — no full result set is
	 * ever loaded into memory. The caller <strong>must close</strong> the stream after use, e.g. via
	 * try-with-resources or a terminal operation that closes it.
	 * </p>
	 * <pre>{@code
	 * try (Stream<Product> s = products.findAll()) {
	 *     s.filter(p -> p.getPrice() < 10).forEach(this::process);
	 * }
	 * }</pre>
	 *
	 * @return Lazy stream of all persisted instances; never {@code null}
	 * @throws DataAccessException if an error occurs opening the stream
	 */
	public Stream<T> findAll() {
		return datastore.query(beanClass).stream();
	}

	/**
	 * Return a <em>lazy</em> {@link Stream} over all instances matching the given filter.
	 * <p>
	 * See {@link #findAll()} for resource-management notes.
	 * </p>
	 *
	 * @param filter The query filter (not null)
	 * @return Lazy stream of matching instances; never {@code null}
	 * @throws DataAccessException if an error occurs opening the stream
	 */
	public Stream<T> findAll(QueryFilter filter) {
		return datastore.query(beanClass).filter(filter).stream();
	}

	/**
	 * Return a <em>lazy</em> {@link Stream} over all instances matching the given filter,
	 * ordered by the given sort. Both parameters may be {@code null}.
	 * <p>
	 * See {@link #findAll()} for resource-management notes.
	 * </p>
	 *
	 * @param filter Optional query filter
	 * @param sort   Optional query sort
	 * @return Lazy stream of matching instances; never {@code null}
	 * @throws DataAccessException if an error occurs opening the stream
	 */
	public Stream<T> findAll(QueryFilter filter, QuerySort sort) {
		BeanQuery<T> q = datastore.query(beanClass);
		if (filter != null) q = q.filter(filter);
		if (sort   != null) q = q.sort(sort);
		return q.stream();
	}

	// -------------------------------------------------------------------------
	// findAll — column projection overloads
	// -------------------------------------------------------------------------

	/**
	 * Return a <em>lazy</em> {@link Stream} over <strong>all</strong> persisted instances, fetching
	 * only the specified columns/properties.
	 * <p>
	 * Each {@link Path} must correspond to a bean property name. Unspecified properties are left
	 * {@code null} in the returned instances. See {@link #findAll()} for resource-management notes.
	 * </p>
	 * <pre>{@code
	 * BeanPropertySet<Product> ps = BeanPropertySet.create(Product.class);
	 * try (Stream<Product> s = products.findAll(ps.property("name"), ps.property("price"))) {
	 *     s.forEach(p -> System.out.println(p.getName() + " / " + p.getPrice()));
	 * }
	 * }</pre>
	 *
	 * @param selection One or more {@link Path}s (bean property paths) to fetch (not null)
	 * @return Lazy stream of partially-populated instances; never {@code null}
	 * @throws DataAccessException if an error occurs opening the stream
	 */
	public Stream<T> findAll(Path<?>... selection) {
		return datastore.query(beanClass).stream(beanClass, selection);
	}

	/**
	 * Return a <em>lazy</em> {@link Stream} matching the given filter, fetching only the specified columns.
	 * <p>
	 * See {@link #findAll(Path...)} for details on column projection and resource-management notes.
	 * </p>
	 *
	 * @param filter    The query filter (not null)
	 * @param selection One or more {@link Path}s to fetch (not null)
	 * @return Lazy stream of partially-populated matching instances; never {@code null}
	 * @throws DataAccessException if an error occurs opening the stream
	 */
	public Stream<T> findAll(QueryFilter filter, Path<?>... selection) {
		return datastore.query(beanClass).filter(filter).stream(beanClass, selection);
	}

	/**
	 * Return a <em>lazy</em> {@link Stream} matching the given filter in the given sort order,
	 * fetching only the specified columns. Both {@code filter} and {@code sort} may be {@code null}.
	 * <p>
	 * See {@link #findAll(Path...)} for details on column projection and resource-management notes.
	 * </p>
	 *
	 * @param filter    Optional query filter
	 * @param sort      Optional query sort
	 * @param selection One or more {@link Path}s to fetch (not null)
	 * @return Lazy stream of partially-populated matching instances; never {@code null}
	 * @throws DataAccessException if an error occurs opening the stream
	 */
	public Stream<T> findAll(QueryFilter filter, QuerySort sort, Path<?>... selection) {
		BeanQuery<T> q = datastore.query(beanClass);
		if (filter != null) q = q.filter(filter);
		if (sort   != null) q = q.sort(sort);
		return q.stream(beanClass, selection);
	}

	/**
	 * Find a single instance matching the given filter, fetching only the specified columns.
	 * Returns empty if none match; throws if more than one matches.
	 *
	 * @param filter    The query filter (not null)
	 * @param selection One or more {@link Path}s to fetch (not null)
	 * @return Matching partially-populated instance or empty
	 * @throws QueryNonUniqueResultException if more than one result is found
	 * @throws DataAccessException           if an error occurs
	 */
	public Optional<T> findOne(QueryFilter filter, Path<?>... selection) {
		return datastore.query(beanClass).filter(filter).stream(beanClass, selection)
				.collect(Collectors.collectingAndThen(Collectors.toList(),
						list -> {
							if (list.isEmpty())   return Optional.empty();
							if (list.size() == 1) return Optional.of(list.get(0));
							throw new QueryNonUniqueResultException();
						}));
	}

	/**
	 * Return the first instance in the given sort order, fetching only the specified columns.
	 * Translates to {@code [ORDER BY …] LIMIT 1 OFFSET 0}.
	 *
	 * @param sort      Optional sort (determines which row is "first")
	 * @param selection One or more {@link Path}s to fetch (not null)
	 * @return First partially-populated row, or empty if none
	 * @throws DataAccessException if an error occurs
	 */
	public Optional<T> findFirst(QuerySort sort, Path<?>... selection) {
		return datastore.query(beanClass).sort(sort).restrict(1, 0)
				.stream(beanClass, selection).findFirst();
	}

	/**
	 * Return the first instance matching the given filter, fetching only the specified columns.
	 * Translates to {@code WHERE … LIMIT 1 OFFSET 0}.
	 *
	 * @param filter    The query filter (not null)
	 * @param selection One or more {@link Path}s to fetch (not null)
	 * @return First partially-populated matching row, or empty if none
	 * @throws DataAccessException if an error occurs
	 */
	public Optional<T> findFirst(QueryFilter filter, Path<?>... selection) {
		return datastore.query(beanClass).filter(filter).restrict(1, 0)
				.stream(beanClass, selection).findFirst();
	}

	/**
	 * Return the first {@code n} instances in the given sort order, fetching only the specified columns.
	 * Translates to {@code ORDER BY … LIMIT n OFFSET 0}.
	 *
	 * @param n         Max results (&gt; 0)
	 * @param sort      The sort order (not null)
	 * @param selection One or more {@link Path}s to fetch (not null)
	 * @return Lazy stream of up to {@code n} partially-populated ordered instances; never {@code null}
	 * @throws IllegalArgumentException if {@code n <= 0}
	 * @throws DataAccessException      if an error occurs
	 */
	public Stream<T> findTop(int n, QuerySort sort, Path<?>... selection) {
		if (n <= 0) throw new IllegalArgumentException("n must be > 0, was: " + n);
		return datastore.query(beanClass).sort(sort).restrict(n, 0)
				.stream(beanClass, selection);
	}

	/**
	 * Return the first {@code n} instances matching the given filter, fetching only the specified columns
	 * (no sort applied). Translates to {@code WHERE … LIMIT n OFFSET 0}.
	 *
	 * @param n         Max results (&gt; 0)
	 * @param filter    The query filter (not null)
	 * @param selection One or more {@link Path}s to fetch (not null)
	 * @return Lazy stream of up to {@code n} partially-populated matching instances; never {@code null}
	 * @throws IllegalArgumentException if {@code n <= 0}
	 * @throws DataAccessException      if an error occurs
	 */
	public Stream<T> findTop(int n, QueryFilter filter, Path<?>... selection) {
		if (n <= 0) throw new IllegalArgumentException("n must be > 0, was: " + n);
		return datastore.query(beanClass).filter(filter).restrict(n, 0)
				.stream(beanClass, selection);
	}

	/**
	 * Return the first {@code n} instances matching the given filter in the given sort order,
	 * fetching only the specified columns. Both {@code filter} and {@code sort} may be {@code null}.
	 * Translates to {@code [WHERE …] [ORDER BY …] LIMIT n OFFSET 0}.
	 *
	 * @param n         Max results (&gt; 0)
	 * @param filter    Optional filter
	 * @param sort      Optional sort
	 * @param selection One or more {@link Path}s to fetch (not null)
	 * @return Lazy stream of up to {@code n} partially-populated instances; never {@code null}
	 * @throws IllegalArgumentException if {@code n <= 0}
	 * @throws DataAccessException      if an error occurs
	 */
	public Stream<T> findTop(int n, QueryFilter filter, QuerySort sort, Path<?>... selection) {
		if (n <= 0) throw new IllegalArgumentException("n must be > 0, was: " + n);
		BeanQuery<T> q = datastore.query(beanClass);
		if (filter != null) q = q.filter(filter);
		if (sort   != null) q = q.sort(sort);
		return q.restrict(n, 0).stream(beanClass, selection);
	}

	/**
	 * Retrieve a page using zero-based page index, fetching only the specified columns.
	 * Translates to {@code LIMIT pageSize OFFSET page*pageSize}.
	 *
	 * @param page      Zero-based page index (&gt;= 0)
	 * @param pageSize  Items per page (&gt; 0)
	 * @param selection One or more {@link Path}s to fetch (not null)
	 * @return The requested page; may be empty
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findPage(int page, int pageSize, Path<?>... selection) {
		return datastore.query(beanClass).restrict(pageSize, page * pageSize)
				.stream(beanClass, selection);
	}

	/**
	 * Retrieve a filtered page using zero-based page index, fetching only the specified columns
	 * (no sort applied).
	 *
	 * @param page      Zero-based page index (&gt;= 0)
	 * @param pageSize  Items per page (&gt; 0)
	 * @param filter    The query filter (not null)
	 * @param selection One or more {@link Path}s to fetch (not null)
	 * @return Lazy stream of the requested page; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findPage(int page, int pageSize, QueryFilter filter, Path<?>... selection) {
		return datastore.query(beanClass).filter(filter).restrict(pageSize, page * pageSize)
				.stream(beanClass, selection);
	}

	/**
	 * Retrieve a sorted page using zero-based page index, fetching only the specified columns
	 * (no filter applied).
	 *
	 * @param page      Zero-based page index (&gt;= 0)
	 * @param pageSize  Items per page (&gt; 0)
	 * @param sort      The query sort (not null)
	 * @param selection One or more {@link Path}s to fetch (not null)
	 * @return Lazy stream of the requested page; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findPage(int page, int pageSize, QuerySort sort, Path<?>... selection) {
		return datastore.query(beanClass).sort(sort).restrict(pageSize, page * pageSize)
				.stream(beanClass, selection);
	}

	/**
	 * Retrieve a filtered and sorted page using zero-based page index, fetching only the specified columns.
	 * Both {@code filter} and {@code sort} may be {@code null}.
	 *
	 * @param page      Zero-based page index (&gt;= 0)
	 * @param pageSize  Items per page (&gt; 0)
	 * @param filter    Optional query filter
	 * @param sort      Optional query sort
	 * @param selection One or more {@link Path}s to fetch (not null)
	 * @return Lazy stream of the requested page; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findPage(int page, int pageSize, QueryFilter filter, QuerySort sort, Path<?>... selection) {
		BeanQuery<T> q = datastore.query(beanClass);
		if (filter != null) q = q.filter(filter);
		if (sort   != null) q = q.sort(sort);
		return q.restrict(pageSize, page * pageSize).stream(beanClass, selection);
	}

	/**
	 * Retrieve a slice using explicit {@code LIMIT} and {@code OFFSET}, fetching only the specified columns.
	 *
	 * @param limit     Max results (&gt; 0)
	 * @param offset    Zero-based start index (&gt;= 0)
	 * @param selection One or more {@link Path}s to fetch (not null)
	 * @return The requested slice; may be empty
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(int limit, int offset, Path<?>... selection) {
		return datastore.query(beanClass).restrict(limit, offset)
				.stream(beanClass, selection);
	}

	/**
	 * Retrieve a filtered slice using explicit {@code LIMIT} and {@code OFFSET}, fetching only
	 * the specified columns (no sort applied).
	 *
	 * @param limit     Max results (&gt; 0)
	 * @param offset    Zero-based start index (&gt;= 0)
	 * @param filter    The query filter (not null)
	 * @param selection One or more {@link Path}s to fetch (not null)
	 * @return Lazy stream of the requested slice; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(int limit, int offset, QueryFilter filter, Path<?>... selection) {
		return datastore.query(beanClass).filter(filter).restrict(limit, offset)
				.stream(beanClass, selection);
	}

	/**
	 * Retrieve a sorted slice using explicit {@code LIMIT} and {@code OFFSET}, fetching only
	 * the specified columns (no filter applied).
	 *
	 * @param limit     Max results (&gt; 0)
	 * @param offset    Zero-based start index (&gt;= 0)
	 * @param sort      The query sort (not null)
	 * @param selection One or more {@link Path}s to fetch (not null)
	 * @return Lazy stream of the requested slice; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(int limit, int offset, QuerySort sort, Path<?>... selection) {
		return datastore.query(beanClass).sort(sort).restrict(limit, offset)
				.stream(beanClass, selection);
	}

	/**
	 * Retrieve a filtered and sorted slice using explicit {@code LIMIT} and {@code OFFSET}, fetching only
	 * the specified columns. Both {@code filter} and {@code sort} may be {@code null}.
	 *
	 * @param limit     Max results (&gt; 0)
	 * @param offset    Zero-based start index (&gt;= 0)
	 * @param filter    Optional query filter
	 * @param sort      Optional query sort
	 * @param selection One or more {@link Path}s to fetch (not null)
	 * @return Lazy stream of the requested slice; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(int limit, int offset, QueryFilter filter, QuerySort sort, Path<?>... selection) {
		BeanQuery<T> q = datastore.query(beanClass);
		if (filter != null) q = q.filter(filter);
		if (sort   != null) q = q.sort(sort);
		return q.restrict(limit, offset).stream(beanClass, selection);
	}


	// -------------------------------------------------------------------------
	// findOne — Optional<T>
	// -------------------------------------------------------------------------

	/**
	 * Find a single instance matching the given filter.
	 * Returns empty if none match; throws if more than one matches.
	 *
	 * @param filter The query filter (not null)
	 * @return Matching instance or empty
	 * @throws QueryNonUniqueResultException if more than one result is found
	 * @throws DataAccessException           if an error occurs
	 */
	public Optional<T> findOne(QueryFilter filter) {
		return BeanDatastoreUtils.findOne(datastore, beanClass, filter);
	}

	/**
	 * Return the first persisted instance, using datastore-default ordering.
	 * <p>
	 * Translates to {@code LIMIT 1 OFFSET 0}. Without an explicit sort the result is
	 * non-deterministic — prefer {@link #findFirst(QueryFilter, QuerySort)} for a stable outcome.
	 * </p>
	 *
	 * @return First row, or empty if the table is empty
	 * @throws DataAccessException if an error occurs
	 */
	public Optional<T> findFirst() {
		return BeanDatastoreUtils.findFirst(datastore, beanClass);
	}

	/**
	 * Return the first instance matching the given filter, using datastore-default ordering.
	 * <p>
	 * Translates to {@code WHERE … LIMIT 1 OFFSET 0}.
	 * </p>
	 *
	 * @param filter The query filter (not null)
	 * @return First matching row, or empty if none matches
	 * @throws DataAccessException if an error occurs
	 */
	public Optional<T> findFirst(QueryFilter filter) {
		return BeanDatastoreUtils.findFirst(datastore, beanClass, filter);
	}

	/**
	 * Return the first instance matching the given filter in the given sort order.
	 * <p>
	 * Both parameters may be {@code null}.
	 * Translates to {@code [WHERE …] [ORDER BY …] LIMIT 1 OFFSET 0}.
	 * </p>
	 *
	 * @param filter Optional filter
	 * @param sort   Optional sort (determines which row is "first")
	 * @return First matching row, or empty if none matches
	 * @throws DataAccessException if an error occurs
	 */
	public Optional<T> findFirst(QueryFilter filter, QuerySort sort) {
		return BeanDatastoreUtils.findFirst(datastore, beanClass, filter, sort);
	}

	// -------------------------------------------------------------------------
	// Pagination / Top  (lazy Stream)
	// -------------------------------------------------------------------------

	/**
	 * Return the first {@code n} instances, using datastore-default ordering.
	 * Translates to {@code LIMIT n OFFSET 0}.
	 *
	 * @param n Max results (&gt; 0)
	 * @return Lazy stream of up to {@code n} instances; never {@code null}
	 * @throws IllegalArgumentException if {@code n <= 0}
	 * @throws DataAccessException      if an error occurs
	 */
	public Stream<T> findTop(int n) {
		if (n <= 0) throw new IllegalArgumentException("n must be > 0, was: " + n);
		return datastore.query(beanClass).restrict(n, 0).stream();
	}

	/**
	 * Return the first {@code n} instances in the given sort order.
	 * Translates to {@code ORDER BY … LIMIT n OFFSET 0}.
	 *
	 * @param n    Max results (&gt; 0)
	 * @param sort The sort order (not null)
	 * @return Lazy stream of up to {@code n} ordered instances; never {@code null}
	 * @throws IllegalArgumentException if {@code n <= 0}
	 * @throws DataAccessException      if an error occurs
	 */
	public Stream<T> findTop(int n, QuerySort sort) {
		if (n <= 0) throw new IllegalArgumentException("n must be > 0, was: " + n);
		return datastore.query(beanClass).sort(sort).restrict(n, 0).stream();
	}

	/**
	 * Return the first {@code n} instances matching the given filter.
	 * Translates to {@code WHERE … LIMIT n OFFSET 0}.
	 *
	 * @param n      Max results (&gt; 0)
	 * @param filter The query filter (not null)
	 * @return Lazy stream of up to {@code n} matching instances; never {@code null}
	 * @throws IllegalArgumentException if {@code n <= 0}
	 * @throws DataAccessException      if an error occurs
	 */
	public Stream<T> findTop(int n, QueryFilter filter) {
		if (n <= 0) throw new IllegalArgumentException("n must be > 0, was: " + n);
		return datastore.query(beanClass).filter(filter).restrict(n, 0).stream();
	}

	/**
	 * Return the first {@code n} instances matching the given filter in the given sort order.
	 * Both parameters may be {@code null}.
	 * Translates to {@code [WHERE …] [ORDER BY …] LIMIT n OFFSET 0}.
	 *
	 * @param n      Max results (&gt; 0)
	 * @param filter Optional filter
	 * @param sort   Optional sort
	 * @return Lazy stream of up to {@code n} instances; never {@code null}
	 * @throws IllegalArgumentException if {@code n <= 0}
	 * @throws DataAccessException      if an error occurs
	 */
	public Stream<T> findTop(int n, QueryFilter filter, QuerySort sort) {
		if (n <= 0) throw new IllegalArgumentException("n must be > 0, was: " + n);
		BeanQuery<T> q = datastore.query(beanClass);
		if (filter != null) q = q.filter(filter);
		if (sort   != null) q = q.sort(sort);
		return q.restrict(n, 0).stream();
	}

	/**
	 * Retrieve a page using zero-based page index.
	 * Translates to {@code LIMIT pageSize OFFSET page*pageSize}.
	 *
	 * @param page     Zero-based page index (&gt;= 0)
	 * @param pageSize Items per page (&gt; 0)
	 * @return Lazy stream of the requested page; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findPage(int page, int pageSize) {
		return datastore.query(beanClass).restrict(pageSize, page * pageSize).stream();
	}

	/**
	 * Retrieve a filtered page using zero-based page index (no sort applied).
	 *
	 * @param page     Zero-based page index (&gt;= 0)
	 * @param pageSize Items per page (&gt; 0)
	 * @param filter   The query filter (not null)
	 * @return Lazy stream of the requested page; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findPage(int page, int pageSize, QueryFilter filter) {
		return datastore.query(beanClass).filter(filter).restrict(pageSize, page * pageSize).stream();
	}

	/**
	 * Retrieve a sorted page using zero-based page index (no filter applied).
	 *
	 * @param page     Zero-based page index (&gt;= 0)
	 * @param pageSize Items per page (&gt; 0)
	 * @param sort     The query sort (not null)
	 * @return Lazy stream of the requested page; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findPage(int page, int pageSize, QuerySort sort) {
		return datastore.query(beanClass).sort(sort).restrict(pageSize, page * pageSize).stream();
	}

	/**
	 * Retrieve a filtered and sorted page using zero-based page index.
	 * Both {@code filter} and {@code sort} may be {@code null}.
	 * <p>
	 * Alternative parameter ordering to {@link #findPage(QueryFilter, QuerySort, int, int)} —
	 * positional (page/pageSize) arguments come first.
	 * </p>
	 *
	 * @param page     Zero-based page index (&gt;= 0)
	 * @param pageSize Items per page (&gt; 0)
	 * @param filter   Optional query filter
	 * @param sort     Optional query sort
	 * @return Lazy stream of the requested page; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findPage(int page, int pageSize, QueryFilter filter, QuerySort sort) {
		BeanQuery<T> q = datastore.query(beanClass);
		if (filter != null) q = q.filter(filter);
		if (sort   != null) q = q.sort(sort);
		return q.restrict(pageSize, page * pageSize).stream();
	}

	/**
	 * Retrieve a filtered and sorted page using zero-based page index.
	 * Both {@code filter} and {@code sort} may be {@code null}.
	 *
	 * @param filter   Optional query filter
	 * @param sort     Optional query sort
	 * @param page     Zero-based page index (&gt;= 0)
	 * @param pageSize Items per page (&gt; 0)
	 * @return Lazy stream of the requested page; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findPage(QueryFilter filter, QuerySort sort, int page, int pageSize) {
		BeanQuery<T> q = datastore.query(beanClass);
		if (filter != null) q = q.filter(filter);
		if (sort   != null) q = q.sort(sort);
		return q.restrict(pageSize, page * pageSize).stream();
	}

	/**
	 * Retrieve a slice using explicit {@code LIMIT} and {@code OFFSET}.
	 *
	 * @param limit  Max results (&gt; 0)
	 * @param offset Zero-based start index (&gt;= 0)
	 * @return Lazy stream of the requested slice; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(int limit, int offset) {
		return datastore.query(beanClass).restrict(limit, offset).stream();
	}

	/**
	 * Retrieve a filtered slice using explicit {@code LIMIT} and {@code OFFSET} (no sort applied).
	 *
	 * @param limit  Max results (&gt; 0)
	 * @param offset Zero-based start index (&gt;= 0)
	 * @param filter The query filter (not null)
	 * @return Lazy stream of the requested slice; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(int limit, int offset, QueryFilter filter) {
		return datastore.query(beanClass).filter(filter).restrict(limit, offset).stream();
	}

	/**
	 * Retrieve a sorted slice using explicit {@code LIMIT} and {@code OFFSET} (no filter applied).
	 *
	 * @param limit  Max results (&gt; 0)
	 * @param offset Zero-based start index (&gt;= 0)
	 * @param sort   The query sort (not null)
	 * @return Lazy stream of the requested slice; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(int limit, int offset, QuerySort sort) {
		return datastore.query(beanClass).sort(sort).restrict(limit, offset).stream();
	}

	/**
	 * Retrieve a filtered and sorted slice using explicit {@code LIMIT} and {@code OFFSET}.
	 * Both {@code filter} and {@code sort} may be {@code null}.
	 * <p>
	 * Alternative to {@link #findSlice(QueryFilter, QuerySort, int, int)} where the positional
	 * arguments lead (useful when building calls programmatically).
	 * </p>
	 *
	 * @param limit  Max results (&gt; 0)
	 * @param offset Zero-based start index (&gt;= 0)
	 * @param filter Optional query filter
	 * @param sort   Optional query sort
	 * @return Lazy stream of the requested slice; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(int limit, int offset, QueryFilter filter, QuerySort sort) {
		BeanQuery<T> q = datastore.query(beanClass);
		if (filter != null) q = q.filter(filter);
		if (sort   != null) q = q.sort(sort);
		return q.restrict(limit, offset).stream();
	}

	/**
	 * Retrieve a filtered and sorted slice using explicit {@code LIMIT} and {@code OFFSET}.
	 * Both {@code filter} and {@code sort} may be {@code null}.
	 *
	 * @param filter Optional query filter
	 * @param sort   Optional query sort
	 * @param limit  Max results (&gt; 0)
	 * @param offset Zero-based start index (&gt;= 0)
	 * @return Lazy stream of the requested slice; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(QueryFilter filter, QuerySort sort, int limit, int offset) {
		BeanQuery<T> q = datastore.query(beanClass);
		if (filter != null) q = q.filter(filter);
		if (sort   != null) q = q.sort(sort);
		return q.restrict(limit, offset).stream();
	}

	// -------------------------------------------------------------------------
	// PageableQuery-based overloads (compatible with Vaadin Query<T,?>)
	// -------------------------------------------------------------------------

	/**
	 * Retrieve a slice using the limit and offset from the given {@link PageableQuery}.
	 * <p>
	 * This is compatible with Vaadin's {@code Query<T, F>} which provides
	 * {@code getLimit()} and {@code getOffset()}.
	 * </p>
	 *
	 * @param query The pageable query providing limit and offset (not null)
	 * @return Lazy stream of the requested slice; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(PageableQuery query) {
		ObjectUtils.argumentNotNull(query, "PageableQuery must be not null");
		return findSlice(query.getLimit(), query.getOffset());
	}

	/**
	 * Retrieve a filtered slice using the limit and offset from the given {@link PageableQuery}.
	 *
	 * @param query  The pageable query providing limit and offset (not null)
	 * @param filter The query filter (not null)
	 * @return Lazy stream of the requested slice; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(PageableQuery query, QueryFilter filter) {
		ObjectUtils.argumentNotNull(query, "PageableQuery must be not null");
		return findSlice(query.getLimit(), query.getOffset(), filter);
	}

	/**
	 * Retrieve a sorted slice using the limit and offset from the given {@link PageableQuery}.
	 *
	 * @param query The pageable query providing limit and offset (not null)
	 * @param sort  The query sort (not null)
	 * @return Lazy stream of the requested slice; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(PageableQuery query, QuerySort sort) {
		ObjectUtils.argumentNotNull(query, "PageableQuery must be not null");
		return findSlice(query.getLimit(), query.getOffset(), sort);
	}

	/**
	 * Retrieve a filtered and sorted slice using the limit and offset from the given {@link PageableQuery}.
	 * Both {@code filter} and {@code sort} may be {@code null}.
	 *
	 * @param query  The pageable query providing limit and offset (not null)
	 * @param filter Optional query filter
	 * @param sort   Optional query sort
	 * @return Lazy stream of the requested slice; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(PageableQuery query, QueryFilter filter, QuerySort sort) {
		ObjectUtils.argumentNotNull(query, "PageableQuery must be not null");
		return findSlice(query.getLimit(), query.getOffset(), filter, sort);
	}

	// -------------------------------------------------------------------------
	// findSlice with List<String> columns (resolved via BeanPropertySet)
	// -------------------------------------------------------------------------

	/**
	 * Retrieve a slice using explicit {@code LIMIT} and {@code OFFSET}, fetching only the
	 * specified columns identified by bean property names.
	 * <p>
	 * Column names are resolved to {@link Path} instances using {@link BeanPropertySet}.
	 * </p>
	 *
	 * @param limit   Max results (&gt; 0)
	 * @param offset  Zero-based start index (&gt;= 0)
	 * @param columns Bean property names identifying the columns to fetch (not null, not empty)
	 * @return Lazy stream of partially-populated instances; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(int limit, int offset, List<String> columns) {
		return findSlice(limit, offset, resolveColumns(columns));
	}

	/**
	 * Retrieve a filtered slice, fetching only the specified columns identified by bean property names.
	 *
	 * @param limit   Max results (&gt; 0)
	 * @param offset  Zero-based start index (&gt;= 0)
	 * @param filter  The query filter (not null)
	 * @param columns Bean property names identifying the columns to fetch (not null, not empty)
	 * @return Lazy stream of partially-populated instances; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(int limit, int offset, QueryFilter filter, List<String> columns) {
		return findSlice(limit, offset, filter, resolveColumns(columns));
	}

	/**
	 * Retrieve a sorted slice, fetching only the specified columns identified by bean property names.
	 *
	 * @param limit   Max results (&gt; 0)
	 * @param offset  Zero-based start index (&gt;= 0)
	 * @param sort    The query sort (not null)
	 * @param columns Bean property names identifying the columns to fetch (not null, not empty)
	 * @return Lazy stream of partially-populated instances; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(int limit, int offset, QuerySort sort, List<String> columns) {
		return findSlice(limit, offset, sort, resolveColumns(columns));
	}

	/**
	 * Retrieve a filtered and sorted slice, fetching only the specified columns identified by
	 * bean property names. Both {@code filter} and {@code sort} may be {@code null}.
	 *
	 * @param limit   Max results (&gt; 0)
	 * @param offset  Zero-based start index (&gt;= 0)
	 * @param filter  Optional query filter
	 * @param sort    Optional query sort
	 * @param columns Bean property names identifying the columns to fetch (not null, not empty)
	 * @return Lazy stream of partially-populated instances; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(int limit, int offset, QueryFilter filter, QuerySort sort, List<String> columns) {
		return findSlice(limit, offset, filter, sort, resolveColumns(columns));
	}

	/**
	 * Retrieve a slice using the given {@link PageableQuery}, fetching only the specified columns
	 * identified by bean property names.
	 *
	 * @param query   The pageable query providing limit and offset (not null)
	 * @param columns Bean property names identifying the columns to fetch (not null, not empty)
	 * @return Lazy stream of partially-populated instances; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(PageableQuery query, List<String> columns) {
		ObjectUtils.argumentNotNull(query, "PageableQuery must be not null");
		return findSlice(query.getLimit(), query.getOffset(), resolveColumns(columns));
	}

	/**
	 * Retrieve a filtered and sorted slice using the given {@link PageableQuery}, fetching only
	 * the specified columns identified by bean property names.
	 * Both {@code filter} and {@code sort} may be {@code null}.
	 *
	 * @param query   The pageable query providing limit and offset (not null)
	 * @param filter  Optional query filter
	 * @param sort    Optional query sort
	 * @param columns Bean property names identifying the columns to fetch (not null, not empty)
	 * @return Lazy stream of partially-populated instances; never {@code null}
	 * @throws DataAccessException if an error occurs
	 */
	public Stream<T> findSlice(PageableQuery query, QueryFilter filter, QuerySort sort, List<String> columns) {
		ObjectUtils.argumentNotNull(query, "PageableQuery must be not null");
		return findSlice(query.getLimit(), query.getOffset(), filter, sort, resolveColumns(columns));
	}

	// -------------------------------------------------------------------------
	// Internal helper
	// -------------------------------------------------------------------------

	/**
	 * Resolve a list of bean property names to an array of {@link Path} instances.
	 */
	private Path<?>[] resolveColumns(List<String> columns) {
		ObjectUtils.argumentNotNull(columns, "Columns list must be not null");
		if (columns.isEmpty()) {
			throw new IllegalArgumentException("Columns list must not be empty");
		}
		BeanPropertySet<T> ps = BeanPropertySet.create(beanClass);
		return columns.stream()
				.map(ps::property)
				.toArray(Path<?>[]::new);
	}

	// -------------------------------------------------------------------------
	// Count / exists
	// -------------------------------------------------------------------------

	/**
	 * Count all persisted instances of this bean type.
	 *
	 * @return Total count
	 * @throws DataAccessException if an error occurs
	 */
	public long count() {
		return BeanDatastoreUtils.count(datastore, beanClass);
	}

	/**
	 * Count instances matching the given filter.
	 *
	 * @param filter The query filter (not null)
	 * @return Matching count
	 * @throws DataAccessException if an error occurs
	 */
	public long count(QueryFilter filter) {
		return BeanDatastoreUtils.count(datastore, beanClass, filter);
	}

	/**
	 * Return {@code true} if at least one instance matches the given filter.
	 *
	 * @param filter The query filter (not null)
	 * @return {@code true} if matching instances exist
	 * @throws DataAccessException if an error occurs
	 */
	public boolean exists(QueryFilter filter) {
		return BeanDatastoreUtils.exists(datastore, beanClass, filter);
	}

	// -------------------------------------------------------------------------
	// Bulk operations
	// -------------------------------------------------------------------------

	/**
	 * Bulk-insert a collection of beans in a single SQL statement.
	 *
	 * @param beans   Non-null, non-empty collection
	 * @param options Optional write options
	 * @return The operation result
	 * @throws DataAccessException if an error occurs
	 */
	public BeanOperationResult<?> bulkInsert(Collection<? extends T> beans, WriteOption... options) {
		return BeanDatastoreUtils.bulkInsert(datastore, beanClass, beans, options);
	}

	/**
	 * Bulk-insert a varargs array of beans in a single SQL statement.
	 *
	 * @param beans Non-null, non-empty instances
	 * @return The operation result
	 * @throws DataAccessException if an error occurs
	 */
	@SafeVarargs
	public final BeanOperationResult<?> bulkInsert(T... beans) {
		return BeanDatastoreUtils.bulkInsert(datastore, beanClass, beans);
	}

	/**
	 * Update each bean in the list individually; returns the total affected-row count.
	 *
	 * @param beans   Non-null, non-empty list
	 * @param options Optional write options
	 * @return Total affected rows
	 * @throws DataAccessException if an error occurs
	 */
	public long bulkUpdate(List<? extends T> beans, WriteOption... options) {
		return BeanDatastoreUtils.bulkUpdate(datastore, beans, options);
	}

	/**
	 * Save (upsert) each bean in the list individually; returns the total affected-row count.
	 *
	 * @param beans   Non-null, non-empty list
	 * @param options Optional write options
	 * @return Total affected rows
	 * @throws DataAccessException if an error occurs
	 */
	public long bulkSave(List<? extends T> beans, WriteOption... options) {
		return BeanDatastoreUtils.bulkSave(datastore, beans, options);
	}

	/**
	 * Delete each bean in the list individually; returns the total affected-row count.
	 *
	 * @param beans   Non-null, non-empty list
	 * @param options Optional write options
	 * @return Total affected rows
	 * @throws DataAccessException if an error occurs
	 */
	public long bulkDelete(List<? extends T> beans, WriteOption... options) {
		return BeanDatastoreUtils.bulkDelete(datastore, beans, options);
	}

	/**
	 * Bulk-delete all instances matching the given filter in a single SQL statement.
	 *
	 * @param filter  The filter (not null)
	 * @param options Optional write options
	 * @return The operation result
	 * @throws DataAccessException if an error occurs
	 */
	public BeanOperationResult<?> bulkDelete(QueryFilter filter, WriteOption... options) {
		return BeanDatastoreUtils.bulkDelete(datastore, beanClass, filter, options);
	}

	/**
	 * Return a {@link BeanBulkUpdate} builder pre-filtered by the given filter.
	 * Chain {@code .set()} calls then call {@code .execute()}.
	 *
	 * <pre>{@code
	 * products.bulkUpdate(homeFilter)
	 *         .set("category", "SALE")
	 *         .set("price",    49.99)
	 *         .execute();
	 * }</pre>
	 *
	 * @param filter  The filter (not null)
	 * @param options Optional write options
	 * @return A configured bulk-update builder
	 */
	public BeanBulkUpdate<T> bulkUpdate(QueryFilter filter, WriteOption... options) {
		return BeanDatastoreUtils.bulkUpdate(datastore, beanClass, filter, options);
	}

	/**
	 * Bulk-update a single property for all instances matching the filter; executes immediately.
	 *
	 * @param filter       The filter (not null)
	 * @param propertyName Property to update (not null)
	 * @param value        New value
	 * @param options      Optional write options
	 * @return The operation result
	 * @throws DataAccessException if an error occurs
	 */
	public BeanOperationResult<?> bulkUpdateProperty(QueryFilter filter,
			String propertyName, Object value, WriteOption... options) {
		return BeanDatastoreUtils.bulkUpdateProperty(datastore, beanClass, filter, propertyName, value, options);
	}

	/**
	 * Bulk-update multiple properties for all instances matching the filter; executes immediately.
	 *
	 * @param filter         The filter (not null)
	 * @param propertyValues Map of {@code propertyName -> value} (not null, not empty)
	 * @param options        Optional write options
	 * @return The operation result
	 * @throws DataAccessException if an error occurs
	 */
	public BeanOperationResult<?> bulkUpdateProperties(QueryFilter filter,
			Map<String, Object> propertyValues, WriteOption... options) {
		return BeanDatastoreUtils.bulkUpdateProperties(datastore, beanClass, filter, propertyValues, options);
	}

	// -------------------------------------------------------------------------
	// Transactions
	// -------------------------------------------------------------------------

	/**
	 * Execute the given operation within a transaction.
	 * Commits on success; rolls back on any exception.
	 *
	 * @param <R>       Result type
	 * @param operation The transactional operation (not null)
	 * @return The operation result
	 * @throws IllegalStateException if the underlying datastore does not support transactions
	 * @throws DataAccessException   if an error occurs
	 */
	public <R> R withTransaction(BeanTransactionalOperation<R> operation) {
		return BeanDatastoreUtils.withTransaction(datastore, operation);
	}

	/**
	 * Return {@code true} if the underlying datastore supports transactions.
	 *
	 * @return {@code true} if transactional
	 */
	public boolean isTransactional() {
		return BeanDatastoreUtils.isTransactional(datastore);
	}
}
