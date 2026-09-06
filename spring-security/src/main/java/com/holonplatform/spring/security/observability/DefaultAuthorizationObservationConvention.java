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
package com.holonplatform.spring.security.observability;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

/**
 * Default {@link AuthorizationObservationConvention} implementation.
 * <p>
 * Uses {@code holon.security.authorization} as the (low-cardinality) observation name, and provides the following
 * low-cardinality key values:
 * </p>
 * <ul>
 * <li>{@code holon.security.authorization.object} - the simple name of the secured object type (e.g.
 * {@code RequestAuthorizationContext} or {@code MethodInvocation}), if available, or {@code unknown} otherwise</li>
 * <li>{@code holon.security.authorization.outcome} - {@code granted} or {@code denied}, or {@code unknown} if the
 * observation has not completed yet</li>
 * </ul>
 * 
 * @since 10.0.0
 */
public class DefaultAuthorizationObservationConvention implements AuthorizationObservationConvention {

	/**
	 * Default observation name.
	 */
	public static final String OBSERVATION_NAME = "holon.security.authorization";

	/*
	 * (non-Javadoc)
	 * @see io.micrometer.observation.ObservationConvention#getName()
	 */
	@Override
	public String getName() {
		return OBSERVATION_NAME;
	}

	/*
	 * (non-Javadoc)
	 * @see io.micrometer.observation.ObservationConvention#getContextualName(io.micrometer.observation.Observation.
	 * Context)
	 */
	@Override
	public String getContextualName(AuthorizationObservationContext context) {
		return "authorize " + context.getSecureObjectType().map(Class::getSimpleName).orElse("unknown");
	}

	/*
	 * (non-Javadoc)
	 * @see io.micrometer.observation.ObservationConvention#getLowCardinalityKeyValues(io.micrometer.observation.
	 * Observation.Context)
	 */
	@Override
	public KeyValues getLowCardinalityKeyValues(AuthorizationObservationContext context) {
		return KeyValues.of(
				KeyValue.of("holon.security.authorization.object",
						context.getSecureObjectType().map(Class::getSimpleName).orElse("unknown")),
				KeyValue.of("holon.security.authorization.outcome",
						context.getGranted().map(granted -> granted ? "granted" : "denied").orElse("unknown")));
	}

}
