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
package com.holonplatform.core.datastore;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.holonplatform.core.datastore.Datastore.OperationResult;
import com.holonplatform.core.datastore.DatastoreOperations.WriteOption;
import com.holonplatform.core.datastore.beans.PageableQuery;
import com.holonplatform.core.datastore.bulk.BulkInsert;
import com.holonplatform.core.datastore.bulk.BulkUpdate;
import com.holonplatform.core.datastore.transaction.TransactionalOperation;
import com.holonplatform.core.datastore.transaction.TransactionalOperation.TransactionalInvocation;
import com.holonplatform.core.exceptions.DataAccessException;
import com.holonplatform.core.internal.utils.ObjectUtils;
import com.holonplatform.core.property.Property;
import com.holonplatform.core.property.PropertyBox;
import com.holonplatform.core.property.PropertySet;
import com.holonplatform.core.query.QueryFilter;
import com.holonplatform.core.query.QueryResults.QueryNonUniqueResultException;
import com.holonplatform.core.query.QuerySort;

/**
 * Typed, instance-based façade over {@link Datastore} that holds a {@link DataTarget} and a {@link PropertySet}
 * so neither ever needs to be repeated on a call.
 *
 * <p>This is the {@link PropertyBox}-oriented complement to {@code BeanDatastoreHelper}. Where that helper
 * works with plain Java beans, this one works directly with {@link PropertyBox} containers and
 * {@link PropertySet}-based projections.
 *
 * <h3>Construction</h3>
 * <pre>{@code
 * // Define properties
 * PathProperty<Long>   ID    = PathProperty.create("id",    Long.class);
 * StringProperty       NAME  = StringProperty.create("name");
 * PathProperty<Double> PRICE = PathProperty.create("price", Double.class);
 *
 * DataTarget<String> TARGET = DataTarget.named("product");
 * PropertySet<?>     PS     = PropertySet.of(ID, NAME, PRICE);
 *
 * PropertyBoxDatastoreHelper<?> products = PropertyBoxDatastoreHelper.of(datastore, TARGET, PS);
 * }</pre>
 *
 * <h3>Return-type convention</h3>
 * <ul>
 *   <li>{@code findAll(…)} returns a <em>lazy</em> {@link Stream} — the datastore only fetches rows as
 *       the stream is consumed. The caller <strong>must close</strong> the stream (use try-with-resources).</li>
 *   <li>{@code findPage(…)}, {@code findSlice(…)}, and {@code findTop(…)} return an eager {@link List}
 *       because they are <em>bounded</em>.</li>
 *   <li>{@code findOne(…)} and {@code findFirst(…)} return {@link Optional}.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // writes
 * products.insert(box);
 * products.update(box);
 * products.save(box);
 * products.delete(box);
 * PropertyBox fresh = products.refresh(box);
 *
 * // lazy streams — must be closed
 * try (Stream<PropertyBox> all = products.findAll()) {
 *     all.forEach(this::process);
 * }
 * try (Stream<PropertyBox> filtered = products.findAll(NAME.contains("widget"))) {
 *     filtered.map(b -> b.getValue(NAME)).forEach(System.out::println);
 * }
 *
 * // bounded / eager results
 * Optional<PropertyBox> one  = products.findOne(ID.eq(42L));
 * List<PropertyBox>     page = products.findPage(0, 20);
 * List<PropertyBox>     top5 = products.findTop(5, PRICE.desc());
 *
 * // count / exists
 * long    total = products.count();
 * boolean any   = products.exists(NAME.isNotNull());
 *
 * // bulk
 * products.bulkInsert(List.of(b1, b2, b3));
 * products.bulkUpdate(activeFilter).set(PRICE, 9.99).execute();
 * products.bulkDelete(obsoleteFilter);
 *
 * // transaction
 * products.withTransaction(tx -> { products.insert(box); tx.commit(); return null; });
 * }</pre>
 *
 * @param <P> Property type of the managed {@link PropertySet}
 * @since 10.0.0
 */
public final class PropertyBoxDatastoreHelper<P extends Property<?>> {

	private final Datastore      datastore;
	private final DataTarget<?>  target;
	private final PropertySet<P> propertySet;

