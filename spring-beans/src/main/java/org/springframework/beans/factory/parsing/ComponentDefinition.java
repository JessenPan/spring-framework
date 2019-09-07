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

package org.springframework.beans.factory.parsing;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;

/**
 * Interface that describes the logical view of a set of {@link BeanDefinition BeanDefinitions}
 * and {@link BeanReference BeanReferences} as presented in some configuration context.
 * <p>
 * 在一些配置上下文中描述beanDefinitions和beanReferences的逻辑集合
 * <p>With the introduction of {@link org.springframework.beans.factory.xml.NamespaceHandler pluggable custom XML tags},
 * it is now possible for a single logical configuration entity, in this case an XML tag, to
 * create multiple {@link BeanDefinition BeanDefinitions} and {@link BeanReference RuntimeBeanReferences}
 * in order to provide more succinct configuration and greater convenience to end users. As such, it can
 * no longer be assumed that each configuration entity (e.g. XML tag) maps to one {@link BeanDefinition}.
 * For tool vendors and other users who wish to present visualization or support for configuring Spring
 * applications it is important that there is some mechanism in place to tie the {@link BeanDefinition BeanDefinitions}
 * in the {@link org.springframework.beans.factory.BeanFactory} back to the configuration data in a way
 * that has concrete meaning to the end user. As such, {@link org.springframework.beans.factory.xml.NamespaceHandler}
 * implementations are able to publish events in the form of a {@code ComponentDefinition} for each
 * logical entity being configured. Third parties can then {@link ReaderEventListener subscribe to these events},
 * allowing for a user-centric view of the bean metadata.
 * <p>
 * 在插件化的NamespaceHandler设计下,为了给终端用户提供更加简明和强大的配置,会出现在xml里的一个标签能够创建出多个beanDefinitions和runtimeBeanReferences的情况
 * <br/>
 * 如果这样的话，就不能假定一个xml标签只对应一个beanDefinition
 * <br/>
 * 对于想要提供可视化或者spring配置支持的一些工具厂商或者用户来说，有一种能够将beanFactory里的beanDefinitions和配置数据相互联系在
 * 一起是机制非常重要的，在某种程度上，对于终端使用的用户来说也是十分有意义的.
 * <br/>
 * 既然这样，namespaceHandler的实现者就可以针对每一个配置的逻辑实体发布componentDefinition这样的事件.
 * 第三方就可以通过readerEventListener订阅这些事件,这样就可以给用户关于bean元素一个统一的视角
 * <p>
 * <p>Each {@code ComponentDefinition} has a {@link #getSource source object} which is configuration-specific.
 * In the case of XML-based configuration this is typically the {@link org.w3c.dom.Node} which contains the user
 * supplied configuration information. In addition to this, each {@link BeanDefinition} enclosed in a
 * {@code ComponentDefinition} has its own {@link BeanDefinition#getSource() source object} which may point
 * to a different, more specific, set of configuration data. Beyond this, individual pieces of bean metadata such
 * as the {@link org.springframework.beans.PropertyValue PropertyValues} may also have a source object giving an
 * even greater level of detail. Source object extraction is handled through the
 * {@link SourceExtractor} which can be customized as required.
 * <p>
 * <p>Whilst direct access to important {@link BeanReference BeanReferences} is provided through
 * {@link #getBeanReferences}, tools may wish to inspect all {@link BeanDefinition BeanDefinitions} to gather
 * the full set of {@link BeanReference BeanReferences}. Implementations are required to provide
 * all {@link BeanReference BeanReferences} that are required to validate the configuration of the
 * overall logical entity as well as those required to provide full user visualisation of the configuration.
 * It is expected that certain {@link BeanReference BeanReferences} will not be important to
 * validation or to the user view of the configuration and as such these may be ommitted. A tool may wish to
 * display any additional {@link BeanReference BeanReferences} sourced through the supplied
 * {@link BeanDefinition BeanDefinitions} but this is not considered to be a typical case.
 * <p>
 * <p>Tools can determine the important of contained {@link BeanDefinition BeanDefinitions} by checking the
 * {@link BeanDefinition#getRole role identifier}. The role is essentially a hint to the tool as to how
 * important the configuration provider believes a {@link BeanDefinition} is to the end user. It is expected
 * that tools will <strong>not</strong> display all {@link BeanDefinition BeanDefinitions} for a given
 * {@code ComponentDefinition} choosing instead to filter based on the role. Tools may choose to make
 * this filtering user configurable. Particular notice should be given to the
 * {@link BeanDefinition#ROLE_INFRASTRUCTURE INFRASTRUCTURE role identifier}. {@link BeanDefinition BeanDefinitions}
 * classified with this role are completely unimportant to the end user and are required only for
 * internal implementation reasons.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see AbstractComponentDefinition
 * @see CompositeComponentDefinition
 * @see BeanComponentDefinition
 * @see ReaderEventListener#componentRegistered(ComponentDefinition)
 * @since 2.0
 */
public interface ComponentDefinition extends BeanMetadataElement {

    /**
     * 获取用户可视的名称
     * Get the user-visible name of this {@code ComponentDefinition}.
     * <p>This should link back directly to the corresponding configuration data
     * for this component in a given context.
     */
    String getName();

    /**
     * 返回被描述组件的可读性描述
     * Return a friendly description of the described component.
     * <p>Implementations are encouraged to return the same value from
     * {@code toString()}.
     */
    String getDescription();

    /**
     * 返回组成这个component的，已经被注册的beanDefinitions
     * <p>
     * Return the {@link BeanDefinition BeanDefinitions} that were registered
     * to form this {@code ComponentDefinition}.
     * <p>It should be noted that a {@code ComponentDefinition} may well be related with
     * other {@link BeanDefinition BeanDefinitions} via {@link BeanReference references},
     * however these are <strong>not</strong> included as they may be not available immediately.
     * Important {@link BeanReference BeanReferences} are available from {@link #getBeanReferences()}.
     * <p>
     * <p>有一点需要注意的是,componentDefinition可以通过beanReference被其它beanDefinition所关联上,
     * 尽管有一些是没有包含的，因为他们当前是不可用的.
     * 重要的beanReferences是可以通过getBeanReferences获取的
     *
     * @return the array of BeanDefinitions, or an empty array if none
     */
    BeanDefinition[] getBeanDefinitions();

    /**
     * 返回此component关联的所有所有内部beans的beanDefinitions
     * Return the {@link BeanDefinition BeanDefinitions} that represent all relevant
     * inner beans within this component.
     * <p>Other inner beans may exist within the associated {@link BeanDefinition BeanDefinitions},
     * however these are not considered to be needed for validation or for user visualization.
     * <p>
     * <p>
     * 在相关联的beanDefinitions里，也有一起其它的内部beans存在,但是这些并不是需要被验证和用户可视化的
     *
     * @return the array of BeanDefinitions, or an empty array if none
     */
    BeanDefinition[] getInnerBeanDefinitions();

    /**
     * 返回beanReferences的集合,这些集合相对此componentDefinition来说是比较重要的
     * <p>
     * Return the set of {@link BeanReference BeanReferences} that are considered
     * to be important to this {@code ComponentDefinition}.
     * <p>Other {@link BeanReference BeanReferences} may exist within the associated
     * {@link BeanDefinition BeanDefinitions}, however these are not considered
     * to be needed for validation or for user visualization.
     *
     * @return the array of BeanReferences, or an empty array if none
     */
    BeanReference[] getBeanReferences();

}
