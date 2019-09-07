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

package org.springframework.beans.factory.config;

/**
 * Interface that defines a registry for shared bean instances.
 * Can be implemented by {@link org.springframework.beans.factory.BeanFactory}
 * implementations in order to expose their singleton management facility
 * in a uniform manner.
 * <p>
 * 用来注册共享实例的接口.可以被BeanFactory接口实现,使得beanFactory可以用一种统一的方式来对外提供单例管理能力
 * <p>The {@link ConfigurableBeanFactory} interface extends this interface.
 * <br/>ConfigurableBeanFactory实现了此接口
 *
 * @author Juergen Hoeller
 * @see ConfigurableBeanFactory
 * @see org.springframework.beans.factory.support.DefaultSingletonBeanRegistry
 * @see org.springframework.beans.factory.support.AbstractBeanFactory
 * @since 2.0
 */
public interface SingletonBeanRegistry {

    /**
     * Register the given existing object as singleton in the bean registry,
     * under the given bean name.
     * <br/>
     * 把已经存在的bean对象作为单例进行注册
     * <p>The given instance is supposed to be fully initialized; the registry
     * will not perform any initialization callbacks (in particular, it won't
     * call InitializingBean's {@code afterPropertiesSet} method).
     * The given instance will not receive any destruction callbacks
     * (like DisposableBean's {@code destroy} method) either.
     * <br/>
     * 指定的实例应当被完全初始化了,注册器不会再进行任何初始化回调函数的调用了。指定的实例也不会收到任何销毁回调函数的调用了.
     * <p>When running within a full BeanFactory: <b>Register a bean definition
     * instead of an existing instance if your bean is supposed to receive
     * initialization and/or destruction callbacks.</b>
     * <br/>
     * 当在一个全初始化的BeanFactory背景下使用此接口时:如果你注册的bean希望接收到初始化和销毁回调时，你应该注册一个beanDefinition
     * <p>Typically invoked during registry configuration, but can also be used
     * for runtime registration of singletons. As a consequence, a registry
     * implementation should synchronize singleton access; it will have to do
     * this anyway if it supports a BeanFactory's lazy initialization of singletons.
     * <br/>
     * 此方法主要在配置注册的时候被调用，但是也可以被用来在运行时注册单例对象.
     * 要实现运行时注册的能力,注册器的实现必须进行并发控制单例对象的访问。只要它支持单例对象的懒初始化能力，
     * 它就必须进行并发控制。
     *
     * @param beanName        the name of the bean
     * @param singletonObject the existing singleton object
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
     * @see org.springframework.beans.factory.DisposableBean#destroy
     * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#registerBeanDefinition
     */
    void registerSingleton(String beanName, Object singletonObject);

    /**
     * Return the (raw) singleton object registered under the given name.
     * <br/>
     * 返回指定bean名称对应的单例对象
     * <p>Only checks already instantiated singletons; does not return an Object
     * for singleton bean definitions which have not been instantiated yet.
     * <br/>
     * 只检查已经实例化的单例对象,不返回还没有实例化的指定bean的beanDefinition
     * <p>The main purpose of this method is to access manually registered singletons
     * (see {@link #registerSingleton}). Can also be used to access a singleton
     * defined by a bean definition that already been created, in a raw fashion.
     * <br/>
     * 此方法主要的作用是访问手动注册的单例.同时，也可以用来访问通过beanDefinition定义的，已经通过原始方式创建的单例bean
     * <p><b>NOTE:</b> This lookup method is not aware of FactoryBean prefixes or aliases.
     * You need to resolve the canonical bean name first before obtaining the singleton instance.
     * <br/>
     * 注意：此方法不会关注FactoryBean的前缀,即&.你需要手动去在获取单例实例之前手动转换
     *
     * @param beanName the name of the bean to look for
     * @return the registered singleton object, or {@code null} if none found
     * @see ConfigurableListableBeanFactory#getBeanDefinition
     */
    Object getSingleton(String beanName);

