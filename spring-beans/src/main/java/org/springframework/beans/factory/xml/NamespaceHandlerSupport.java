/*
 * Copyright 2002-2009 the original author or authors.
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
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;

/**
 * Support class for implementing custom {@link NamespaceHandler NamespaceHandlers}.
 * Parsing and decorating of individual {@link Node Nodes} is done via {@link BeanDefinitionParser}
 * and {@link BeanDefinitionDecorator} strategy interfaces, respectively.
 * <p>
 * {@link NamespaceHandler}接口实现的支持类.
 * 解析和装饰单个xml里的node节点的工作是通过BeanDefinitionParser完成的
 * <p>Provides the {@link #registerBeanDefinitionParser} and {@link #registerBeanDefinitionDecorator}
 * methods for registering a {@link BeanDefinitionParser} or {@link BeanDefinitionDecorator}
 * to handle a specific element.
 * <br/>
 * 提供了{@link #registerBeanDefinitionParser}和{@link #registerBeanDefinitionDecorator}
 * 方法给子类去注册{@link BeanDefinitionParser}和{@link BeanDefinitionDecorator},用它们来处理具体的指定xml元素
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see #registerBeanDefinitionParser(String, BeanDefinitionParser)
 * @see #registerBeanDefinitionDecorator(String, BeanDefinitionDecorator)
 * @since 2.0
 */
public abstract class NamespaceHandlerSupport implements NamespaceHandler {

    /**
     * Stores the {@link BeanDefinitionParser} implementations keyed by the
     * local name of the {@link Element Elements} they handle.
     * <br/>
     * 存储了beanDefinitionParser和xml节点名称的映射关系,映射的key是需要处理的xml的node的名称
     */
    private final Map<String, BeanDefinitionParser> parsers = new HashMap<String, BeanDefinitionParser>();

    /**
     * Stores the {@link BeanDefinitionDecorator} implementations keyed by the
     * local name of the {@link Element Elements} they handle.
     * <br/>
     * 存储了beanDefinitionDecorator和xml节点名称的映射关系,映射的key是需要处理的xml的node名称
     */
    private final Map<String, BeanDefinitionDecorator> decorators = new HashMap<String, BeanDefinitionDecorator>();

    /**
     * Stores the {@link BeanDefinitionDecorator} implementations keyed by the local
     * name of the {@link Attr Attrs} they handle.
     * <br/>
     * 存储了beanDefinitionDecorator和xml文件节点包含的自定义属性名称的映射关系,
     * key是属性名称
     */
    private final Map<String, BeanDefinitionDecorator> attributeDecorators = new HashMap<String, BeanDefinitionDecorator>();


    /**
     * Parses the supplied {@link Element} by delegating to the {@link BeanDefinitionParser} that is
     * registered for that {@link Element}.
     * <br/>
     * 通过委托给BeanDefinitionParser来解析xml的element元素
     */
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        return findParserForElement(element, parserContext).parse(element, parserContext);
    }

    /**
     * Locates the {@link BeanDefinitionParser} from the register implementations using
     * the local name of the supplied {@link Element}.
     * <br/>
     * 使用xml元素的名称来定位对应的BeanDefinitionParser
     */
    private BeanDefinitionParser findParserForElement(Element element, ParserContext parserContext) {
        String localName = parserContext.getDelegate().getLocalName(element);
        BeanDefinitionParser parser = this.parsers.get(localName);
        if (parser == null) {
            parserContext.getReaderContext().fatal("Cannot locate BeanDefinitionParser for element [" + localName + "]", element);
        }
        return parser;
    }

    /**
     * Decorates the supplied {@link Node} by delegating to the {@link BeanDefinitionDecorator} that
     * is registered to handle that {@link Node}.
     * <br/>
     * 委托给BeanDefinitionDecorator来装饰对应的xml的node节点,
     * 注意这里的入参是node，而不是element.表示它可以输入为Attr,Comment,或者为Element.
     * 即可以装饰Attr或者Element
     */
    public BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext) {

        return findDecoratorForNode(node, parserContext).decorate(node, definition, parserContext);
    }

    /**
     * Locates the {@link BeanDefinitionParser} from the register implementations using
     * the local name of the supplied {@link Node}. Supports both {@link Element Elements}
     * and {@link Attr Attrs}.
     * <br/>
     * 获取Attr或者Element对应的装饰器,使用node对应的名称来定位
     */
    private BeanDefinitionDecorator findDecoratorForNode(Node node, ParserContext parserContext) {
        BeanDefinitionDecorator decorator = null;
        String localName = parserContext.getDelegate().getLocalName(node);
        if (node instanceof Element) {
            decorator = this.decorators.get(localName);
        } else if (node instanceof Attr) {
            decorator = this.attributeDecorators.get(localName);
        } else {
            parserContext.getReaderContext().fatal("Cannot decorate based on Nodes of type [" + node.getClass().getName() + "]", node);
        }
        if (decorator == null) {
            String nodeType = (node instanceof Element ? "element" : "attribute");
            parserContext.getReaderContext().fatal("Cannot locate BeanDefinitionDecorator for " + nodeType + " [" + localName + "]", node);
        }
        return decorator;
    }


    /**
     * Subclasses can call this to register the supplied {@link BeanDefinitionParser} to
     * handle the specified element. The element name is the local (non-namespace qualified)
     * name.
     * <br/>
     * 子类可以调用此方法进行注册元素名称到对应解析器的映射
     */
    protected final void registerBeanDefinitionParser(String elementName, BeanDefinitionParser parser) {
        this.parsers.put(elementName, parser);
    }

    /**
     * Subclasses can call this to register the supplied {@link BeanDefinitionDecorator} to
     * handle the specified element. The element name is the local (non-namespace qualified)
     * name.
     * <br/>
     * 子类可以调用此方法进行注册元素名称到对应装饰器beanDefinitionDecorator
     */
    protected final void registerBeanDefinitionDecorator(String elementName, BeanDefinitionDecorator dec) {
        this.decorators.put(elementName, dec);
    }

    /**
     * Subclasses can call this to register the supplied {@link BeanDefinitionDecorator} to
     * handle the specified attribute. The attribute name is the local (non-namespace qualified)
     * name.
     * <br/>
     * 子类可以调用此方法注册属性名称到对应装饰器beanDefinitionDecorator
     */
    protected final void registerBeanDefinitionDecoratorForAttribute(String attrName, BeanDefinitionDecorator dec) {
        this.attributeDecorators.put(attrName, dec);
    }

}
