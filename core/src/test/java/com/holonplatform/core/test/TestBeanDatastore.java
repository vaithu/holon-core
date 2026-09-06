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
package com.holonplatform.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import com.holonplatform.core.Expression;
import com.holonplatform.core.ExpressionResolver;
import com.holonplatform.core.datastore.DataTarget;
import com.holonplatform.core.datastore.Datastore;
import com.holonplatform.core.datastore.Datastore.OperationResult;
import com.holonplatform.core.datastore.DatastoreCommodity;
import com.holonplatform.core.datastore.DatastoreOperations.WriteOption;
import com.holonplatform.core.datastore.DefaultWriteOption;
import com.holonplatform.core.datastore.beans.BeanBulkDelete;
import com.holonplatform.core.datastore.beans.BeanBulkInsert;
import com.holonplatform.core.datastore.beans.BeanBulkUpdate;
import com.holonplatform.core.datastore.beans.BeanDatastore;
import com.holonplatform.core.datastore.beans.BeanDatastore.BeanOperationResult;
import com.holonplatform.core.datastore.beans.BeanQuery;
import com.holonplatform.core.datastore.bulk.BulkDelete;
import com.holonplatform.core.datastore.bulk.BulkInsert;
import com.holonplatform.core.datastore.bulk.BulkUpdate;
import com.holonplatform.core.datastore.operation.Insert;
import com.holonplatform.core.datastore.transaction.Transactional;
import com.holonplatform.core.query.Query;
import com.holonplatform.core.test.data.TestBean;
import com.holonplatform.core.property.PropertyBox;

@SuppressWarnings({ "rawtypes", "unchecked" })
class TestBeanDatastore {

