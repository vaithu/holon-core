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
import com.holonplatform.core.query.temporal.TemporalFunction.CurrentTimestamp;

/**
 * A {@link TemporalFunction} to obtain the current timestamp as a {@link Long} number which represents the milliseconds
 * since January 1, 1970, 00:00:00 GMT (Unix epoch). A negative number is the number of milliseconds before January 1, 1970, 00:00:00
 * GMT.
 *
 * @since 5.1.0
 */
public class CurrentTimestampFunction implements CurrentTimestamp {

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.query.QueryFunction#getResultType()
	 */
	@Override
	public Class<? extends Long> getResultType() {
		return Long.class;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.Expression#validate()
	 */
	@Override
	public void validate() throws InvalidExpressionException {
	}

}
