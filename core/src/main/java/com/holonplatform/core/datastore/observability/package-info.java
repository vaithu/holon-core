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
/**
 * Optional Micrometer Observation naming/tagging SPI for {@link com.holonplatform.core.datastore.Datastore}
 * operations.
 * <p>
 * This package does not perform any observability instrumentation by itself: {@code holon-core} never starts or
 * stops a Micrometer {@code Observation}, since it never executes an operation against a real data source - that is
 * always delegated to a concrete {@link com.holonplatform.core.datastore.Datastore} implementation (e.g. a JDBC or
 * JPA based implementation).
 * </p>
 * <p>
 * Instead, this package defines a shared, optional contract -
 * {@link com.holonplatform.core.datastore.observability.DatastoreOperationObservationConvention} and
 * {@link com.holonplatform.core.datastore.observability.DatastoreOperationObservationContext} - that concrete
 * Datastore implementations can adopt to consistently name and tag the Observations they create around their actual
 * operation execution. Adoption is entirely optional and has no effect unless a Datastore implementation explicitly
 * uses these types together with a Micrometer {@code ObservationRegistry}.
 * </p>
 * <p>
 * The {@code io.micrometer:micrometer-observation} dependency is declared as an <em>optional</em> Maven dependency of
 * the {@code holon-core} artifact: it is only required on the classpath of a consumer that actually uses the types in
 * this package.
 * </p>
 * 
 * @since 10.0.0
 */
package com.holonplatform.core.datastore.observability;
