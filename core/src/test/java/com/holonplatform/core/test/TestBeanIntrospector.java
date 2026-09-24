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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.time.Month;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.holonplatform.core.DataMappable;
import com.holonplatform.core.beans.BeanIntrospector;
import com.holonplatform.core.beans.BeanProperty;
import com.holonplatform.core.beans.BeanPropertySet;
import com.holonplatform.core.property.BooleanProperty;
import com.holonplatform.core.property.NumericProperty;
import com.holonplatform.core.property.PathProperty;
import com.holonplatform.core.property.PropertyBox;
import com.holonplatform.core.property.StringProperty;
import com.holonplatform.core.property.TemporalProperty;
import com.holonplatform.core.temporal.TemporalType;
import com.holonplatform.core.test.data.TestBean4;
import com.holonplatform.core.test.data.TestBeanPropertyBean;
import com.holonplatform.core.test.data.TestEnum;
import com.holonplatform.core.test.data.TestEnum2;

class TestBeanIntrospector {

	@Test
	void testProperties() {

		BeanPropertySet<TestBeanPropertyBean> set = BeanIntrospector.get().getPropertySet(TestBeanPropertyBean.class);

		assertTrue(set.getProperty("name").isPresent());
		assertTrue(set.getProperty("text").isPresent());
		assertTrue(set.getProperty("numbool").isPresent());
		assertTrue(set.getProperty("enm").isPresent());
		assertTrue(set.getProperty("enmOrdinal").isPresent());
		assertTrue(set.getProperty("date").isPresent());
		assertTrue(set.getProperty("lng").isPresent());
		assertTrue(set.getProperty("notneg").isPresent());
		assertTrue(set.getProperty("intval").isPresent());
		assertTrue(set.getProperty("legacyDate").isPresent());

		assertTrue(set.contains("name"));
		assertFalse(set.contains("xyz"));

		assertFalse(set.getProperty("ignore").isPresent());

		assertEquals("TheName", set.getProperty("name").get().getMessage());

		assertNotNull(set.getProperty("numbool").get().getConverter());
		assertNotNull(set.getProperty("enm").get().getConverter());
		assertNotNull(set.getProperty("date").get().getConverter());

		assertEquals(TemporalType.DATE_TIME,
				set.getProperty("legacyDate").get().getConfiguration().getTemporalType().orElse(null));

		assertEquals(boolean.class, set.getProperty("numbool").get().getType());
		assertEquals(TestEnum2.class, set.getProperty("enm").get().getType());
		assertEquals(TestEnum.class, set.getProperty("enmOrdinal").get().getType());
		assertEquals(LocalDate.class, set.getProperty("date").get().getType());

		PathProperty<Boolean> property1 = set.property("numbool");
		assertEquals(Integer.valueOf(1), property1.getConvertedValue(Boolean.TRUE));

		PathProperty<TestEnum2> property2 = set.property("enm");
		assertEquals(TestEnum2.B.getStringValue(), property2.getConvertedValue(TestEnum2.B));

		PathProperty<LocalDate> property3 = set.property("date");
		Date date = (Date) property3.getConvertedValue(LocalDate.of(1979, Month.MARCH, 9));
		assertNotNull(date);

		Calendar c = Calendar.getInstance();
		c.setTime(date);

		assertEquals(1979, c.get(Calendar.YEAR));
		assertEquals(2, c.get(Calendar.MONTH));
		assertEquals(9, c.get(Calendar.DAY_OF_MONTH));

		assertTrue(set.property("notneg").getConfiguration().hasNotNullParameter("k1"));
		assertEquals("v1", set.property("notneg").getConfiguration().getParameter("k1", String.class, null));

		assertTrue(set.property("notneg").getConfiguration().hasNotNullParameter("k2"));
		assertEquals("v2", set.property("notneg").getConfiguration().getParameter("k2", String.class, null));

	}

