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

package org.springframework.beans.factory.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic registry for shared bean instances, implementing the
 * {@link org.springframework.beans.factory.config.SingletonBeanRegistry}.
 * Allows for registering singleton instances that should be shared
 * for all callers of the registry, to be obtained via bean name.
 * <p>
 * 通用的共享bean实例注册器,实现SingletonBeanRegistry接口,允许注册被调用者共享的实例,只要通过bean名称即可
 * <p>Also supports registration of
 * {@link org.springframework.beans.factory.DisposableBean} instances,
 * (which might or might not correspond to registered singletons),
 * to be destroyed on shutdown of the registry. Dependencies between
 * beans can be registered to enforce an appropriate shutdown order.
 * <p>
 * 也支持DisposableBean实例的注册,它们会在注册器关闭时被销毁.bean之间的关系可以被注册，用来保证它们之间的关闭顺序
 * <p>This class mainly serves as base class for
 * {@link org.springframework.beans.factory.BeanFactory} implementations,
 * factoring out the common management of singleton bean instances. Note that
 * the {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * interface extends the {@link SingletonBeanRegistry} interface.
 * <p>
 * 此类主要用来作为beanFactory实现的基础类,用来统一单例实例的管理.请注意的是:ConfigurableBeanFactory接口实现了SingletonBeanRegistry
 * <p>Note that this class assumes neither a bean definition concept
 * nor a specific creation process for bean instances, in contrast to
 * {@link AbstractBeanFactory} and {@link DefaultListableBeanFactory}
 * (which inherit from it). Can alternatively also be used as a nested
 * helper to delegate to.
 * <br/>
 * 请注意的是，与AbstractBeanFactory和DefaultListableBeanFactory相比，
 * 此类既不表示beanDefinition相关的概念,也不表示特定bean的创建过程.
 * 此外，此类也可以选择作为内部委托帮助类使用
 *
 * @author Juergen Hoeller
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 * @since 2.0
 */
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

    private static final Log LOGGER = LogFactory.getLog(DefaultSingletonBeanRegistry.class);


    /**
     * Internal marker for a null singleton object:
     * used as marker value for concurrent Maps (which don't support null values).
     * <br/>
     * 针对null的单例对象在内部的一个标识符
     * <br/>
     * 被用在并发map作为一个标识符,并发map不支持null元素
     */
    protected static final Object NULL_OBJECT = new Object();

    /**
     * Cache of singleton objects: bean name --> bean instance
     * <br/>
     * 单例对象的缓存:beanName到bean实例的存储结构
     */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>(64);

    /**
     * Cache of singleton factories: bean name --> ObjectFactory
     * <br/>
     * bean名称和ObjectFactory的缓存
     */
    private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<String, ObjectFactory<?>>(16);

    /**
     * Cache of early singleton objects: bean name --> bean instance
     * <br/>
     * bean名称和bean实例缓存,  提前创建的单例对象
     */
    private final Map<String, Object> earlySingletonObjects = new HashMap<String, Object>(16);

    /**
     * Set of registered singletons, containing the bean names in registration order
     * <br/>
     * 已注册的单例实例名称集合,集合里的数据顺序就是注册顺序
     */
    private final Set<String> registeredSingletons = new LinkedHashSet<String>(64);

    /**
     * Names of beans that are currently in creation (using a ConcurrentHashMap as a Set)
     */
    private final Map<String, Boolean> singletonsCurrentlyInCreation = new ConcurrentHashMap<String, Boolean>(16);

    /**
     * Names of beans currently excluded from in creation checks (using a ConcurrentHashMap as a Set)
     * <br/>
     * 不需要校验正在创建的bean名称列表
     */
    private final Map<String, Boolean> inCreationCheckExclusions = new ConcurrentHashMap<String, Boolean>(16);
    /**
     * Disposable bean instances: bean name --> disposable instance
     */
    private final Map<String, Object> disposableBeans = new LinkedHashMap<String, Object>();
    /**
     * Map between containing bean names: bean name --> Set of bean names that the bean contains
     */
    private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<String, Set<String>>(16);
    /**
     * Map between dependent bean names: bean name --> Set of dependent bean names
     * <br/>
     * 依赖的bean名称的映射关系，bean名称->依赖前面bean的所有bean名称
     */
    private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<String, Set<String>>(64);
    /**
     * Map between depending bean names: bean name --> Set of bean names for the bean's dependencies
     * <br/>
     * bean名称->此bean依赖的所有bean的名称
     */
    private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<String, Set<String>>(64);
    /**
     * List of suppressed Exceptions, available for associating related causes
     * <br/>
     * 被抑制的或者没有被跑出的异常列表,可用来关联有相关性的异常
     */
    private Set<Exception> suppressedExceptions;
    /**
     * Flag that indicates whether we're currently within destroySingletons
     * <br/>
     * 标志位,用来表示当前对象是否处于销毁单例实例的过程中
     */
    private boolean singletonsCurrentlyInDestruction = false;

    public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
        Assert.hasText(beanName, "'beanName' must not be null");
        Assert.notNull(singletonObject, "singletonObj不能为空!");
        //因为singletonObjects存储是全局变量,读写单例的存储时，需要并发控制
        //singletonObjects是主存储,所有修改其它存储都必须先修改它。所以锁就加在singletonObjects即可
        synchronized (this.singletonObjects) {
            Object oldObject = this.singletonObjects.get(beanName);
            if (oldObject != null) {
                throw new IllegalStateException("Could not register object [" + singletonObject + "] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
            }
            addSingleton(beanName, singletonObject);
        }
    }

    /**
     * Add the given singleton object to the singleton cache of this factory.
     * <br/>
     * 添加指定的单例到这个工厂的单例缓存里去
     * <p>To be called for eager registration of singletons.
     * <br/>
     * 在优先注册单例时被调用
     *
     * @param beanName        the name of the bean
     * @param singletonObject the singleton object
     */
    private void addSingleton(String beanName, Object singletonObject) {
        //synchronized可以换为synchronized(getSingletonMutex)
        synchronized (this.singletonObjects) {
            this.singletonObjects.put(beanName, (singletonObject != null ? singletonObject : NULL_OBJECT));
            this.singletonFactories.remove(beanName);
            this.earlySingletonObjects.remove(beanName);
            this.registeredSingletons.add(beanName);
        }
    }

    /**
     * Add the given singleton factory for building the specified singleton
     * if necessary.
     * <br/>
     * 添加指定的单例工厂来构建指定的单例
     * * <p>To be called for eager registration of singletons, e.g. to be able to
     * resolve circular references.
     * <br/>
     * 在优先注册单例对象时被调用.例如，用来解决循环引用
     *
     * @param beanName         the name of the bean
     * @param singletonFactory the factory for the singleton object
     */
    protected void addSingletonFactory(String beanName, ObjectFactory singletonFactory) {
        Assert.notNull(singletonFactory, "Singleton factory must not be null");
        Assert.hasText(beanName, "beanName不能为空!");
        synchronized (this.singletonObjects) {
            if (!this.singletonObjects.containsKey(beanName)) {
                this.singletonFactories.put(beanName, singletonFactory);
                this.earlySingletonObjects.remove(beanName);
                this.registeredSingletons.add(beanName);
            }
        }
    }

    public Object getSingleton(String beanName) {
        return getSingleton(beanName, true);
    }

    /**
     * Return the (raw) singleton object registered under the given name.
     * <p>Checks already instantiated singletons and also allows for an early
     * reference to a currently created singleton (resolving a circular reference).
     * <br/>
     * 返回原始的注册在指定名称下面的单例对象
     * <br/>
     * 此方法首先会检测已经实例化的单例对象,如果未找到,同时对象正在创建中,则也可以返回一个early引用
     *
     * @param beanName            the name of the bean to look for
     * @param allowEarlyReference whether early references should be created or not
     * @return the registered singleton object, or {@code null} if none found
     */
    protected Object getSingleton(String beanName, boolean allowEarlyReference) {
        Object singletonObject = this.singletonObjects.get(beanName);
        if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
            synchronized (this.singletonObjects) {
                //double-check here???
                singletonObject = this.earlySingletonObjects.get(beanName);
                if (singletonObject == null && allowEarlyReference) {
                    ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                    if (singletonFactory != null) {
                        singletonObject = singletonFactory.getObject();
                        this.earlySingletonObjects.put(beanName, singletonObject);
                        this.singletonFactories.remove(beanName);
                    }
                }
            }
        }
        return (singletonObject != NULL_OBJECT ? singletonObject : null);
    }

    /**
     * Return the (raw) singleton object registered under the given name,
     * creating and registering a new one if none registered yet.
     * <br/>
     * 返回指定bean名称下的单例对象,如果单例对象没有注册、则创建并注册一个新的
     *
     * @param beanName         the name of the bean
     * @param singletonFactory the ObjectFactory to lazily create the singleton
     *                         with, if necessary
     * @return the registered singleton object
     */
    public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
        Assert.hasText(beanName, "'beanName' must not be null");
        synchronized (this.singletonObjects) {
            Object singletonObject = this.singletonObjects.get(beanName);
            if (singletonObject == null) {
                if (this.singletonsCurrentlyInDestruction) {
                    throw new BeanCreationNotAllowedException(beanName, "Singleton bean creation not allowed while the singletons of this factory are in destruction " +
                            "(Do not request a bean from a BeanFactory in a destroy method implementation!)");
                }
                beforeSingletonCreation(beanName);
                boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
                if (recordSuppressedExceptions) {
                    this.suppressedExceptions = new LinkedHashSet<Exception>();
                }
                try {
                    singletonObject = singletonFactory.getObject();
                } catch (BeanCreationException ex) {
                    if (recordSuppressedExceptions) {
                        for (Exception suppressedException : this.suppressedExceptions) {
                            ex.addRelatedCause(suppressedException);
                        }
                    }
                    throw ex;
                } finally {
                    if (recordSuppressedExceptions) {
                        this.suppressedExceptions = null;
                    }
                    afterSingletonCreation(beanName);
                }
                addSingleton(beanName, singletonObject);
            }
            return (singletonObject != NULL_OBJECT ? singletonObject : null);
        }
    }

    /**
     * Register an Exception that happened to get suppressed during the creation of a
     * singleton bean instance, e.g. a temporary circular reference resolution problem.
     *
     * @param ex the Exception to register
     */
    protected void onSuppressedException(Exception ex) {
        synchronized (this.singletonObjects) {
            if (this.suppressedExceptions != null) {
                this.suppressedExceptions.add(ex);
            }
        }
    }

    /**
     * Remove the bean with the given name from the singleton cache of this factory,
     * to be able to clean up eager registration of a singleton if creation failed.
     *
     * @param beanName the name of the bean
     * @see #getSingletonMutex()
     */
    protected void removeSingleton(String beanName) {
        synchronized (this.singletonObjects) {
            this.singletonObjects.remove(beanName);
            this.singletonFactories.remove(beanName);
            this.earlySingletonObjects.remove(beanName);
            this.registeredSingletons.remove(beanName);
        }
    }

    public boolean containsSingleton(String beanName) {
        return (this.singletonObjects.containsKey(beanName));
    }

    public String[] getSingletonNames() {
        synchronized (this.singletonObjects) {
            return StringUtils.toStringArray(this.registeredSingletons);
        }
    }

    public int getSingletonCount() {
        synchronized (this.singletonObjects) {
            return this.registeredSingletons.size();
        }
    }


    public void setCurrentlyInCreation(String beanName, boolean inCreation) {
        Assert.hasText(beanName, "Bean name must not be null");
        if (!inCreation) {
            this.inCreationCheckExclusions.put(beanName, Boolean.TRUE);
        } else {
            this.inCreationCheckExclusions.remove(beanName);
        }
    }

    public boolean isCurrentlyInCreation(String beanName) {
        Assert.notNull(beanName, "Bean name must not be null");
        return (!this.inCreationCheckExclusions.containsKey(beanName) && isActuallyInCreation(beanName));
    }

    protected boolean isActuallyInCreation(String beanName) {
        return isSingletonCurrentlyInCreation(beanName);
    }

    /**
     * Return whether the specified singleton bean is currently in creation
     * (within the entire factory).
     *
     * @param beanName the name of the bean
     */
    public boolean isSingletonCurrentlyInCreation(String beanName) {
        return this.singletonsCurrentlyInCreation.containsKey(beanName);
    }

    /**
     * Callback before singleton creation.
     * <p>The default implementation register the singleton as currently in creation.
     * <br/>
     * 在单例实例创建前的回调函数
     *
     * @param beanName the name of the singleton about to be created
     * @see #isSingletonCurrentlyInCreation
     */
    protected void beforeSingletonCreation(String beanName) {
        if (!this.inCreationCheckExclusions.containsKey(beanName) &&
                this.singletonsCurrentlyInCreation.put(beanName, Boolean.TRUE) != null) {
            throw new BeanCurrentlyInCreationException(beanName);
        }
    }

    /**
     * Callback after singleton creation.
     * <p>The default implementation marks the singleton as not in creation anymore.
     * <br/>
     * 单例创建结束的回调函数
     * <br/>
     * 默认实现是标记单例不在创建阶段了
     *
     * @param beanName the name of the singleton that has been created
     * @see #isSingletonCurrentlyInCreation
     */
    protected void afterSingletonCreation(String beanName) {
        if (!this.inCreationCheckExclusions.containsKey(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
            throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
        }
    }


    /**
     * Add the given bean to the list of disposable beans in this registry.
     * <br/>
     * 添加disposableBean到此注册器中去
     * <p>Disposable beans usually correspond to registered singletons,
     * matching the bean name but potentially being a different instance
     * (for example, a DisposableBean adapter for a singleton that does not
     * naturally implement Spring's DisposableBean interface).
     * <br/>
     * disposableBean通常相对应已经注册的单例对象,它们都使用同一个bean名称，但是可能是不同的实例
     *
     * @param beanName the name of the bean
     * @param bean     the bean instance
     */
    public void registerDisposableBean(String beanName, DisposableBean bean) {
        synchronized (this.disposableBeans) {
            this.disposableBeans.put(beanName, bean);
        }
    }

    /**
     * Register a containment relationship between two beans,
     * e.g. between an inner bean and its containing outer bean.
     * <br/>
     * 注册两个bean之间的包含关系,包含  包含和被包含的bean
     * <p>Also registers the containing bean as dependent on the contained bean
     * in terms of destruction order.
     * <br/>
     * 同时,它也会注册包含的bean依赖被包含的bean这层关系，因为涉及到销毁的顺序问题
     *
     * @param containedBeanName  the name of the contained (inner) bean  被包含的bean名称
     * @param containingBeanName the name of the containing (outer) bean  包含的bean名称
     * @see #registerDependentBean
     */
    public void registerContainedBean(String containedBeanName, String containingBeanName) {
        synchronized (this.containedBeanMap) {
            Set<String> containedBeans = this.containedBeanMap.get(containingBeanName);
            if (containedBeans == null) {
                containedBeans = new LinkedHashSet<String>(8);
                this.containedBeanMap.put(containingBeanName, containedBeans);
            }
            containedBeans.add(containedBeanName);
        }
        registerDependentBean(containedBeanName, containingBeanName);
    }

    /**
     * Register a dependent bean for the given bean,
     * to be destroyed before the given bean is destroyed.
     *
     * @param beanName          the name of the bean
     * @param dependentBeanName the name of the dependent bean
     */
    public void registerDependentBean(String beanName, String dependentBeanName) {
        String canonicalName = canonicalName(beanName);
        synchronized (this.dependentBeanMap) {
            Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
            if (dependentBeans == null) {
                dependentBeans = new LinkedHashSet<String>(8);
                this.dependentBeanMap.put(canonicalName, dependentBeans);
            }
            dependentBeans.add(dependentBeanName);
        }
        synchronized (this.dependenciesForBeanMap) {
            Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(dependentBeanName);
            if (dependenciesForBean == null) {
                dependenciesForBean = new LinkedHashSet<String>(8);
                this.dependenciesForBeanMap.put(dependentBeanName, dependenciesForBean);
            }
            dependenciesForBean.add(canonicalName);
        }
    }

    /**
     * Determine whether a dependent bean has been registered for the given name.
     *
     * @param beanName the name of the bean to check
     */
    protected boolean hasDependentBean(String beanName) {
        return this.dependentBeanMap.containsKey(beanName);
    }

    /**
     * Return the names of all beans which depend on the specified bean, if any.
     *
     * @param beanName the name of the bean
     * @return the array of dependent bean names, or an empty array if none
     */
    public String[] getDependentBeans(String beanName) {
        Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
        if (dependentBeans == null) {
            return new String[0];
        }
        return StringUtils.toStringArray(dependentBeans);
    }

    /**
     * Return the names of all beans that the specified bean depends on, if any.
     *
     * @param beanName the name of the bean
     * @return the array of names of beans which the bean depends on,
     * or an empty array if none
     */
    public String[] getDependenciesForBean(String beanName) {
        Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
        if (dependenciesForBean == null) {
            return new String[0];
        }
        return dependenciesForBean.toArray(new String[dependenciesForBean.size()]);
    }

    public void destroySingletons() {
        synchronized (this.singletonObjects) {
            this.singletonsCurrentlyInDestruction = true;
        }

        String[] disposableBeanNames;
        synchronized (this.disposableBeans) {
            disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
        }
        for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
            destroySingleton(disposableBeanNames[i]);
        }

        this.containedBeanMap.clear();
        this.dependentBeanMap.clear();
        this.dependenciesForBeanMap.clear();

        synchronized (this.singletonObjects) {
            this.singletonObjects.clear();
            this.singletonFactories.clear();
            this.earlySingletonObjects.clear();
            this.registeredSingletons.clear();
            this.singletonsCurrentlyInDestruction = false;
        }
    }

    /**
     * Destroy the given bean. Delegates to {@code destroyBean}
     * if a corresponding disposable bean instance is found.
     * <br/>
     * 销毁指定的bean.委托销毁bean的操作给destroyBean实例,如果相对应的disposable实例被找到
     *
     * @param beanName the name of the bean
     * @see #destroyBean
     */
    public void destroySingleton(String beanName) {
        // Remove a registered singleton of the given name, if any.
        removeSingleton(beanName);

        // Destroy the corresponding DisposableBean instance.
        DisposableBean disposableBean;
        //写操作加锁
        synchronized (this.disposableBeans) {
            disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
        }
        destroyBean(beanName, disposableBean);
    }

    /**
     * Destroy the given bean. Must destroy beans that depend on the given
     * bean before the bean itself. Should not throw any exceptions.
     * <br/>
     * 销毁指定的bean.在销毁指定bean之前必须销毁所有依赖它的bean.
     * 此类不会抛出任何异常
     *
     * @param beanName the name of the bean
     * @param bean     the bean instance to destroy
     */
    private void destroyBean(String beanName, DisposableBean bean) {
        // Trigger destruction of dependent beans first...
        Set<String> dependencies = this.dependentBeanMap.remove(beanName);
        if (dependencies != null) {
            for (String dependentBeanName : dependencies) {
                destroySingleton(dependentBeanName);
            }
        }

        // Actually destroy the bean now...
        if (bean != null) {
            try {
                bean.destroy();
            } catch (Throwable ex) {
                LOGGER.error("Destroy method on bean with name '" + beanName + "' threw an exception", ex);
            }
        }

        // Trigger destruction of contained beans...
        Set<String> containedBeans = this.containedBeanMap.remove(beanName);
        if (containedBeans != null) {
            for (String containedBeanName : containedBeans) {
                destroySingleton(containedBeanName);
            }
        }

        // Remove destroyed bean from other beans' dependencies.
        //把被销毁的bean从依赖别的bean的存储中移除
        synchronized (this.dependentBeanMap) {
            for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Set<String>> entry = it.next();
                Set<String> dependenciesToClean = entry.getValue();
                dependenciesToClean.remove(beanName);
                if (dependenciesToClean.isEmpty()) {
                    it.remove();
                }
            }
        }

        // Remove destroyed bean's prepared dependency information.
        this.dependenciesForBeanMap.remove(beanName);
    }

    /**
     * Exposes the singleton mutex to subclasses and external collaborators.
     * <p>Subclasses should synchronize on the given Object if they perform
     * any sort of extended singleton creation phase. In particular, subclasses
     * should <i>not</i> have their own mutexes involved in singleton creation,
     * to avoid the potential for deadlocks in lazy-init situations.
     * <br/>
     * 对子类和外部协作者暴露单例数据操作的mutex。
     * <br/>
     * 如果子类需要对于单例创建有任何扩展的时候，子类都要使用此mutex进行数据的同步。
     * <br/>
     * 特别需要注意的是，子类不应该在单例创建的时候使用它们自己的mutex,以避免在懒初始化情况下出现死锁
     */
    public Object getSingletonMutex() {
        return this.singletonObjects;
    }

}
