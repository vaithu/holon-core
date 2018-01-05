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
package com.holonplatform.core.internal.query.temporal;

import com.holonplatform.core.query.temporal.TemporalFunction;

/**
 * Abstract {@link TemporalFunction} with an {@link Integer} result type.
 *
 * @since 5.1.0
 */
public abstract class AbstractIntegerTemporalFunction implements TemporalFunction<Integer> {

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.query.QueryFunction#getResultType()
	 */
	@Override
	public Class<? extends Integer> getResultType() {
		return Integer.class;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.Expression#validate()
	 */
	@Override
	public void validate() throws InvalidExpressionException {
		if (getResultType() == null) {
			throw new InvalidExpressionException("Null function result type");
		}
	}

}
