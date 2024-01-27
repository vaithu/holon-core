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
package com.holonplatform.core.internal.property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.holonplatform.core.Path;
import com.holonplatform.core.exceptions.TypeMismatchException;
import com.holonplatform.core.internal.utils.ObjectUtils;
import com.holonplatform.core.internal.utils.TypeUtils;
import com.holonplatform.core.property.PathPropertySetAdapter;
import com.holonplatform.core.property.Property;
import com.holonplatform.core.property.PropertySet;

/**
 * Default {@link PathPropertySetAdapter} implementation.
 *
 * @since 5.1.0
 */
@SuppressWarnings("rawtypes")
public class DefaultPathPropertySetAdapter implements PathPropertySetAdapter {

	/**
	 * The property set
	 */
	private final PropertySet<? extends Property> propertySet;

	/**
	 * Default {@link PathConverter}
	 */
	private PathConverter pathConverter = DefaultPathConverter.INSTANCE;

	/**
	 * Default {@link PathMatcher}
	 */
	private PathMatcher pathMatcher = DefaultPathMatcher.INSTANCE;

	private final Map<Path, Property> pathPropertyCache;
	private Map<String, Property> namePropertyCache;

	/**
	 * Constructor.
	 * @param propertySet The adapted property set (not null)
	 */
	public DefaultPathPropertySetAdapter(PropertySet<? extends Property> propertySet) {
		super();
		ObjectUtils.argumentNotNull(propertySet, "PropertySet must be not null");
		this.propertySet = propertySet;
		this.pathPropertyCache = new HashMap<>(propertySet.size());
	}

	/**
	 * Get the adapted property set.
	 * @return the property set
	 */
	protected PropertySet<? extends Property> getPropertySet() {
		return propertySet;
	}

	/**
	 * Get the path converter.
	 * @return the path converter
	 */
	protected PathConverter getPathConverter() {
		return pathConverter;
	}

	/**
	 * Get the path matcher.
	 * @return the path matcher
	 */
	protected PathMatcher getPathMatcher() {
		return pathMatcher;
	}

	/**
	 * Set the path converter.
	 * @param pathConverter the path converter to set (not null)
	 */
	public void setPathConverter(PathConverter pathConverter) {
		ObjectUtils.argumentNotNull(pathConverter, "PathConverter must be not null");
		this.pathConverter = pathConverter;
	}

