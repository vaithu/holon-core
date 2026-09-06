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

import java.util.Optional;

import org.springframework.security.core.Authentication;

import com.holonplatform.core.internal.utils.ObjectUtils;

import io.micrometer.observation.Observation;

/**
 * A Micrometer {@link Observation.Context} implementation which carries the information about a Spring Security
 * authentication attempt being observed.
 * <p>
 * The outcome and, in case of failure, the exception type are only known once the authentication attempt has
 * completed, so they are set on this context by {@link ObservedAuthenticationProvider} right before the observation
 * is stopped.
 * </p>
 * 
 * @since 10.0.0
 * 
 * @see AuthenticationObservationConvention
 * @see ObservedAuthenticationProvider
 */
public class AuthenticationObservationContext extends Observation.Context {

	/**
	 * Authentication observation outcome.
	 */
	public enum Outcome {

		/**
		 * Authentication succeeded.
		 */
		SUCCESS,

		/**
		 * Authentication failed.
		 */
		FAILURE;

	}

	private final Class<? extends Authentication> authenticationType;
	private final String principalName;

	private Outcome outcome;
	private String failureType;

	/**
	 * Constructor.
	 * @param authenticationType The Spring Security {@link Authentication} implementation type being authenticated
	 *        (not null)
	 * @param principalName The authentication principal name, if available
	 */
	public AuthenticationObservationContext(Class<? extends Authentication> authenticationType,
			String principalName) {
		super();
		ObjectUtils.argumentNotNull(authenticationType, "The Authentication type must be not null");
		this.authenticationType = authenticationType;
		this.principalName = principalName;
	}

	/**
	 * Get the Spring Security {@link Authentication} implementation type being authenticated.
	 * @return the Authentication type
	 */
	public Class<? extends Authentication> getAuthenticationType() {
		return authenticationType;
	}

	/**
	 * Get the authentication principal name, if available.
	 * @return Optional principal name
	 */
	public Optional<String> getPrincipalName() {
		return Optional.ofNullable(principalName);
	}

	/**
	 * Get the authentication outcome, if the observation has completed.
	 * @return Optional authentication outcome
	 */
	public Optional<Outcome> getOutcome() {
		return Optional.ofNullable(outcome);
	}

	/**
	 * Set the authentication outcome.
	 * @param outcome the outcome to set
	 */
	public void setOutcome(Outcome outcome) {
		this.outcome = outcome;
	}

	/**
	 * Get the authentication failure exception simple type name, if the authentication failed.
	 * @return Optional failure exception type name
	 */
	public Optional<String> getFailureType() {
		return Optional.ofNullable(failureType);
	}

	/**
	 * Set the authentication failure exception simple type name.
	 * @param failureType the failure type name to set
	 */
	public void setFailureType(String failureType) {
		this.failureType = failureType;
	}

}
