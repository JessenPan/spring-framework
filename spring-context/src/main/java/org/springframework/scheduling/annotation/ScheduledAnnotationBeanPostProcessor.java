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

package org.springframework.scheduling.annotation;

import org.apache.commons.logging.LogFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Bean post-processor that registers methods annotated with @{@link Scheduled}
 * to be invoked by a {@link org.springframework.scheduling.TaskScheduler} according
 * to the "fixedRate", "fixedDelay", or "cron" expression provided via the annotation.
 * <p>
 * <p>This post-processor is automatically registered by Spring's
 * {@code <task:annotation-driven>} XML element, and also by the
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @{@link EnableScheduling} annotation.
 * <p>
 * <p>Autodetects any {@link SchedulingConfigurer} instances in the container,
 * allowing for customization of the scheduler to be used or for fine-grained
 * control over task registration (e.g. registration of {@link Trigger} tasks.
 * See the @{@link EnableScheduling} javadocs for complete usage details.
 * @see Scheduled
 * @see EnableScheduling
 * @see SchedulingConfigurer
 * @see org.springframework.scheduling.TaskScheduler
 * @see org.springframework.scheduling.config.ScheduledTaskRegistrar
 * @since 3.0
 */
public class ScheduledAnnotationBeanPostProcessor
        implements BeanPostProcessor, Ordered, EmbeddedValueResolverAware, ApplicationContextAware,
        ApplicationListener<ContextRefreshedEvent>, DisposableBean {

    private final ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
    private final Map<Class<?>, Boolean> nonAnnotatedClasses = new ConcurrentHashMap<Class<?>, Boolean>(64);
    private Object scheduler;
    private StringValueResolver embeddedValueResolver;
    private ApplicationContext applicationContext;

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

    /**
     * Set the {@link org.springframework.scheduling.TaskScheduler} that will invoke
     * the scheduled methods, or a {@link java.util.concurrent.ScheduledExecutorService}
     * to be wrapped as a TaskScheduler.
     */
    public void setScheduler(Object scheduler) {
        this.scheduler = scheduler;
    }

    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext() != this.applicationContext) {
            return;
        }

        if (this.scheduler != null) {
            this.registrar.setScheduler(this.scheduler);
        }

        Map<String, SchedulingConfigurer> configurers =
                this.applicationContext.getBeansOfType(SchedulingConfigurer.class);
        for (SchedulingConfigurer configurer : configurers.values()) {
            configurer.configureTasks(this.registrar);
        }

        if (this.registrar.hasTasks() && this.registrar.getScheduler() == null) {
            Map<String, ? super Object> schedulers = new HashMap<String, Object>();
            schedulers.putAll(this.applicationContext.getBeansOfType(TaskScheduler.class));
            schedulers.putAll(this.applicationContext.getBeansOfType(ScheduledExecutorService.class));
            if (schedulers.size() == 0) {
                // do nothing -> fall back to default scheduler
            } else if (schedulers.size() == 1) {
                this.registrar.setScheduler(schedulers.values().iterator().next());
            } else if (schedulers.size() >= 2) {
                throw new IllegalStateException("More than one TaskScheduler and/or ScheduledExecutorService  " +
                        "exist within the context. Remove all but one of the beans; or implement the " +
                        "SchedulingConfigurer interface and call ScheduledTaskRegistrar#setScheduler " +
                        "explicitly within the configureTasks() callback. Found the following beans: " +
                        schedulers.keySet());
            }
        }

        this.registrar.afterPropertiesSet();
    }


    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    public Object postProcessAfterInitialization(final Object bean, String beanName) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        if (!this.nonAnnotatedClasses.containsKey(targetClass)) {
            final Set<Method> annotatedMethods = new LinkedHashSet<Method>(1);
            ReflectionUtils.doWithMethods(targetClass, new MethodCallback() {
                public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                    Scheduled scheduled = AnnotationUtils.getAnnotation(method, Scheduled.class);
                    if (scheduled != null) {
                        processScheduled(scheduled, method, bean);
                        annotatedMethods.add(method);
                    }
                }
            });
            if (annotatedMethods.isEmpty()) {
                this.nonAnnotatedClasses.put(targetClass, Boolean.TRUE);
            }
        }
        return bean;
    }

    private void processScheduled(Scheduled scheduled, Method method, Object bean) {
        try {
            Assert.isTrue(void.class.equals(method.getReturnType()),
                    "Only void-returning methods may be annotated with @Scheduled");
            Assert.isTrue(method.getParameterTypes().length == 0,
                    "Only no-arg methods may be annotated with @Scheduled");

            if (AopUtils.isJdkDynamicProxy(bean)) {
                try {
                    // Found a @Scheduled method on the target class for this JDK proxy ->
                    // is it also present on the proxy itself?
                    method = bean.getClass().getMethod(method.getName(), method.getParameterTypes());
                } catch (SecurityException ex) {
                    ReflectionUtils.handleReflectionException(ex);
                } catch (NoSuchMethodException ex) {
                    throw new IllegalStateException(String.format(
                            "@Scheduled method '%s' found on bean target class '%s' but not " +
                                    "found in any interface(s) for a dynamic proxy. Either pull the " +
                                    "method up to a declared interface or switch to subclass (CGLIB) " +
                                    "proxies by setting proxy-target-class/proxyTargetClass to 'true'",
                            method.getName(), method.getDeclaringClass().getSimpleName()));
                }
            } else if (AopUtils.isCglibProxy(bean)) {
                // Common problem: private methods end up in the proxy instance, not getting delegated.
                if (Modifier.isPrivate(method.getModifiers())) {
                    LogFactory.getLog(ScheduledAnnotationBeanPostProcessor.class).warn(String.format(
                            "@Scheduled method '%s' found on CGLIB proxy for target class '%s' but cannot " +
                                    "be delegated to target bean. Switch its visibility to package or protected.",
                            method.getName(), method.getDeclaringClass().getSimpleName()));
                }
            }

            Runnable runnable = new ScheduledMethodRunnable(bean, method);
            boolean processedSchedule = false;
            String errorMessage =
                    "Exactly one of the 'cron', 'fixedDelay(String)', or 'fixedRate(String)' attributes is required";

            // Determine initial delay
            long initialDelay = scheduled.initialDelay();
            String initialDelayString = scheduled.initialDelayString();
            if (StringUtils.hasText(initialDelayString)) {
                Assert.isTrue(initialDelay < 0, "Specify 'initialDelay' or 'initialDelayString', not both");
                if (this.embeddedValueResolver != null) {
                    initialDelayString = this.embeddedValueResolver.resolveStringValue(initialDelayString);
                }
                try {
                    initialDelay = Integer.parseInt(initialDelayString);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(
                            "Invalid initialDelayString value \"" + initialDelayString + "\" - cannot parse into integer");
                }
            }

            // Check cron expression
            String cron = scheduled.cron();
            if (StringUtils.hasText(cron)) {
                Assert.isTrue(initialDelay == -1, "'initialDelay' not supported for cron triggers");
                processedSchedule = true;
                if (this.embeddedValueResolver != null) {
                    cron = this.embeddedValueResolver.resolveStringValue(cron);
                }
                this.registrar.addCronTask(new CronTask(runnable, cron));
            }
            // At this point we don't need to differentiate between initial delay set or not anymore
            if (initialDelay < 0) {
                initialDelay = 0;
            }

            // Check fixed delay
            long fixedDelay = scheduled.fixedDelay();
            if (fixedDelay >= 0) {
                Assert.isTrue(!processedSchedule, errorMessage);
                processedSchedule = true;
                this.registrar.addFixedDelayTask(new IntervalTask(runnable, fixedDelay, initialDelay));
            }
            String fixedDelayString = scheduled.fixedDelayString();
            if (StringUtils.hasText(fixedDelayString)) {
                Assert.isTrue(!processedSchedule, errorMessage);
                processedSchedule = true;
                if (this.embeddedValueResolver != null) {
                    fixedDelayString = this.embeddedValueResolver.resolveStringValue(fixedDelayString);
                }
                try {
                    fixedDelay = Integer.parseInt(fixedDelayString);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(
                            "Invalid fixedDelayString value \"" + fixedDelayString + "\" - cannot parse into integer");
                }
                this.registrar.addFixedDelayTask(new IntervalTask(runnable, fixedDelay, initialDelay));
            }

            // Check fixed rate
            long fixedRate = scheduled.fixedRate();
            if (fixedRate >= 0) {
                Assert.isTrue(!processedSchedule, errorMessage);
                processedSchedule = true;
                this.registrar.addFixedRateTask(new IntervalTask(runnable, fixedRate, initialDelay));
            }
            String fixedRateString = scheduled.fixedRateString();
            if (StringUtils.hasText(fixedRateString)) {
                Assert.isTrue(!processedSchedule, errorMessage);
                processedSchedule = true;
                if (this.embeddedValueResolver != null) {
                    fixedRateString = this.embeddedValueResolver.resolveStringValue(fixedRateString);
                }
                try {
                    fixedRate = Integer.parseInt(fixedRateString);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(
                            "Invalid fixedRateString value \"" + fixedRateString + "\" - cannot parse into integer");
                }
                this.registrar.addFixedRateTask(new IntervalTask(runnable, fixedRate, initialDelay));
            }

            // Check whether we had any attribute set
            Assert.isTrue(processedSchedule, errorMessage);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Encountered invalid @Scheduled method '" + method.getName() + "': " + ex.getMessage());
        }
    }


    public void destroy() throws Exception {
        this.registrar.destroy();
    }

}
