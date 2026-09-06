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
 * Default {@link AuthenticationObservationConvention} implementation.
 * <p>
 * Uses {@code holon.security.authentication} as the (low-cardinality) observation name, and provides the following
 * low-cardinality key values:
 * </p>
 * <ul>
 * <li>{@code holon.security.authentication.type} - the simple name of the Spring Security {@link
 * org.springframework.security.core.Authentication} implementation type being authenticated</li>
 * <li>{@code holon.security.authentication.outcome} - {@code success} or {@code failure}, or {@code unknown} if the
 * observation has not completed yet</li>
 * <li>{@code holon.security.authentication.failure} - the authentication failure exception simple type name, if the
 * authentication failed, or {@code none} otherwise</li>
 * </ul>
 * 
 * @since 10.0.0
 */
public class DefaultAuthenticationObservationConvention implements AuthenticationObservationConvention {

	/**
	 * Default observation name.
	 */
	public static final String OBSERVATION_NAME = "holon.security.authentication";

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
	public String getContextualName(AuthenticationObservationContext context) {
		return "authenticate " + context.getAuthenticationType().getSimpleName();
	}

	/*
	 * (non-Javadoc)
	 * @see io.micrometer.observation.ObservationConvention#getLowCardinalityKeyValues(io.micrometer.observation.
	 * Observation.Context)
	 */
	@Override
	public KeyValues getLowCardinalityKeyValues(AuthenticationObservationContext context) {
		return KeyValues.of(
				KeyValue.of("holon.security.authentication.type", context.getAuthenticationType().getSimpleName()),
				KeyValue.of("holon.security.authentication.outcome",
						context.getOutcome().map(o -> o.name().toLowerCase()).orElse("unknown")),
				KeyValue.of("holon.security.authentication.failure", context.getFailureType().orElse("none")));
	}

}
