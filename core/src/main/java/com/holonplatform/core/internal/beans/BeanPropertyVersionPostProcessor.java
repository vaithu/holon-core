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
package com.holonplatform.core.internal.beans;

import com.holonplatform.core.beans.BeanProperty;
import com.holonplatform.core.beans.BeanPropertyPostProcessor;
import com.holonplatform.core.beans.Identifier;
import com.holonplatform.core.beans.Version;
import com.holonplatform.core.internal.Logger;
import jakarta.annotation.Priority;

/**
 * A {@link BeanPropertyPostProcessor} to set a property as version using the {@link org.hibernate.Version} annotation.
 *
 * @since 5.1.0
 */
@Priority(130)
public class BeanPropertyVersionPostProcessor implements BeanPropertyPostProcessor {

	/**
	 * Logger
	 */
	private static final Logger LOGGER = BeanLogger.create();

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanPropertyPostProcessor#processBeanProperty(com.holonplatform.core.beans.
	 * BeanProperty.Builder, java.lang.Class)
	 */
	@Override
	public BeanProperty.Builder<?> processBeanProperty(BeanProperty.Builder<?> property, Class<?> beanOrNestedClass) {

		property.getAnnotation(Version.class).ifPresent(a -> {
			property.version(true);
			LOGGER.debug(() -> "BeanPropertyIdentifierPostProcessor: property [" + property + "] set as version");
		});

		return property;
	}

}
