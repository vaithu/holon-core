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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.holonplatform.core.Path;
import com.holonplatform.core.beans.BeanProperty;
import com.holonplatform.core.beans.IgnoreMode;
import com.holonplatform.core.internal.property.AbstractPathProperty;
import com.holonplatform.core.internal.utils.ObjectUtils;

/**
 * Abstract {@link BeanProperty} class.
 * 
 * @param <T> Property type
 *
 * @since 5.1.0
 */
public abstract class AbstractBeanProperty<T> extends AbstractPathProperty<T, BeanProperty<T>, BeanProperty.Builder<T>>
		implements BeanProperty.Builder<T> {

	private static final long serialVersionUID = -8878075341525987185L;

	/**
	 * Getter method
	 */
	private transient Method readMethod;

	/**
	 * Setter method
	 */
	private transient Method writeMethod;

	/**
	 * Field
	 */
	private transient Field field;

	/**
	 * Getter method descriptor, to restore the method reference after deserialization
	 */
	private MemberDescriptor readMethodDescriptor;

	/**
	 * Setter method descriptor, to restore the method reference after deserialization
	 */
	private MemberDescriptor writeMethodDescriptor;

	/**
	 * Field descriptor, to restore the field reference after deserialization
	 */
	private MemberDescriptor fieldDescriptor;

	/**
	 * Declared field annotations
	 */
	private transient Map<Class<? extends Annotation>, Annotation> annotations;

	/**
	 * Optional property sequence
	 */
	private Integer sequence;

	/**
	 * Identifier property
	 */
	private boolean identifier;

	/**
	 * Version property
	 */
	private boolean version;

	/**
	 * Optional ignore mode
	 */
	private IgnoreMode ignoreMode;

	/**
	 * Constructor.
	 * @param name Property name (not null)
	 * @param type Property value type (not null)
	 */
	public AbstractBeanProperty(String name, Class<? extends T> type) {
		super(name, type);
	}

	@Override
	protected BeanProperty.Builder<T> getActualBuilder() {
		return this;
	}

	@Override
	protected BeanProperty<T> getActualProperty() {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanProperty#getParentProperty()
	 */
	@Override
	public Optional<BeanProperty<?>> getParentProperty() {
		Path<?> parent = getParent().orElse(null);
		if (parent != null && parent instanceof BeanProperty property) {
			return Optional.of(property);
		}
		return Optional.empty();
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanProperty#getReadMethod()
	 */
	@Override
	public Optional<Method> getReadMethod() {
		if (readMethod == null) {
			readMethod = resolveMethod(readMethodDescriptor);
		}
		return Optional.ofNullable(readMethod);
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanProperty#getWriteMethod()
	 */
	@Override
	public Optional<Method> getWriteMethod() {
		if (writeMethod == null) {
			writeMethod = resolveMethod(writeMethodDescriptor);
		}
		return Optional.ofNullable(writeMethod);
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanProperty#getField()
	 */
	@Override
	public Optional<Field> getField() {
		if (field == null) {
			field = resolveField(fieldDescriptor);
		}
		return Optional.ofNullable(field);
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanProperty#getSequence()
	 */
	@Override
	public Optional<Integer> getSequence() {
		return Optional.ofNullable(sequence);
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanProperty#isIdentifier()
	 */
	@Override
	public boolean isIdentifier() {
		return identifier;
	}

	@Override
	public boolean isVersion() {
		return version;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanProperty#getAnnotation(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
		ObjectUtils.argumentNotNull(annotationClass, "Annotation class must be not null");
		if (annotations != null) {
			return Optional.ofNullable((A) annotations.get(annotationClass));
		}
		return Optional.empty();
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanProperty.Builder#readMethod(java.lang.reflect.Method)
	 */
	@Override
	public BeanProperty.Builder<T> readMethod(Method method) {
		this.readMethod = method;
		this.readMethodDescriptor = MemberDescriptor.create(method);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanProperty.Builder#writeMethod(java.lang.reflect.Method)
	 */
	@Override
	public BeanProperty.Builder<T> writeMethod(Method method) {
		this.writeMethod = method;
		this.writeMethodDescriptor = MemberDescriptor.create(method);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanProperty.Builder#field(java.lang.reflect.Field)
	 */
	@Override
	public BeanProperty.Builder<T> field(Field field) {
		this.field = field;
		this.fieldDescriptor = MemberDescriptor.create(field);
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanProperty.Builder#sequence(java.lang.Integer)
	 */
	@Override
	public BeanProperty.Builder<T> sequence(Integer sequence) {
		this.sequence = sequence;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanProperty.Builder#identifier(boolean)
	 */
	@Override
	public BeanProperty.Builder<T> identifier(boolean identifier) {
		this.identifier = identifier;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanProperty.Builder#identifier(boolean)
	 */

	@Override
	public BeanProperty.Builder<T> version(boolean version) {
		this.version = version;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanProperty.Builder#annotations(java.lang.annotation.Annotation[])
	 */
	@Override
	public BeanProperty.Builder<T> annotations(Annotation[] annotations) {
		this.annotations = null;
		if (annotations != null && annotations.length > 0) {
			this.annotations = new HashMap<>(annotations.length);
			for (Annotation annotation : annotations) {
				this.annotations.put(annotation.annotationType(), annotation);
			}
		}
		return this;
	}

	/**
	 * Set the property field annotations.
	 * @param annotations the annotations to set
	 */
	protected void setAnnotations(Map<Class<? extends Annotation>, Annotation> annotations) {
		this.annotations = annotations;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanProperty.Builder#ignoreMode(com.holonplatform.core.beans.IgnoreMode)
	 */
	@Override
	public BeanProperty.Builder<T> ignoreMode(IgnoreMode ignoreMode) {
		this.ignoreMode = ignoreMode;
		return this;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.beans.BeanProperty.Builder#getIgnoreMode()
	 */
	@Override
	public Optional<IgnoreMode> getIgnoreMode() {
		return Optional.ofNullable(ignoreMode);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BeanProperty [getName()=" + getName() + ", getType()=" + getType() + "]";
	}

	/**
	 * Resolve the {@link Method} described by given descriptor, if available.
	 * @param descriptor The member descriptor (may be null)
	 * @return The resolved method, or <code>null</code> if not available
	 */
	private static Method resolveMethod(MemberDescriptor descriptor) {
		if (descriptor == null) {
			return null;
		}
		try {
			Method method = descriptor.getDeclaringClass().getDeclaredMethod(descriptor.getName(),
					descriptor.getParameterTypes());
			if (!java.lang.reflect.Modifier.isPublic(descriptor.getDeclaringClass().getModifiers())) {
				method.trySetAccessible();
			}
			return method;
		} catch (NoSuchMethodException | SecurityException e) {
			return null;
		}
	}

	/**
	 * Resolve the {@link Field} described by given descriptor, if available.
	 * @param descriptor The member descriptor (may be null)
	 * @return The resolved field, or <code>null</code> if not available
	 */
	private static Field resolveField(MemberDescriptor descriptor) {
		if (descriptor == null) {
			return null;
		}
		try {
			return descriptor.getDeclaringClass().getDeclaredField(descriptor.getName());
		} catch (NoSuchFieldException | SecurityException e) {
			return null;
		}
	}

	/**
	 * Serializable descriptor of a class member ({@link Method} or {@link Field}), used to restore the member
	 * reflection reference when the transient member reference is not available, for example after property
	 * deserialization.
	 */
	private static final class MemberDescriptor implements java.io.Serializable {

		private static final long serialVersionUID = 1L;

		private static final Class<?>[] NO_PARAMETERS = new Class<?>[0];

		private final Class<?> declaringClass;
		private final String name;
		private final Class<?>[] parameterTypes;

		private MemberDescriptor(Class<?> declaringClass, String name, Class<?>[] parameterTypes) {
			this.declaringClass = declaringClass;
			this.name = name;
			this.parameterTypes = parameterTypes;
		}

		static MemberDescriptor create(Method method) {
			return (method != null) ? new MemberDescriptor(method.getDeclaringClass(), method.getName(),
					method.getParameterTypes()) : null;
		}

		static MemberDescriptor create(Field field) {
			return (field != null)
					? new MemberDescriptor(field.getDeclaringClass(), field.getName(), NO_PARAMETERS)
					: null;
		}

		Class<?> getDeclaringClass() {
			return declaringClass;
		}

		String getName() {
			return name;
		}

		Class<?>[] getParameterTypes() {
			return parameterTypes;
		}

	}

}
