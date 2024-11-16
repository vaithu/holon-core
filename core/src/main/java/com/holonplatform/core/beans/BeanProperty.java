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
package com.holonplatform.core.beans;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import com.holonplatform.core.internal.beans.DefaultBeanProperty;
import com.holonplatform.core.property.PathProperty;

/**
 * Represents a Java Bean property as a {@link PathProperty}, providing additional configuration informations and
 * read/write methods.
 * <p>
 * This class is mainly intended for internal use.
 * </p>
 * 
 * @param <T> Property type
 * 
 * @since 5.0.0
 * 
 * @see BeanIntrospector
 */
public interface BeanProperty<T> extends PathProperty<T> {

	/**
	 * Get the parent bean property, if any
	 * @return Optional parent bean property
	 */
	Optional<BeanProperty<?>> getParentProperty();

	/**
	 * Get the bean method to be used to read property value, if available.
	 * @return Optional read method
	 */
	Optional<Method> getReadMethod();

	/**
	 * Get the bean method to be used to write property value, if available.
	 * @return Optional write method
	 */
	Optional<Method> getWriteMethod();

	/**
	 * Get the field to which the bean property is bound
	 * @return Optional bean property field
	 */
	Optional<Field> getField();

	/**
	 * Get the property sequence within a property set, if configured.
	 * @return Optional property sequence
	 */
	Optional<Integer> getSequence();

	/**
	 * Get whether the property is declared as an identifier for the bean property set.
	 * @return <code>true</code> if it is an identifier property, <code>false</code> otherwise
	 * @since 5.1.0
	 */
	boolean isIdentifier();

	/**
	 * Get whether the property is declared as a Version for the bean property set.
	 * @return <code>true</code> if it is a Version property, <code>false</code> otherwise
	 * @since 5.5.2
	 */
	boolean isVersion();

	/**
	 * Gets the annotation of given <code>annotationClass</code> type declared on this property, if available.
	 * <p>
	 * Only annotations declared on the {@link Field} which corresponds to this property are taken into account, any
	 * annotation on read/write methods is ignored.
	 * </p>
	 * @param <A> Annotation type
	 * @param annotationClass Annotation class to obtain
	 * @return Optional Annotation instance
	 */
	<A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass);

	/**
	 * Checks whether an annotation of given <code>annotationClass</code> is present on this property.
	 * @param <A> Annotation type
	 * @param annotationClass Annotation class to check
	 * @return <code>true</code> if an annotation of given type is present on this property, <code>false</code>
	 *         otherwise
	 */
	default <A extends Annotation> boolean hasAnnotation(Class<A> annotationClass) {
		return getAnnotation(annotationClass).isPresent();
	}

	// Builder

	/**
	 * Get a builder to create a {@link BeanProperty}.
	 * @param <T> Property type
	 * @param name Property name (not null)
	 * @param type Property type (not null)
	 * @return BeanProperty builder
	 */
	static <T> Builder<T> builder(String name, Class<T> type) {
		return new DefaultBeanProperty<>(name, type);
	}

	/**
	 * BeanProperty builder.
	 * @param <T> Property type
	 */
	public interface Builder<T> extends PathProperty.Builder<T, BeanProperty<T>, Builder<T>>, BeanProperty<T> {

		/**
		 * Set the bean property read (get) method
		 * @param method Method to set
		 * @return this
		 */
		BeanProperty.Builder<T> readMethod(Method method);

		/**
		 * Set the bean property write (set) method
		 * @param method Method to set
		 * @return this
		 */
		BeanProperty.Builder<T> writeMethod(Method method);

		/**
		 * Set the bean property field
		 * @param field Field to set
		 * @return this
		 */
		BeanProperty.Builder<T> field(Field field);

		/**
		 * Set the bean property sequence
		 * @param sequence Sequence to set
		 * @return this
		 */
		BeanProperty.Builder<T> sequence(Integer sequence);

		/**
		 * Set the bean property as an identifier property.
		 * @param identifier Whether the property is an identifier for the bean property set
		 * @return this
		 * @since 5.1.0
		 */
		BeanProperty.Builder<T> identifier(boolean identifier);

		/**
		 * Set the bean property as a  version property.
		 * @param version Whether the property is a version for the bean property set
		 * @return this
		 * @since 5.5.2
		 */
		BeanProperty.Builder<T> version(boolean version);

		/**
		 * Set the property annotations
		 * @param annotations Annotations to set
		 * @return this
		 */
		BeanProperty.Builder<T> annotations(Annotation[] annotations);

		/**
		 * Mark the property as to be ignored (i.e. not to be part of the bean property set) or not.
		 * @param ignoreMode The ignore mode
		 * @return this
		 */
		BeanProperty.Builder<T> ignoreMode(IgnoreMode ignoreMode);

		/**
		 * Get whether the property is marked as to be ignored and the ignore modality.
		 * @return Optional property ignore mode
		 */
		Optional<IgnoreMode> getIgnoreMode();

	}

}
