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

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import com.holonplatform.core.internal.utils.ObjectUtils;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

/**
 * A Spring Security {@link AuthenticationProvider} decorator which instruments the wrapped provider's
 * {@link #authenticate(Authentication)} calls using Micrometer {@link Observation}s, recording the authentication
 * outcome (success/failure) and, in case of failure, the exception type as observation key values (see
 * {@link DefaultAuthenticationObservationConvention}).
 * <p>
 * This decorator works with <b>any</b> {@link AuthenticationProvider}, including (but not limited to) the Holon
 * platform {@code AuthenticationProviderAdapter} bridge. It is entirely opt-in: wrap an existing provider with this
 * class only where authentication observability is desired.
 * </p>
 * <p>
 * Typical usage:
 * </p>
 * 
 * <pre>
 * AuthenticationProvider provider = SpringSecurity.authenticationProvider(authenticator, ...);
 * AuthenticationProvider observed = new ObservedAuthenticationProvider(provider, observationRegistry);
 * </pre>
 * 
 * @since 10.0.0
 */
public class ObservedAuthenticationProvider implements AuthenticationProvider {

	private final AuthenticationProvider delegate;
	private final ObservationRegistry observationRegistry;
	private final AuthenticationObservationConvention convention;

	/**
	 * Constructor using the {@link DefaultAuthenticationObservationConvention}.
	 * @param delegate The {@link AuthenticationProvider} to decorate (not null)
	 * @param observationRegistry The Micrometer {@link ObservationRegistry} to use (not null)
	 */
	public ObservedAuthenticationProvider(AuthenticationProvider delegate, ObservationRegistry observationRegistry) {
		this(delegate, observationRegistry, new DefaultAuthenticationObservationConvention());
	}

	/**
	 * Constructor.
	 * @param delegate The {@link AuthenticationProvider} to decorate (not null)
	 * @param observationRegistry The Micrometer {@link ObservationRegistry} to use (not null)
	 * @param convention The {@link AuthenticationObservationConvention} to use to name and tag the observation (not
	 *        null)
	 */
	public ObservedAuthenticationProvider(AuthenticationProvider delegate, ObservationRegistry observationRegistry,
			AuthenticationObservationConvention convention) {
		super();
		ObjectUtils.argumentNotNull(delegate, "The AuthenticationProvider to decorate must be not null");
		ObjectUtils.argumentNotNull(observationRegistry, "The ObservationRegistry must be not null");
		ObjectUtils.argumentNotNull(convention, "The AuthenticationObservationConvention must be not null");
		this.delegate = delegate;
		this.observationRegistry = observationRegistry;
		this.convention = convention;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.security.authentication.AuthenticationProvider#supports(java.lang.Class)
	 */
	@Override
	public boolean supports(Class<?> authentication) {
		return delegate.supports(authentication);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.springframework.security.authentication.AuthenticationProvider#authenticate(org.springframework.security.
	 * core.Authentication)
	 */
	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		final AuthenticationObservationContext context = new AuthenticationObservationContext(
				authentication.getClass(), authentication.getName());
		return Observation.createNotStarted(convention, () -> context, observationRegistry).observe(() -> {
			try {
				final Authentication result = delegate.authenticate(authentication);
				context.setOutcome(AuthenticationObservationContext.Outcome.SUCCESS);
				return result;
			} catch (AuthenticationException e) {
				context.setOutcome(AuthenticationObservationContext.Outcome.FAILURE);
				context.setFailureType(e.getClass().getSimpleName());
				throw e;
			}
		});
	}

}
