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

package org.springframework.context.event;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.OrderComparator;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract implementation of the {@link ApplicationEventMulticaster} interface,
 * providing the basic listener registration facility.
 * <p>
 * <p>Doesn't permit multiple instances of the same listener by default,
 * as it keeps listeners in a linked Set. The collection class used to hold
 * ApplicationListener objects can be overridden through the "collectionClass"
 * bean property.
 * <p>
 * <p>Implementing ApplicationEventMulticaster's actual {@link #multicastEvent} method
 * is left to subclasses. {@link SimpleApplicationEventMulticaster} simply multicasts
 * all events to all registered listeners, invoking them in the calling thread.
 * Alternative implementations could be more sophisticated in those respects.
 *
 * @author Juergen Hoeller
 * @see #getApplicationListeners(ApplicationEvent)
 * @see SimpleApplicationEventMulticaster
 * @since 1.2.3
 */
public abstract class AbstractApplicationEventMulticaster
        implements ApplicationEventMulticaster, BeanClassLoaderAware, BeanFactoryAware {

    private final ListenerRetriever defaultRetriever = new ListenerRetriever(false);

    private final Map<ListenerCacheKey, ListenerRetriever> retrieverCache = new ConcurrentHashMap<>(64);

    private ClassLoader beanClassLoader;

    private BeanFactory beanFactory;

    private Object retrievalMutex = this.defaultRetriever;


    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

    private BeanFactory getBeanFactory() {
        if (this.beanFactory == null) {
            throw new IllegalStateException("ApplicationEventMulticaster cannot retrieve listener beans " +
                    "because it is not associated with a BeanFactory");
        }
        return this.beanFactory;
    }

    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        if (this.beanClassLoader == null && beanFactory instanceof ConfigurableBeanFactory) {
            this.beanClassLoader = ((ConfigurableBeanFactory) beanFactory).getBeanClassLoader();
        }
        if (beanFactory instanceof AbstractBeanFactory) {
            this.retrievalMutex = ((AbstractBeanFactory) beanFactory).getSingletonMutex();
        }
    }

    public void addApplicationListener(ApplicationListener listener) {
        synchronized (this.retrievalMutex) {
            this.defaultRetriever.applicationListeners.add(listener);
            this.retrieverCache.clear();
        }
    }

    public void addApplicationListenerBean(String listenerBeanName) {
        synchronized (this.retrievalMutex) {
            this.defaultRetriever.applicationListenerBeans.add(listenerBeanName);
            this.retrieverCache.clear();
        }
    }

    public void removeApplicationListener(ApplicationListener listener) {
        synchronized (this.retrievalMutex) {
            this.defaultRetriever.applicationListeners.remove(listener);
            this.retrieverCache.clear();
        }
    }

    public void removeApplicationListenerBean(String listenerBeanName) {
        synchronized (this.retrievalMutex) {
            this.defaultRetriever.applicationListenerBeans.remove(listenerBeanName);
            this.retrieverCache.clear();
        }
    }

    public void removeAllListeners() {
        synchronized (this.retrievalMutex) {
            this.defaultRetriever.applicationListeners.clear();
            this.defaultRetriever.applicationListenerBeans.clear();
            this.retrieverCache.clear();
        }
    }


    /**
     * Return a Collection containing all ApplicationListeners.
     *
     * @return a Collection of ApplicationListeners
     * @see org.springframework.context.ApplicationListener
     */
    protected Collection<ApplicationListener> getApplicationListeners() {
        synchronized (this.retrievalMutex) {
            return this.defaultRetriever.getApplicationListeners();
        }
    }

    /**
     * Return a Collection of ApplicationListeners matching the given
     * event type. Non-matching listeners get excluded early.
     *
     * @param event the event to be propagated. Allows for excluding
     *              non-matching listeners early, based on cached matching information.
     * @return a Collection of ApplicationListeners
     * @see org.springframework.context.ApplicationListener
     */
    protected Collection<ApplicationListener> getApplicationListeners(ApplicationEvent event) {
        Class<? extends ApplicationEvent> eventType = event.getClass();
        Object source = event.getSource();
        Class<?> sourceType = (source != null ? source.getClass() : null);
        ListenerCacheKey cacheKey = new ListenerCacheKey(eventType, sourceType);

        // Quick check for existing entry on ConcurrentHashMap...
        ListenerRetriever retriever = this.retrieverCache.get(cacheKey);
        if (retriever != null) {
            return retriever.getApplicationListeners();
        }

        if (this.beanClassLoader == null ||
                (ClassUtils.isCacheSafe(eventType, this.beanClassLoader) &&
                        (sourceType == null || ClassUtils.isCacheSafe(sourceType, this.beanClassLoader)))) {
            // Fully synchronized building and caching of a ListenerRetriever
            synchronized (this.retrievalMutex) {
                retriever = this.retrieverCache.get(cacheKey);
                if (retriever != null) {
                    return retriever.getApplicationListeners();
                }
                retriever = new ListenerRetriever(true);
                Collection<ApplicationListener> listeners =
                        retrieveApplicationListeners(eventType, sourceType, retriever);
                this.retrieverCache.put(cacheKey, retriever);
                return listeners;
            }
        } else {
            // No ListenerRetriever caching -> no synchronization necessary
            return retrieveApplicationListeners(eventType, sourceType, null);
        }
    }

    /**
     * Actually retrieve the application listeners for the given event and source type.
     *
     * @param eventType  the application event type
     * @param sourceType the event source type
     * @param retriever  the ListenerRetriever, if supposed to populate one (for caching purposes)
     * @return the pre-filtered list of application listeners for the given event and source type
     */
    private Collection<ApplicationListener> retrieveApplicationListeners(
            Class<? extends ApplicationEvent> eventType, Class<?> sourceType, ListenerRetriever retriever) {

        LinkedList<ApplicationListener> allListeners = new LinkedList<ApplicationListener>();
        Set<ApplicationListener> listeners;
        Set<String> listenerBeans;
        synchronized (this.retrievalMutex) {
            listeners = new LinkedHashSet<ApplicationListener>(this.defaultRetriever.applicationListeners);
            listenerBeans = new LinkedHashSet<String>(this.defaultRetriever.applicationListenerBeans);
        }
        for (ApplicationListener listener : listeners) {
            if (supportsEvent(listener, eventType, sourceType)) {
                if (retriever != null) {
                    retriever.applicationListeners.add(listener);
                }
                allListeners.add(listener);
            }
        }
        if (!listenerBeans.isEmpty()) {
            BeanFactory beanFactory = getBeanFactory();
            for (String listenerBeanName : listenerBeans) {
                ApplicationListener listener = beanFactory.getBean(listenerBeanName, ApplicationListener.class);
                if (!allListeners.contains(listener) && supportsEvent(listener, eventType, sourceType)) {
                    if (retriever != null) {
                        retriever.applicationListenerBeans.add(listenerBeanName);
                    }
                    allListeners.add(listener);
                }
            }
        }
        OrderComparator.sort(allListeners);
        return allListeners;
    }

    /**
     * Determine whether the given listener supports the given event.
     * <p>The default implementation detects the {@link SmartApplicationListener}
     * interface. In case of a standard {@link ApplicationListener}, a
     * {@link GenericApplicationListenerAdapter} will be used to introspect
     * the generically declared type of the target listener.
     *
     * @param listener   the target listener to check
     * @param eventType  the event type to check against
     * @param sourceType the source type to check against
     * @return whether the given listener should be included in the candidates
     * for the given event type
     */
    protected boolean supportsEvent(
            ApplicationListener listener, Class<? extends ApplicationEvent> eventType, Class<?> sourceType) {

        SmartApplicationListener smartListener = (listener instanceof SmartApplicationListener ?
                (SmartApplicationListener) listener : new GenericApplicationListenerAdapter(listener));
        return (smartListener.supportsEventType(eventType) && smartListener.supportsSourceType(sourceType));
    }


    /**
     * Cache key for ListenerRetrievers, based on event type and source type.
     */
    private static class ListenerCacheKey {

        private final Class<?> eventType;

        private final Class<?> sourceType;

        public ListenerCacheKey(Class<?> eventType, Class<?> sourceType) {
            this.eventType = eventType;
            this.sourceType = sourceType;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            ListenerCacheKey otherKey = (ListenerCacheKey) other;
            return ObjectUtils.nullSafeEquals(this.eventType, otherKey.eventType) &&
                    ObjectUtils.nullSafeEquals(this.sourceType, otherKey.sourceType);
        }

        @Override
        public int hashCode() {
            return ObjectUtils.nullSafeHashCode(this.eventType) * 29 + ObjectUtils.nullSafeHashCode(this.sourceType);
        }
    }


    /**
     * Helper class that encapsulates a specific set of target listeners,
     * allowing for efficient retrieval of pre-filtered listeners.
     * <p>An instance of this helper gets cached per event type and source type.
     */
    private class ListenerRetriever {

        public final Set<ApplicationListener> applicationListeners;

        public final Set<String> applicationListenerBeans;

        private final boolean preFiltered;

        public ListenerRetriever(boolean preFiltered) {
            this.applicationListeners = new LinkedHashSet<ApplicationListener>();
            this.applicationListenerBeans = new LinkedHashSet<String>();
            this.preFiltered = preFiltered;
        }

        public Collection<ApplicationListener> getApplicationListeners() {
            LinkedList<ApplicationListener> allListeners = new LinkedList<ApplicationListener>();
            for (ApplicationListener listener : this.applicationListeners) {
                allListeners.add(listener);
            }
            if (!this.applicationListenerBeans.isEmpty()) {
                BeanFactory beanFactory = getBeanFactory();
                for (String listenerBeanName : this.applicationListenerBeans) {
                    ApplicationListener listener = beanFactory.getBean(listenerBeanName, ApplicationListener.class);
                    if (this.preFiltered || !allListeners.contains(listener)) {
                        allListeners.add(listener);
                    }
                }
            }
            OrderComparator.sort(allListeners);
            return allListeners;
        }
    }

}
