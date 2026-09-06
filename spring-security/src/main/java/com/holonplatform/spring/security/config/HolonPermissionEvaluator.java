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
package com.holonplatform.spring.security.config;

import java.io.Serializable;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import com.holonplatform.auth.Authorizer;
import com.holonplatform.auth.Permission;
import com.holonplatform.core.internal.utils.ObjectUtils;
import com.holonplatform.spring.security.SpringSecurity;

/**
 * A Spring Security {@link PermissionEvaluator} implementation which bridges the standard method security
 * {@code hasPermission(...)} SpEL expressions to a Holon platform {@link Authorizer}.
 * <p>
 * Since the Holon {@link Permission} model is not object-instance aware (permissions are plain, flat
 * {@link String} representations), this evaluator translates the target domain object (or target type) into a
 * permission String using the {@code <targetType>:<permission>} naming convention, e.g. checking
 * {@code hasPermission(#orderId, 'Order', 'view')} is equivalent to checking the {@code Order:view} Holon
 * permission through the configured {@link Authorizer}. When no target type is available (2-arguments form,
 * {@code hasPermission(target, permission)}), the target domain object's simple class name is used, or, if the
 * target itself is <code>null</code>, only the plain permission String is checked.
 * </p>
 * <p>
 * This is intentionally a simple bridge, suited to the common case of coarse-grained, type-scoped permissions.
 * Applications which require full per-instance access control lists (ACLs) should provide a dedicated
 * {@link PermissionEvaluator} implementation instead.
 * </p>
 * 
 * @since 10.0.0
 * 
 * @see HolonMethodSecuritySupport
 */
public class HolonPermissionEvaluator implements PermissionEvaluator {

	private final Authorizer<Permission> authorizer;

	/**
	 * Constructor.
	 * @param authorizer The {@link Authorizer} to use to check permissions (not null)
	 */
	public HolonPermissionEvaluator(Authorizer<Permission> authorizer) {
		super();
		ObjectUtils.argumentNotNull(authorizer, "Authorizer must be not null");
		this.authorizer = authorizer;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.security.access.PermissionEvaluator#hasPermission(org.springframework.security.core.
	 * Authentication, java.lang.Object, java.lang.Object)
	 */
	@Override
	public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
		if (authentication == null || permission == null) {
			return false;
		}
		final String targetType = (targetDomainObject != null) ? targetDomainObject.getClass().getSimpleName()
				: null;
		return checkPermission(authentication, targetType, permission);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.security.access.PermissionEvaluator#hasPermission(org.springframework.security.core.
	 * Authentication, java.io.Serializable, java.lang.String, java.lang.Object)
	 */
	@Override
	public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
			Object permission) {
		if (authentication == null || permission == null) {
			return false;
		}
		return checkPermission(authentication, targetType, permission);
	}

	/**
	 * Build the permission String to check (using the {@code <targetType>:<permission>} convention when a target
	 * type is available) and delegate to the configured {@link Authorizer}.
	 * @param authentication Spring Security {@link Authentication}
	 * @param targetType Optional target type name
	 * @param permission Required permission
	 * @return <code>true</code> if the permission is granted
	 */
	protected boolean checkPermission(Authentication authentication, String targetType, Object permission) {
		final String permissionValue = permission.toString();
		final String permissionToCheck = (targetType != null) ? (targetType + ":" + permissionValue)
				: permissionValue;
		return authorizer.isPermitted(SpringSecurity.asAuthentication(authentication), permissionToCheck);
	}

}
