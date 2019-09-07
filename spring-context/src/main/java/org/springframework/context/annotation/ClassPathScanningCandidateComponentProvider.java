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

package org.springframework.context.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 组件提供者,通过基础包扫描classpath来获得组件.
 * 通过对扫描的结果应用exclude和include过滤器来获取获选集
 * A component provider that scans the classpath from a base package. It then
 * applies exclude and include filters to the resulting classes to find candidates.
 * <p>
 * <p>This implementation is based on Spring's
 * {@link org.springframework.core.type.classreading.MetadataReader MetadataReader}
 * facility, backed by an ASM {@link org.springframework.asm.ClassReader ClassReader}.
 * <p>
 * <p>
 * 此类的实现是基于spring的MetadataReader这个基础设施类,它的实现又基于classReader.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Chris Beams
 * @see org.springframework.core.type.classreading.MetadataReaderFactory
 * @see org.springframework.core.type.AnnotationMetadata
 * @see ScannedGenericBeanDefinition
 * @since 2.5
 */
public class ClassPathScanningCandidateComponentProvider implements EnvironmentCapable, ResourceLoaderAware {

    //默认的资源匹配模式
    public static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

    private static final Log logger = LogFactory.getLog(ClassPathScanningCandidateComponentProvider.class);

    //使用不可变字段
    private final List<TypeFilter> includeFilters = new LinkedList<TypeFilter>();
    private final List<TypeFilter> excludeFilters = new LinkedList<TypeFilter>();
    private Environment environment;
    private ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(this.resourcePatternResolver);
    //资源匹配模式
    private String resourcePattern = DEFAULT_RESOURCE_PATTERN;


    /**
     * Create a ClassPathScanningCandidateComponentProvider with a {@link StandardEnvironment}.
     *
     * @param useDefaultFilters whether to register the default filters for the
     *                          {@link Component @Component}, {@link Repository @Repository},
     *                          {@link Service @Service}, and {@link Controller @Controller}
     *                          stereotype annotations
     * @see #registerDefaultFilters()
     */
    public ClassPathScanningCandidateComponentProvider(boolean useDefaultFilters) {
        this(useDefaultFilters, new StandardEnvironment());
    }

    /**
     * Create a ClassPathScanningCandidateComponentProvider with the given {@link Environment}.
     *
     * @param useDefaultFilters whether to register the default filters for the
     *                          {@link Component @Component}, {@link Repository @Repository},
     *                          {@link Service @Service}, and {@link Controller @Controller}
     *                          stereotype annotations
     * @param environment       the Environment to use
     * @see #registerDefaultFilters()
     */
    public ClassPathScanningCandidateComponentProvider(boolean useDefaultFilters, Environment environment) {
        if (useDefaultFilters) {
            registerDefaultFilters();
        }
        this.environment = environment;
    }

    /**
     * Return the ResourceLoader that this component provider uses.
     */
    public final ResourceLoader getResourceLoader() {
        return this.resourcePatternResolver;
    }