	/**
	 * Set the path matcher.
	 * @param pathMatcher the path matcher to set (not null)
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		ObjectUtils.argumentNotNull(pathMatcher, "PathMatcher must be not null");
		this.pathMatcher = pathMatcher;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.property.PathPropertySetAdapter#contains(com.holonplatform.core.Path)
	 */
	@Override
	public boolean contains(Path<?> path) {
		return getProperty(path).isPresent();
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.property.PathPropertySetAdapter#getProperty(com.holonplatform.core.Path)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<Property<T>> getProperty(Path<T> path) {
		ObjectUtils.argumentNotNull(path, "Path must be not null");
		return Optional.ofNullable(pathPropertyCache.computeIfAbsent(path, p -> {
			for (Property property : getPropertySet()) {
				Optional<Path<?>> propertyPath = getPathConverter().convert(property);
				if (propertyPath.isPresent() && getPathMatcher().match(propertyPath.get(), p)) {
					return property;
				}
			}
			return null;
		}));
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.property.PathPropertySetAdapter#getPath(com.holonplatform.core.property.Property)
	 */
	@Override
	public <T> Optional<Path<T>> getPath(Property<T> property) {
		ObjectUtils.argumentNotNull(property, "Property must be not null");
		return getPropertySet().contains(property) ? getPathConverter().convert(property) : Optional.empty();
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.property.PathPropertySetAdapter#getPathIdentifiers()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<Path<?>> getPathIdentifiers() {
		Set<Path<?>> pathIds = new HashSet<>(getPropertySet().getIdentifiers().size());
		for (Property id : getPropertySet().getIdentifiers()) {
			Optional<Path<?>> path = getPathConverter().convert(id);
			if (path.isEmpty()) {
				return Collections.emptySet();
			}
			pathIds.add(path.get());
		}
		return pathIds;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.property.PathPropertySetAdapter#pathStream()
	 */
	@Override
	public Stream<Path<?>> paths() {
		List<Path<?>> paths = new ArrayList<>(getPropertySet().size());
		for (Property<?> property : getPropertySet()) {
			getPathConverter().convert(property).ifPresent(p -> paths.add(p));
		}
		return paths.stream();
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.property.PathPropertySetAdapter#propertyPaths()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Stream<PropertyPath<?>> propertyPaths() {
		final List<PropertyPath<?>> pps = new ArrayList<>(getPropertySet().size());
		for (Property<?> property : getPropertySet()) {
			getPathConverter().convert(property).ifPresent(path -> pps.add(new DefaultPropertyPath(property, path)));
		}
		return pps.stream();
	}

	// ------- by name

	/**
	 * Get the name-property cache.
	 * @return the the name-property cache
	 */
	protected Map<String, Property> getNamePropertyCache() {
		if (namePropertyCache == null) {
			namePropertyCache = new HashMap<>(getPropertySet().size());
		}
		return namePropertyCache;
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.property.PathPropertySetAdapter#contains(java.lang.String)
	 */
	@Override
	public boolean contains(String name) {
		return getProperty(name).isPresent();
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.property.PathPropertySetAdapter#getProperty(java.lang.String, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<Property<T>> getProperty(String name, Class<T> type) {
		ObjectUtils.argumentNotNull(name, "Property name must be not null");
		ObjectUtils.argumentNotNull(type, "Property type must be not null");
		Optional<Property<?>> property = getProperty(name);
		property.ifPresent(p -> {
			if (!TypeUtils.isAssignable(p.getType(), type)) {
				throw new TypeMismatchException("Property type " + p.getType().getName()
						+ " is not compatible with required type " + type.getName());
			}
		});
		return property.map(p -> (Property<T>) p);
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.property.PathPropertySetAdapter#getProperty(java.lang.String)
	 */
	@Override
	public Optional<Property<?>> getProperty(String name) {
		ObjectUtils.argumentNotNull(name, "Property name must be not null");
		return Optional.ofNullable(getNamePropertyCache().computeIfAbsent(name, pn -> {
			for (Property property : getPropertySet()) {
				if (pn.equals(property.getName())) {
					return property;
				}
			}
			return null;
		}));
	}

	/*
	 * (non-Javadoc)
	 * @see com.holonplatform.core.property.PathPropertySetAdapter#nameStream()
	 */
	@Override
	public Stream<String> names() {
		return getPropertySet().stream().filter(p -> p.getName() != null).map(p -> p.getName());
	}

	// ------- Internal types

	/**
	 * Default {@link PropertyPath} implementation.
	 * 
	 * @param <T> Property and path type
	 */
	public static class DefaultPropertyPath<T> implements PropertyPath<T> {

		private final Property<T> property;
		private final Path<T> path;

		public DefaultPropertyPath(Property<T> property, Path<T> path) {
			super();
			this.property = property;
			this.path = path;
		}

		@Override
		public Property<T> getProperty() {
			return property;
		}

		@Override
		public Path<T> getPath() {
			return path;
		}

	}

	// ------- Builder

	/**
	 * Default {@link PathPropertySetAdapterBuilder}.
	 */
	public static class DefaultPathPropertySetAdapterBuilder extends
			AbstractPathPropertySetAdapterBuilder<PathPropertySetAdapterBuilder, PathPropertySetAdapter, DefaultPathPropertySetAdapter>
			implements PathPropertySetAdapterBuilder {

		/**
		 * Constructor.
		 * @param propertySet The adapted property set (not null)
		 */
		public DefaultPathPropertySetAdapterBuilder(PropertySet<? extends Property> propertySet) {
			super(new DefaultPathPropertySetAdapter(propertySet));
		}

		/*
		 * (non-Javadoc)
		 * @see com.holonplatform.core.internal.property.AbstractPathPropertySetAdapterBuilder#getBuilder()
		 */
		@Override
		protected PathPropertySetAdapterBuilder getBuilder() {
			return this;
		}

		/*
		 * (non-Javadoc)
		 * @see com.holonplatform.core.property.PathPropertySetAdapter.Builder#build()
		 */
		@Override
		public PathPropertySetAdapter build() {
			return instance;
		}

	}

}
