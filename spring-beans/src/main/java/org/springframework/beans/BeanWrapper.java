/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans;

import java.beans.PropertyDescriptor;

/**
 * The central interface of Spring's low-level JavaBeans infrastructure.
 * <p>
 * <p>Typically not used directly but rather implicitly via a
 * {@link org.springframework.beans.factory.BeanFactory} or a
 * {@link org.springframework.validation.DataBinder}.
 * <p>
 * <p>Provides operations to analyze and manipulate standard JavaBeans:
 * the ability to get and set property values (individually or in bulk),
 * get property descriptors, and query the readability/writability of properties.
 * <p>
 * <p>This interface supports <b>nested properties</b> enabling the setting
 * of properties on subproperties to an unlimited depth.
 * <p>
 * <p>A BeanWrapper's default for the "extractOldValueForEditor" setting
 * is "false", to avoid side effects caused by getter method invocations.
 * Turn this to "true" to expose present property values to custom editors.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see PropertyAccessor
 * @see PropertyEditorRegistry
 * @see PropertyAccessorFactory#forBeanPropertyAccess
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.validation.BeanPropertyBindingResult
 * @see org.springframework.validation.DataBinder#initBeanPropertyAccess()
 * @since 13 April 2001
 */
//pattern adapter 想通过名称get,set对象属性,但是原生obj不支持,定义一个适配器接口
public interface BeanWrapper extends ConfigurablePropertyAccessor {

    /**
     * Return the bean instance wrapped by this object, if any.
     *
     * @return the bean instance, or {@code null} if none set
     */
    Object getWrappedInstance();

    /**
     * Return the type of the wrapped JavaBean object.
     *
     * @return the type of the wrapped bean instance,
     * or {@code null} if no wrapped object has been set
     */
    Class<?> getWrappedClass();

    /**
     * Obtain the PropertyDescriptors for the wrapped object
     * (as determined by standard JavaBeans introspection).
     *
     * @return the PropertyDescriptors for the wrapped object
     */
    PropertyDescriptor[] getPropertyDescriptors();

    /**
     * Obtain the property descriptor for a specific property
     * of the wrapped object.
     *
     * @param propertyName the property to obtain the descriptor for
     *                     (may be a nested path, but no indexed/mapped property)
     * @return the property descriptor for the specified property
     * @throws InvalidPropertyException if there is no such property
     */
    PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException;

    /**
     * Return whether "auto-growing" of nested paths has been activated.
     */
    boolean isAutoGrowNestedPaths();

    /**
     * Set whether this BeanWrapper should attempt to "auto-grow" a
     * nested path that contains a {@code null} value.
     * <p>If {@code true}, a {@code null} path location will be populated
     * with a default object value and traversed instead of resulting in a
     * {@link NullValueInNestedPathException}. Turning this flag on also enables
     * auto-growth of collection elements when accessing an out-of-bounds index.
     * <p>Default is {@code false} on a plain BeanWrapper.
     */
    void setAutoGrowNestedPaths(boolean autoGrowNestedPaths);

    /**
     * Return the limit for array and collection auto-growing.
     */
    int getAutoGrowCollectionLimit();

    /**
     * Specify a limit for array and collection auto-growing.
     * <p>Default is unlimited on a plain BeanWrapper.
     */
    void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

}
