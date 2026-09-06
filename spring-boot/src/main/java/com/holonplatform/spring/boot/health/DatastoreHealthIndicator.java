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
package com.holonplatform.spring.boot.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import com.holonplatform.core.datastore.DataTarget;
import com.holonplatform.core.datastore.Datastore;
import com.holonplatform.core.exceptions.DataAccessException;
import com.holonplatform.core.internal.utils.ObjectUtils;

/**
 * A Spring Boot {@link HealthIndicator} which checks a {@link Datastore} availability by executing a lightweight,
 * read-only {@code count()} query against a given {@link DataTarget}.
 * <p>
 * Since {@code holon-core} defines the {@link Datastore} API but never provides a concrete, executing
 * implementation itself (that is always supplied by a downstream, backend-specific module, e.g. a JDBC or JPA
 * Datastore), this health indicator works with <b>any</b> concrete {@link Datastore} implementation, exactly like
 * Spring Boot's own {@code DataSourceHealthIndicator} performs a {@code SELECT 1} against a JDBC
 * {@code DataSource}.
 * </p>
 * <p>
 * This class is not auto-configured, since neither a default {@link Datastore} bean nor a default
 * {@link DataTarget} to check can be assumed at the library level. Applications should declare their own bean,
 * for example:
 * </p>
 * 
 * <pre>
 * &#64;Bean
 * HealthIndicator datastoreHealthIndicator(Datastore datastore) {
 * 	return new DatastoreHealthIndicator(datastore, DataTarget.named("my_table"));
 * }
 * </pre>
 * 
 * @since 10.0.0
 */
public class DatastoreHealthIndicator implements HealthIndicator {

	private final Datastore datastore;
	private final DataTarget<?> target;

	/**
	 * Constructor.
	 * @param datastore The {@link Datastore} to check (not null)
	 * @param target The {@link DataTarget} to use to perform the check query (not null)
	 */
	public DatastoreHealthIndicator(Datastore datastore, DataTarget<?> target) {
		super();
		ObjectUtils.argumentNotNull(datastore, "Datastore must be not null");
		ObjectUtils.argumentNotNull(target, "DataTarget must be not null");
		this.datastore = datastore;
		this.target = target;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.boot.health.contributor.HealthIndicator#health()
	 */
	@Override
	public Health health() {
		try {
			final long count = datastore.query(target).count();
			return Health.up().withDetail("target", target.getName()).withDetail("count", count).build();
		} catch (DataAccessException e) {
			return Health.down(e).withDetail("target", target.getName()).build();
		} catch (RuntimeException e) {
			return Health.down(e).withDetail("target", target.getName()).build();
		}
	}

}
