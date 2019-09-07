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

package org.springframework.beans.factory.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Support base class for singleton registries which need to handle
 * {@link org.springframework.beans.factory.FactoryBean} instances,
 * integrated with {@link DefaultSingletonBeanRegistry}'s singleton management.
 * <p>
 * 单例注册的基础支持类,主要用来处理factoryBean实例,和DefaultSingletonBeanRegistry的单例管理功能进行集成使用
 * <p>Serves as base class for {@link AbstractBeanFactory}.
 * <br/>
 * 主要用来作为AbstractBeanFactory的基类
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 */
public abstract class FactoryBeanRegistrySupport extends DefaultSingletonBeanRegistry {

    private static final Log LOGGER = LogFactory.getLog(FactoryBeanRegistrySupport.class);

    /**
     * Cache of singleton objects created by FactoryBeans: FactoryBean name --> object
     * <br/>
     * 通过factoryBean创建的单例对象缓存:工厂bean名称
     */
    private final Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<String, Object>(16);


    /**
     * Determine the type for the given FactoryBean.
     * <br/>
     * 获取factoryBean对应的类型
     *
     * @param factoryBean the FactoryBean instance to check
     * @return the FactoryBean's object type,
     * or {@code null} if the type cannot be determined yet
     */
    protected Class<?> getTypeForFactoryBean(final FactoryBean<?> factoryBean) {
        try {
            return factoryBean.getObjectType();
        } catch (Throwable ex) {
            return null;
        }
    }

    /**
     * Obtain an object to expose from the given FactoryBean, if available
     * in cached form. Quick check for minimal synchronization.
     * <br/>
     * 从指定的factoryBean获取一个对象object,如果能从缓存中获取到.
     *
     * @param beanName the name of the bean
     * @return the object obtained from the FactoryBean,
     * or {@code null} if not available
     */
    protected Object getCachedObjectForFactoryBean(String beanName) {
        Object object = this.factoryBeanObjectCache.get(beanName);
        return (object != NULL_OBJECT ? object : null);
    }

    /**
     * Obtain an object to expose from the given FactoryBean.
     * <br/>
     * 从指定的factoryBean中获取对象
     *
     * @param factory           the FactoryBean instance
     * @param beanName          the name of the bean
     * @param shouldPostProcess whether the bean is subject to post-processing
     * @return the object obtained from the FactoryBean
     * @throws BeanCreationException if FactoryBean object creation failed
     * @see org.springframework.beans.factory.FactoryBean#getObject()
     */
    protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName, boolean shouldPostProcess) {
        if (factory.isSingleton() && containsSingleton(beanName)) {
            synchronized (getSingletonMutex()) {
                Object object = this.factoryBeanObjectCache.get(beanName);
                if (object == null) {
                    object = doGetObjectFromFactoryBean(factory, beanName);
                    // Only post-process and store if not put there already during getObject() call above
                    // (e.g. because of circular reference processing triggered by custom getBean calls)
                    //如果在上述调用期间没有其它线程把对应的对象放入进入缓存中时，才进行后置处理以及存储。
                    //例如:被自定义的getBean调用所触发的循环引用
                    Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
                    if (alreadyThere != null) {
                        object = alreadyThere;
                    } else {
                        if (object != null && shouldPostProcess) {
                            try {
                                object = postProcessObjectFromFactoryBean(object, beanName);
                            } catch (Throwable ex) {
                                throw new BeanCreationException(beanName, "Post-processing of FactoryBean's singleton object failed", ex);
                            }
                        }
                        this.factoryBeanObjectCache.put(beanName, (object != null ? object : NULL_OBJECT));
                    }
                }
                return (object != NULL_OBJECT ? object : null);
            }
        } else {
            Object object = doGetObjectFromFactoryBean(factory, beanName);
            if (object != null && shouldPostProcess) {
                try {
                    object = postProcessObjectFromFactoryBean(object, beanName);
                } catch (Throwable ex) {
                    throw new BeanCreationException(beanName, "Post-processing of FactoryBean's object failed", ex);
                }
            }
            return object;
        }
    }

    /**
     * Obtain an object to expose from the given FactoryBean.
     * <br/>
     * 根据指定的factoryBean获取到对应的object
     *
     * @param factory  the FactoryBean instance
     * @param beanName the name of the bean
     * @return the object obtained from the FactoryBean
     * @throws BeanCreationException if FactoryBean object creation failed
     * @see org.springframework.beans.factory.FactoryBean#getObject()
     */
    private Object doGetObjectFromFactoryBean(final FactoryBean<?> factory, final String beanName) throws BeanCreationException {

        Object object;
        try {
            object = factory.getObject();
        } catch (FactoryBeanNotInitializedException ex) {
            throw new BeanCurrentlyInCreationException(beanName, ex.toString());
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
        }

        // Do not accept a null value for a FactoryBean that's not fully
        // initialized yet: Many FactoryBeans just return null then.
        if (object == null && isSingletonCurrentlyInCreation(beanName)) {
            throw new BeanCurrentlyInCreationException(beanName, "FactoryBean which is currently in creation returned null from getObject");
        }
        return object;
    }

    /**
     * Post-process the given object that has been obtained from the FactoryBean.
     * The resulting object will get exposed for bean references.
     * <br/>
     * 后置处理已经从factoryBean获取到的对象
     * <p>The default implementation simply returns the given object as-is.
     * Subclasses may override this, for example, to apply post-processors.
     * <br/>
     * 默认的实现只是简单的把传入的对象给返回回去,此类的子类应该重写此类，例如，应用后置处理器
     *
     * @param object   the object obtained from the FactoryBean.
     * @param beanName the name of the bean
     * @return the object to expose
     * @throws org.springframework.beans.BeansException if any post-processing failed
     */
    protected Object postProcessObjectFromFactoryBean(Object object, String beanName) throws BeansException {
        return object;
    }

    /**
     * Get a FactoryBean for the given bean if possible.
     * <br/>
     * 找到指定的bean的对应的FactoryBean
     *
     * @param beanName     the name of the bean
     * @param beanInstance the corresponding bean instance
     * @return the bean instance as FactoryBean
     * @throws BeansException if the given bean cannot be exposed as a FactoryBean
     */
    protected FactoryBean<?> getFactoryBean(String beanName, Object beanInstance) throws BeansException {
        if (!(beanInstance instanceof FactoryBean)) {
            throw new BeanCreationException(beanName, "Bean instance of type [" + beanInstance.getClass() + "] is not a FactoryBean");
        }
        return (FactoryBean<?>) beanInstance;
    }

    /**
     * Overridden to clear the FactoryBean object cache as well.
     * <br/>
     * 方法重载,用来移除factoryBean的缓存
     */
    @Override
    protected void removeSingleton(String beanName) {
        super.removeSingleton(beanName);
        this.factoryBeanObjectCache.remove(beanName);
    }
    
}
