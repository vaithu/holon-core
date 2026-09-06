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

import java.util.function.Supplier;

import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;

import com.holonplatform.core.internal.utils.ObjectUtils;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

/**
 * A Spring Security {@link AuthorizationManager} decorator which instruments the wrapped manager's
 * {@link #authorize(Supplier, Object)} calls using Micrometer {@link Observation}s, recording the authorization
 * outcome (granted/denied) as an observation key value (see {@link DefaultAuthorizationObservationConvention}).
 * <p>
 * This decorator works with <b>any</b> {@link AuthorizationManager}, including (but not limited to) the Holon
 * platform {@link com.holonplatform.spring.security.config.HolonAuthorizationManager} bridge, and with any secured
 * object type {@code T} (e.g. {@code RequestAuthorizationContext} for HTTP security, {@code MethodInvocation} for
 * method security). It is entirely opt-in: wrap an existing manager with this class only where authorization
 * observability is desired.
 * </p>
 * <p>
 * Typical usage:
 * </p>
 * 
 * <pre>
 * AuthorizationManager&lt;RequestAuthorizationContext&gt; manager = HolonAuthorizationManager.hasPermission(realm, "view");
 * AuthorizationManager&lt;RequestAuthorizationContext&gt; observed = new ObservedAuthorizationManager&lt;&gt;(manager,
 * 		observationRegistry);
 * </pre>
 * 
 * @param <T> The type of object being secured
 * 
 * @since 10.0.0
 */
public class ObservedAuthorizationManager<T> implements AuthorizationManager<T> {

	private final AuthorizationManager<T> delegate;
	private final ObservationRegistry observationRegistry;
	private final AuthorizationObservationConvention convention;

	/**
	 * Constructor using the {@link DefaultAuthorizationObservationConvention}.
	 * @param delegate The {@link AuthorizationManager} to decorate (not null)
	 * @param observationRegistry The Micrometer {@link ObservationRegistry} to use (not null)
	 */
	public ObservedAuthorizationManager(AuthorizationManager<T> delegate, ObservationRegistry observationRegistry) {
		this(delegate, observationRegistry, new DefaultAuthorizationObservationConvention());
	}

	/**
	 * Constructor.
	 * @param delegate The {@link AuthorizationManager} to decorate (not null)
	 * @param observationRegistry The Micrometer {@link ObservationRegistry} to use (not null)
	 * @param convention The {@link AuthorizationObservationConvention} to use to name and tag the observation (not
	 *        null)
	 */
	public ObservedAuthorizationManager(AuthorizationManager<T> delegate, ObservationRegistry observationRegistry,
			AuthorizationObservationConvention convention) {
		super();
		ObjectUtils.argumentNotNull(delegate, "The AuthorizationManager to decorate must be not null");
		ObjectUtils.argumentNotNull(observationRegistry, "The ObservationRegistry must be not null");
		ObjectUtils.argumentNotNull(convention, "The AuthorizationObservationConvention must be not null");
		this.delegate = delegate;
		this.observationRegistry = observationRegistry;
		this.convention = convention;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.security.authorization.AuthorizationManager#authorize(java.util.function.Supplier,
	 * java.lang.Object)
	 */
	@Override
	public AuthorizationResult authorize(Supplier<? extends Authentication> authentication, T object) {
		final AuthorizationObservationContext context = new AuthorizationObservationContext(
				(object != null) ? object.getClass() : null);
		return Observation.createNotStarted(convention, () -> context, observationRegistry).observe(() -> {
			final AuthorizationResult result = delegate.authorize(authentication, object);
			context.setGranted(result != null && result.isGranted());
			return result;
		});
	}

}
