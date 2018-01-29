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
package com.holonplatform.core.internal.datastore.operation;

import com.holonplatform.core.property.PropertyBox;

/**
 * Abstract {@link PropertyBoxOperationDefinition}.
 *
 * @since 5.1.0
 */
public class DefaultPropertyBoxOperationDefinition extends AbstractDatastoreOperationDefinition
		implements PropertyBoxOperationDefinition {

	private PropertyBox value;

	/*
	 * (non-Javadoc)
	 * @see
	 * com.holonplatform.core.internal.datastore.operation.PropertyBoxOperationDefinition#setValue(com.holonplatform.
	 * core.property.PropertyBox)
	 */
	@Override
	public void setValue(PropertyBox value) {
		this.value = value;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.datastore.operation.PropertyBoxOperationConfiguration#getValue()
	 */
	@Override
	public PropertyBox getValue() {
		return value;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.internal.datastore.operation.AbstractDatastoreOperationDefinition#validate()
	 */
	@Override
	public void validate() throws InvalidExpressionException {
		super.validate();
		if (getValue() == null) {
			throw new InvalidExpressionException("Missing operation PropertyBox value");
		}
	}

}
