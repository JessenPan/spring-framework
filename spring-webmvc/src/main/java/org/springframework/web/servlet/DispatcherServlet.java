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

package org.springframework.web.servlet;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.OrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.ui.context.ThemeSource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Central dispatcher for HTTP request handlers/controllers, e.g. for web UI controllers
 * or HTTP-based remote service exporters. Dispatches to registered handlers for processing
 * a web request, providing convenient mapping and exception handling facilities.
 * <p>
 * <p>This servlet is very flexible: It can be used with just about any workflow, with the
 * installation of the appropriate adapter classes. It offers the following functionality
 * that distinguishes it from other request-driven web MVC frameworks:
 * <p>
 * <ul>
 * <li>It is based around a JavaBeans configuration mechanism.
 * <p>
 * <li>It can use any {@link HandlerMapping} implementation - pre-built or provided as part
 * of an application - to control the routing of requests to handler objects. Default is
 * {@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping} and
 * {@link org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping}.
 * HandlerMapping objects can be defined as beans in the servlet's application context,
 * implementing the HandlerMapping interface, overriding the default HandlerMapping if
 * present. HandlerMappings can be given any bean name (they are tested by type).
 * <p>
 * <li>It can use any {@link HandlerAdapter}; this allows for using any handler interface.
 * Default adapters are {@link org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter},
 * {@link org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter}, for Spring's
 * {@link org.springframework.web.HttpRequestHandler} and
 * {@link org.springframework.web.servlet.mvc.Controller} interfaces, respectively. A default
 * {@link org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter}
 * will be registered as well. HandlerAdapter objects can be added as beans in the
 * application context, overriding the default HandlerAdapters. Like HandlerMappings,
 * HandlerAdapters can be given any bean name (they are tested by type).
 * <p>
 * <li>The dispatcher's exception resolution strategy can be specified via a
 * {@link HandlerExceptionResolver}, for example mapping certain exceptions to error pages.
 * Default are
 * {@link org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerExceptionResolver},
 * {@link org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver}, and
 * {@link org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver}.
 * These HandlerExceptionResolvers can be overridden through the application context.
 * HandlerExceptionResolver can be given any bean name (they are tested by type).
 * <p>
 * <li>Its view resolution strategy can be specified via a {@link ViewResolver}
 * implementation, resolving symbolic view names into View objects. Default is
 * {@link org.springframework.web.servlet.view.InternalResourceViewResolver}.
 * ViewResolver objects can be added as beans in the application context, overriding the
 * default ViewResolver. ViewResolvers can be given any bean name (they are tested by type).
 * <p>
 * <li>If a {@link View} or view name is not supplied by the user, then the configured
 * {@link RequestToViewNameTranslator} will translate the current request into a view name.
 * The corresponding bean name is "viewNameTranslator"; the default is
 * {@link org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator}.
 * <p>
 * <li>The dispatcher's strategy for resolving multipart requests is determined by a
 * {@link org.springframework.web.multipart.MultipartResolver} implementation.
 * Implementations for Apache Commons FileUpload and Servlet 3 are included; the typical
 * choice is {@link org.springframework.web.multipart.commons.CommonsMultipartResolver}.
 * The MultipartResolver bean name is "multipartResolver"; default is none.
 * <p>
 * <li>Its locale resolution strategy is determined by a {@link LocaleResolver}.
 * Out-of-the-box implementations work via HTTP accept header, cookie, or session.
 * The LocaleResolver bean name is "localeResolver"; default is
 * {@link org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver}.
 * <p>
 * <li>Its theme resolution strategy is determined by a {@link ThemeResolver}.
 * Implementations for a fixed theme and for cookie and session storage are included.
 * The ThemeResolver bean name is "themeResolver"; default is
 * {@link org.springframework.web.servlet.theme.FixedThemeResolver}.
 * </ul>
 * <p>
 * <p><b>NOTE: The {@code @RequestMapping} annotation will only be processed if a
 * corresponding {@code HandlerMapping} (for type-level annotations) and/or
 * {@code HandlerAdapter} (for method-level annotations) is present in the dispatcher.</b>
 * This is the case by default. However, if you are defining custom {@code HandlerMappings}
 * or {@code HandlerAdapters}, then you need to make sure that a corresponding custom
 * {@code DefaultAnnotationHandlerMapping} and/or {@code AnnotationMethodHandlerAdapter}
 * is defined as well - provided that you intend to use {@code @RequestMapping}.
 * <p>
 * <p><b>A web application can define any number of DispatcherServlets.</b>
 * Each servlet will operate in its own namespace, loading its own application context
 * with mappings, handlers, etc. Only the root application context as loaded by
 * {@link org.springframework.web.context.ContextLoaderListener}, if any, will be shared.
 * <p>
 * <p>As of Spring 3.1, {@code DispatcherServlet} may now be injected with a web
 * application context, rather than creating its own internally. This is useful in Servlet
 * 3.0+ environments, which support programmatic registration of servlet instances.
 * See the {@link #DispatcherServlet(WebApplicationContext)} javadoc for details.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @see org.springframework.web.HttpRequestHandler
 * @see org.springframework.web.servlet.mvc.Controller
 * @see org.springframework.web.context.ContextLoaderListener
 */
@SuppressWarnings("serial")
public class DispatcherServlet extends FrameworkServlet {

    /**
     * Well-known name for the MultipartResolver object in the bean factory for this namespace.
     */
    private static final String MULTIPART_RESOLVER_BEAN_NAME = "multipartResolver";

    /**
     * Well-known name for the LocaleResolver object in the bean factory for this namespace.
     */
    public static final String LOCALE_RESOLVER_BEAN_NAME = "localeResolver";

    /**
     * Well-known name for the ThemeResolver object in the bean factory for this namespace.
     */
    public static final String THEME_RESOLVER_BEAN_NAME = "themeResolver";

    /**
     * Well-known name for the HandlerMapping object in the bean factory for this namespace.
     * Only used when "detectAllHandlerMappings" is turned off.
     *
     * @see #setDetectAllHandlerMappings
     */
    private static final String HANDLER_MAPPING_BEAN_NAME = "handlerMapping";

    /**
     * Well-known name for the HandlerAdapter object in the bean factory for this namespace.
     * Only used when "detectAllHandlerAdapters" is turned off.
     *
     * @see #setDetectAllHandlerAdapters
     */
    private static final String HANDLER_ADAPTER_BEAN_NAME = "handlerAdapter";

    /**
     * Well-known name for the HandlerExceptionResolver object in the bean factory for this namespace.
     * Only used when "detectAllHandlerExceptionResolvers" is turned off.
     *
     * @see #setDetectAllHandlerExceptionResolvers
     */
    private static final String HANDLER_EXCEPTION_RESOLVER_BEAN_NAME = "handlerExceptionResolver";

    /**
     * Well-known name for the RequestToViewNameTranslator object in the bean factory for this namespace.
     */
    private static final String REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME = "viewNameTranslator";

    /**
     * Well-known name for the ViewResolver object in the bean factory for this namespace.
     * Only used when "detectAllViewResolvers" is turned off.
     *
     * @see #setDetectAllViewResolvers
     */
    private static final String VIEW_RESOLVER_BEAN_NAME = "viewResolver";

    /**
     * Well-known name for the FlashMapManager object in the bean factory for this namespace.
     */
    private static final String FLASH_MAP_MANAGER_BEAN_NAME = "flashMapManager";

    /**
     * Request attribute to hold the current web application context.
     * Otherwise only the global web app context is obtainable by tags etc.
     *
     * @see org.springframework.web.servlet.support.RequestContextUtils#getWebApplicationContext
     */
    public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = DispatcherServlet.class.getName() + ".CONTEXT";

    /**
     * Request attribute to hold the current LocaleResolver, retrievable by views.
     *
     * @see org.springframework.web.servlet.support.RequestContextUtils#getLocaleResolver
     */
    public static final String LOCALE_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".LOCALE_RESOLVER";

    /**
     * Request attribute to hold the current ThemeResolver, retrievable by views.
     *
     * @see org.springframework.web.servlet.support.RequestContextUtils#getThemeResolver
     */
    public static final String THEME_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".THEME_RESOLVER";

    /**
     * Request attribute to hold the current ThemeSource, retrievable by views.
     *
     * @see org.springframework.web.servlet.support.RequestContextUtils#getThemeSource
     */
    public static final String THEME_SOURCE_ATTRIBUTE = DispatcherServlet.class.getName() + ".THEME_SOURCE";

    /**
     * Name of request attribute that holds a read-only {@code Map<String,?>}
     * with "input" flash attributes saved by a previous request, if any.
     *
     * @see org.springframework.web.servlet.support.RequestContextUtils#getInputFlashMap(HttpServletRequest)
     */
    public static final String INPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".INPUT_FLASH_MAP";

    /**
     * Name of request attribute that holds the "output" {@link FlashMap} with
     * attributes to save for a subsequent request.
     *
     * @see org.springframework.web.servlet.support.RequestContextUtils#getOutputFlashMap(HttpServletRequest)
     */
    public static final String OUTPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".OUTPUT_FLASH_MAP";

    /**
     * Name of request attribute that holds the {@link FlashMapManager}.
     *
     * @see org.springframework.web.servlet.support.RequestContextUtils#getFlashMapManager(HttpServletRequest)
     */
    public static final String FLASH_MAP_MANAGER_ATTRIBUTE = DispatcherServlet.class.getName() + ".FLASH_MAP_MANAGER";

    /**
     * Name of the class path resource (relative to the DispatcherServlet class)
     * that defines DispatcherServlet's default strategy names.
     */
    private static final String DEFAULT_STRATEGIES_PATH = "DispatcherServlet.properties";

    /**
     * 通过DEFAULT_STRATEGIES_PATH加载的配置文件内容
     */
    private static final Properties DEFAULT_STRATEGIES;

    static {
        //静态代码块
        //从配置DispatcherServlet中获取默认的策略接口,当外部没有自定义配置时,使用这些默认配置
        try {
            ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, DispatcherServlet.class);
            DEFAULT_STRATEGIES = PropertiesLoaderUtils.loadProperties(resource);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load 'DispatcherServlet.properties': " + ex.getMessage());
        }
    }

    /**
     * Detect all HandlerMappings or just expect "handlerMapping" bean?
     */
    private boolean detectAllHandlerMappings = true;

    /**
     * Detect all HandlerAdapters or just expect "handlerAdapter" bean?
     */
    private boolean detectAllHandlerAdapters = true;

    /**
     * Detect all HandlerExceptionResolvers or just expect "handlerExceptionResolver" bean?
     */
    private boolean detectAllHandlerExceptionResolvers = true;

    /**
     * Detect all ViewResolvers or just expect "viewResolver" bean?
     */
    private boolean detectAllViewResolvers = true;

    /**
     * Perform cleanup of request attributes after include request?
     */
    private boolean cleanupAfterInclude = true;

    /**
     * MultipartResolver used by this servlet
     */
    private MultipartResolver multipartResolver;

    /**
     * LocaleResolver used by this servlet
     */
    private LocaleResolver localeResolver;

    /**
     * ThemeResolver used by this servlet
     */
    private ThemeResolver themeResolver;

    /**
     * List of HandlerMappings used by this servlet
     */
    private List<HandlerMapping> handlerMappings;

    /**
     * List of HandlerAdapters used by this servlet
     */
    private List<HandlerAdapter> handlerAdapters;

    /**
     * List of HandlerExceptionResolvers used by this servlet
     */
    private List<HandlerExceptionResolver> handlerExceptionResolvers;

    /**
     * RequestToViewNameTranslator used by this servlet
     */
    private RequestToViewNameTranslator viewNameTranslator;

    /**
     * FlashMapManager used by this servlet
     */
    private FlashMapManager flashMapManager;

    /**
     * List of ViewResolvers used by this servlet
     */
    private List<ViewResolver> viewResolvers;

    /**
     * Create a new {@code DispatcherServlet} that will create its own internal web
     * application context based on defaults and values provided through servlet
     * init-params. Typically used in Servlet 2.5 or earlier environments, where the only
     * option for servlet registration is through {@code web.xml} which requires the use
     * of a no-arg constructor.
     * <p>Calling {@link #setContextConfigLocation} (init-param 'contextConfigLocation')
     * will dictate which XML files will be loaded by the
     * {@linkplain #DEFAULT_CONTEXT_CLASS default XmlWebApplicationContext}
     * <p>Calling {@link #setContextClass} (init-param 'contextClass') overrides the
     * default {@code XmlWebApplicationContext} and allows for specifying an alternative class,
     * such as {@code AnnotationConfigWebApplicationContext}.
     * <p>Calling {@link #setContextInitializerClasses} (init-param 'contextInitializerClasses')
     * indicates which {@code ApplicationContextInitializer} classes should be used to
     * further configure the internal application context prior to refresh().
     *
     * @see #DispatcherServlet(WebApplicationContext)
     */
    public DispatcherServlet() {
        super();
    }

    /**
     * Create a new {@code DispatcherServlet} with the given web application context. This
     * constructor is useful in Servlet 3.0+ environments where instance-based registration
     * of servlets is possible through the {@link ServletContext#addServlet} API.
     * <p>Using this constructor indicates that the following properties / init-params
     * will be ignored:
     * <ul>
     * <li>{@link #setContextClass(Class)} / 'contextClass'</li>
     * <li>{@link #setContextConfigLocation(String)} / 'contextConfigLocation'</li>
     * <li>{@link #setContextAttribute(String)} / 'contextAttribute'</li>
     * <li>{@link #setNamespace(String)} / 'namespace'</li>
     * </ul>
     * <p>The given web application context may or may not yet be {@linkplain
     * ConfigurableApplicationContext#refresh() refreshed}. If it has <strong>not</strong>
     * already been refreshed (the recommended approach), then the following will occur:
     * <ul>
     * <li>If the given context does not already have a {@linkplain
     * ConfigurableApplicationContext#setParent parent}, the root application context
     * will be set as the parent.</li>
     * <li>If the given context has not already been assigned an {@linkplain
     * ConfigurableApplicationContext#setId id}, one will be assigned to it</li>
     * <li>{@code ServletContext} and {@code ServletConfig} objects will be delegated to
     * the application context</li>
     * <li>{@link #postProcessWebApplicationContext} will be called</li>
     * <li>Any {@code ApplicationContextInitializer}s specified through the
     * "contextInitializerClasses" init-param or through the {@link
     * #setContextInitializers} property will be applied.</li>
     * <li>{@link ConfigurableApplicationContext#refresh refresh()} will be called if the
     * context implements {@link ConfigurableApplicationContext}</li>
     * </ul>
     * If the context has already been refreshed, none of the above will occur, under the
     * assumption that the user has performed these actions (or not) per their specific
     * needs.
     * <p>See {@link org.springframework.web.WebApplicationInitializer} for usage examples.
     *
     * @param webApplicationContext the context to use
     * @see #initWebApplicationContext
     * @see #configureAndRefreshWebApplicationContext
     * @see org.springframework.web.WebApplicationInitializer
     */
    public DispatcherServlet(WebApplicationContext webApplicationContext) {
        super(webApplicationContext);
    }

    /**
     * Set whether to detect all HandlerMapping beans in this servlet's context. Otherwise,
     * just a single bean with name "handlerMapping" will be expected.
     * <p>Default is "true". Turn this off if you want this servlet to use a single
     * HandlerMapping, despite multiple HandlerMapping beans being defined in the context.
     */
    public void setDetectAllHandlerMappings(boolean detectAllHandlerMappings) {
        this.detectAllHandlerMappings = detectAllHandlerMappings;
    }

    /**
     * Set whether to detect all HandlerAdapter beans in this servlet's context. Otherwise,
     * just a single bean with name "handlerAdapter" will be expected.
     * <p>Default is "true". Turn this off if you want this servlet to use a single
     * HandlerAdapter, despite multiple HandlerAdapter beans being defined in the context.
     */
    public void setDetectAllHandlerAdapters(boolean detectAllHandlerAdapters) {
        this.detectAllHandlerAdapters = detectAllHandlerAdapters;
    }

    /**
     * Set whether to detect all HandlerExceptionResolver beans in this servlet's context. Otherwise,
     * just a single bean with name "handlerExceptionResolver" will be expected.
     * <p>Default is "true". Turn this off if you want this servlet to use a single
     * HandlerExceptionResolver, despite multiple HandlerExceptionResolver beans being defined in the context.
     */
    public void setDetectAllHandlerExceptionResolvers(boolean detectAllHandlerExceptionResolvers) {
        this.detectAllHandlerExceptionResolvers = detectAllHandlerExceptionResolvers;
    }

    /**
     * Set whether to detect all ViewResolver beans in this servlet's context. Otherwise,
     * just a single bean with name "viewResolver" will be expected.
     * <p>Default is "true". Turn this off if you want this servlet to use a single
     * ViewResolver, despite multiple ViewResolver beans being defined in the context.
     */
    public void setDetectAllViewResolvers(boolean detectAllViewResolvers) {
        this.detectAllViewResolvers = detectAllViewResolvers;
    }

    /**
     * Set whether to perform cleanup of request attributes after an include request, that is,
     * whether to reset the original state of all request attributes after the DispatcherServlet
     * has processed within an include request. Otherwise, just the DispatcherServlet's own
     * request attributes will be reset, but not model attributes for JSPs or special attributes
     * set by views (for example, JSTL's).
     * <p>Default is "true", which is strongly recommended. Views should not rely on request attributes
     * having been set by (dynamic) includes. This allows JSP views rendered by an included controller
     * to use any model attributes, even with the same names as in the main JSP, without causing side
     * effects. Only turn this off for special needs, for example to deliberately allow main JSPs to
     * access attributes from JSP views rendered by an included controller.
     */
    public void setCleanupAfterInclude(boolean cleanupAfterInclude) {
        this.cleanupAfterInclude = cleanupAfterInclude;
    }

    @Override
    protected void onRefresh(ApplicationContext context) {
        // 此方法在applicationContext刷新之后被调用
        initStrategies(context);
    }

    /**
     * Initialize the strategy objects that this servlet uses.
     * <p>May be overridden in subclasses in order to initialize further strategy objects.
     * <p>
     *     初始化此servlet使用的各种策略对象
     * </p>
     * <p>
     *     此类可以被子类重写来提供更多的策略对象
     * </p>
     */
    protected void initStrategies(ApplicationContext context) {
        initMultipartResolver(context);
        initLocaleResolver(context);
        initThemeResolver(context);
        initHandlerMappings(context);
        initHandlerAdapters(context);
        initHandlerExceptionResolvers(context);
        initRequestToViewNameTranslator(context);
        initViewResolvers(context);
        initFlashMapManager(context);
    }

    /**
     * Initialize the MultipartResolver used by this class.
     * <p>If no bean is defined with the given name in the BeanFactory for this namespace,
     * no multipart handling is provided.
     */
    private void initMultipartResolver(ApplicationContext context) {
        try {
            this.multipartResolver = context.getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
        } catch (NoSuchBeanDefinitionException ex) {
            // Default is no multipart resolver.
            this.multipartResolver = null;
        }
    }

    /**
     * Initialize the LocaleResolver used by this class.
     * <p>If no bean is defined with the given name in the BeanFactory for this namespace,
     * we default to AcceptHeaderLocaleResolver.
     */
    private void initLocaleResolver(ApplicationContext context) {
        try {
            this.localeResolver = context.getBean(LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class);
        } catch (NoSuchBeanDefinitionException ex) {
            // We need to use the default.
            this.localeResolver = getDefaultStrategy(context, LocaleResolver.class);
        }
    }

    /**
     * Initialize the ThemeResolver used by this class.
     * <p>If no bean is defined with the given name in the BeanFactory for this namespace,
     * we default to a FixedThemeResolver.
     */
    private void initThemeResolver(ApplicationContext context) {
        try {
            this.themeResolver = context.getBean(THEME_RESOLVER_BEAN_NAME, ThemeResolver.class);
        } catch (NoSuchBeanDefinitionException ex) {
            // We need to use the default.
            this.themeResolver = getDefaultStrategy(context, ThemeResolver.class);
        }
    }

    /**
     * Initialize the HandlerMappings used by this class.
     * <p>If no HandlerMapping beans are defined in the BeanFactory for this namespace,
     * we default to BeanNameUrlHandlerMapping.
     */
    private void initHandlerMappings(ApplicationContext context) {
        this.handlerMappings = null;

        if (this.detectAllHandlerMappings) {
            // Find all HandlerMappings in the ApplicationContext, including ancestor contexts.
            Map<String, HandlerMapping> matchingBeans =
                    BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
            if (!matchingBeans.isEmpty()) {
                this.handlerMappings = new ArrayList<HandlerMapping>(matchingBeans.values());
                // We keep HandlerMappings in sorted order.
                OrderComparator.sort(this.handlerMappings);
            }
        } else {
            try {
                HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
                this.handlerMappings = Collections.singletonList(hm);
            } catch (NoSuchBeanDefinitionException ex) {
                // Ignore, we'll add a default HandlerMapping later.
            }
        }

        // Ensure we have at least one HandlerMapping, by registering
        // a default HandlerMapping if no other mappings are found.
        if (this.handlerMappings == null) {
            this.handlerMappings = getDefaultStrategies(context, HandlerMapping.class);
        }
    }

    /**
     * Initialize the HandlerAdapters used by this class.
     * <p>If no HandlerAdapter beans are defined in the BeanFactory for this namespace,
     * we default to SimpleControllerHandlerAdapter.
     */
    private void initHandlerAdapters(ApplicationContext context) {
        this.handlerAdapters = null;

        if (this.detectAllHandlerAdapters) {
            // Find all HandlerAdapters in the ApplicationContext, including ancestor contexts.
            Map<String, HandlerAdapter> matchingBeans =
                    BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerAdapter.class, true, false);
            if (!matchingBeans.isEmpty()) {
                this.handlerAdapters = new ArrayList<HandlerAdapter>(matchingBeans.values());
                // We keep HandlerAdapters in sorted order.
                OrderComparator.sort(this.handlerAdapters);
            }
        } else {
            try {
                HandlerAdapter ha = context.getBean(HANDLER_ADAPTER_BEAN_NAME, HandlerAdapter.class);
                this.handlerAdapters = Collections.singletonList(ha);
            } catch (NoSuchBeanDefinitionException ex) {
                // Ignore, we'll add a default HandlerAdapter later.
            }
        }

        // Ensure we have at least some HandlerAdapters, by registering
        // default HandlerAdapters if no other adapters are found.
        if (this.handlerAdapters == null) {
            this.handlerAdapters = getDefaultStrategies(context, HandlerAdapter.class);
        }
    }

    /**
     * Initialize the HandlerExceptionResolver used by this class.
     * <p>If no bean is defined with the given name in the BeanFactory for this namespace,
     * we default to no exception resolver.
     */
    private void initHandlerExceptionResolvers(ApplicationContext context) {
        this.handlerExceptionResolvers = null;

        if (this.detectAllHandlerExceptionResolvers) {
            // Find all HandlerExceptionResolvers in the ApplicationContext, including ancestor contexts.
            Map<String, HandlerExceptionResolver> matchingBeans = BeanFactoryUtils
                    .beansOfTypeIncludingAncestors(context, HandlerExceptionResolver.class, true, false);
            if (!matchingBeans.isEmpty()) {
                this.handlerExceptionResolvers = new ArrayList<HandlerExceptionResolver>(matchingBeans.values());
                // We keep HandlerExceptionResolvers in sorted order.
                OrderComparator.sort(this.handlerExceptionResolvers);
            }
        } else {
            try {
                HandlerExceptionResolver her = context.getBean(HANDLER_EXCEPTION_RESOLVER_BEAN_NAME, HandlerExceptionResolver.class);
                this.handlerExceptionResolvers = Collections.singletonList(her);
            } catch (NoSuchBeanDefinitionException ex) {
                // Ignore, no HandlerExceptionResolver is fine too.
            }
        }

        // Ensure we have at least some HandlerExceptionResolvers, by registering
        // default HandlerExceptionResolvers if no other resolvers are found.
        if (this.handlerExceptionResolvers == null) {
            this.handlerExceptionResolvers = getDefaultStrategies(context, HandlerExceptionResolver.class);
        }
    }

    /**
     * Initialize the RequestToViewNameTranslator used by this servlet instance.
     * <p>If no implementation is configured then we default to DefaultRequestToViewNameTranslator.
     */
    private void initRequestToViewNameTranslator(ApplicationContext context) {
        try {
            this.viewNameTranslator = context.getBean(REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME, RequestToViewNameTranslator.class);
        } catch (NoSuchBeanDefinitionException ex) {
            // We need to use the default.
            this.viewNameTranslator = getDefaultStrategy(context, RequestToViewNameTranslator.class);
        }
    }

    /**
     * Initialize the ViewResolvers used by this class.
     * <p>If no ViewResolver beans are defined in the BeanFactory for this
     * namespace, we default to InternalResourceViewResolver.
     */
    private void initViewResolvers(ApplicationContext context) {
        this.viewResolvers = null;

        if (this.detectAllViewResolvers) {
            // Find all ViewResolvers in the ApplicationContext, including ancestor contexts.
            Map<String, ViewResolver> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(context, ViewResolver.class, true, false);
            if (!matchingBeans.isEmpty()) {
                this.viewResolvers = new ArrayList<ViewResolver>(matchingBeans.values());
                // We keep ViewResolvers in sorted order.
                OrderComparator.sort(this.viewResolvers);
            }
        } else {
            try {
                ViewResolver vr = context.getBean(VIEW_RESOLVER_BEAN_NAME, ViewResolver.class);
                this.viewResolvers = Collections.singletonList(vr);
            } catch (NoSuchBeanDefinitionException ex) {
                // Ignore, we'll add a default ViewResolver later.
            }
        }

        // Ensure we have at least one ViewResolver, by registering
        // a default ViewResolver if no other resolvers are found.
        if (this.viewResolvers == null) {
            this.viewResolvers = getDefaultStrategies(context, ViewResolver.class);
        }
    }

    /**
     * Initialize the {@link FlashMapManager} used by this servlet instance.
     * <p>If no implementation is configured then we default to
     * {@code org.springframework.web.servlet.support.DefaultFlashMapManager}.
     */
    private void initFlashMapManager(ApplicationContext context) {
        try {
            this.flashMapManager = context.getBean(FLASH_MAP_MANAGER_BEAN_NAME, FlashMapManager.class);
        } catch (NoSuchBeanDefinitionException ex) {
            // We need to use the default.
            this.flashMapManager = getDefaultStrategy(context, FlashMapManager.class);
        }
    }

    /**
     * Return this servlet's ThemeSource, if any; else return {@code null}.
     * <p>Default is to return the WebApplicationContext as ThemeSource,
     * provided that it implements the ThemeSource interface.
     *
     * @return the ThemeSource, if any
     * @see #getWebApplicationContext()
     */
    private ThemeSource getThemeSource() {
        if (getWebApplicationContext() instanceof ThemeSource) {
            return (ThemeSource) getWebApplicationContext();
        } else {
            return null;
        }
    }

    /**
     * Obtain this servlet's MultipartResolver, if any.
     *
     * @return the MultipartResolver used by this servlet, or {@code null} if none
     * (indicating that no multipart support is available)
     */
    public final MultipartResolver getMultipartResolver() {
        return this.multipartResolver;
    }

    /**
     * Return the default strategy object for the given strategy interface.
     * <p>The default implementation delegates to {@link #getDefaultStrategies},
     * expecting a single object in the list.
     *
     * @param context           the current WebApplicationContext
     * @param strategyInterface the strategy interface
     * @return the corresponding strategy object
     * @see #getDefaultStrategies
     */
    private <T> T getDefaultStrategy(ApplicationContext context, Class<T> strategyInterface) {
        List<T> strategies = getDefaultStrategies(context, strategyInterface);
        if (strategies.size() != 1) {
            throw new BeanInitializationException("DispatcherServlet needs exactly 1 strategy for interface [" + strategyInterface.getName() + "]");
        }
        return strategies.get(0);
    }

    /**
     * Create a List of default strategy objects for the given strategy interface.
     * <p>The default implementation uses the "DispatcherServlet.properties" file (in the same
     * package as the DispatcherServlet class) to determine the class names. It instantiates
     * the strategy objects through the context's BeanFactory.
     *
     * @param context           the current WebApplicationContext
     * @param strategyInterface the strategy interface
     * @return the List of corresponding strategy objects
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> getDefaultStrategies(ApplicationContext context, Class<T> strategyInterface) {
        String key = strategyInterface.getName();
        String value = DEFAULT_STRATEGIES.getProperty(key);
        if (value != null) {
            String[] classNames = StringUtils.commaDelimitedListToStringArray(value);
            List<T> strategies = new ArrayList<T>(classNames.length);
            for (String className : classNames) {
                try {
                    Class<?> clazz = ClassUtils.forName(className, DispatcherServlet.class.getClassLoader());
                    Object strategy = createDefaultStrategy(context, clazz);
                    strategies.add((T) strategy);
                } catch (ClassNotFoundException ex) {
                    throw new BeanInitializationException(
                            "Could not find DispatcherServlet's default strategy class [" + className + "] for interface [" + key + "]", ex);
                } catch (LinkageError err) {
                    throw new BeanInitializationException(
                            "Error loading DispatcherServlet's default strategy class [" + className + "] for interface [" + key + "]: problem with class file or dependent class", err);
                }
            }
            return strategies;
        } else {
            return new LinkedList<T>();
        }
    }

    /**
     * Create a default strategy.
     * <p>The default implementation uses {@link org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean}.
     *
     * @param context the current WebApplicationContext
     * @param clazz   the strategy implementation class to instantiate
     * @return the fully configured strategy instance
     * @see org.springframework.context.ApplicationContext#getAutowireCapableBeanFactory()
     * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#createBean
     */
    private Object createDefaultStrategy(ApplicationContext context, Class<?> clazz) {
        return context.getAutowireCapableBeanFactory().createBean(clazz);
    }

    /**
     * 做一些数据初始化设置，主要是把需要一些springMvc相关的对象设置到httpRequest的attribute里，
     * 以便后续使用
     */
    @Override
    protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {

        //在include情况下保持一份http请求的属性快照,以便在include之后恢复原始属性
        Map<String, Object> attributesSnapshot = null;
        if (WebUtils.isIncludeRequest(request)) {
            attributesSnapshot = new HashMap<>();
            Enumeration<?> attrNames = request.getAttributeNames();
            while (attrNames.hasMoreElements()) {
                String attrName = (String) attrNames.nextElement();
                if (this.cleanupAfterInclude || attrName.startsWith("org.springframework.web.servlet")) {
                    attributesSnapshot.put(attrName, request.getAttribute(attrName));
                }
            }
        }

        //把spring中的对象设置到http请求中，以便在view和control层可以使用
        request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
        request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
        request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
        request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());

        FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
        if (inputFlashMap != null) {
            request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
        }
        request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
        request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);

        try {
            doDispatch(request, response);
        } finally {
            if (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
                // Restore the original attribute snapshot, in case of an include.
                //在http include情况下，同时在非异步情况下，恢复原始属性
                if (attributesSnapshot != null) {
                    restoreAttributesAfterInclude(request, attributesSnapshot);
                }
            }
        }
    }

    /**
     * Process the actual dispatching to the handler.
     * <p>The handler will be obtained by applying the servlet's HandlerMappings in order.
     * The HandlerAdapter will be obtained by querying the servlet's installed HandlerAdapters
     * to find the first that supports the handler class.
     * <p>All HTTP methods are handled by this method. It's up to HandlerAdapters or handlers
     * themselves to decide which methods are acceptable.
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @throws Exception in case of any kind of processing failure
     */
    private void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        //变量声明，变量声明的位置决定的使用的作用域
        HttpServletRequest processedRequest = request;
        HandlerExecutionChain handlerChain = null;
        boolean multipartRequestParsed = false;

        WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

        try {
            ModelAndView mv = null;
            Exception dispatchException = null;

            try {
                //此try-catch块主要是handler调用，同时也包含了handler责任链的前置和后置调用以及责任链构建
                processedRequest = checkMultipart(request);
                //解析后的request和原始的不相等，则判断的文件上传http请求
                multipartRequestParsed = (processedRequest != request);

                // Determine handler for the current request.
                handlerChain = getHandlerChain(processedRequest);
                if (handlerChain == null || handlerChain.getHandler() == null) {
                    //没有找到对应的handler，直接返回404给客户端
                    //TODO 测试在web.xml中指定404错误码
                    noHandlerFound(processedRequest, response);
                    return;
                }

                // Determine handler adapter for the current request.
                HandlerAdapter ha = getHandlerAdapter(handlerChain.getHandler());

                // Process last-modified header, if supported by the handler.
                String method = request.getMethod();
                boolean isGet = "GET".equals(method);
                if (isGet || "HEAD".equals(method)) {
                    long lastModified = ha.getLastModified(request, handlerChain.getHandler());
                    //判断是否返回数据没有修改,直接返回,提高性能
                    if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
                        return;
                    }
                }

                //调用过滤器
                if (!handlerChain.applyPreHandle(processedRequest, response)) {
                    return;
                }

                //调用具体的处理http请求的handler
                mv = ha.handle(processedRequest, response, handlerChain.getHandler());

                if (asyncManager.isConcurrentHandlingStarted()) {
                    return;
                }

                applyDefaultViewName(request, mv);
                handlerChain.applyPostHandle(processedRequest, response, mv);
            } catch (Exception ex) {
                dispatchException = ex;
            }
            processDispatchResult(processedRequest, response, handlerChain, mv, dispatchException);
        } catch (Exception ex) {
            triggerAfterCompletion(processedRequest, response, handlerChain, ex);
        } catch (Error err) {
            triggerAfterCompletionWithError(processedRequest, response, handlerChain, err);
        } finally {
            if (asyncManager.isConcurrentHandlingStarted()) {
                // Instead of postHandle and afterCompletion
                if (handlerChain != null) {
                    handlerChain.applyAfterConcurrentHandlingStarted(processedRequest, response);
                }
            } else {
                // Clean up any resources used by a multipart request.
                if (multipartRequestParsed) {
                    cleanupMultipart(processedRequest);
                }
            }
        }
    }

    /**
     * Do we need view name translation?
     */
    private void applyDefaultViewName(HttpServletRequest request, ModelAndView mv) throws Exception {
        if (mv != null && !mv.hasView()) {
            mv.setViewName(getDefaultViewName(request));
        }
    }

    /**
     * Handle the result of handler selection and handler invocation, which is
     * either a ModelAndView or an Exception to be resolved to a ModelAndView.
     */
    private void processDispatchResult(HttpServletRequest request, HttpServletResponse response, HandlerExecutionChain mappedHandler, ModelAndView mv, Exception exception) throws Exception {

        boolean errorView = false;

        if (exception != null) {
            if (exception instanceof ModelAndViewDefiningException) {
                mv = ((ModelAndViewDefiningException) exception).getModelAndView();
            } else {
                Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
                mv = processHandlerException(request, response, handler, exception);
                errorView = (mv != null);
            }
        }

        // Did the handler return a view to render?
        if (mv != null && !mv.wasCleared()) {
            render(mv, request, response);
            if (errorView) {
                WebUtils.clearErrorRequestAttributes(request);
            }
        }

        if (WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
            // Concurrent handling started during a forward
            return;
        }

        if (mappedHandler != null) {
            mappedHandler.triggerAfterCompletion(request, response, null);
        }
    }

    /**
     * Build a LocaleContext for the given request, exposing the request's primary locale as current locale.
     * <p>The default implementation uses the dispatcher's LocaleResolver to obtain the current locale,
     * which might change during a request.
     *
     * @param request current HTTP request
     * @return the corresponding LocaleContext
     */
    @Override
    protected LocaleContext buildLocaleContext(final HttpServletRequest request) {
        return new LocaleContext() {
            public Locale getLocale() {
                return localeResolver.resolveLocale(request);
            }

            public String toString() {
                return getLocale().toString();
            }
        };
    }

    /**
     * Convert the request into a multipart request, and make multipart resolver available.
     * <p>If no multipart resolver is set, simply use the existing request.
     *
     * @param request current HTTP request
     * @return the processed request (multipart wrapper if necessary)
     * @see MultipartResolver#resolveMultipart
     */
    private HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
        if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
            if (WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class) == null) {
                return this.multipartResolver.resolveMultipart(request);
            }
        }
        return request;
    }

    /**
     * Clean up any resources used by the given multipart request (if any).
     *
     * @param request current HTTP request
     * @see MultipartResolver#cleanupMultipart
     */
    private void cleanupMultipart(HttpServletRequest request) {
        MultipartHttpServletRequest multipartRequest = WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
        if (multipartRequest != null) {
            this.multipartResolver.cleanupMultipart(multipartRequest);
        }
    }


    /**
     * Return the HandlerExecutionChain for this request.
     * <p>Tries all handler mappings in order.
     *
     * @param request current HTTP request
     * @return the HandlerExecutionChain, or {@code null} if no handler could be found
     */
    private HandlerExecutionChain getHandlerChain(HttpServletRequest request) throws Exception {

        // 这里使用handlerMapping的列表来找到对应exeChain主要是为了保证可扩展性,即开闭原则<p>
        // 因为spring中可以通过@RequestMapping或者其它方式来注明具体的处理器<p>
        // 但是,如果用一个大而全的HandlerMapping来实现所有方式的映射,那么会存在问题:<p>
        // 如果新增加一个handler注明方式,那么就需要去修改大而全的HandlerMapping.这验证违反了开闭原则,扩展起来非常困难!!<p>
        // 而使用handlerMapping的列表则可以很好的解决这一点,对于每一种新的handler处理类型,只需要增加一个对应的handlerMapping即可<p>
        // 同时再借助spring自己的注入发现即可把所有实现HandlerMapping接口的实现类都注入到系统中,达到无缝扩展
        for (HandlerMapping hm : this.handlerMappings) {
            HandlerExecutionChain handlerChain = hm.getHandler(request);
            if (handlerChain != null) {
                return handlerChain;
            }
        }
        return null;
    }

    /**
     * No handler found -> set appropriate HTTP response status.
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @throws Exception if preparing the response failed
     */
    private void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * 返回当前映射的handler的调用适配器
     *
     * @param handler 用来寻找适配器的handler
     * @throws ServletException 如果没有对应的handler适配器被找到，抛出此异常.这是个致命错误.
     */
    private HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
        //使用列表处理，是为了满足开闭原则.即可以无需修改现有代码，新增handlerAdapter
        for (HandlerAdapter ha : this.handlerAdapters) {
            if (ha.supports(handler)) {
                return ha;
            }
        }
        throw new ServletException("No adapter for handler [" + handler + "]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");
    }

    /**
     * Determine an error ModelAndView via the registered HandlerExceptionResolvers.
     *
     * @param request  current HTTP request
     * @param response current HTTP response
     * @param handler  the executed handler, or {@code null} if none chosen at the time of the exception
     *                 (for example, if multipart resolution failed)
     * @param ex       the exception that got thrown during handler execution
     * @return a corresponding ModelAndView to forward to
     * @throws Exception if no error ModelAndView found
     */
    private ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        // Check registered HandlerExceptionResolvers...
        ModelAndView exMv = null;
        for (HandlerExceptionResolver handlerExceptionResolver : this.handlerExceptionResolvers) {
            exMv = handlerExceptionResolver.resolveException(request, response, handler, ex);
            if (exMv != null) {
                break;
            }
        }
        if (exMv != null) {
            if (exMv.isEmpty()) {
                return null;
            }
            // We might still need view name translation for a plain error model...
            if (!exMv.hasView()) {
                exMv.setViewName(getDefaultViewName(request));
            }
            WebUtils.exposeErrorRequestAttributes(request, ex, getServletName());
            return exMv;
        }

        throw ex;
    }

    /**
     * Render the given ModelAndView.
     * <p>This is the last stage in handling a request. It may involve resolving the view by name.
     *
     * @param mv       the ModelAndView to render
     * @param request  current HTTP servlet request
     * @param response current HTTP servlet response
     * @throws ServletException if view is missing or cannot be resolved
     * @throws Exception        if there's a problem rendering the view
     */
    protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Determine locale for request and apply it to the response.
        Locale locale = this.localeResolver.resolveLocale(request);
        response.setLocale(locale);

        View view;
        if (mv.isReference()) {
            // We need to resolve the view name.
            view = resolveViewName(mv.getViewName(), mv.getModelInternal(), locale, request);
            if (view == null) {
                throw new ServletException("Could not resolve view with name '" + mv.getViewName() +
                        "' in servlet with name '" + getServletName() + "'");
            }
        } else {
            // No need to lookup: the ModelAndView object contains the actual View object.
            view = mv.getView();
            if (view == null) {
                throw new ServletException("ModelAndView [" + mv + "] neither contains a view name nor a " +
                        "View object in servlet with name '" + getServletName() + "'");
            }
        }

        // Delegate to the View object for rendering.
        view.render(mv.getModelInternal(), request, response);
    }

    /**
     * Translate the supplied request into a default view name.
     *
     * @param request current HTTP servlet request
     * @return the view name (or {@code null} if no default found)
     * @throws Exception if view name translation failed
     */
    private String getDefaultViewName(HttpServletRequest request) throws Exception {
        return this.viewNameTranslator.getViewName(request);
    }

    /**
     * Resolve the given view name into a View object (to be rendered).
     * <p>The default implementations asks all ViewResolvers of this dispatcher.
     * Can be overridden for custom resolution strategies, potentially based on
     * specific model attributes or request parameters.
     *
     * @param viewName the name of the view to resolve
     * @param model    the model to be passed to the view
     * @param locale   the current locale
     * @param request  current HTTP servlet request
     * @return the View object, or {@code null} if none found
     * @throws Exception if the view cannot be resolved
     *                   (typically in case of problems creating an actual View object)
     * @see ViewResolver#resolveViewName
     */
    protected View resolveViewName(String viewName, Map<String, Object> model, Locale locale, HttpServletRequest request) throws Exception {

        for (ViewResolver viewResolver : this.viewResolvers) {
            View view = viewResolver.resolveViewName(viewName, locale);
            if (view != null) {
                return view;
            }
        }
        return null;
    }

    private void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response, HandlerExecutionChain mappedHandler, Exception ex) throws Exception {

        if (mappedHandler != null) {
            mappedHandler.triggerAfterCompletion(request, response, ex);
        }
        throw ex;
    }

    private void triggerAfterCompletionWithError(HttpServletRequest request, HttpServletResponse response, HandlerExecutionChain mappedHandler, Error error) throws Exception {

        ServletException ex = new NestedServletException("Handler processing failed", error);
        if (mappedHandler != null) {
            mappedHandler.triggerAfterCompletion(request, response, ex);
        }
        throw ex;
    }

    /**
     * Restore the request attributes after an include.
     *
     * @param request            current HTTP request
     * @param attributesSnapshot the snapshot of the request attributes before the include
     */
    @SuppressWarnings("unchecked")
    private void restoreAttributesAfterInclude(HttpServletRequest request, Map<?, ?> attributesSnapshot) {
        // Need to copy into separate Collection here, to avoid side effects
        // on the Enumeration when removing attributes.
        Set<String> attrsToCheck = new HashSet<String>();
        Enumeration<?> attrNames = request.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = (String) attrNames.nextElement();
            if (this.cleanupAfterInclude || attrName.startsWith("org.springframework.web.servlet")) {
                attrsToCheck.add(attrName);
            }
        }

        // Add attributes that may have been removed
        attrsToCheck.addAll((Set<String>) attributesSnapshot.keySet());

        // Iterate over the attributes to check, restoring the original value
        // or removing the attribute, respectively, if appropriate.
        for (String attrName : attrsToCheck) {
            Object attrValue = attributesSnapshot.get(attrName);
            if (attrValue == null) {
                request.removeAttribute(attrName);
            } else if (attrValue != request.getAttribute(attrName)) {
                request.setAttribute(attrName, attrValue);
            }
        }
    }

}
