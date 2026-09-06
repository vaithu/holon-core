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

import io.micrometer.observation.Observation;

/**
 * A Micrometer {@link Observation.Context} implementation which carries the information about a Spring Security
 * authorization decision being observed.
 * <p>
 * The granted/denied outcome is only known once the authorization decision has been made, so it is set on this
 * context by {@link ObservedAuthorizationManager} right before the observation is stopped.
 * </p>
 * 
 * @since 10.0.0
 * 
 * @see AuthorizationObservationConvention
 * @see ObservedAuthorizationManager
 */
public class AuthorizationObservationContext extends Observation.Context {

	private final Class<?> secureObjectType;

	private Boolean granted;

	/**
	 * Constructor.
	 * @param secureObjectType The type of the object being secured (e.g. {@code RequestAuthorizationContext} or
	 *        {@code MethodInvocation}), if available
	 */
	public AuthorizationObservationContext(Class<?> secureObjectType) {
		super();
		this.secureObjectType = secureObjectType;
	}

	/**
	 * Get the type of the object being secured, if available.
	 * @return Optional secure object type
	 */
	public Optional<Class<?>> getSecureObjectType() {
		return Optional.ofNullable(secureObjectType);
	}

	/**
	 * Get the authorization outcome, if the observation has completed.
	 * @return Optional authorization outcome (<code>true</code> if granted, <code>false</code> if denied)
	 */
	public Optional<Boolean> getGranted() {
		return Optional.ofNullable(granted);
	}

	/**
	 * Set the authorization outcome.
	 * @param granted <code>true</code> if granted, <code>false</code> if denied
	 */
	public void setGranted(boolean granted) {
		this.granted = granted;
	}

}
