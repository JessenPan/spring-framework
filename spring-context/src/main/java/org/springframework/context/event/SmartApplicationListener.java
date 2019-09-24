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

package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

/**
 * Extended variant of the standard {@link ApplicationListener} interface,
 * exposing further metadata such as the supported event type.
 * <p>
 * 标准ApplicationListener接口的扩展变种
 * </p>
 * <p>
 * 公开进一步的元数据，如受支持的事件类型
 * </p>
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public interface SmartApplicationListener extends ApplicationListener<ApplicationEvent>, Ordered {

    /**
     * Determine whether this listener actually supports the given event type.
     * <p>
     * 此监听器是否支持指定的事件
     * </p>
     */
    boolean supportsEventType(Class<? extends ApplicationEvent> eventType);

    /**
     * Determine whether this listener actually supports the given source type.
     * <p>
     * 此监听器是否支持指定的事件源
     * </p>
     */
    boolean supportsSourceType(Class<?> sourceType);

}