	@Test
	void testBeanDatastoreMethods() {

		final Datastore delegate = mock(Datastore.class);
		final BeanDatastore beanDatastore = BeanDatastore.of(delegate);

		final Query rawQuery = mock(Query.class);
		final Query targetQuery = mock(Query.class);

		when(delegate.query()).thenReturn(rawQuery);
		when(delegate.query(any(DataTarget.class))).thenReturn(targetQuery);

		final OperationResult insertResult = OperationResult.builder().type(Datastore.OperationType.INSERT).affectedCount(1)
				.build();
		final OperationResult updateResult = OperationResult.builder().type(Datastore.OperationType.UPDATE).affectedCount(1)
				.build();
		final OperationResult saveResult = OperationResult.builder().type(Datastore.OperationType.INSERT).affectedCount(1)
				.build();
		final OperationResult deleteResult = OperationResult.builder().type(Datastore.OperationType.DELETE).affectedCount(1)
				.build();

		when(delegate.refresh(any(DataTarget.class), any(PropertyBox.class))).thenAnswer(i -> i.getArgument(1));
		when(delegate.insert(any(DataTarget.class), any(PropertyBox.class), ArgumentMatchers.any(WriteOption[].class)))
				.thenReturn(insertResult);
		when(delegate.update(any(DataTarget.class), any(PropertyBox.class), ArgumentMatchers.any(WriteOption[].class)))
				.thenReturn(updateResult);
		when(delegate.save(any(DataTarget.class), any(PropertyBox.class), ArgumentMatchers.any(WriteOption[].class)))
				.thenReturn(saveResult);
		when(delegate.delete(any(DataTarget.class), any(PropertyBox.class), ArgumentMatchers.any(WriteOption[].class)))
				.thenReturn(deleteResult);

		final BulkInsert bulkInsert = mock(BulkInsert.class);
		final BulkUpdate bulkUpdate = mock(BulkUpdate.class);
		final BulkDelete bulkDelete = mock(BulkDelete.class);
		when(delegate.bulkInsert(any(DataTarget.class), any())).thenReturn(bulkInsert);
		when(delegate.bulkUpdate(any(DataTarget.class))).thenReturn(bulkUpdate);
		when(delegate.bulkDelete(any(DataTarget.class))).thenReturn(bulkDelete);

		final Transactional transactional = mock(Transactional.class);
		when(delegate.isTransactional()).thenReturn(Optional.of(transactional));
		when(delegate.requireTransactional()).thenReturn(transactional);
		when(delegate.getDataContextId()).thenReturn(Optional.of("ctx-bean"));

		final Set<Class<? extends DatastoreCommodity>> commodities = Set.of(Query.class);
		when(delegate.getAvailableCommodities()).thenReturn(commodities);
		when(delegate.create(Query.class)).thenReturn(rawQuery);

		final ExpressionResolver<Expression, Expression> resolver = mock(ExpressionResolver.class);

		beanDatastore.addExpressionResolver(resolver);
		beanDatastore.removeExpressionResolver(resolver);
		beanDatastore.addExpressionResolvers(List.of(resolver, resolver));

		verify(delegate, times(3)).addExpressionResolver(resolver);
		verify(delegate).removeExpressionResolver(resolver);

		assertEquals(Optional.of("ctx-bean"), beanDatastore.getDataContextId());
		assertEquals(Optional.of(transactional), beanDatastore.isTransactional());
		assertSame(transactional, beanDatastore.requireTransactional());

		assertEquals(commodities, beanDatastore.getAvailableCommodities());
		assertTrue(beanDatastore.hasCommodity(Query.class));
		assertFalse(beanDatastore.hasCommodity(Insert.class));
		assertSame(rawQuery, beanDatastore.create(Query.class));

		final TestBean<Integer> bean = new TestBean<>("one", 1);

		final TestBean<Integer> refreshed = beanDatastore.refresh(bean);
		assertEquals("one", refreshed.getName());
		assertEquals(1, refreshed.getSequence());

		final BeanOperationResult<TestBean<Integer>> insert = beanDatastore.insert(bean);
		final BeanOperationResult<TestBean<Integer>> update = beanDatastore.update(bean);
		final BeanOperationResult<TestBean<Integer>> save = beanDatastore.save(bean);
		final BeanOperationResult<TestBean<Integer>> delete = beanDatastore.delete(bean);

		assertEquals(Datastore.OperationType.INSERT, insert.getOperationType().orElse(null));
		assertEquals(1, insert.getAffectedCount());
		assertTrue(insert.getResult().isPresent());
		assertEquals("one", insert.getResult().get().getName());

		assertEquals(Datastore.OperationType.UPDATE, update.getOperationType().orElse(null));
		assertEquals(1, update.getAffectedCount());
		assertTrue(update.getResult().isPresent());

		assertEquals(Datastore.OperationType.INSERT, save.getOperationType().orElse(null));
		assertEquals(1, save.getAffectedCount());
		assertTrue(save.getResult().isPresent());

		assertEquals(Datastore.OperationType.DELETE, delete.getOperationType().orElse(null));
		assertEquals(1, delete.getAffectedCount());
		assertFalse(delete.getResult().isPresent());

		verify(delegate).insert(any(DataTarget.class), any(), eq(DefaultWriteOption.BRING_BACK_GENERATED_IDS_AND_VERSION));
		verify(delegate).update(any(DataTarget.class), any(), eq(DefaultWriteOption.BRING_BACK_GENERATED_IDS_AND_VERSION));
		verify(delegate).save(any(DataTarget.class), any(), eq(DefaultWriteOption.BRING_BACK_GENERATED_IDS_AND_VERSION));

		final BeanBulkInsert<TestBean> beanBulkInsert = beanDatastore.bulkInsert(TestBean.class);
		final BeanBulkUpdate<TestBean> beanBulkUpdate = beanDatastore.bulkUpdate(TestBean.class);
		final BeanBulkDelete<TestBean> beanBulkDelete = beanDatastore.bulkDelete(TestBean.class);

		assertNotNull(beanBulkInsert);
		assertNotNull(beanBulkUpdate);
		assertNotNull(beanBulkDelete);

		verify(delegate).bulkInsert(any(DataTarget.class), any());
		verify(delegate).bulkUpdate(any(DataTarget.class));
		verify(delegate).bulkDelete(any(DataTarget.class));

		final BeanQuery<TestBean> beanQuery = beanDatastore.query(TestBean.class);
		assertNotNull(beanQuery);
		beanQuery.count();
		beanQuery.pageable(2, 10);

		verify(delegate).query();
		verify(rawQuery).count();
		verify(rawQuery).restrict(10, 20);
		verify(rawQuery).target(any(DataTarget.class));

		assertSame(rawQuery, beanDatastore.query());
		verify(delegate, times(2)).query();

		final DataTarget<?> target = DataTarget.named("bean_target");
		assertSame(targetQuery, beanDatastore.query(target));
		verify(delegate).query(eq(target));
	}

}
