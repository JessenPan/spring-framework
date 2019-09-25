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

import org.jessenpan.spring.comment.annotation.DesignPrinciple;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import static org.jessenpan.spring.comment.annotation.DesignPrincipleEnum.ISP;
import static org.jessenpan.spring.comment.annotation.DesignPrincipleEnum.SRP;

/**
 * Interface to be implemented by objects that can manage a number of
 * {@link ApplicationListener} objects, and publish events to them.
 * <p>
 * <p>An {@link org.springframework.context.ApplicationEventPublisher}, typically
 * a Spring {@link org.springframework.context.ApplicationContext}, can use an
 * ApplicationEventMulticaster as a delegate for actually publishing events.
 *
 * <p>
 * 实现此接口的类能够管理一批 ApplicationListener，并向他们发布事件
 * </p>
 * <p>
 * 一个ApplicationEventPublisher对象可以将此类作为内部实现，来进行真正的事件发布。
 * 典型的ApplicationEventPublisher类的实现者是 ApplicationContext。
 * </p>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
//这个接口和ApplicationEventPublisher职责单一和接口隔离
@DesignPrinciple(value = { SRP, ISP })
public interface ApplicationEventMulticaster {

    /**
     * Add a listener to be notified of all events.
     *
     * @param listener the listener to add
     */
    void addApplicationListener(ApplicationListener listener);

    /**
     * Add a listener bean to be notified of all events.
     *
     * @param listenerBeanName the name of the listener bean to add
     */
    void addApplicationListenerBean(String listenerBeanName);

    /**
     * Remove a listener from the notification list.
     *
     * @param listener the listener to remove
     */
    void removeApplicationListener(ApplicationListener listener);

    /**
     * Remove a listener bean from the notification list.
     *
     * @param listenerBeanName the name of the listener bean to add
     */
    void removeApplicationListenerBean(String listenerBeanName);

    /**
     * Remove all listeners registered with this multicaster.
     * <p>After a remove call, the multicaster will perform no action
     * on event notification until new listeners are being registered.
     */
    void removeAllListeners();

    /**
     * Multicast the given application event to appropriate listeners.
     *
     * @param event the event to multicast
     */
    void multicastEvent(ApplicationEvent event);

}
