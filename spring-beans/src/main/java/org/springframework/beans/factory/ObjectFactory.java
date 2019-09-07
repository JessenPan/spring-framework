/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory;

import org.springframework.beans.BeansException;

/**
 * Defines a factory which can return an Object instance
 * (possibly shared or independent) when invoked.
 * <p>
 * 定义一个在调用时可以返回一个对象实例的工厂,此实例可以为共享的的，可以是独立的.
 * <p>This interface is typically used to encapsulate a generic factory which
 * returns a new instance (prototype) of some target object on each invocation.
 * <p>
 * 比较常见的一种实现是，此接口封装了一个更加通用的工厂类，它每次被调用时都会返回一个原型实例
 * <p>This interface is similar to {@link FactoryBean}, but implementations
 * of the latter are normally meant to be defined as SPI instances in a
 * {@link BeanFactory}, while implementations of this class are normally meant
 * to be fed as an API to other beans (through injection). As such, the
 * {@code getObject()} method has different exception handling behavior.
 * <br/>
 * 此接口和{@link FactoryBean}接口是十分类型的,但是后者更普通的意义是被用作BeanFactory里的一个实例SPI接口.
 * <br/>
 * 此外，这两个接口有着不同的异常处理表现
 *
 * @author Colin Sampaleanu
 * @see FactoryBean
 * @since 1.0.2
 */
public interface ObjectFactory<T> {

    /**
     * Return an instance (possibly shared or independent)
     * of the object managed by this factory.
     * <br/>
     * 返回此工厂管理的实例对象，该对象可能为共享的，也可能是独立的
     *
     * @return an instance of the bean (should never be {@code null})
     * @throws BeansException in case of creation errors
     */
    T getObject() throws BeansException;

}
