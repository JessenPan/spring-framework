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

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Abstract {@link BeanDefinitionParser} implementation providing
 * a number of convenience methods and a
 * {@link AbstractBeanDefinitionParser#parseInternal template method}
 * that subclasses must override to provide the actual parsing logic.
 * <p>
 * BeanDefinitionParser的抽象实现类,提供了一些简便方法,
 * 以及一个{@link AbstractBeanDefinitionParser#parseInternal}模板方法
 * <p>Use this {@link BeanDefinitionParser} implementation when you want
 * to parse some arbitrarily complex XML into one or more
 * {@link BeanDefinition BeanDefinitions}. If you just want to parse some
 * XML into a single {@code BeanDefinition}, you may wish to consider
 * the simpler convenience extensions of this class, namely
 * {@link AbstractSingleBeanDefinitionParser} and
 * {@link AbstractSimpleBeanDefinitionParser}.
 * <br/>
 * 当你希望解析一些非常复杂的xml配置到多个BeanDefinitions时,可以考虑使用此抽象实现.
 * <br/>
 * 如果你只是希望解析xml配置为一个BeanDefinition,你应该会想使用一些更加简单的扩展类,分别为
 * {@link AbstractSingleBeanDefinitionParser}和{@link AbstractSimpleBeanDefinitionParser}
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Dave Syer
 * @since 2.0
 */
public abstract class AbstractBeanDefinitionParser implements BeanDefinitionParser {

    /**
     * Constant for the id attribute
     */
    protected static final String ID_ATTRIBUTE = "id";

    /**
     * Constant for the name attribute
     */
    private static final String NAME_ATTRIBUTE = "name";

    public final BeanDefinition parse(Element element, ParserContext parserContext) {
        //调用定义的模板方法
        AbstractBeanDefinition definition = parseInternal(element, parserContext);
        if (definition != null && !parserContext.isNested()) {
            //返回了beanDefinition,同时不在内嵌模式下
            try {
                String id = resolveId(element, definition, parserContext);
                //没有beanDefinition的id属性值,抛错
                if (!StringUtils.hasText(id)) {
                    parserContext.getReaderContext().error("Id is required for element '" + parserContext.getDelegate().getLocalName(element)
                            + "' when used as a top-level tag", element);
                }
                String[] aliases = new String[0];
                String name = element.getAttribute(NAME_ATTRIBUTE);
                if (StringUtils.hasLength(name)) {
                    //有别名,并以分隔符形式进行分隔
                    aliases = StringUtils.trimArrayElements(StringUtils.commaDelimitedListToStringArray(name));
                }
                //创建对应的beanDefinitionHolder
                BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, id, aliases);
                //手动注册beanDefinitionHolder到beanDefinitionRegistry里去
                registerBeanDefinition(holder, parserContext.getRegistry());
                if (shouldFireEvents()) {
                    //是否应该触发事件
                    BeanComponentDefinition componentDefinition = new BeanComponentDefinition(holder);
                    //模板或者钩子函数,给子类自定义设置beanComponentDefinition的机会
                    postProcessComponentDefinition(componentDefinition);
                    //触发&注册componentDefinition事件
                    parserContext.registerComponent(componentDefinition);
                }
            } catch (BeanDefinitionStoreException ex) {
                parserContext.getReaderContext().error(ex.getMessage(), element);
                return null;
            }
        }
        return definition;
    }

    /**
     * Resolve the ID for the supplied {@link BeanDefinition}.
     * <p>When using {@link #shouldGenerateId generation}, a name is generated automatically.
     * Otherwise, the ID is extracted from the "id" attribute, potentially with a
     * {@link #shouldGenerateIdAsFallback() fallback} to a generated id.
     *
     * @param element       the element that the bean definition has been built from
     * @param definition    the bean definition to be registered
     * @param parserContext the object encapsulating the current state of the parsing process;
     *                      provides access to a {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
     * @return the resolved id
     * @throws BeanDefinitionStoreException if no unique name could be generated
     *                                      for the given bean definition
     */
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
            throws BeanDefinitionStoreException {

        if (shouldGenerateId()) {
            return parserContext.getReaderContext().generateBeanName(definition);
        } else {
            String id = element.getAttribute(ID_ATTRIBUTE);
            if (!StringUtils.hasText(id) && shouldGenerateIdAsFallback()) {
                id = parserContext.getReaderContext().generateBeanName(definition);
            }
            return id;
        }
    }

    /**
     * Register the supplied {@link BeanDefinitionHolder bean} with the supplied
     * {@link BeanDefinitionRegistry registry}.
     * <p>Subclasses can override this method to control whether or not the supplied
     * {@link BeanDefinitionHolder bean} is actually even registered, or to
     * register even more beans.
     * <p>The default implementation registers the supplied {@link BeanDefinitionHolder bean}
     * with the supplied {@link BeanDefinitionRegistry registry} only if the {@code isNested}
     * parameter is {@code false}, because one typically does not want inner beans
     * to be registered as top level beans.
     *
     * @param definition the bean definition to be registered
     * @param registry   the registry that the bean is to be registered with
     * @see BeanDefinitionReaderUtils#registerBeanDefinition(BeanDefinitionHolder, BeanDefinitionRegistry)
     */
    protected void registerBeanDefinition(BeanDefinitionHolder definition, BeanDefinitionRegistry registry) {
        BeanDefinitionReaderUtils.registerBeanDefinition(definition, registry);
    }


    /**
     * Central template method to actually parse the supplied {@link Element}
     * into one or more {@link BeanDefinition BeanDefinitions}.
     *
     * @param element       the element that is to be parsed into one or more {@link BeanDefinition BeanDefinitions}
     * @param parserContext the object encapsulating the current state of the parsing process;
     *                      provides access to a {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}
     * @return the primary {@link BeanDefinition} resulting from the parsing of the supplied {@link Element}
     * @see #parse(org.w3c.dom.Element, ParserContext)
     * @see #postProcessComponentDefinition(org.springframework.beans.factory.parsing.BeanComponentDefinition)
     */
    protected abstract AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext);

    /**
     * Should an ID be generated instead of read from the passed in {@link Element}?
     * <p>Disabled by default; subclasses can override this to enable ID generation.
     * Note that this flag is about <i>always</i> generating an ID; the parser
     * won't even check for an "id" attribute in this case.
     *
     * @return whether the parser should always generate an id
     */
    protected boolean shouldGenerateId() {
        return false;
    }

    /**
     * Should an ID be generated instead if the passed in {@link Element} does not
     * specify an "id" attribute explicitly?
     * <p>Disabled by default; subclasses can override this to enable ID generation
     * as fallback: The parser will first check for an "id" attribute in this case,
     * only falling back to a generated ID if no value was specified.
     *
     * @return whether the parser should generate an id if no id was specified
     */
    protected boolean shouldGenerateIdAsFallback() {
        return false;
    }

    /**
     * Controls whether this parser is supposed to fire a
     * {@link org.springframework.beans.factory.parsing.BeanComponentDefinition}
     * event after parsing the bean definition.
     * <p>This implementation returns {@code true} by default; that is,
     * an event will be fired when a bean definition has been completely parsed.
     * Override this to return {@code false} in order to suppress the event.
     *
     * @return {@code true} in order to fire a component registration event
     * after parsing the bean definition; {@code false} to suppress the event
     * @see #postProcessComponentDefinition
     * @see org.springframework.beans.factory.parsing.ReaderContext#fireComponentRegistered
     */
    protected boolean shouldFireEvents() {
        return true;
    }

    /**
     * Hook method called after the primary parsing of a
     * {@link BeanComponentDefinition} but before the
     * {@link BeanComponentDefinition} has been registered with a
     * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}.
     * <p>Derived classes can override this method to supply any custom logic that
     * is to be executed after all the parsing is finished.
     * <p>The default implementation is a no-op.
     *
     * @param componentDefinition the {@link BeanComponentDefinition} that is to be processed
     */
    protected void postProcessComponentDefinition(BeanComponentDefinition componentDefinition) {
    }

}
