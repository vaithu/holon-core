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
package com.holonplatform.async.datastore.test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.holonplatform.async.datastore.AsyncDatastore;
import com.holonplatform.async.datastore.operation.AsyncBulkDelete;
import com.holonplatform.async.datastore.operation.AsyncBulkInsert;
import com.holonplatform.async.datastore.operation.AsyncBulkUpdate;
import com.holonplatform.async.datastore.operation.AsyncDelete;
import com.holonplatform.async.datastore.operation.AsyncInsert;
import com.holonplatform.async.datastore.operation.AsyncQuery;
import com.holonplatform.async.datastore.operation.AsyncRefresh;
import com.holonplatform.async.datastore.operation.AsyncSave;
import com.holonplatform.async.datastore.operation.AsyncUpdate;
import com.holonplatform.async.datastore.transaction.AsyncTransactional;
import com.holonplatform.core.datastore.DataTarget;
import com.holonplatform.core.datastore.Datastore.OperationResult;
import com.holonplatform.core.datastore.DatastoreOperations.WriteOption;
import com.holonplatform.core.exceptions.DataAccessException;
import com.holonplatform.core.property.PropertyBox;
import com.holonplatform.core.property.PropertySet;

class AsyncDatastoreTest {

	private AsyncDatastore datastore;
	private DataTarget<?> target;
	private PropertyBox propertyBox;

	@BeforeEach
	void setUp() {
		datastore = mock(AsyncDatastore.class, Mockito.CALLS_REAL_METHODS);
		target = mock(DataTarget.class);
		propertyBox = mock(PropertyBox.class);
	}

	// --- refresh ---

	@Test
	void refresh_shouldDelegateToAsyncRefresh() {
		AsyncRefresh refreshOp = mock(AsyncRefresh.class);
		CompletionStage<PropertyBox> expected = CompletableFuture.completedFuture(propertyBox);

		when(datastore.create(AsyncRefresh.class)).thenReturn(refreshOp);
		when(refreshOp.target(target)).thenReturn(refreshOp);
		when(refreshOp.value(propertyBox)).thenReturn(refreshOp);
		when(refreshOp.execute()).thenReturn(expected);

		CompletionStage<PropertyBox> result = datastore.refresh(target, propertyBox);

		assertSame(expected, result);
		verify(datastore).create(AsyncRefresh.class);
	}

	@Test
	void refresh_shouldThrowDataAccessExceptionOnFailure() {
		when(datastore.create(AsyncRefresh.class)).thenThrow(new RuntimeException("fail"));

		assertThrows(DataAccessException.class, () -> datastore.refresh(target, propertyBox));
	}

	// --- insert ---

	@Test
	void insert_shouldDelegateToAsyncInsert() {
		AsyncInsert insertOp = mock(AsyncInsert.class);
		OperationResult opResult = mock(OperationResult.class);
		CompletionStage<OperationResult> expected = CompletableFuture.completedFuture(opResult);

		when(datastore.create(AsyncInsert.class)).thenReturn(insertOp);
		when(insertOp.target(target)).thenReturn(insertOp);
		when(insertOp.value(propertyBox)).thenReturn(insertOp);
		when(insertOp.withWriteOptions(any(WriteOption[].class))).thenReturn(insertOp);
		when(insertOp.execute()).thenReturn(expected);

		CompletionStage<OperationResult> result = datastore.insert(target, propertyBox);

		assertSame(expected, result);
	}

	@Test
	void insert_shouldThrowDataAccessExceptionOnFailure() {
		when(datastore.create(AsyncInsert.class)).thenThrow(new RuntimeException("fail"));

		assertThrows(DataAccessException.class, () -> datastore.insert(target, propertyBox));
	}

	// --- update ---

	@Test
	void update_shouldDelegateToAsyncUpdate() {
		AsyncUpdate updateOp = mock(AsyncUpdate.class);
		OperationResult opResult = mock(OperationResult.class);
		CompletionStage<OperationResult> expected = CompletableFuture.completedFuture(opResult);

		when(datastore.create(AsyncUpdate.class)).thenReturn(updateOp);
		when(updateOp.target(target)).thenReturn(updateOp);
		when(updateOp.value(propertyBox)).thenReturn(updateOp);
		when(updateOp.withWriteOptions(any(WriteOption[].class))).thenReturn(updateOp);
		when(updateOp.execute()).thenReturn(expected);

		CompletionStage<OperationResult> result = datastore.update(target, propertyBox);

		assertSame(expected, result);
	}

	@Test
	void update_shouldThrowDataAccessExceptionOnFailure() {
		when(datastore.create(AsyncUpdate.class)).thenThrow(new RuntimeException("fail"));

		assertThrows(DataAccessException.class, () -> datastore.update(target, propertyBox));
	}

	// --- save ---

	@Test
	void save_shouldDelegateToAsyncSave() {
		AsyncSave saveOp = mock(AsyncSave.class);
		OperationResult opResult = mock(OperationResult.class);
		CompletionStage<OperationResult> expected = CompletableFuture.completedFuture(opResult);

		when(datastore.create(AsyncSave.class)).thenReturn(saveOp);
		when(saveOp.target(target)).thenReturn(saveOp);
		when(saveOp.value(propertyBox)).thenReturn(saveOp);
		when(saveOp.withWriteOptions(any(WriteOption[].class))).thenReturn(saveOp);
		when(saveOp.execute()).thenReturn(expected);

		CompletionStage<OperationResult> result = datastore.save(target, propertyBox);

		assertSame(expected, result);
	}

