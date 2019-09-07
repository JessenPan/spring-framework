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

import org.springframework.core.io.Resource;

/**
 * Simple strategy allowing tools to control how source metadata is attached
 * to the bean definition metadata.
 * <p>
 * 简单的策略接口，用来给其它工具控制如何让配置的元数据被依附在bean definition元数据里
 * <p>Configuration parsers <strong>may</strong> provide the ability to attach
 * source metadata during the parse phase. They will offer this metadata in a
 * generic format which can be further modified by a {@link SourceExtractor}
 * before being attached to the bean definition metadata.
 * <br/>
 * spring配置的解析器可能会提供在配置阶段捆绑配置源元数据的能力。在被捆绑到beanDefinition元数据之前，
 * 它们可以提供一个被sourceExtractor进一步修改
 * 的通用的元数据格式
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see org.springframework.beans.BeanMetadataElement#getSource()
 * @see org.springframework.beans.factory.config.BeanDefinition
 * @since 2.0
 */
//pattern strategy
public interface SourceExtractor {

    /**
     * Extract the source metadata from the candidate object supplied
     * by the configuration parser.
     *
     * @param sourceCandidate  the original source metadata (never {@code null})
     * @param definingResource the resource that defines the given source object
     *                         (may be {@code null})
     * @return the source metadata object to store (may be {@code null})
     */
    Object extractSource(Object sourceCandidate, Resource definingResource);

}
