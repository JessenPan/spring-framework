/*
 * Copyright 2002-2015 the original author or authors.
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

import org.apache.commons.logging.LogFactory;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Extension of the standard JavaBeans {@link PropertyDescriptor} class,
 * overriding {@code getPropertyType()} such that a generically declared
 * type variable will be resolved against the containing bean class.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
class GenericTypeAwarePropertyDescriptor extends PropertyDescriptor {

    private final Class<?> beanClass;

    private final Method readMethod;

    private final Method writeMethod;
    private final Class<?> propertyEditorClass;
    private volatile Set<Method> ambiguousWriteMethods;
    private MethodParameter writeMethodParameter;
    private Class<?> propertyType;


    public GenericTypeAwarePropertyDescriptor(Class<?> beanClass, String propertyName,
                                              Method readMethod, Method writeMethod, Class<?> propertyEditorClass)
            throws IntrospectionException {

        super(propertyName, null, null);

        if (beanClass == null) {
            throw new IntrospectionException("Bean class must not be null");
        }
        this.beanClass = beanClass;

        Method readMethodToUse = BridgeMethodResolver.findBridgedMethod(readMethod);
        Method writeMethodToUse = BridgeMethodResolver.findBridgedMethod(writeMethod);
        if (writeMethodToUse == null && readMethodToUse != null) {
            // Fallback: Original JavaBeans introspection might not have found matching setter
            // method due to lack of bridge method resolution, in case of the getter using a
            // covariant return type whereas the setter is defined for the concrete property type.
            Method candidate = ClassUtils.getMethodIfAvailable(
                    this.beanClass, "set" + StringUtils.capitalize(getName()), (Class<?>[]) null);
            if (candidate != null && candidate.getParameterTypes().length == 1) {
                writeMethodToUse = candidate;
            }
        }
        this.readMethod = readMethodToUse;
        this.writeMethod = writeMethodToUse;

        if (this.writeMethod != null && this.readMethod == null) {
            // Write method not matched against read method: potentially ambiguous through
            // several overloaded variants, in which case an arbitrary winner has been chosen
            // by the JDK's JavaBeans Introspector...
            Set<Method> ambiguousCandidates = new HashSet<Method>();
            for (Method method : beanClass.getMethods()) {
                if (method.getName().equals(writeMethodToUse.getName()) &&
                        !method.equals(writeMethodToUse) && !method.isBridge() &&
                        method.getParameterTypes().length == writeMethodToUse.getParameterTypes().length) {
                    ambiguousCandidates.add(method);
                }
            }
            if (!ambiguousCandidates.isEmpty()) {
                this.ambiguousWriteMethods = ambiguousCandidates;
            }
        }

        this.propertyEditorClass = propertyEditorClass;
    }


    public Class<?> getBeanClass() {
        return this.beanClass;
    }

    @Override
    public Method getReadMethod() {
        return this.readMethod;
    }

    @Override
    public Method getWriteMethod() {
        return this.writeMethod;
    }

    public Method getWriteMethodForActualAccess() {
        Set<Method> ambiguousCandidates = this.ambiguousWriteMethods;
        if (ambiguousCandidates != null) {
            this.ambiguousWriteMethods = null;
            LogFactory.getLog(GenericTypeAwarePropertyDescriptor.class).warn("Invalid JavaBean property '" +
                    getName() + "' being accessed! Ambiguous write methods found next to actually used [" +
                    this.writeMethod + "]: " + ambiguousCandidates);
        }
        return this.writeMethod;
    }

    public synchronized MethodParameter getWriteMethodParameter() {
        if (this.writeMethod == null) {
            return null;
        }
        if (this.writeMethodParameter == null) {
            this.writeMethodParameter = new MethodParameter(this.writeMethod, 0);
            GenericTypeResolver.resolveParameterType(this.writeMethodParameter, this.beanClass);
        }
        return this.writeMethodParameter;
    }

    @Override
    public synchronized Class<?> getPropertyType() {
        if (this.propertyType == null) {
            if (this.readMethod != null) {
                this.propertyType = GenericTypeResolver.resolveReturnType(this.readMethod, this.beanClass);
            } else {
                MethodParameter writeMethodParam = getWriteMethodParameter();
                if (writeMethodParam != null) {
                    this.propertyType = writeMethodParam.getParameterType();
                } else {
                    this.propertyType = super.getPropertyType();
                }
            }
        }
        return this.propertyType;
    }

    @Override
    public Class<?> getPropertyEditorClass() {
        return this.propertyEditorClass;
    }

}
