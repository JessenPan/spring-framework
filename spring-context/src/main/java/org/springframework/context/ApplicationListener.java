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

package org.springframework.context;

import org.jessenpan.spring.comment.annotation.DesignPattern;

import java.util.EventListener;

import static org.jessenpan.spring.comment.annotation.DesignPatternEnum.OBSERVER;

/**
 * Interface to be implemented by application event listeners.
 * Based on the standard {@code java.util.EventListener} interface
 * for the Observer design pattern.
 * <p>
 * <p>As of Spring 3.0, an ApplicationListener can generically declare the event type
 * that it is interested in. When registered with a Spring ApplicationContext, events
 * will be filtered accordingly, with the listener getting invoked for matching event
 * objects only.
 *
 * <p>
 * 具体的应用监听器实现此接口，基于标准的{@code java.util.EventListener}接口实现观察者模式
 * </p>
 * <p>
 * 在spring3.0里，应用监听器可以订阅自己感兴趣的事件。当监听器注册到spring的应用上下文后，所有发生的事件将会根据注册情况进行过滤，
 * 并调用对此事件感兴趣的监听器。
 * </p>
 *
 * @param <E> the specific ApplicationEvent subclass to listen to
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.context.event.ApplicationEventMulticaster
 */
@DesignPattern(value = { OBSERVER })
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {

    /**
     * Handle an application event.
     * <p>
     * 此方法处理应用的事件
     * </p>
     *
     * @param event the event to respond to
     */
    void onApplicationEvent(E event);

}
