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
 * Simple {@link SourceExtractor} implementation that just passes
 * the candidate source metadata object through for attachment.
 * <p>
 * 简单的sourceExtractor实现,它只是返回输入的候选的源metadata。
 * <p>Using this implementation means that tools will get raw access to the
 * underlying configuration source metadata provided by the tool.
 * <p>
 * 使用这个实现意味着其它工具会直接访问原始的配置元数据,没有任何封装
 * <p>This implementation <strong>should not</strong> be used in a production
 * application since it is likely to keep too much metadata in memory
 * (unnecessarily).
 * 此实现不应该被用在生产环境中，因为它可能导致大量的元数据驻留在内存中
 *
 * @author Rob Harrop
 * @since 2.0
 */
public class PassThroughSourceExtractor implements SourceExtractor {

    /**
     * Simply returns the supplied {@code sourceCandidate} as-is.
     * <br/>
     * 总是返回输入的sourceCandidate作为输出
     *
     * @param sourceCandidate the source metadata
     * @return the supplied {@code sourceCandidate}
     */
    public Object extractSource(Object sourceCandidate, Resource definingResource) {
        return sourceCandidate;
    }

}