    /**
     * Set the ResourceLoader to use for resource locations.
     * This will typically be a ResourcePatternResolver implementation.
     * <p>Default is PathMatchingResourcePatternResolver, also capable of
     * resource pattern resolving through the ResourcePatternResolver interface.
     *
     * @see org.springframework.core.io.support.ResourcePatternResolver
     * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
     */
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
        this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);
    }

    /**
     * Return the MetadataReaderFactory used by this component provider.
     */
    public final MetadataReaderFactory getMetadataReaderFactory() {
        return this.metadataReaderFactory;
    }

    /**
     * Set the {@link MetadataReaderFactory} to use.
     * <p>Default is a {@link CachingMetadataReaderFactory} for the specified
     * {@linkplain #setResourceLoader resource loader}.
     * <p>Call this setter method <i>after</i> {@link #setResourceLoader} in order
     * for the given MetadataReaderFactory to override the default factory.
     */
    public void setMetadataReaderFactory(MetadataReaderFactory metadataReaderFactory) {
        this.metadataReaderFactory = metadataReaderFactory;
    }

    public final Environment getEnvironment() {
        return this.environment;
    }

    /**
     * Set the Environment to use when resolving placeholders and evaluating
     * {@link Profile @Profile}-annotated component classes.
     * <p>The default is a {@link StandardEnvironment}
     *
     * @param environment the Environment to use
     */
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * Set the resource pattern to use when scanning the classpath.
     * This value will be appended to each base package name.
     *
     * @see #findCandidateComponents(String)
     * @see #DEFAULT_RESOURCE_PATTERN
     */
    public void setResourcePattern(String resourcePattern) {
        Assert.notNull(resourcePattern, "'resourcePattern' must not be null");
        this.resourcePattern = resourcePattern;
    }

    /**
     * Add an include type filter to the <i>end</i> of the inclusion list.
     */
    public void addIncludeFilter(TypeFilter includeFilter) {
        this.includeFilters.add(includeFilter);
    }

    /**
     * Add an exclude type filter to the <i>front</i> of the exclusion list.
     */
    public void addExcludeFilter(TypeFilter excludeFilter) {
        this.excludeFilters.add(0, excludeFilter);
    }

    /**
     * Reset the configured type filters.
     *
     * @param useDefaultFilters whether to re-register the default filters for
     *                          the {@link Component @Component}, {@link Repository @Repository},
     *                          {@link Service @Service}, and {@link Controller @Controller}
     *                          stereotype annotations
     * @see #registerDefaultFilters()
     */
    public void resetFilters(boolean useDefaultFilters) {
        this.includeFilters.clear();
        this.excludeFilters.clear();
        if (useDefaultFilters) {
            registerDefaultFilters();
        }
    }

    /**
     * Register the default filter for {@link Component @Component}.
     * <p>This will implicitly register all annotations that have the
     * {@link Component @Component} meta-annotation including the
     * {@link Repository @Repository}, {@link Service @Service}, and
     * {@link Controller @Controller} stereotype annotations.
     * <p>Also supports Java EE 6's {@link javax.annotation.ManagedBean} and
     * JSR-330's {@link javax.inject.Named} annotations, if available.
     * <p>
     * 注册component注解的过滤器为默认过滤器.
     * 这会隐式的将包含component注解作为元注解的其它注解都包含进来,包含的有repository,service以及controller这些注解.
     * 此外,如果可能的话，也会将managedBean和Named注解作为过滤类型
     */
    @SuppressWarnings("unchecked")
    private void registerDefaultFilters() {
        this.includeFilters.add(new AnnotationTypeFilter(Component.class));
        ClassLoader cl = ClassPathScanningCandidateComponentProvider.class.getClassLoader();
        try {
            this.includeFilters.add(new AnnotationTypeFilter(((Class<? extends Annotation>) ClassUtils.forName("javax.annotation.ManagedBean", cl)), false));
            logger.debug("JSR-250 'javax.annotation.ManagedBean' found and supported for component scanning");
        } catch (ClassNotFoundException ex) {
            // JSR-250 1.1 API (as included in Java EE 6) not available - simply skip.
        }
        try {
            this.includeFilters.add(new AnnotationTypeFilter(((Class<? extends Annotation>) ClassUtils.forName("javax.inject.Named", cl)), false));
            logger.debug("JSR-330 'javax.inject.Named' annotation found and supported for component scanning");
        } catch (ClassNotFoundException ex) {
            // JSR-330 API not available - simply skip.
        }
    }


    /**
     * Scan the class path for candidate components.
     * <p>
     * 扫描classpath寻找候选组件
     *
     * @param basePackage the package to check for annotated classes
     * @return a corresponding Set of autodetected bean definitions
     */
    public Set<BeanDefinition> findCandidateComponents(String basePackage) {
        Set<BeanDefinition> candidates = new LinkedHashSet<BeanDefinition>();
        try {
            //构建扫描路径格式:classpath*:包路径/**/.class
            //review 可以用一个build方法,职责单一同时其它地方也可以共用
            String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                    resolveBasePackage(basePackage) + "/" + this.resourcePattern;
            Resource[] resources = this.resourcePatternResolver.getResources(packageSearchPath);
            for (Resource resource : resources) {
                if (resource.isReadable()) {
                    try {
                        MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(resource);
                        if (isCandidateComponent(metadataReader)) {
                            ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);
                            sbd.setResource(resource);
                            sbd.setSource(resource);
                            if (isCandidateComponent(sbd)) {
                                candidates.add(sbd);
                            }
                        }
                    } catch (Throwable ex) {
                        throw new BeanDefinitionStoreException("Failed to read candidate component class: " + resource, ex);
                    }
                }
            }
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);
        }
        return candidates;
    }


    /**
     * Resolve the specified base package into a pattern specification for
     * the package search path.
     * <p>
     * 将指定的基础包路径解析为用于包搜索的规范模式
     * <p>The default implementation resolves placeholders against system properties,
     * and converts a "."-based package path to a "/"-based resource path.
     * <p>
     * 默认实现是将占位符进行解析替换，然后将.转换为/
     *
     * @param basePackage the base package as specified by the user
     * @return the pattern specification to be used for package searching
     */
    private String resolveBasePackage(String basePackage) {
        return ClassUtils.convertClassNameToResourcePath(environment.resolveRequiredPlaceholders(basePackage));
    }

    /**
     * 判断一个指定的class，是否不匹配所有排除(exclude)过滤器以及匹配至少一个包含(include)过滤器
     * Determine whether the given class does not match any exclude filter
     * and does match at least one include filter.
     *
     * @param metadataReader the ASM ClassReader for the class 基于asm的classReader
     * @return whether the class qualifies as a candidate component
     */
    private boolean isCandidateComponent(MetadataReader metadataReader) throws IOException {
        for (TypeFilter tf : this.excludeFilters) {
            if (tf.match(metadataReader, this.metadataReaderFactory)) {
                //只要被一个排除(exclude)过滤器命中，则返回false
                return false;
            }
        }
        for (TypeFilter tf : this.includeFilters) {
            if (tf.match(metadataReader, this.metadataReaderFactory)) {
                AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();
                if (!metadata.isAnnotated(Profile.class.getName())) {
                    return true;
                }
                AnnotationAttributes profile = MetadataUtils.attributesFor(metadata, Profile.class);
                return this.environment.acceptsProfiles(profile.getStringArray("value"));
            }
        }
        //没有被至少一个包含过滤器命中,返回false，不是候选candidate组件
        return false;
    }

    /**
     * Determine whether the given bean definition qualifies as candidate.
     * <p>The default implementation checks whether the class is concrete
     * (i.e. not abstract and not an interface). Can be overridden in subclasses.
     *
     * @param beanDefinition the bean definition to check
     * @return whether the bean definition qualifies as a candidate component
     */
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return (beanDefinition.getMetadata().isConcrete() && beanDefinition.getMetadata().isIndependent());
    }


    /**
     * Clear the underlying metadata cache, removing all cached class metadata.
     */
    public void clearCache() {
        if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {
            ((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();
        }
    }

}
