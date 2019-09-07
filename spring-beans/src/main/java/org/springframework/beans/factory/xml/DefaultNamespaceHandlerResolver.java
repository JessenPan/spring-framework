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

package org.springframework.beans.factory.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of the {@link NamespaceHandlerResolver} interface.
 * Resolves namespace URIs to implementation classes based on the mappings
 * contained in mapping file.
 * <p>
 * NamespaceHandlerResolver接口的模式实现类.根据在映射文件中指定的关系，将命名空间uri指向对应的实现类
 * <p>By default, this implementation looks for the mapping file at
 * {@code META-INF/spring.handlers}, but this can be changed using the
 * {@link #DefaultNamespaceHandlerResolver(ClassLoader, String)} constructor.
 * <br/>
 * 默认情况下，此实现会查找META-INF/spring.handlers文件，作为映射文件.
 * 但可以通过使用{@link #DefaultNamespaceHandlerResolver(ClassLoader, String)}构造函数来改变这一行为
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see NamespaceHandler
 * @see DefaultBeanDefinitionDocumentReader
 * @since 2.0
 */
public class DefaultNamespaceHandlerResolver implements NamespaceHandlerResolver {


    private static final Log LOGGER = LogFactory.getLog(DefaultNamespaceHandlerResolver.class);

    /**
     * The location to look for the mapping files. Can be present in multiple JAR files.
     */
    private static final String DEFAULT_HANDLER_MAPPINGS_LOCATION = "META-INF/spring.handlers";


    /**
     * ClassLoader to use for NamespaceHandler classes
     */
    private final ClassLoader classLoader;

    /**
     * Resource location to search for
     */
    private final String handlerMappingsLocation;

    /**
     * Stores the mappings from namespace URI to NamespaceHandler class name / instance
     * <br/>
     * 存储命名空间到对应的namespaceHandler的实例映射
     */
    private volatile Map<String, Object> handlerMappings;


    /**
     * Create a new {@code DefaultNamespaceHandlerResolver} using the
     * default mapping file location.
     * <p>This constructor will result in the thread context ClassLoader being used
     * to load resources.
     *
     * @see #DEFAULT_HANDLER_MAPPINGS_LOCATION
     */
    public DefaultNamespaceHandlerResolver() {
        this(null, DEFAULT_HANDLER_MAPPINGS_LOCATION);
    }

    /**
     * Create a new {@code DefaultNamespaceHandlerResolver} using the
     * default mapping file location.
     *
     * @param classLoader the {@link ClassLoader} instance used to load mapping resources
     *                    (may be {@code null}, in which case the thread context ClassLoader will be used)
     * @see #DEFAULT_HANDLER_MAPPINGS_LOCATION
     */
    public DefaultNamespaceHandlerResolver(ClassLoader classLoader) {
        this(classLoader, DEFAULT_HANDLER_MAPPINGS_LOCATION);
    }

    /**
     * Create a new {@code DefaultNamespaceHandlerResolver} using the
     * supplied mapping file location.
     *
     * @param classLoader             the {@link ClassLoader} instance used to load mapping resources
     *                                may be {@code null}, in which case the thread context ClassLoader will be used)
     * @param handlerMappingsLocation the mapping file location
     */
    public DefaultNamespaceHandlerResolver(ClassLoader classLoader, String handlerMappingsLocation) {
        Assert.notNull(handlerMappingsLocation, "Handler mappings location must not be null");
        this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
        this.handlerMappingsLocation = handlerMappingsLocation;
    }


    /**
     * Locate the {@link NamespaceHandler} for the supplied namespace URI
     * from the configured mappings.
     *
     * @param namespaceUri the relevant namespace URI
     * @return the located {@link NamespaceHandler}, or {@code null} if none found
     */
    public NamespaceHandler resolve(String namespaceUri) {
        //获取所有已经配置的handler映射
        Map<String, Object> handlerMappings = getHandlerMappings();
        //获取对应的handler类名
        Object handlerOrClassName = handlerMappings.get(namespaceUri);
        if (handlerOrClassName == null) {
            //没有找到，直接返回null
            return null;
        } else if (handlerOrClassName instanceof NamespaceHandler) {
            //实例,直接返回
            return (NamespaceHandler) handlerOrClassName;
        } else {
            //此时存的是namespaceHandler对应的类,需要进行实例化
            String className = (String) handlerOrClassName;
            try {
                Class<?> handlerClass = ClassUtils.forName(className, this.classLoader);
                if (!NamespaceHandler.class.isAssignableFrom(handlerClass)) {
                    throw new FatalBeanException("Class [" + className + "] for namespace [" + namespaceUri +
                            "] does not implement the [" + NamespaceHandler.class.getName() + "] interface");
                }
                //实例化对象
                NamespaceHandler namespaceHandler = (NamespaceHandler) BeanUtils.instantiateClass(handlerClass);
                //调用初始化方法
                namespaceHandler.init();
                //将实例化之后的对象放回map中去，替换到之前存储的className
                handlerMappings.put(namespaceUri, namespaceHandler);
                return namespaceHandler;
            } catch (ClassNotFoundException ex) {
                throw new FatalBeanException("NamespaceHandler class [" + className + "] for namespace [" +
                        namespaceUri + "] not found", ex);
            } catch (LinkageError err) {
                throw new FatalBeanException("Invalid NamespaceHandler class [" + className + "] for namespace [" +
                        namespaceUri + "]: problem with handler class file or dependent class", err);
            }
        }
    }

    /**
     * Load the specified NamespaceHandler mappings lazily.
     * <br/>
     * 惰性的或者被触发式的，加载命名空间名称到命名空间handler的映射
     */
    private Map<String, Object> getHandlerMappings() {
        if (this.handlerMappings == null) {
            synchronized (this) {
                //double check,因为是惰性触发，可能同时被并发的触发,要做并发控制
                //如果是积极(early)加载，只会在构造函数触发一次，就不存在并发问题
                if (this.handlerMappings == null) {
                    try {
                        //属性文件加载工具,加载所有的匹配的属性文件，并合并到一个mapping里去
                        Properties mappings = PropertiesLoaderUtils.loadAllProperties(this.handlerMappingsLocation, this.classLoader);
                        Map<String, Object> handlerMappings = new ConcurrentHashMap<String, Object>(mappings.size());
                        //把属性properties合并到指定的map中去
                        CollectionUtils.mergePropertiesIntoMap(mappings, handlerMappings);
                        this.handlerMappings = handlerMappings;
                    } catch (IOException ex) {
                        throw new IllegalStateException("Unable to load NamespaceHandler mappings from location [" + this.handlerMappingsLocation + "]", ex);
                    }
                }
            }
        }
        return this.handlerMappings;
    }


    @Override
    public String toString() {
        return "NamespaceHandlerResolver using mappings " + getHandlerMappings();
    }

}
