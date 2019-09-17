/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jessenpan.spring.comment.annotation.DesignPattern;
import org.springframework.beans.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResourceLoader;
import org.springframework.web.context.support.StandardServletEnvironment;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import static org.jessenpan.spring.comment.annotation.DesignPattern.DesignPatternEnum.TEMPLATE;

/**
 * Simple extension of {@link javax.servlet.http.HttpServlet} which treats
 * its config parameters ({@code init-param} entries within the
 * {@code servlet} tag in {@code web.xml}) as bean properties.
 * <p>
 * <p>A handy superclass for any type of servlet. Type conversion of config
 * parameters is automatic, with the corresponding setter method getting
 * invoked with the converted value. It is also possible for subclasses to
 * specify required properties. Parameters without matching bean property
 * setter will simply be ignored.
 * <p>
 * <p>This servlet leaves request handling to subclasses, inheriting the default
 * behavior of HttpServlet ({@code doGet}, {@code doPost}, etc).
 * <p>
 * <p>This generic servlet base class has no dependency on the Spring
 * {@link org.springframework.context.ApplicationContext} concept. Simple
 * servlets usually don't load their own context but rather access service
 * beans from the Spring root application context, accessible via the
 * filter's {@link #getServletContext() ServletContext} (see
 * {@link org.springframework.web.context.support.WebApplicationContextUtils}).
 * <p>
 * <p>The {@link FrameworkServlet} class is a more specific servlet base
 * class which loads its own application context. FrameworkServlet serves
 * as direct base class of Spring's full-fledged {@link DispatcherServlet}.
 * <br/>
 * 
 * <p>
 *     HttpServlet的简单扩展。它把在web.xml里，init-param配置区域的参数当成bean的属性
 * </p>
 * <br/>
 * <p>
 *     它可以是任一类型servlet的可以快速复用的父类。配置的参数类型转换是自动的，并调用对应的方法进行设置值。
 *     同时，此类也暴露了接口给任一子类进行设置那些属性是必须的。没有和bean的属性名匹配的web.xml的参数会被自动忽略。
 * </p>
 * <br/>
 * <p>
 *     对于http请求的处理流程，此类没有进行任何实现。子类需要去实现
 * </p>
 * <br/>
 * <p>
 *     此通用的servlet基类没有任何spring应用上下文的依赖。
 *     简单的servlet通常不加载自己的spring上下文，而是通过从servlet的过滤器(Filter)中获取ServletContext，再获取对应的spring上下文来访问对应的bean。
 * </p>
 * <br/>
 * <p>
 *     FrameworkServlet是一个更加特定的类，加载了它自己的应用上下文。
 *     FrameworkServlet是更加成熟的DispatcherServlet的直接父类。
 * </p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #addRequiredProperty
 * @see #initServletBean
 * @see #doGet
 * @see #doPost
 * 
 */