	@Test
	void save_shouldThrowDataAccessExceptionOnFailure() {
		when(datastore.create(AsyncSave.class)).thenThrow(new RuntimeException("fail"));

		assertThrows(DataAccessException.class, () -> datastore.save(target, propertyBox));
	}

	// --- delete ---

	@Test
	void delete_shouldDelegateToAsyncDelete() {
		AsyncDelete deleteOp = mock(AsyncDelete.class);
		OperationResult opResult = mock(OperationResult.class);
		CompletionStage<OperationResult> expected = CompletableFuture.completedFuture(opResult);

		when(datastore.create(AsyncDelete.class)).thenReturn(deleteOp);
		when(deleteOp.target(target)).thenReturn(deleteOp);
		when(deleteOp.value(propertyBox)).thenReturn(deleteOp);
		when(deleteOp.withWriteOptions(any(WriteOption[].class))).thenReturn(deleteOp);
		when(deleteOp.execute()).thenReturn(expected);

		CompletionStage<OperationResult> result = datastore.delete(target, propertyBox);

		assertSame(expected, result);
	}

	@Test
	void delete_shouldThrowDataAccessExceptionOnFailure() {
		when(datastore.create(AsyncDelete.class)).thenThrow(new RuntimeException("fail"));

		assertThrows(DataAccessException.class, () -> datastore.delete(target, propertyBox));
	}

	// --- bulkInsert ---

	@Test
	void bulkInsert_shouldDelegateToAsyncBulkInsert() {
		AsyncBulkInsert bulkInsertOp = mock(AsyncBulkInsert.class);
		PropertySet<?> propertySet = mock(PropertySet.class);

		when(datastore.create(AsyncBulkInsert.class)).thenReturn(bulkInsertOp);
		when(bulkInsertOp.target(target)).thenReturn(bulkInsertOp);
		when(bulkInsertOp.propertySet(propertySet)).thenReturn(bulkInsertOp);
		when(bulkInsertOp.withWriteOptions(any(WriteOption[].class))).thenReturn(bulkInsertOp);

		AsyncBulkInsert result = datastore.bulkInsert(target, propertySet);

		assertSame(bulkInsertOp, result);
	}

	// --- bulkUpdate ---

	@Test
	void bulkUpdate_shouldDelegateToAsyncBulkUpdate() {
		AsyncBulkUpdate bulkUpdateOp = mock(AsyncBulkUpdate.class);

		when(datastore.create(AsyncBulkUpdate.class)).thenReturn(bulkUpdateOp);
		when(bulkUpdateOp.target(target)).thenReturn(bulkUpdateOp);
		when(bulkUpdateOp.withWriteOptions(any(WriteOption[].class))).thenReturn(bulkUpdateOp);

		AsyncBulkUpdate result = datastore.bulkUpdate(target);

		assertSame(bulkUpdateOp, result);
	}

	// --- bulkDelete ---

	@Test
	void bulkDelete_shouldDelegateToAsyncBulkDelete() {
		AsyncBulkDelete bulkDeleteOp = mock(AsyncBulkDelete.class);

		when(datastore.create(AsyncBulkDelete.class)).thenReturn(bulkDeleteOp);
		when(bulkDeleteOp.target(target)).thenReturn(bulkDeleteOp);
		when(bulkDeleteOp.withWriteOptions(any(WriteOption[].class))).thenReturn(bulkDeleteOp);

		AsyncBulkDelete result = datastore.bulkDelete(target);

		assertSame(bulkDeleteOp, result);
	}

	// --- query ---

	@Test
	void query_shouldDelegateToCreate() {
		AsyncQuery queryOp = mock(AsyncQuery.class);

		when(datastore.create(AsyncQuery.class)).thenReturn(queryOp);

		AsyncQuery result = datastore.query();

		assertSame(queryOp, result);
	}

	@Test
	void queryWithTarget_shouldSetTarget() {
		AsyncQuery queryOp = mock(AsyncQuery.class);

		when(datastore.create(AsyncQuery.class)).thenReturn(queryOp);
		when(queryOp.target(target)).thenReturn(queryOp);

		AsyncQuery result = datastore.query(target);

		assertSame(queryOp, result);
		verify(queryOp).target(target);
	}

	@Test
	void queryWithTarget_shouldRejectNull() {
		assertThrows(IllegalArgumentException.class, () -> datastore.query(null));
	}

	// --- isTransactional ---

	@Test
	void isTransactional_shouldReturnEmptyWhenNotTransactional() {
		Optional<AsyncTransactional> result = datastore.isTransactional();

		assertTrue(result.isEmpty());
	}

	@Test
	void isTransactional_shouldReturnPresentWhenTransactional() {
		// Create a mock that implements both interfaces
		AsyncDatastore transactionalDs = mock(AsyncDatastore.class,
				withSettings().extraInterfaces(AsyncTransactional.class).defaultAnswer(Mockito.CALLS_REAL_METHODS));

		Optional<AsyncTransactional> result = transactionalDs.isTransactional();

		assertTrue(result.isPresent());
		assertSame(transactionalDs, result.get());
	}

	// --- requireTransactional ---

	@Test
	void requireTransactional_shouldThrowWhenNotTransactional() {
		assertThrows(IllegalStateException.class, () -> datastore.requireTransactional());
	}

	@Test
	void requireTransactional_shouldReturnWhenTransactional() {
		AsyncDatastore transactionalDs = mock(AsyncDatastore.class,
				withSettings().extraInterfaces(AsyncTransactional.class).defaultAnswer(Mockito.CALLS_REAL_METHODS));

		AsyncTransactional result = transactionalDs.requireTransactional();

		assertNotNull(result);
		assertSame(transactionalDs, result);
	}
}

