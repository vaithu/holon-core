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
package com.holonplatform.core.query;

import com.holonplatform.core.ExpressionResolver;
import com.holonplatform.core.ExpressionResolver.ExpressionResolverBuilder;
import com.holonplatform.core.config.ConfigProperty;
import com.holonplatform.core.datastore.DataTarget;
import com.holonplatform.core.datastore.DataTarget.DataTargetSupport;
import com.holonplatform.core.query.QueryAggregation.QueryAggregationSupport;
import com.holonplatform.core.query.QueryFilter.QueryFilterSupport;
import com.holonplatform.core.query.QuerySort.QuerySortSupport;

/**
 * Builder to configure a {@link Query}, managing query {@link DataTarget} and query clauses such as {@link QueryFilter}
 * and {@link QuerySort} and handling {@link ExpressionResolver}s registration.
 * 
 * @since 5.0.0
 * 
 * @param <Q> Concrete QueryBuilder
 *
 * @see Query
 */
public interface QueryBuilder<Q extends QueryBuilder<Q>> extends QueryFilterSupport<Q>, QuerySortSupport<Q>,
		DataTargetSupport<Q>, QueryAggregationSupport<Q>, ExpressionResolverBuilder<Q> {

	/**
	 * Get the current {@link QueryConfiguration}.
	 * @return the current {@link QueryConfiguration}
	 */
	QueryConfiguration getQueryConfiguration();

	/**
	 * Limit the fetched result set
	 * @param limit Results limit. Must be &gt; 0. A value &lt;= 0 indicates no limit.
	 * @return this
	 */
	Q limit(int limit);

	/**
	 * Starts the query results at a particular zero-based offset.
	 * @param offset Results offset 0-based index. Must be &gt;= 0.
	 * @return this
	 */
	Q offset(int offset);

	/**
	 * Convenience method to set {@link #limit(int)} and {@link #offset(int)} of query results both in one call
	 * @param limit limit Results limit. Must be &gt; 0. A value &lt;= 0 indicates no limit.
	 * @param offset offset Results offset 0-based index. Must be &gt;= 0.
	 * @return this
	 */
	Q restrict(int limit, int offset);

	/**
	 * Page the fetched result set
	 * @param page Results page. Must be &gt; 0. A value &lt;= 0 indicates no page.
	 * @return this
	 */
//	Q page(int page);

	/**
	 * Starts the query results at a particular zero-based pageSize.
	 * @param pageSize Results pageSize 0-based index. Must be &gt;= 0.
	 * @return this
	 */
//	Q pageSize(int pageSize);

	/**
	 * Convenience method to set {@link #page(int)} and {@link #pageSize(int)} of query results both in one call
	 * @param page page Results page. Must be &gt; 0. A value &lt;= 0 indicates no page.
	 * @param pageSize pageSize Results pageSize 0-based index. Must be &gt;= 0.
	 * @return this
	 */
//	Q pageable(int page, int pageSize);


	/**
	 * Add a generic parameter to query
	 * @param name Parameter name
	 * @param value Parameter value
	 * @return this
	 */
	Q parameter(String name, Object value);

	/**
	 * Add a parameter to query using a {@link ConfigProperty} and {@link ConfigProperty#getKey()} as parameter name.
	 * @param <T> Config property type
	 * @param property The configuration property (not null)
	 * @param value Parameter value
	 * @return this
	 */
	<T> Q parameter(ConfigProperty<T> property, T value);

	/**
	 * Configure the query to return <em>distinct</em> query projection result values.
	 * @return this
	 * @since 5.2.0
	 */
	Q distinct();

}