	@Test
	void testPropertyReadWrite() {

		BeanPropertySet<TestBeanPropertyBean> set = BeanIntrospector.get().getPropertySet(TestBeanPropertyBean.class);

		TestBeanPropertyBean instance = new TestBeanPropertyBean();

		set.write("name", "test", instance);
		set.write("lng", 7L, instance);
		set.write("notneg", 1, instance);
		set.write("numbool", Boolean.TRUE, instance);
		set.write("enm", TestEnum2.B, instance);
		set.write("enmOrdinal", TestEnum.ONE, instance);

		assertEquals("test", set.read("name", instance));
		assertEquals(Boolean.TRUE, set.read("numbool", instance));
		assertEquals(TestEnum2.B, set.read("enm", instance));
		assertEquals(TestEnum.ONE, set.read("enmOrdinal", instance));
		assertEquals(Long.valueOf(7), set.read("lng", instance));
		assertEquals(Integer.valueOf(1), set.read("notneg", instance));

		PathProperty<Integer> modelEnum = PathProperty.create("enmOrdinal", Integer.class);
		PropertyBox pb = PropertyBox.create(modelEnum);

		set.read(pb, instance);

	}

	@Test
	void testPropertyBox() {

		BeanPropertySet<TestBeanPropertyBean> set = BeanIntrospector.get().getPropertySet(TestBeanPropertyBean.class);

		TestBeanPropertyBean instance = new TestBeanPropertyBean();
		instance.setName("test");
		instance.setNotneg(1);
		instance.setNumbool(true);
		instance.setLng(7L);
		instance.setEnm(TestEnum2.B);

		// read

		PropertyBox pb = set.read(instance);

		assertNotNull(pb);

		assertTrue(pb.containsValue(set.property("name")));
		assertTrue(pb.containsValue(set.property("numbool")));
		assertTrue(pb.containsValue(set.property("enm")));
		assertTrue(pb.containsValue(set.property("lng")));
		assertTrue(pb.containsValue(set.property("notneg")));

		assertEquals("test", pb.getValue(set.property("name")));
		assertEquals(Boolean.TRUE, pb.getValue(set.property("numbool")));
		assertEquals(TestEnum2.B, pb.getValue(set.property("enm")));
		assertEquals(Long.valueOf(7), pb.getValue(set.property("lng")));
		assertEquals(Integer.valueOf(1), pb.getValue(set.property("notneg")));

		PropertyBox box = PropertyBox.create(set.property("name"), set.property("enm"));
		box = set.read(box, instance);

		assertEquals("test", box.getValue(set.property("name")));
		assertEquals(TestEnum2.B, box.getValue(set.property("enm")));

		// write

		PropertyBox wb = PropertyBox.builder(set).set(set.property("name"), "test2")
				.set(set.property("numbool"), Boolean.FALSE).set(set.property("enm"), TestEnum2.A)
				.set(set.property("lng"), 7L).build();

		set.write(wb, instance);

	}

	@Test
	void testPropertyIdentifier() {
		BeanPropertySet<TestBeanPropertyBean> set = BeanIntrospector.get().getPropertySet(TestBeanPropertyBean.class);
		assertTrue(set.getFirstIdentifier().isPresent());
		assertEquals("name", set.getFirstIdentifier().get().getName());

		TestBeanPropertyBean b1 = new TestBeanPropertyBean();
		b1.setName("n1");
		TestBeanPropertyBean b2 = new TestBeanPropertyBean();
		b2.setName("n2");
		TestBeanPropertyBean b3 = new TestBeanPropertyBean();
		b3.setName("n1");

		PropertyBox p1 = set.read(b1);
		PropertyBox p2 = set.read(b2);
		PropertyBox p3 = set.read(b3);

		assertNotEquals(p1, p2);
		assertEquals(p1, p3);
	}

	@Test
	void testBooleanBeanPropertyWriteMethod() {
		BeanPropertySet<BooleanAccessorBean> set = BeanPropertySet.create(BooleanAccessorBean.class);

		assertTrue(set.getProperty("active").isPresent());

		BooleanAccessorBean bean = new BooleanAccessorBean();
		set.write("active", Boolean.FALSE, bean);

		assertFalse(bean.isActive());
		assertEquals(Boolean.FALSE, set.read("active", bean));
	}

	public static final class BooleanAccessorBean {
		private boolean active = true;

		public boolean isActive() {
		    return active;
		}

		public Boolean getActive() {
		    return active;
		}

		public void setActive(boolean active) {
		    this.active = active;
		}
	}