	private PropertyBoxDatastoreHelper(Datastore datastore, DataTarget<?> target, PropertySet<P> propertySet) {
		ObjectUtils.argumentNotNull(datastore,   "Datastore must be not null");
		ObjectUtils.argumentNotNull(target,      "DataTarget must be not null");
		ObjectUtils.argumentNotNull(propertySet, "PropertySet must be not null");
		this.datastore   = datastore;
		this.target      = target;
		this.propertySet = propertySet;
	}

	// -------------------------------------------------------------------------
	// Factory
	// -------------------------------------------------------------------------

	/**
	 * Create a helper for the given datastore, target, and property set.
	 *
	 * @param <P>         Property type
	 * @param datastore   The {@link Datastore} to use (not null)
	 * @param target      The {@link DataTarget} (table/collection) (not null)
	 * @param propertySet The {@link PropertySet} used as the default projection (not null)
	 * @return A new typed {@link PropertyBoxDatastoreHelper}
	 */
	public static <P extends Property<?>> PropertyBoxDatastoreHelper<P> of(
			Datastore datastore, DataTarget<?> target, PropertySet<P> propertySet) {
		return new PropertyBoxDatastoreHelper<>(datastore, target, propertySet);
	}

	/**
	 * Create a helper for the given datastore, target, and a varargs set of properties.
	 *
	 * @param <P>        Property type
	 * @param datastore  The {@link Datastore} to use (not null)
	 * @param target     The {@link DataTarget} (table/collection) (not null)
	 * @param properties The properties that form the default projection (not null)
	 * @return A new typed {@link PropertyBoxDatastoreHelper}
	 */
	@SafeVarargs
	public static <P extends Property<?>> PropertyBoxDatastoreHelper<P> of(
			Datastore datastore, DataTarget<?> target, P... properties) {
		return new PropertyBoxDatastoreHelper<>(datastore, target, PropertySet.of(properties));
	}

	/** Return the underlying {@link Datastore}. */
	public Datastore getDatastore()      { return datastore; }

	/** Return the {@link DataTarget} managed by this helper. */
	public DataTarget<?> getTarget()     { return target; }

	/** Return the default {@link PropertySet} used for projections. */
	public PropertySet<P> getPropertySet() { return propertySet; }

	// -------------------------------------------------------------------------
	// Write operations
	// -------------------------------------------------------------------------

	/**
	 * Insert a {@link PropertyBox}.
	 *
	 * @param box     The box to insert (not null)
	 * @param options Optional write options
	 * @return The operation result
	 * @throws DataAccessException if an error occurs
	 */
	public OperationResult insert(PropertyBox box, WriteOption... options) {
		return datastore.insert(target, box, options);
	}

	/**
	 * Update a {@link PropertyBox}.
	 *
	 * @param box     The box to update (not null)
	 * @param options Optional write options
	 * @return The operation result
	 * @throws DataAccessException if an error occurs
	 */
	public OperationResult update(PropertyBox box, WriteOption... options) {
		return datastore.update(target, box, options);
	}

	/**
	 * Save (upsert) a {@link PropertyBox} — inserts if absent, updates otherwise.
	 *
	 * @param box     The box to save (not null)
	 * @param options Optional write options
	 * @return The operation result
	 * @throws DataAccessException if an error occurs
	 */
	public OperationResult save(PropertyBox box, WriteOption... options) {
		return datastore.save(target, box, options);
	}

	/**
	 * Delete a {@link PropertyBox}.
	 *
	 * @param box     The box to delete (not null)
	 * @param options Optional write options
	 * @return The operation result
	 * @throws DataAccessException if an error occurs
	 */
	public OperationResult delete(PropertyBox box, WriteOption... options) {
		return datastore.delete(target, box, options);
	}

	/**
	 * Refresh a {@link PropertyBox}, reloading all its properties from the datastore.
	 *
	 * @param box The box to refresh (not null)
	 * @return The refreshed {@link PropertyBox}
	 * @throws DataAccessException if an error occurs
	 */
	public PropertyBox refresh(PropertyBox box) {
		return datastore.refresh(target, box);
	}