    /**
     * Check if this registry contains a singleton instance with the given name.
     * <br/>
     * 校验此注册器是否包含指定名字的单例对象
     * <p>Only checks already instantiated singletons; does not return {@code true}
     * for singleton bean definitions which have not been instantiated yet.
     * <br/>
     * 此方法只会检测已经实例化的单例，当单例对应的beanDefinition还没有实例化时，它不会返回true
     * <p>The main purpose of this method is to check manually registered singletons
     * (see {@link #registerSingleton}). Can also be used to check whether a
     * singleton defined by a bean definition has already been created.
     * <br/>
     * 此方法的主要作用是检测已经注册的单例，但也可以用来检测通过beanDefinition定义的，但已经实例化的单例.
     * <p>To check whether a bean factory contains a bean definition with a given name,
     * use ListableBeanFactory's {@code containsBeanDefinition}. Calling both
     * {@code containsBeanDefinition} and {@code containsSingleton} answers
     * whether a specific bean factory contains a local bean instance with the given name.
     * <br/>
     * 如果需要检测beanFactory是否包含指定的beanDefinition,请使用ListableBeanFactory#containsBeanDefinition.
     * 调用containsBeanDefinition和containSingleton这两个方法可以得到指定的bean工厂是否包含指定名称的本地bean实例.
     * <p>Use BeanFactory's {@code containsBean} for general checks whether the
     * factory knows about a bean with a given name (whether manually registered singleton
     * instance or created by bean definition), also checking ancestor factories.
     * <br/>
     * 使用bean工厂的containBean来进行更加通用的判断工厂是否包含指定名称对应的bean，同时也会检测父工厂.
     * <p><b>NOTE:</b> This lookup method is not aware of FactoryBean prefixes or aliases.
     * You need to resolve the canonical bean name first before checking the singleton status.
     *
     * @param beanName the name of the bean to look for
     * @return if this bean factory contains a singleton instance with the given name
     * @see #registerSingleton
     * @see org.springframework.beans.factory.ListableBeanFactory#containsBeanDefinition
     * @see org.springframework.beans.factory.BeanFactory#containsBean
     */
    boolean containsSingleton(String beanName);

    /**
     * Return the names of singleton beans registered in this registry.
     * <p>Only checks already instantiated singletons; does not return names
     * for singleton bean definitions which have not been instantiated yet.
     * <p>The main purpose of this method is to check manually registered singletons
     * (see {@link #registerSingleton}). Can also be used to check which singletons
     * defined by a bean definition have already been created.
     * <br/>
     * 返回此注册器中所有注册的单例bean的名称
     *
     * @return the list of names as a String array (never {@code null})
     * @see #registerSingleton
     * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#getBeanDefinitionNames
     * @see org.springframework.beans.factory.ListableBeanFactory#getBeanDefinitionNames
     */
    String[] getSingletonNames();

    /**
     * Return the number of singleton beans registered in this registry.
     * <br/>
     * 返回此注册器中单例对象的数量
     * <p>Only checks already instantiated singletons; does not count
     * singleton bean definitions which have not been instantiated yet.
     * <br/>
     * 只会检测已经实例化的单例，不会计算还没有实例化的单例对象定义
     * <p>The main purpose of this method is to check manually registered singletons
     * (see {@link #registerSingleton}). Can also be used to count the number of
     * singletons defined by a bean definition that have already been created.
     * <br/>
     * 此方法的主要作用是检测手动注册的单例对象.但也可以被用来计算通过beanDefinition定义的，已经被实例化的单例数量
     *
     * @return the number of singleton beans
     * @see #registerSingleton
     * @see org.springframework.beans.factory.support.BeanDefinitionRegistry#getBeanDefinitionCount
     * @see org.springframework.beans.factory.ListableBeanFactory#getBeanDefinitionCount
     */
    int getSingletonCount();

}
