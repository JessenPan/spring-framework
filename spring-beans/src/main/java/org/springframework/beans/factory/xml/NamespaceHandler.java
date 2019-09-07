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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Base interface used by the {@link DefaultBeanDefinitionDocumentReader}
 * for handling custom namespaces in a Spring XML configuration file.
 * <p>
 * 基础接口,它被{@link DefaultBeanDefinitionDocumentReader}用来处理spring配置文件里的自定义命名空间
 * <p>Implementations are expected to return implementations of the
 * {@link BeanDefinitionParser} interface for custom top-level tags and
 * implementations of the {@link BeanDefinitionDecorator} interface for
 * custom nested tags.
 * <p>
 * 此接口实现者针对自定义顶级标签需要返回beanDefinitionParser接口,对自定义内嵌标签返回beanDefinitionDecorator
 * <p>The parser will call {@link #parse} when it encounters a custom tag
 * directly under the {@code &lt;beans&gt;} tags and {@link #decorate} when
 * it encounters a custom tag directly under a {@code &lt;bean&gt;} tag.
 * <p>
 * 当遇到beans下面的自定义标签时，解析器会直接调用{@link #parse}方法. 当遇到bean标签下的自定义标签时，会调用{@link #decorate}
 * 方法
 * <p>Developers writing their own custom element extensions typically will
 * not implement this interface directly, but rather make use of the provided
 * {@link NamespaceHandlerSupport} class.
 * <br/>
 * 当开发者定义他们自己的自定义标签时，它们不需要直接继承此接口，可以使用已经提供好的{@link NamespaceHandlerSupport}类
 *
 * @author Rob Harrop
 * @author Erik Wiersma
 * @see DefaultBeanDefinitionDocumentReader
 * @see NamespaceHandlerResolver
 * @since 2.0
 */
public interface NamespaceHandler {

    /**
     * Invoked by the {@link DefaultBeanDefinitionDocumentReader} after
     * construction but before any custom elements are parsed.
     * <br/>
     * 初始化方法,在此实例构造完成之后，但在任何自定义元素被解析之前调用
     *
     * @see NamespaceHandlerSupport#registerBeanDefinitionParser(String, BeanDefinitionParser)
     */
    void init();

    /**
     * Parse the specified {@link Element} and register any resulting
     * {@link BeanDefinition BeanDefinitions} with the
     * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
     * that is embedded in the supplied {@link ParserContext}.
     * <p>Implementations should return the primary {@code BeanDefinition}
     * that results from the parse phase if they wish to be used nested
     * inside (for example) a {@code &lt;property&gt;} tag.
     * <p>Implementations may return {@code null} if they will
     * <strong>not</strong> be used in a nested scenario.
     *
     * @param element       the element that is to be parsed into one or more {@code BeanDefinitions}
     * @param parserContext the object encapsulating the current state of the parsing process
     * @return the primary {@code BeanDefinition} (can be {@code null} as explained above)
     */
    BeanDefinition parse(Element element, ParserContext parserContext);

    /**
     * Parse the specified {@link Node} and decorate the supplied
     * {@link BeanDefinitionHolder}, returning the decorated definition.
     * <p>The {@link Node} may be either an {@link org.w3c.dom.Attr} or an
     * {@link Element}, depending on whether a custom attribute or element
     * is being parsed.
     * <p>Implementations may choose to return a completely new definition,
     * which will replace the original definition in the resulting
     * {@link org.springframework.beans.factory.BeanFactory}.
     * <p>The supplied {@link ParserContext} can be used to register any
     * additional beans needed to support the main definition.
     *
     * @param source        the source element or attribute that is to be parsed
     * @param definition    the current bean definition
     * @param parserContext the object encapsulating the current state of the parsing process
     * @return the decorated definition (to be registered in the BeanFactory),
     * or simply the original bean definition if no decoration is required.
     * A {@code null} value is strictly speaking invalid, but will be leniently
     * treated like the case where the original bean definition gets returned.
     */
    BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder definition, ParserContext parserContext);

}
