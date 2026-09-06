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
package com.holonplatform.core.datastore.observability;

/**
 * Enumeration of the {@link com.holonplatform.core.datastore.Datastore} operation types which can be observed
 * through the Datastore operation observability SPI.
 * 
 * @since 10.0.0
 * 
 * @see DatastoreOperationObservationContext
 */
public enum DatastoreOperationType {

	/**
	 * A <em>refresh</em> operation.
	 */
	REFRESH,

	/**
	 * An <em>insert</em> operation.
	 */
	INSERT,

	/**
	 * An <em>update</em> operation.
	 */
	UPDATE,

	/**
	 * A <em>save</em> (insert-or-update) operation.
	 */
	SAVE,

	/**
	 * A <em>delete</em> operation.
	 */
	DELETE,

	/**
	 * A <em>bulk insert</em> operation.
	 */
	BULK_INSERT,

	/**
	 * A <em>bulk update</em> operation.
	 */
	BULK_UPDATE,

	/**
	 * A <em>bulk delete</em> operation.
	 */
	BULK_DELETE,

	/**
	 * A <em>query</em> operation.
	 */
	QUERY;

}
