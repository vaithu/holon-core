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
 * Optional Micrometer {@link io.micrometer.observation.Observation} instrumentation for Spring Security
 * authentication and authorization events performed through the Holon platform bridges provided by the
 * {@link com.holonplatform.spring.security.config} package ({@code AuthenticationProviderAdapter} and
 * {@link com.holonplatform.spring.security.config.HolonAuthorizationManager}).
 * <p>
 * Unlike the {@code holon-core} Datastore observability SPI (which only defines a naming/tagging contract, since
 * {@code holon-core} never actually executes operations itself), this package provides <b>working, ready to use
 * decorators</b> - {@link com.holonplatform.spring.security.observability.ObservedAuthenticationProvider} and
 * {@link com.holonplatform.spring.security.observability.ObservedAuthorizationManager} - since authentication and
 * authorization decisions <em>are</em> actually performed by this module.
 * </p>
 * <p>
 * This is an entirely opt-in feature: nothing is instrumented unless one of these decorators is explicitly used to
 * wrap an existing {@code AuthenticationProvider} or {@code AuthorizationManager}, and requires the (optional)
 * {@code micrometer-observation} dependency to be present on the classpath.
 * </p>
 * 
 * @since 10.0.0
 */
package com.holonplatform.spring.security.observability;
