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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.holonplatform.core.datastore.Datastore;
import com.holonplatform.core.datastore.DatastoreOperations.WriteOption;
import com.holonplatform.core.datastore.beans.BeanDatastore.BeanOperationResult;
import com.holonplatform.core.exceptions.DataAccessException;
import com.holonplatform.core.internal.utils.ObjectUtils;
import com.holonplatform.core.query.QueryFilter;
import com.holonplatform.core.query.QuerySort;
import com.holonplatform.core.query.QueryResults.QueryNonUniqueResultException;

/**
 * Utility class providing convenient static methods for common bean CRUD operations
 * backed by a {@link BeanDatastore}.
 *
 * <p>
 * All methods validate their {@link BeanDatastore} argument and throw {@link IllegalArgumentException}
 * for {@code null} inputs. Data-access failures propagate as {@link DataAccessException}.
 * </p>
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * BeanDatastore bds = BeanDatastore.of(myDatastore);
 *
 * // Insert
 * MyBean saved = BeanDatastoreUtils.insert(bds, new MyBean(...)).getResult().orElseThrow();
 *
 * // Find all with filter + sort
 * List<MyBean> results = BeanDatastoreUtils.findAll(bds, MyBean.class,
 *         QueryFilter.eq(myProperty, value),
 *         QuerySort.asc(nameProperty));
 *
 * // Paginated list (zero-based page index)
 * List<MyBean> page = BeanDatastoreUtils.findPage(bds, MyBean.class, 0, 20);
 * }</pre>
 *
 * @since 5.5.0
 */
public final class BeanDatastoreUtils {

	private BeanDatastoreUtils() {
		// utility class
	}

	// -------------------------------------------------------------------------
	// Single-bean write operations
	// -------------------------------------------------------------------------