	@Test
	void testBeanPropertyAccessorsSurviveGarbageCollection() {
		final BeanPropertySet<StandardBooleanBean> set = BeanPropertySet.create(StandardBooleanBean.class);

		// ensure weak references have been cleared by the garbage collector
		final java.lang.ref.WeakReference<Object> sentinel = new java.lang.ref.WeakReference<>(new Object());
		for (int i = 0; i < 20 && sentinel.get() != null; i++) {
			System.gc();
		}
		assertNull(sentinel.get());

		final BeanProperty<?> property = (BeanProperty<?>) set.property("active");
		assertTrue(property.getWriteMethod().isPresent());
		assertTrue(property.getField().isPresent());

		final StandardBooleanBean bean = new StandardBooleanBean();
		set.write("active", Boolean.FALSE, bean);
		assertFalse(bean.isActive());
		assertEquals(Boolean.FALSE, set.read("active", bean));

		set.write("name", "test", bean);
		assertEquals("test", bean.getName());
	}

	@Test
	void testBeanPropertyAccessorsSurviveSerialization() throws Exception {
		final BeanPropertySet<StandardBooleanBean> set = BeanPropertySet.create(StandardBooleanBean.class);

		final BeanProperty<?> property = (BeanProperty<?>) set.property("active");

		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(property);
		}
		final BeanProperty<?> deserialized;
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
			deserialized = (BeanProperty<?>) ois.readObject();
		}

		assertTrue(deserialized.getReadMethod().isPresent());
		assertTrue(deserialized.getWriteMethod().isPresent());
		assertTrue(deserialized.getField().isPresent());
	}

	public static class StandardBooleanBean {

		private boolean active = true;
		private String name;

		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Test
	void testPathPropertyTypes() {
		final BeanPropertySet<TestBeanPropertyBean> set = BeanPropertySet.create(TestBeanPropertyBean.class);

		StringProperty sp = set.propertyString("name");
		assertNotNull(sp);

		BooleanProperty bp = set.propertyBoolean("numbool");
		assertNotNull(bp);

		NumericProperty<Number> np1 = set.propertyNumeric("intval");
		assertNotNull(np1);

		NumericProperty<Integer> np2 = set.propertyNumeric("intval");
		assertNotNull(np2);

		NumericProperty<Integer> np3 = set.propertyNumeric("intval", Integer.class);
		assertNotNull(np3);

		NumericProperty<Long> np4 = set.propertyNumeric("lng", Long.class);
		assertNotNull(np4);

		TemporalProperty<Date> tp1 = set.propertyTemporal("legacyDate");
		assertNotNull(tp1);

		TemporalProperty<Date> tp2 = set.propertyTemporal("legacyDate", Date.class);
		assertNotNull(tp2);

		TemporalProperty<LocalDate> tp3 = set.propertyTemporal("date");
		assertNotNull(tp3);

		TemporalProperty<LocalDate> tp4 = set.propertyTemporal("date", LocalDate.class);
		assertNotNull(tp4);
	}

	@Test
	void testDataPath() {
		BeanPropertySet<TestBean4> set = BeanIntrospector.get().getPropertySet(TestBean4.class);

		assertTrue(set.getConfiguration().getParameter(DataMappable.PATH).isPresent());
		assertEquals("beanPath", set.getConfiguration().getParameter(DataMappable.PATH, null));

		assertTrue(set.getProperty("id").isPresent());
		PathProperty<Long> p1 = set.property("id");
		assertFalse(p1.getConfiguration().getParameter(DataMappable.PATH).isPresent());

		assertTrue(set.getProperty("text").isPresent());
		PathProperty<String> p2 = set.property("text");
		assertTrue(p2.getConfiguration().getParameter(DataMappable.PATH).isPresent());
		assertEquals("path1", p2.getConfiguration().getParameter(DataMappable.PATH, null));

		assertTrue(set.getProperty("value").isPresent());
		PathProperty<Double> p3 = set.property("value");
		assertTrue(p3.getConfiguration().getParameter(DataMappable.PATH).isPresent());
		assertEquals("path3", p3.getConfiguration().getParameter(DataMappable.PATH, null));
	}

}