	// -------------------------------------------------------------------------
	// Query / Read — full projection (uses the helper's PropertySet)
	// -------------------------------------------------------------------------

	/**
	 * Return a <em>lazy</em> {@link Stream} over <strong>all</strong> rows, projecting onto this
	 * helper's {@link PropertySet}.
	 * <p>
	 * Rows are fetched on demand as the stream is consumed. The caller <strong>must close</strong>
	 * the stream, e.g. via try-with-resources.
	 * </p>
	 * <pre>{@code
	 * try (Stream<PropertyBox> s = products.findAll()) {
	 *     s.forEach(this::process);
	 * }
	 * }</pre>
	 *
	 * @return Lazy stream of all rows; never {@code null}
	 * @throws DataAccessException if an error occurs opening the stream
	 */
	public Stream<PropertyBox> findAll() {
		return datastore.query(target).stream(propertySet);
	}

	/**
	 * Return a <em>lazy</em> {@link Stream} over all rows matching the given filter.
	 * <p>See {@link #findAll()} for resource-management notes.</p>
	 *
	 * @param filter The query filter (not null)
	 * @return Lazy stream of matching rows; never {@code null}
	 * @throws DataAccessException if an error occurs opening the stream
	 */
	public Stream<PropertyBox> findAll(QueryFilter filter) {
		return datastore.query(target).filter(filter).stream(propertySet);
	}

	/**
	 * Return a <em>lazy</em> {@link Stream} over all rows matching the given filter,
	 * ordered by the given sort. Both parameters may be {@code null}.
	 * <p>See {@link #findAll()} for resource-management notes.</p>
	 *
	 * @param filter Optional query filter
	 * @param sort   Optional query sort
	 * @return Lazy stream of matching rows; never {@code null}
	 * @throws DataAccessException if an error occurs opening the stream
	 */
	public Stream<PropertyBox> findAll(QueryFilter filter, QuerySort sort) {
		var q = datastore.query(target);
		if (filter != null) q = q.filter(filter);
		if (sort   != null) q = q.sort(sort);
		return q.stream(propertySet);
	}

	/**
	 * Find the single row matching the given filter.
	 * Returns empty if no row matches; throws if more than one matches.
	 *
	 * @param filter The query filter (not null)
	 * @return Matching row or empty
	 * @throws QueryNonUniqueResultException if more than one result is found
	 * @throws DataAccessException           if an error occurs
	 */
	public Optional<PropertyBox> findOne(QueryFilter filter) {
		return datastore.query(target).filter(filter).findOne(propertySet);
	}

	/**
	 * Return the first row using datastore-default ordering.
	 * Translates to {@code LIMIT 1 OFFSET 0}. Without an explicit sort this is non-deterministic;
	 * prefer {@link #findFirst(QueryFilter, QuerySort)} for a stable outcome.
	 *
	 * @return First row, or empty if the target is empty
	 * @throws DataAccessException if an error occurs
	 */
	public Optional<PropertyBox> findFirst() {
		return datastore.query(target).restrict(1, 0).stream(propertySet).findFirst();
	}

	/**
	 * Return the first row matching the given filter.
	 * Translates to {@code WHERE … LIMIT 1 OFFSET 0}.
	 *
	 * @param filter The query filter (not null)
	 * @return First matching row, or empty
	 * @throws DataAccessException if an error occurs
	 */
	public Optional<PropertyBox> findFirst(QueryFilter filter) {
		return datastore.query(target).filter(filter).restrict(1, 0).stream(propertySet).findFirst();
	}

	/**
	 * Return the first row matching the given filter in the given sort order.
	 * Both parameters may be {@code null}.
	 * Translates to {@code [WHERE …] [ORDER BY …] LIMIT 1 OFFSET 0}.
	 *
	 * @param filter Optional filter
	 * @param sort   Optional sort
	 * @return First matching row, or empty
	 * @throws DataAccessException if an error occurs
	 */
	public Optional<PropertyBox> findFirst(QueryFilter filter, QuerySort sort) {
		var q = datastore.query(target);
		if (filter != null) q = q.filter(filter);
		if (sort   != null) q = q.sort(sort);
		return q.restrict(1, 0).stream(propertySet).findFirst();
	}