@DesignPattern({ TEMPLATE})
@SuppressWarnings("serial")
public abstract class HttpServletBean extends HttpServlet
        implements EnvironmentCapable, EnvironmentAware {

    /**
     * Logger available to subclasses
     */
    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * Set of required properties (Strings) that must be supplied as
     * config parameters to this servlet.
     * <p>
     *     必须提供给此servlet的配置参数，这些配置参数的格式是String格式的
     * </p>
     */
    private final Set<String> requiredProperties = new HashSet<String>();

    /**
     * 环境对象
     */
    private ConfigurableEnvironment environment;


    /**
     * Subclasses can invoke this method to specify that this property
     * (which must match a JavaBean property they expose) is mandatory,
     * and must be supplied as a config parameter. This should be called
     * from the constructor of a subclass.
     * <p>This method is only relevant in case of traditional initialization
     * driven by a ServletConfig instance.
     *
     * @param property name of the required property
     */
    protected final void addRequiredProperty(String property) {
        this.requiredProperties.add(property);
    }

    /**
     * Map config parameters onto bean properties of this servlet, and
     * invoke subclass initialization.
     *
     * @throws ServletException if bean properties are invalid (or required
     *                          properties are missing), or if subclass initialization fails.
     */
    @Override
    public final void init() throws ServletException {

        try {
            // 基于servletConfig创建一个PropertyValues对象
            PropertyValues pvs = new ServletConfigPropertyValues(getServletConfig(), this.requiredProperties);
            //通过此ServletBean及其子类创建一个beanWrapper。
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(this);
            //对于Resource类型的字段设置对象的编辑器
            ResourceLoader resourceLoader = new ServletContextResourceLoader(getServletContext());
            bw.registerCustomEditor(Resource.class, new ResourceEditor(resourceLoader, getEnvironment()));
            initBeanWrapper(bw);
            //将propertyValues的值通过beanWrapper设置给此servletBean上。
            bw.setPropertyValues(pvs, true);
        } catch (BeansException ex) {
            logger.error("Failed to set bean properties on servlet '" + getServletName() + "'", ex);
            throw ex;
        }
        
        initServletBean();
    }

    /**
     * Initialize the BeanWrapper for this HttpServletBean,
     * possibly with custom editors.
     * <p>This default implementation is empty.
     * <p>
     *    给beanWrapper设置自定义的属性字段编辑器
     * </p>
     * <p>
     *     此方法是模板方法，子类可以覆写
     * </p>
     *
     * @param bw the BeanWrapper to initialize
     * @throws BeansException if thrown by BeanWrapper methods
     * @see org.springframework.beans.BeanWrapper#registerCustomEditor
     */
    @DesignPattern({ TEMPLATE })
    protected void initBeanWrapper(BeanWrapper bw) throws BeansException {
    }


    /**
     * Overridden method that simply returns {@code null} when no
     * ServletConfig set yet.
     *
     * @see #getServletConfig()
     */
    @Override
    public final String getServletName() {
        return (getServletConfig() != null ? getServletConfig().getServletName() : null);
    }

    /**
     * Overridden method that simply returns {@code null} when no
     * ServletConfig set yet.
     *
     * @see #getServletConfig()
     */
    @Override
    public final ServletContext getServletContext() {
        return (getServletConfig() != null ? getServletConfig().getServletContext() : null);
    }


    /**
     * Subclasses may override this to perform custom initialization.
     * All bean properties of this servlet will have been set before this
     * method is invoked.
     * <p>
     *     模板方法，子类可以覆写此方法进行自定义的初始化过程。
     * </p>
     * <p>
     *     此servlet的bean属性在此方法被调用前已经全部初始化完成
     * </p>
     *
     * @throws ServletException if subclass initialization fails
     */
    protected void initServletBean() throws ServletException {
    }

    /**
     * {@inheritDoc}
     * <p>If {@code null}, a new environment will be initialized via
     * {@link #createEnvironment()}.
     */
    public ConfigurableEnvironment getEnvironment() {
        if (this.environment == null) {
            this.environment = this.createEnvironment();
        }
        return this.environment;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if environment is not assignable to
     *                                  {@code ConfigurableEnvironment}.
     */
    public void setEnvironment(Environment environment) {
        Assert.isInstanceOf(ConfigurableEnvironment.class, environment);
        this.environment = (ConfigurableEnvironment) environment;
    }

    /**
     * Create and return a new {@link StandardServletEnvironment}. Subclasses may override
     * in order to configure the environment or specialize the environment type returned.
     * <p>
     *     创建并返回一个新的{@link StandardServletEnvironment}对象。
     * </p>
     * <p>
     *     此方法是一个protected级别的函数，子类可以覆写并返回自定义的实现。
     * </p>
     */
    protected ConfigurableEnvironment createEnvironment() {
        return new StandardServletEnvironment();
    }


    /**
     * PropertyValues implementation created from ServletConfig init parameters.
     * <p>
     *     PropertyValues接口的扩展实现
     * </p>
     * <p>
     *     此实现从ServletConfig的初始化参数构建值
     * </p>
     */
    private static class ServletConfigPropertyValues extends MutablePropertyValues {

        /**
         * Create new ServletConfigPropertyValues.
         *
         * @param config             ServletConfig we'll use to take PropertyValues from
         * @param requiredProperties set of property names we need, where
         *                           we can't accept default values
         * @throws ServletException if any required properties are missing
         */
        public ServletConfigPropertyValues(ServletConfig config, Set<String> requiredProperties)
                throws ServletException {

            Set<String> missingProps = (requiredProperties != null && !requiredProperties.isEmpty()) ?
                    new HashSet<String>(requiredProperties) : null;

            /**
             * 
             * <p>
             *     从servletConfig里遍历配置的key并添加到存储里，同时删除requiredProperties里对应项
             * </p>
             */
            Enumeration en = config.getInitParameterNames();
            while (en.hasMoreElements()) {
                String property = (String) en.nextElement();
                Object value = config.getInitParameter(property);
                addPropertyValue(new PropertyValue(property, value));
                if (missingProps != null) {
                    missingProps.remove(property);
                }
            }

            // Fail if we are still missing properties.
            if (missingProps != null && missingProps.size() > 0) {
                throw new ServletException(
                        "Initialization from ServletConfig for servlet '" + config.getServletName() +
                                "' failed; the following required properties were missing: " +
                                StringUtils.collectionToDelimitedString(missingProps, ", "));
            }
        }
    }

}
