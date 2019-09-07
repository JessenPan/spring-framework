/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.web.servlet.config.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Add this annotation to an {@code @Configuration} class to have the Spring MVC
 * configuration defined in {@link WebMvcConfigurationSupport} imported:
 * <p>
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebMvc
 * &#064;ComponentScan(basePackageClasses = { MyConfiguration.class })
 * public class MyWebConfiguration {
 * <p>
 * }
 * </pre>
 * <p>Customize the imported configuration by implementing the
 * {@link WebMvcConfigurer} interface or more likely by extending the
 * {@link WebMvcConfigurerAdapter} base class and overriding individual methods:
 * <p>
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableWebMvc
 * &#064;ComponentScan(basePackageClasses = { MyConfiguration.class })
 * public class MyConfiguration extends WebMvcConfigurerAdapter {
 * <p>
 * &#064;Override
 * public void addFormatters(FormatterRegistry formatterRegistry) {
 * formatterRegistry.addConverter(new MyConverter());
 * }
 * <p>
 * &#064;Override
 * public void configureMessageConverters(List&lt;HttpMessageConverter&lt;?&gt;&gt; converters) {
 * converters.add(new MyHttpMessageConverter());
 * }
 * <p>
 * // More overridden methods ...
 * }
 * </pre>
 * <p>
 * <p>If the customization options of {@link WebMvcConfigurer} do not expose
 * something you need to configure, consider removing the {@code @EnableWebMvc}
 * annotation and extending directly from {@link WebMvcConfigurationSupport}
 * overriding selected {@code @Bean} methods:
 * <p>
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan(basePackageClasses = { MyConfiguration.class })
 * public class MyConfiguration extends WebMvcConfigurationSupport {
 * <p>
 * &#064;Override
 * public void addFormatters(FormatterRegistry formatterRegistry) {
 * formatterRegistry.addConverter(new MyConverter());
 * }
 * <p>
 * &#064;Bean
 * public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
 * // Create or delegate to "super" to create and
 * // customize properties of RequestMapingHandlerAdapter
 * }
 * }
 * </pre>
 *
 * @author Dave Syer
 * @author Rossen Stoyanchev
 * @see WebMvcConfigurer
 * @see WebMvcConfigurerAdapter
 * @since 3.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebMvcConfiguration.class)
public @interface EnableWebMvc {
}