	/**
	 * Return the first {@code n} rows using datastore-default ordering.
	 * Translates to {@code LIMIT n OFFSET 0}.
	 *
	 * @param n Max results (&gt; 0)
	 * @return Up to {@code n} rows; may be empty
	 * @throws IllegalArgumentException if {@code n <= 0}
	 * @throws DataAccessException      if an error occurs
	 */
	public List<PropertyBox> findTop(int n) {
		if (n <= 0) throw new IllegalArgumentException("n must be > 0, was: " + n);
		return datastore.query(target).restrict(n, 0).stream(propertySet).collect(Collectors.toList());
	}

	/**
	 * Return the first {@code n} rows in the given sort order.
	 * Translates to {@code ORDER BY … LIMIT n OFFSET 0}.
	 *
	 * @param n    Max results (&gt; 0)
	 * @param sort The sort order (not null)
	 * @return Up to {@code n} ordered rows; may be empty
	 * @throws IllegalArgumentException if {@code n <= 0}
	 * @throws DataAccessException      if an error occurs
	 */
	public List<PropertyBox> findTop(int n, QuerySort sort) {
		if (n <= 0) throw new IllegalArgumentException("n must be > 0, was: " + n);
		return datastore.query(target).sort(sort).restrict(n, 0).stream(propertySet).collect(Collectors.toList());
	}

	/**
	 * Return the first {@code n} rows matching the given filter.
	 * Translates to {@code WHERE … LIMIT n OFFSET 0}.
	 *
	 * @param n      Max results (&gt; 0)
	 * @param filter The query filter (not null)
	 * @return Up to {@code n} matching rows; may be empty
	 * @throws IllegalArgumentException if {@code n <= 0}
	 * @throws DataAccessException      if an error occurs
	 */
	public List<PropertyBox> findTop(int n, QueryFilter filter) {
		if (n <= 0) throw new IllegalArgumentException("n must be > 0, was: " + n);
		return datastore.query(target).filter(filter).restrict(n, 0).stream(propertySet).collect(Collectors.toList());
	}

	/**
	 * Return the first {@code n} rows matching the given filter in the given sort order.
	 * Both {@code filter} and {@code sort} may be {@code null}.
	 * Translates to {@code [WHERE …] [ORDER BY …] LIMIT n OFFSET 0}.
	 *
	 * @param n      Max results (&gt; 0)
	 * @param filter Optional filter
	 * @param sort   Optional sort
	 * @return Up to {@code n} rows; may be empty
	 * @throws IllegalArgumentException if {@code n <= 0}
	 * @throws DataAccessException      if an error occurs
	 */
	public List<PropertyBox> findTop(int n, QueryFilter filter, QuerySort sort) {
		if (n <= 0) throw new IllegalArgumentException("n must be > 0, was: " + n);
		var q = datastore.query(target);
		if (filter != null) q = q.filter(filter);
		if (sort   != null) q = q.sort(sort);
		return q.restrict(n, 0).stream(propertySet).collect(Collectors.toList());
	}

	// -------------------------------------------------------------------------
	// Pagination (bounded → eager List is safe)
	// -------------------------------------------------------------------------

	/**
	 * Retrieve a page using zero-based page index.
	 * Translates to {@code LIMIT pageSize OFFSET page*pageSize}.
	 *
	 * @param page     Zero-based page index (&gt;= 0)
	 * @param pageSize Items per page (&gt; 0)
	 * @return The requested page; may be empty
	 * @throws DataAccessException if an error occurs
	 */
	public List<PropertyBox> findPage(int page, int pageSize) {
		return datastore.query(target).restrict(pageSize, page * pageSize)
				.stream(propertySet).collect(Collectors.toList());
	}

	/**
	 * Retrieve a filtered and sorted page using zero-based page index.
	 * Both {@code filter} and {@code sort} may be {@code null}.
	 *
	 * @param filter   Optional query filter
	 * @param sort     Optional query sort
	 * @param page     Zero-based page index (&gt;= 0)
	 * @param pageSize Items per page (&gt; 0)
	 * @return The requested page; may be empty
	 * @throws DataAccessException if an error occurs
	 */
	public List<PropertyBox> findPage(QueryFilter filter, QuerySort sort, int page, int pageSize) {
		var q = datastore.query(target);
		if (filter != null) q = q.filter(filter);
		if (sort   != null) q = q.sort(sort);
		return q.restrict(pageSize, page * pageSize).stream(propertySet).collect(Collectors.toList());
	}