	/**
	 * Insert a bean instance into the datastore.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param bean      The bean instance to insert (not null)
	 * @param options   Optional write options
	 * @return The {@link BeanOperationResult} describing the outcome
	 * @throws IllegalArgumentException if {@code datastore} or {@code bean} is null
	 * @throws DataAccessException      if an error occurs during the operation
	 */
	public static <T> BeanOperationResult<T> insert(BeanDatastore datastore, T bean, WriteOption... options) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(bean, "Bean instance must be not null");
		return datastore.insert(bean, options);
	}

	/**
	 * Update a bean instance in the datastore.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param bean      The bean instance to update (not null)
	 * @param options   Optional write options
	 * @return The {@link BeanOperationResult} describing the outcome
	 * @throws IllegalArgumentException if {@code datastore} or {@code bean} is null
	 * @throws DataAccessException      if an error occurs during the operation
	 */
	public static <T> BeanOperationResult<T> update(BeanDatastore datastore, T bean, WriteOption... options) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(bean, "Bean instance must be not null");
		return datastore.update(bean, options);
	}

	/**
	 * Save a bean instance (insert if absent, update otherwise).
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param bean      The bean instance to save (not null)
	 * @param options   Optional write options
	 * @return The {@link BeanOperationResult} describing the outcome
	 * @throws IllegalArgumentException if {@code datastore} or {@code bean} is null
	 * @throws DataAccessException      if an error occurs during the operation
	 */
	public static <T> BeanOperationResult<T> save(BeanDatastore datastore, T bean, WriteOption... options) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(bean, "Bean instance must be not null");
		return datastore.save(bean, options);
	}

	/**
	 * Delete a bean instance from the datastore.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param bean      The bean instance to delete (not null)
	 * @param options   Optional write options
	 * @return The {@link BeanOperationResult} describing the outcome
	 * @throws IllegalArgumentException if {@code datastore} or {@code bean} is null
	 * @throws DataAccessException      if an error occurs during the operation
	 */
	public static <T> BeanOperationResult<T> delete(BeanDatastore datastore, T bean, WriteOption... options) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(bean, "Bean instance must be not null");
		return datastore.delete(bean, options);
	}

	/**
	 * Refresh a bean instance, reloading its properties from the datastore.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param bean      The bean instance to refresh (not null)
	 * @return The refreshed bean instance
	 * @throws IllegalArgumentException if {@code datastore} or {@code bean} is null
	 * @throws DataAccessException      if an error occurs during the operation
	 */
	public static <T> T refresh(BeanDatastore datastore, T bean) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(bean, "Bean instance must be not null");
		return datastore.refresh(bean);
	}

	// -------------------------------------------------------------------------
	// Query / Read operations
	// -------------------------------------------------------------------------

	/**
	 * Retrieve all bean instances of the given type, without any filter.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @return A {@link List} with all persisted instances; never {@code null}
	 * @throws IllegalArgumentException if any argument is null
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> List<T> findAll(BeanDatastore datastore, Class<T> beanClass) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		return datastore.query(beanClass).list();
	}

	/**
	 * Retrieve all bean instances matching the given filter.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param filter    The query filter (not null)
	 * @return A {@link List} with matching instances; never {@code null}
	 * @throws IllegalArgumentException if any argument is null
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> List<T> findAll(BeanDatastore datastore, Class<T> beanClass, QueryFilter filter) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		ObjectUtils.argumentNotNull(filter, "QueryFilter must be not null");
		return datastore.query(beanClass).filter(filter).list();
	}

	/**
	 * Retrieve all bean instances matching the given filter, ordered by the given sort.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param filter    The query filter (may be {@code null} to return all instances)
	 * @param sort      The query sort (may be {@code null} to use default ordering)
	 * @return A {@link List} with matching instances; never {@code null}
	 * @throws IllegalArgumentException if {@code datastore} or {@code beanClass} is null
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> List<T> findAll(BeanDatastore datastore, Class<T> beanClass, QueryFilter filter,
			QuerySort sort) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		BeanQuery<T> query = datastore.query(beanClass);
		if (filter != null) {
			query = query.filter(filter);
		}
		if (sort != null) {
			query = query.sort(sort);
		}
		return query.list();
	}

	/**
	 * Retrieve a single bean instance matching the given filter.
	 * <p>
	 * Returns an empty {@link Optional} when no records match. Throws
	 * {@link QueryNonUniqueResultException} when more than one record matches.
	 * </p>
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param filter    The query filter (not null)
	 * @return An {@link Optional} containing the matching bean, or empty
	 * @throws IllegalArgumentException        if any argument is null
	 * @throws QueryNonUniqueResultException   if more than one result is found
	 * @throws DataAccessException             if an error occurs during the query
	 */
	public static <T> Optional<T> findOne(BeanDatastore datastore, Class<T> beanClass, QueryFilter filter) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		ObjectUtils.argumentNotNull(filter, "QueryFilter must be not null");
		return datastore.query(beanClass).filter(filter).findOne();
	}

	/**
	 * Return the first bean instance with no filter applied, using datastore-default ordering.
	 * <p>
	 * Equivalent to {@code LIMIT 1 OFFSET 0} with no {@code ORDER BY}. Since ordering is not
	 * specified the result is non-deterministic — prefer
	 * {@link #findFirst(BeanDatastore, Class, QueryFilter, QuerySort)} when a stable first row
	 * is required.
	 * </p>
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @return The first row, or empty if the table is empty
	 * @throws IllegalArgumentException if any argument is null
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> Optional<T> findFirst(BeanDatastore datastore, Class<T> beanClass) {
		return findFirst(datastore, beanClass, null, null);
	}

	/**
	 * Return the first bean instance matching the given filter, using datastore-default ordering.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param filter    The query filter (not null)
	 * @return The first matching row, or empty if none
	 * @throws IllegalArgumentException if any argument is null
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> Optional<T> findFirst(BeanDatastore datastore, Class<T> beanClass, QueryFilter filter) {
		return findFirst(datastore, beanClass, filter, null);
	}

	/**
	 * Return the first bean instance matching the given filter in the given sort order.
	 * <p>
	 * Both {@code filter} and {@code sort} are optional and may be {@code null}.
	 * Translates to {@code [WHERE …] [ORDER BY …] LIMIT 1 OFFSET 0}.
	 * </p>
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param filter    Optional query filter
	 * @param sort      Optional query sort (determines which row is "first")
	 * @return The first matching row, or empty if the result set is empty
	 * @throws IllegalArgumentException if {@code datastore} or {@code beanClass} is null
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> Optional<T> findFirst(BeanDatastore datastore, Class<T> beanClass,
			QueryFilter filter, QuerySort sort) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		BeanQuery<T> query = datastore.query(beanClass);
		if (filter != null) query = query.filter(filter);
		if (sort   != null) query = query.sort(sort);
		List<T> result = query.restrict(1, 0).list();
		return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
	}

	/**
	 * Return the first {@code n} bean instances with no filter, using datastore-default ordering.
	 * Equivalent to {@code LIMIT n OFFSET 0}.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param n         Maximum number of results (&gt; 0)
	 * @return Up to {@code n} instances; may be empty but never {@code null}
	 * @throws IllegalArgumentException if any argument is null or {@code n <= 0}
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> List<T> findTop(BeanDatastore datastore, Class<T> beanClass, int n) {
		return findTop(datastore, beanClass, n, null, null);
	}

	/**
	 * Return the first {@code n} bean instances ordered by the given sort.
	 * Equivalent to {@code ORDER BY … LIMIT n OFFSET 0}.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param n         Maximum number of results (&gt; 0)
	 * @param sort      The query sort (not null)
	 * @return Up to {@code n} ordered instances; may be empty but never {@code null}
	 * @throws IllegalArgumentException if any required argument is null or {@code n <= 0}
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> List<T> findTop(BeanDatastore datastore, Class<T> beanClass, int n, QuerySort sort) {
		return findTop(datastore, beanClass, n, null, sort);
	}

	/**
	 * Return the first {@code n} bean instances matching the given filter.
	 * Equivalent to {@code WHERE … LIMIT n OFFSET 0}.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param n         Maximum number of results (&gt; 0)
	 * @param filter    The query filter (not null)
	 * @return Up to {@code n} matching instances; may be empty but never {@code null}
	 * @throws IllegalArgumentException if any required argument is null or {@code n <= 0}
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> List<T> findTop(BeanDatastore datastore, Class<T> beanClass, int n, QueryFilter filter) {
		return findTop(datastore, beanClass, n, filter, null);
	}

	/**
	 * Return the first {@code n} bean instances matching the given filter in the given sort order.
	 * Equivalent to {@code [WHERE …] [ORDER BY …] LIMIT n OFFSET 0}.
	 * Both {@code filter} and {@code sort} are optional and may be {@code null}.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param n         Maximum number of results (&gt; 0)
	 * @param filter    Optional query filter
	 * @param sort      Optional query sort
	 * @return Up to {@code n} instances; may be empty but never {@code null}
	 * @throws IllegalArgumentException if {@code datastore} or {@code beanClass} is null, or {@code n <= 0}
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> List<T> findTop(BeanDatastore datastore, Class<T> beanClass, int n,
			QueryFilter filter, QuerySort sort) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		if (n <= 0) throw new IllegalArgumentException("n must be > 0");
		BeanQuery<T> query = datastore.query(beanClass);
		if (filter != null) query = query.filter(filter);
		if (sort   != null) query = query.sort(sort);
		return query.restrict(n, 0).list();
	}

	/**
	 * Return a {@link Stream} of bean instances of the given type, without any filter.
	 * <p>
	 * The caller is responsible for closing the stream after use.
	 * </p>
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @return A {@link Stream} of persisted instances; never {@code null}
	 * @throws IllegalArgumentException if any argument is null
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> Stream<T> stream(BeanDatastore datastore, Class<T> beanClass) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		return datastore.query(beanClass).stream();
	}

	/**
	 * Return a {@link Stream} of bean instances matching the given filter.
	 * <p>
	 * The caller is responsible for closing the stream after use.
	 * </p>
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param filter    The query filter (not null)
	 * @return A {@link Stream} of matching instances; never {@code null}
	 * @throws IllegalArgumentException if any argument is null
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> Stream<T> stream(BeanDatastore datastore, Class<T> beanClass, QueryFilter filter) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		ObjectUtils.argumentNotNull(filter, "QueryFilter must be not null");
		return datastore.query(beanClass).filter(filter).stream();
	}

	// -------------------------------------------------------------------------
	// Pagination
	// -------------------------------------------------------------------------

	/**
	 * Retrieve a page of bean instances of the given type using zero-based page index.
	 * <p>
	 * Translates page index to raw limit / offset using the formula
	 * {@code limit = pageSize}, {@code offset = page * pageSize}, then delegates to
	 * {@link BeanQueryBuilder#restrict(int, int)}.
	 * This method issues a <strong>single</strong> data-fetching query and never
	 * triggers an automatic count query.  Use {@link #count(BeanDatastore, Class)}
	 * explicitly when a total count is needed.
	 * </p>
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param page      Zero-based page index (must be &gt;= 0)
	 * @param pageSize  Number of results per page (must be &gt; 0)
	 * @return A {@link List} containing the requested page; may be empty but never {@code null}
	 * @throws IllegalArgumentException if any argument is null or page/pageSize values are invalid
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> List<T> findPage(BeanDatastore datastore, Class<T> beanClass, int page, int pageSize) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		if (page < 0) {
			throw new IllegalArgumentException("Page index must be >= 0");
		}
		if (pageSize <= 0) {
			throw new IllegalArgumentException("Page size must be > 0");
		}
		// restrict(limit, offset) = restrict(pageSize, page * pageSize)
		return datastore.query(beanClass).restrict(pageSize, page * pageSize).list();
	}

	/**
	 * Retrieve a filtered and sorted page of bean instances using zero-based page index.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param filter    The query filter (may be {@code null} to return all instances)
	 * @param sort      The query sort (may be {@code null} to use default ordering)
	 * @param page      Zero-based page index (must be &gt;= 0)
	 * @param pageSize  Number of results per page (must be &gt; 0)
	 * @return A {@link List} containing the requested page; may be empty but never {@code null}
	 * @throws IllegalArgumentException if any required argument is null or page/pageSize values are invalid
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> List<T> findPage(BeanDatastore datastore, Class<T> beanClass, QueryFilter filter,
			QuerySort sort, int page, int pageSize) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		if (page < 0) {
			throw new IllegalArgumentException("Page index must be >= 0");
		}
		if (pageSize <= 0) {
			throw new IllegalArgumentException("Page size must be > 0");
		}
		BeanQuery<T> query = datastore.query(beanClass);
		if (filter != null) {
			query = query.filter(filter);
		}
		if (sort != null) {
			query = query.sort(sort);
		}
		// restrict(limit, offset) = restrict(pageSize, page * pageSize)
		return query.restrict(pageSize, page * pageSize).list();
	}

	/**
	 * Retrieve a filtered page of bean instances using zero-based page index (no sort applied).
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param filter    The query filter (may be {@code null} to return all instances)
	 * @param page      Zero-based page index (must be &gt;= 0)
	 * @param pageSize  Number of results per page (must be &gt; 0)
	 * @return A {@link List} containing the requested page; may be empty but never {@code null}
	 * @throws IllegalArgumentException if any required argument is null or page/pageSize values are invalid
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> List<T> findPage(BeanDatastore datastore, Class<T> beanClass, QueryFilter filter,
			int page, int pageSize) {
		return findPage(datastore, beanClass, filter, null, page, pageSize);
	}

	/**
	 * Retrieve a sorted page of bean instances using zero-based page index (no filter applied).
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param sort      The query sort (may be {@code null} to use default ordering)
	 * @param page      Zero-based page index (must be &gt;= 0)
	 * @param pageSize  Number of results per page (must be &gt; 0)
	 * @return A {@link List} containing the requested page; may be empty but never {@code null}
	 * @throws IllegalArgumentException if any required argument is null or page/pageSize values are invalid
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> List<T> findPage(BeanDatastore datastore, Class<T> beanClass, QuerySort sort,
			int page, int pageSize) {
		return findPage(datastore, beanClass, null, sort, page, pageSize);
	}

	/**
	 * Retrieve a slice of bean instances using explicit <strong>limit</strong> and <strong>offset</strong> values.
	 * <p>
	 * Use this overload when you already know the raw limit/offset values and do not want to express them
	 * as a page index.  For page-index–based access use {@link #findPage(BeanDatastore, Class, int, int)}.
	 * </p>
	 * <pre>{@code
	 * // SQL equivalent:  SELECT * FROM product ORDER BY name LIMIT 10 OFFSET 25
	 * List<Product> slice = BeanDatastoreUtils.findSlice(bds, Product.class, 10, 25);
	 * }</pre>
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param limit     Maximum number of results to return (must be &gt; 0)
	 * @param offset    Zero-based index of the first result to return (must be &gt;= 0)
	 * @return A {@link List} with at most {@code limit} items starting at {@code offset}; never {@code null}
	 * @throws IllegalArgumentException if any required argument is null or limit/offset values are invalid
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> List<T> findSlice(BeanDatastore datastore, Class<T> beanClass, int limit, int offset) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		if (limit <= 0) {
			throw new IllegalArgumentException("Limit must be > 0");
		}
		if (offset < 0) {
			throw new IllegalArgumentException("Offset must be >= 0");
		}
		return datastore.query(beanClass).restrict(limit, offset).list();
	}

	/**
	 * Retrieve a filtered and sorted slice of bean instances using explicit <strong>limit</strong>
	 * and <strong>offset</strong> values.
	 * <p>
	 * Both {@code filter} and {@code sort} are optional and may be {@code null}.
	 * </p>
	 * <pre>{@code
	 * // SQL equivalent:
	 * //   SELECT * FROM product WHERE category = 'TECH' ORDER BY price ASC LIMIT 10 OFFSET 25
	 * QueryFilter categoryFilter = QueryFilter.eq(PRODUCT.category, "TECH");
	 * QuerySort  priceAsc        = QuerySort.asc(PRODUCT.price);
	 * List<Product> slice = BeanDatastoreUtils.findSlice(bds, Product.class, categoryFilter, priceAsc, 10, 25);
	 * }</pre>
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param filter    Optional query filter; {@code null} means no restriction
	 * @param sort      Optional query sort; {@code null} means datastore-default ordering
	 * @param limit     Maximum number of results to return (must be &gt; 0)
	 * @param offset    Zero-based index of the first result to return (must be &gt;= 0)
	 * @return A {@link List} with at most {@code limit} items; never {@code null}
	 * @throws IllegalArgumentException if any required argument is null or limit/offset values are invalid
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> List<T> findSlice(BeanDatastore datastore, Class<T> beanClass, QueryFilter filter,
			QuerySort sort, int limit, int offset) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		if (limit <= 0) {
			throw new IllegalArgumentException("Limit must be > 0");
		}
		if (offset < 0) {
			throw new IllegalArgumentException("Offset must be >= 0");
		}
		BeanQuery<T> query = datastore.query(beanClass);
		if (filter != null) {
			query = query.filter(filter);
		}
		if (sort != null) {
			query = query.sort(sort);
		}
		return query.restrict(limit, offset).list();
	}

	/**
	 * Retrieve a filtered slice of bean instances using explicit limit and offset (no sort applied).
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param filter    Optional query filter; {@code null} means no restriction
	 * @param limit     Maximum number of results to return (must be &gt; 0)
	 * @param offset    Zero-based index of the first result to return (must be &gt;= 0)
	 * @return A {@link List} with at most {@code limit} items; never {@code null}
	 * @throws IllegalArgumentException if any required argument is null or limit/offset values are invalid
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> List<T> findSlice(BeanDatastore datastore, Class<T> beanClass, QueryFilter filter,
			int limit, int offset) {
		return findSlice(datastore, beanClass, filter, null, limit, offset);
	}

	/**
	 * Retrieve a sorted slice of bean instances using explicit limit and offset (no filter applied).
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param sort      Optional query sort; {@code null} means datastore-default ordering
	 * @param limit     Maximum number of results to return (must be &gt; 0)
	 * @param offset    Zero-based index of the first result to return (must be &gt;= 0)
	 * @return A {@link List} with at most {@code limit} items; never {@code null}
	 * @throws IllegalArgumentException if any required argument is null or limit/offset values are invalid
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> List<T> findSlice(BeanDatastore datastore, Class<T> beanClass, QuerySort sort,
			int limit, int offset) {
		return findSlice(datastore, beanClass, null, sort, limit, offset);
	}

	// -------------------------------------------------------------------------
	// Count
	// -------------------------------------------------------------------------

	/**
	 * Count all bean instances of the given type.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @return Total number of persisted instances
	 * @throws IllegalArgumentException if any argument is null
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> long count(BeanDatastore datastore, Class<T> beanClass) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		return datastore.query(beanClass).count();
	}

	/**
	 * Count bean instances matching the given filter.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param filter    The query filter (not null)
	 * @return Number of matching instances
	 * @throws IllegalArgumentException if any argument is null
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> long count(BeanDatastore datastore, Class<T> beanClass, QueryFilter filter) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		ObjectUtils.argumentNotNull(filter, "QueryFilter must be not null");
		return datastore.query(beanClass).filter(filter).count();
	}

	/**
	 * Check whether at least one bean instance matching the given filter exists.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param filter    The query filter (not null)
	 * @return {@code true} if one or more matching instances exist
	 * @throws IllegalArgumentException if any argument is null
	 * @throws DataAccessException      if an error occurs during the query
	 */
	public static <T> boolean exists(BeanDatastore datastore, Class<T> beanClass, QueryFilter filter) {
        return datastore.query(beanClass)
                .filter(filter)
                .restrict(1, 0)
                .count() > 0;
	}

	// -------------------------------------------------------------------------
	// Bulk operations
	// -------------------------------------------------------------------------

	/**
	 * Bulk-insert a collection of bean instances in a single operation.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param beans     The bean instances to insert (not null, must not be empty)
	 * @param options   Optional write options
	 * @return The {@link BeanOperationResult} describing the outcome
	 * @throws IllegalArgumentException if any argument is null or the collection is empty
	 * @throws DataAccessException      if an error occurs during the operation
	 */
	public static <T> BeanOperationResult<?> bulkInsert(BeanDatastore datastore, Class<T> beanClass,
			Collection<? extends T> beans, WriteOption... options) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		ObjectUtils.argumentNotNull(beans, "Bean collection must be not null");
		if (beans.isEmpty()) {
			throw new IllegalArgumentException("Bean collection must not be empty");
		}
		BeanBulkInsert<T> op = datastore.bulkInsert(beanClass, options);
		beans.forEach(op::add);
		return op.execute();
	}

	/**
	 * Bulk-insert a varargs array of bean instances in a single operation.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param beans     The bean instances to insert (not null, must not be empty)
	 * @return The {@link BeanOperationResult} describing the outcome
	 * @throws IllegalArgumentException if any argument is null or no beans are provided
	 * @throws DataAccessException      if an error occurs during the operation
	 */
	@SafeVarargs
	public static <T> BeanOperationResult<?> bulkInsert(BeanDatastore datastore, Class<T> beanClass, T... beans) {
		ObjectUtils.argumentNotNull(beans, "Bean array must be not null");
		return bulkInsert(datastore, beanClass, Arrays.asList(beans));
	}

	/**
	 * Update each bean instance in the given list individually and return the total affected-row count.
	 * <p>
	 * Issues one {@code UPDATE} statement per bean. For a single-statement bulk update based on a
	 * filter and property assignments use
	 * {@link #bulkUpdate(BeanDatastore, Class, QueryFilter, WriteOption...)} instead.
	 * </p>
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beans     Non-null, non-empty list of bean instances to update
	 * @param options   Optional write options
	 * @return Sum of affected-row counts across all individual UPDATE operations
	 * @throws IllegalArgumentException if {@code datastore} or {@code beans} is null, or the list is empty
	 * @throws DataAccessException      if an error occurs during any individual update
	 */
	public static <T> long bulkUpdate(BeanDatastore datastore, List<? extends T> beans, WriteOption... options) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beans, "Bean list must be not null");
		if (beans.isEmpty()) {
			throw new IllegalArgumentException("Bean list must not be empty");
		}
		return beans.stream()
				.mapToLong(bean -> datastore.update(bean, options).getAffectedCount())
				.sum();
	}

	/**
	 * Save (upsert) each bean instance in the given list individually and return the total affected-row count.
	 * <p>
	 * Calls {@link BeanDatastore#save(Object, WriteOption...)} for every element — each call inserts the bean if it
	 * does not exist yet, or updates it otherwise. Issues one statement per bean.
	 * </p>
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beans     Non-null, non-empty list of bean instances to save
	 * @param options   Optional write options
	 * @return Sum of affected-row counts across all individual SAVE operations
	 * @throws IllegalArgumentException if {@code datastore} or {@code beans} is null, or the list is empty
	 * @throws DataAccessException      if an error occurs during any individual save
	 */
	public static <T> long bulkSave(BeanDatastore datastore, List<? extends T> beans, WriteOption... options) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beans, "Bean list must be not null");
		if (beans.isEmpty()) {
			throw new IllegalArgumentException("Bean list must not be empty");
		}
		return beans.stream()
				.mapToLong(bean -> datastore.save(bean, options).getAffectedCount())
				.sum();
	}

	/**
	 * Delete each bean instance in the given list individually and return the total affected-row count.
	 * <p>
	 * Issues one {@code DELETE} statement per bean. For a single-statement bulk delete based on a
	 * filter use {@link #bulkDelete(BeanDatastore, Class, QueryFilter, WriteOption...)} instead.
	 * </p>
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beans     Non-null, non-empty list of bean instances to delete
	 * @param options   Optional write options
	 * @return Sum of affected-row counts across all individual DELETE operations
	 * @throws IllegalArgumentException if {@code datastore} or {@code beans} is null, or the list is empty
	 * @throws DataAccessException      if an error occurs during any individual delete
	 */
	public static <T> long bulkDelete(BeanDatastore datastore, List<? extends T> beans, WriteOption... options) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beans, "Bean list must be not null");
		if (beans.isEmpty()) {
			throw new IllegalArgumentException("Bean list must not be empty");
		}
		return beans.stream()
				.mapToLong(bean -> datastore.delete(bean, options).getAffectedCount())
				.sum();
	}

	/**
	 * Bulk-delete all bean instances matching the given filter.
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param filter    The filter that selects the instances to delete (not null)
	 * @param options   Optional write options
	 * @return The {@link BeanOperationResult} describing the outcome
	 * @throws IllegalArgumentException if any argument is null
	 * @throws DataAccessException      if an error occurs during the operation
	 */
	public static <T> BeanOperationResult<?> bulkDelete(BeanDatastore datastore, Class<T> beanClass,
			QueryFilter filter, WriteOption... options) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		ObjectUtils.argumentNotNull(filter, "QueryFilter must be not null");
		return datastore.bulkDelete(beanClass, options).filter(filter).execute();
	}

	/**
	 * Bulk-update all bean instances matching the given filter.
	 * <p>
	 * The caller configures the actual property assignments via the returned {@link BeanBulkUpdate} builder
	 * before calling {@code execute()}.
	 * </p>
	 *
	 * @param <T>       Bean type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param beanClass The bean class (not null)
	 * @param filter    The filter that selects the instances to update (not null)
	 * @param options   Optional write options
	 * @return A configured {@link BeanBulkUpdate} builder ready for further customisation and execution
	 * @throws IllegalArgumentException if any argument is null
	 */
	public static <T> BeanBulkUpdate<T> bulkUpdate(BeanDatastore datastore, Class<T> beanClass,
			QueryFilter filter, WriteOption... options) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(beanClass, "Bean class must be not null");
		ObjectUtils.argumentNotNull(filter, "QueryFilter must be not null");
		return datastore.bulkUpdate(beanClass, options).filter(filter);
	}

	/**
	 * Bulk-update a <strong>single bean property</strong> for all instances matching the given filter,
	 * executing the operation immediately.
	 * <p>
	 * Shortcut for:
	 * <pre>{@code
	 * bulkUpdate(datastore, beanClass, filter, options)
	 *     .set(propertyName, value)
	 *     .execute();
	 * }</pre>
	 *
	 * @param <T>          Bean type
	 * @param datastore    The {@link BeanDatastore} to use (not null)
	 * @param beanClass    The bean class (not null)
	 * @param filter       The filter that selects the instances to update (not null)
	 * @param propertyName Bean property name to update (not null)
	 * @param value        New value to set
	 * @param options      Optional write options
	 * @return The {@link BeanOperationResult} describing the outcome
	 * @throws IllegalArgumentException if any required argument is null
	 * @throws DataAccessException      if an error occurs during the operation
	 */
	public static <T> BeanOperationResult<?> bulkUpdateProperty(BeanDatastore datastore, Class<T> beanClass,
			QueryFilter filter, String propertyName, Object value, WriteOption... options) {
		ObjectUtils.argumentNotNull(propertyName, "Property name must be not null");
		return bulkUpdate(datastore, beanClass, filter, options)
				.set(propertyName, value)
				.execute();
	}

	/**
	 * Bulk-update <strong>multiple bean properties</strong> for all instances matching the given filter,
	 * executing the operation immediately.
	 * <p>
	 * Each entry in {@code propertyValues} maps a bean property name to its new value. Example:
	 * <pre>{@code
	 * Map<String, Object> updates = new LinkedHashMap<>();
	 * updates.put("category", "SALE");
	 * updates.put("price",    49.99);
	 * BeanDatastoreUtils.bulkUpdateProperties(bds, Product.class, filter, updates);
	 * }</pre>
	 *
	 * @param <T>            Bean type
	 * @param datastore      The {@link BeanDatastore} to use (not null)
	 * @param beanClass      The bean class (not null)
	 * @param filter         The filter that selects the instances to update (not null)
	 * @param propertyValues A non-null, non-empty map of {@code propertyName -> value} assignments
	 * @param options        Optional write options
	 * @return The {@link BeanOperationResult} describing the outcome
	 * @throws IllegalArgumentException if any required argument is null or the map is empty
	 * @throws DataAccessException      if an error occurs during the operation
	 */
	public static <T> BeanOperationResult<?> bulkUpdateProperties(BeanDatastore datastore, Class<T> beanClass,
			QueryFilter filter, java.util.Map<String, Object> propertyValues, WriteOption... options) {
		ObjectUtils.argumentNotNull(propertyValues, "Property-values map must be not null");
		if (propertyValues.isEmpty()) {
			throw new IllegalArgumentException("Property-values map must not be empty");
		}
		BeanBulkUpdate<T> op = bulkUpdate(datastore, beanClass, filter, options);
		propertyValues.forEach(op::set);
		return op.execute();
	}

	// -------------------------------------------------------------------------
	// Transaction helpers
	// -------------------------------------------------------------------------

	/**
	 * Execute the given {@link BeanTransactionalOperation} within a transaction using the
	 * underlying {@link com.holonplatform.core.datastore.transaction.Transactional} datastore.
	 * <p>
	 * The transaction is committed on success and rolled back if {@code operation}
	 * throws any exception.
	 * </p>
	 *
	 * @param <R>       Result type
	 * @param datastore The {@link BeanDatastore} to use (not null)
	 * @param operation The transactional operation to execute (not null)
	 * @return The value returned by {@code operation}
	 * @throws IllegalArgumentException if any argument is null
	 * @throws IllegalStateException    if the underlying datastore does not support transactions
	 * @throws DataAccessException      if an error occurs during operation execution
	 */
	public static <R> R withTransaction(BeanDatastore datastore,
			BeanTransactionalOperation<R> operation) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		ObjectUtils.argumentNotNull(operation, "BeanTransactionalOperation must be not null");
		return datastore.requireTransactional().withTransaction(tx -> {
			try {
				return operation.execute(datastore, tx);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new DataAccessException("Transaction operation failed", e);
			}
		});
	}

	/**
	 * Check whether the underlying {@link Datastore} supports transactions.
	 *
	 * @param datastore The {@link BeanDatastore} to check (not null)
	 * @return {@code true} if transactions are supported
	 * @throws IllegalArgumentException if {@code datastore} is null
	 */
	public static boolean isTransactional(BeanDatastore datastore) {
		ObjectUtils.argumentNotNull(datastore, "BeanDatastore must be not null");
		return datastore.isTransactional().isPresent();
	}

	// -------------------------------------------------------------------------
	// Factory helpers
	// -------------------------------------------------------------------------

	/**
	 * Convenience factory: build a {@link BeanDatastore} from a concrete {@link Datastore}.
	 *
	 * @param datastore The concrete {@link Datastore} (not null)
	 * @return A new {@link BeanDatastore} adapter
	 * @throws IllegalArgumentException if {@code datastore} is null
	 */
	public static BeanDatastore of(Datastore datastore) {
		ObjectUtils.argumentNotNull(datastore, "Datastore must be not null");
		return BeanDatastore.of(datastore);
	}

	// -------------------------------------------------------------------------
	// Supporting types
	// -------------------------------------------------------------------------

	/**
	 * Functional interface representing a bean-datastore operation to be executed within a
	 * {@link com.holonplatform.core.datastore.transaction.Transactional} context.
	 *
	 * <p>Example usage:</p>
	 * <pre>{@code
	 * String result = BeanDatastoreUtils.withTransaction(bds, (datastore, tx) -> {
	 *     BeanDatastoreUtils.insert(datastore, myBean);
	 *     tx.commit();
	 *     return "ok";
	 * });
	 * }</pre>
	 *
	 * @param <R> The result type
	 */
	@FunctionalInterface
	public interface BeanTransactionalOperation<R> {

		/**
		 * Execute the operation within the active transaction.
		 *
		 * @param datastore   The {@link BeanDatastore} to use for persistence operations
		 * @param transaction The active {@link com.holonplatform.core.datastore.transaction.Transaction}
		 *                    can be used to call {@code commit()} or {@code rollback()} explicitly;
		 *                    if neither is called the framework performs an automatic commit on success
		 *                    or rollback on failure.
		 * @return The operation result
		 * @throws Exception if an error occurs (causes automatic rollback)
		 */
		R execute(BeanDatastore datastore,
				com.holonplatform.core.datastore.transaction.Transaction transaction)
				throws Exception;
	}
}