	/**
	 * Retrieve a slice using explicit {@code LIMIT} and {@code OFFSET}.
	 *
	 * @param limit  Max results (&gt; 0)
	 * @param offset Zero-based start index (&gt;= 0)
	 * @return The requested slice; may be empty
	 * @throws DataAccessException if an error occurs
	 */
	public List<PropertyBox> findSlice(int limit, int offset) {
		return datastore.query(target).restrict(limit, offset)
				.stream(propertySet).collect(Collectors.toList());
	}

	/**
	 * Retrieve a filtered and sorted slice using explicit {@code LIMIT} and {@code OFFSET}.
	 * Both {@code filter} and {@code sort} may be {@code null}.
	 *
	 * @param filter Optional query filter
	 * @param sort   Optional query sort
	 * @param limit  Max results (&gt; 0)
	 * @param offset Zero-based start index (&gt;= 0)
	 * @return The requested slice; may be empty
	 * @throws DataAccessException if an error occurs
	 */
	public List<PropertyBox> findSlice(QueryFilter filter, QuerySort sort, int limit, int offset) {
		var q = datastore.query(target);
		if (filter != null) q = q.filter(filter);
		if (sort   != null) q = q.sort(sort);
		return q.restrict(limit, offset).stream(propertySet).collect(Collectors.toList());
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
	 * @return The requested slice; may be empty
	 * @throws DataAccessException if an error occurs
	 */
	public List<PropertyBox> findSlice(PageableQuery query) {
		ObjectUtils.argumentNotNull(query, "PageableQuery must be not null");
		return findSlice(query.getLimit(), query.getOffset());
	}

	/**
	 * Retrieve a filtered slice using the limit and offset from the given {@link PageableQuery}.
	 *
	 * @param query  The pageable query providing limit and offset (not null)
	 * @param filter The query filter (not null)
	 * @return The requested slice; may be empty
	 * @throws DataAccessException if an error occurs
	 */
	public List<PropertyBox> findSlice(PageableQuery query, QueryFilter filter) {
		ObjectUtils.argumentNotNull(query, "PageableQuery must be not null");
		return findSlice(filter, null, query.getLimit(), query.getOffset());
	}

	/**
	 * Retrieve a sorted slice using the limit and offset from the given {@link PageableQuery}.
	 *
	 * @param query The pageable query providing limit and offset (not null)
	 * @param sort  The query sort (not null)
	 * @return The requested slice; may be empty
	 * @throws DataAccessException if an error occurs
	 */
	public List<PropertyBox> findSlice(PageableQuery query, QuerySort sort) {
		ObjectUtils.argumentNotNull(query, "PageableQuery must be not null");
		return findSlice(null, sort, query.getLimit(), query.getOffset());
	}

	/**
	 * Retrieve a filtered and sorted slice using the limit and offset from the given {@link PageableQuery}.
	 * Both {@code filter} and {@code sort} may be {@code null}.
	 *
	 * @param query  The pageable query providing limit and offset (not null)
	 * @param filter Optional query filter
	 * @param sort   Optional query sort
	 * @return The requested slice; may be empty
	 * @throws DataAccessException if an error occurs
	 */
	public List<PropertyBox> findSlice(PageableQuery query, QueryFilter filter, QuerySort sort) {
		ObjectUtils.argumentNotNull(query, "PageableQuery must be not null");
		return findSlice(filter, sort, query.getLimit(), query.getOffset());
	}

	// -------------------------------------------------------------------------
	// Count / exists
	// -------------------------------------------------------------------------

	/**
	 * Count all rows in the target.
	 *
	 * @return Total count
	 * @throws DataAccessException if an error occurs
	 */
	public long count() {
		return datastore.query(target).count();
	}

	/**
	 * Count rows matching the given filter.
	 *
	 * @param filter The query filter (not null)
	 * @return Matching count
	 * @throws DataAccessException if an error occurs
	 */
	public long count(QueryFilter filter) {
		return datastore.query(target).filter(filter).count();
	}

	/**
	 * Return {@code true} if at least one row matches the given filter.
	 *
	 * @param filter The query filter (not null)
	 * @return {@code true} if matching rows exist
	 * @throws DataAccessException if an error occurs
	 */
	public boolean exists(QueryFilter filter) {
		return count(filter) > 0;
	}

	// -------------------------------------------------------------------------
	// Bulk operations
	// -------------------------------------------------------------------------

	/**
	 * Bulk-insert a collection of {@link PropertyBox} instances in a single SQL statement.
	 * <p>
	 * The helper's {@link PropertySet} is used as the column definition.
	 * </p>
	 *
	 * @param boxes   Non-null, non-empty collection of boxes to insert
	 * @param options Optional write options
	 * @return The operation result
	 * @throws DataAccessException if an error occurs
	 */
	public OperationResult bulkInsert(Collection<? extends PropertyBox> boxes, WriteOption... options) {
		BulkInsert op = datastore.bulkInsert(target, propertySet, options);
		boxes.forEach(op::add);
		return op.execute();
	}

	/**
	 * Return a {@link BulkUpdate} builder pre-set to this helper's {@link DataTarget}.
	 * Chain {@code .set(property, value)} calls then call {@code .execute()}.
	 *
	 * <pre>{@code
	 * products.bulkUpdate()
	 *         .set(PRICE, 0.0)
	 *         .filter(PRICE.lt(0.0))
	 *         .execute();
	 * }</pre>
	 *
	 * @param options Optional write options
	 * @return A configured bulk-update builder (filter must be set before executing)
	 */
	public BulkUpdate bulkUpdate(WriteOption... options) {
		return datastore.bulkUpdate(target, options);
	}

	/**
	 * Return a {@link BulkUpdate} builder pre-filtered by the given filter.
	 * Chain {@code .set()} calls then call {@code .execute()}.
	 *
	 * <pre>{@code
	 * products.bulkUpdate(activeFilter)
	 *         .set(PRICE, 9.99)
	 *         .execute();
	 * }</pre>
	 *
	 * @param filter  The filter (not null)
	 * @param options Optional write options
	 * @return A configured bulk-update builder
	 */
	public BulkUpdate bulkUpdate(QueryFilter filter, WriteOption... options) {
		return datastore.bulkUpdate(target, options).filter(filter);
	}

	/**
	 * Bulk-delete all rows matching the given filter in a single SQL statement.
	 *
	 * @param filter  The filter (not null)
	 * @param options Optional write options
	 * @return The operation result
	 * @throws DataAccessException if an error occurs
	 */
	public OperationResult bulkDelete(QueryFilter filter, WriteOption... options) {
		return datastore.bulkDelete(target, options).filter(filter).execute();
	}

	// -------------------------------------------------------------------------
	// Transactions
	// -------------------------------------------------------------------------

	/**
	 * Execute the given operation within a transaction, returning a result.
	 * Commits on success; rolls back on any exception.
	 *
	 * @param <R>       Result type
	 * @param operation The transactional operation (not null); receives the active {@link com.holonplatform.core.datastore.transaction.Transaction}
	 * @return The operation result
	 * @throws IllegalStateException if the underlying datastore does not support transactions
	 * @throws DataAccessException   if an error occurs
	 */
	public <R> R withTransaction(TransactionalOperation<R> operation) {
		return datastore.requireTransactional().withTransaction(operation);
	}

	/**
	 * Execute the given operation within a transaction without returning a result.
	 * Commits on success; rolls back on any exception.
	 *
	 * @param operation The transactional operation (not null)
	 * @throws IllegalStateException if the underlying datastore does not support transactions
	 * @throws DataAccessException   if an error occurs
	 */
	public void withTransaction(TransactionalInvocation operation) {
		datastore.requireTransactional().withTransaction(operation);
	}

	/**
	 * Return {@code true} if the underlying datastore supports transactions.
	 *
	 * @return {@code true} if transactional
	 */
	public boolean isTransactional() {
		return datastore.isTransactional().isPresent();
	}
}


