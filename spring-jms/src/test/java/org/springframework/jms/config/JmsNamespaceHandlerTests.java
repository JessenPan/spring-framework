/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.jms.config;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.*;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.Phased;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jca.endpoint.GenericMessageEndpointManager;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.endpoint.JmsMessageEndpointManager;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.ErrorHandler;

import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Christian Dupuis
 */
public class JmsNamespaceHandlerTests {

    private static final String DEFAULT_CONNECTION_FACTORY = "connectionFactory";

    private static final String EXPLICIT_CONNECTION_FACTORY = "testConnectionFactory";

    private ToolingTestApplicationContext context;


    @Before
    public void setUp() throws Exception {
        this.context = new ToolingTestApplicationContext("jmsNamespaceHandlerTests.xml", getClass());
    }

    @After
    public void tearDown() throws Exception {
        this.context.close();
    }


    @Test
    public void testBeansCreated() {
        Map containers = context.getBeansOfType(DefaultMessageListenerContainer.class);
        assertEquals("Context should contain 3 JMS listener containers", 3, containers.size());

        containers = context.getBeansOfType(GenericMessageEndpointManager.class);
        assertEquals("Context should contain 3 JCA endpoint containers", 3, containers.size());
    }

    @Test
    public void testContainerConfiguration() throws Exception {
        Map<String, DefaultMessageListenerContainer> containers = context.getBeansOfType(DefaultMessageListenerContainer.class);
        ConnectionFactory defaultConnectionFactory = context.getBean(DEFAULT_CONNECTION_FACTORY, ConnectionFactory.class);
        ConnectionFactory explicitConnectionFactory = context.getBean(EXPLICIT_CONNECTION_FACTORY, ConnectionFactory.class);

        int defaultConnectionFactoryCount = 0;
        int explicitConnectionFactoryCount = 0;

        for (DefaultMessageListenerContainer container : containers.values()) {
            if (container.getConnectionFactory().equals(defaultConnectionFactory)) {
                defaultConnectionFactoryCount++;
                assertEquals(2, container.getConcurrentConsumers());
                assertEquals(3, container.getMaxConcurrentConsumers());
            } else if (container.getConnectionFactory().equals(explicitConnectionFactory)) {
                explicitConnectionFactoryCount++;
                assertEquals(1, container.getConcurrentConsumers());
                assertEquals(2, container.getMaxConcurrentConsumers());
            }
        }

        assertEquals("1 container should have the default connectionFactory", 1, defaultConnectionFactoryCount);
        assertEquals("2 containers should have the explicit connectionFactory", 2, explicitConnectionFactoryCount);
    }

    @Test
    public void testListeners() throws Exception {
        TestBean testBean1 = context.getBean("testBean1", TestBean.class);
        TestBean testBean2 = context.getBean("testBean2", TestBean.class);
        TestMessageListener testBean3 = context.getBean("testBean3", TestMessageListener.class);

        assertNull(testBean1.getName());
        assertNull(testBean2.getName());
        assertNull(testBean3.message);

        TextMessage message1 = mock(TextMessage.class);
        given(message1.getText()).willReturn("Test1");

        MessageListener listener1 = getListener("listener1");
        listener1.onMessage(message1);
        assertEquals("Test1", testBean1.getName());

        TextMessage message2 = mock(TextMessage.class);
        given(message2.getText()).willReturn("Test2");

        MessageListener listener2 = getListener("listener2");
        listener2.onMessage(message2);
        assertEquals("Test2", testBean2.getName());

        TextMessage message3 = mock(TextMessage.class);

        MessageListener listener3 = getListener(DefaultMessageListenerContainer.class.getName() + "#0");
        listener3.onMessage(message3);
        assertSame(message3, testBean3.message);
    }

    @Test
    public void testErrorHandlers() {
        ErrorHandler expected = this.context.getBean("testErrorHandler", ErrorHandler.class);
        ErrorHandler errorHandler1 = getErrorHandler("listener1");
        ErrorHandler errorHandler2 = getErrorHandler("listener2");
        ErrorHandler defaultErrorHandler = getErrorHandler(DefaultMessageListenerContainer.class.getName() + "#0");
        assertSame(expected, errorHandler1);
        assertSame(expected, errorHandler2);
        assertNull(defaultErrorHandler);
    }

    @Test
    public void testPhases() {
        int phase1 = getPhase("listener1");
        int phase2 = getPhase("listener2");
        int phase3 = getPhase("listener3");
        int phase4 = getPhase("listener4");
        int defaultPhase = getPhase(DefaultMessageListenerContainer.class.getName() + "#0");
        assertEquals(99, phase1);
        assertEquals(99, phase2);
        assertEquals(77, phase3);
        assertEquals(77, phase4);
        assertEquals(Integer.MAX_VALUE, defaultPhase);
    }

    private MessageListener getListener(String containerBeanName) {
        DefaultMessageListenerContainer container = this.context.getBean(containerBeanName, DefaultMessageListenerContainer.class);
        return (MessageListener) container.getMessageListener();
    }

    private ErrorHandler getErrorHandler(String containerBeanName) {
        DefaultMessageListenerContainer container = this.context.getBean(containerBeanName, DefaultMessageListenerContainer.class);
        return (ErrorHandler) new DirectFieldAccessor(container).getPropertyValue("errorHandler");
    }

    public int getPhase(String containerBeanName) {
        Object container = this.context.getBean(containerBeanName);
        if (!(container instanceof Phased)) {
            throw new IllegalStateException("Container '" + containerBeanName + "' does not implement Phased.");
        }
        return ((Phased) container).getPhase();
    }

    @Test
    public void testComponentRegistration() {
        assertTrue("Parser should have registered a component named 'listener1'", context.containsComponentDefinition("listener1"));
        assertTrue("Parser should have registered a component named 'listener2'", context.containsComponentDefinition("listener2"));
        assertTrue("Parser should have registered a component named 'listener3'", context.containsComponentDefinition("listener3"));
        assertTrue("Parser should have registered a component named '" + DefaultMessageListenerContainer.class.getName() + "#0'",
                context.containsComponentDefinition(DefaultMessageListenerContainer.class.getName() + "#0"));
        assertTrue("Parser should have registered a component named '" + JmsMessageEndpointManager.class.getName() + "#0'",
                context.containsComponentDefinition(JmsMessageEndpointManager.class.getName() + "#0"));
    }

    @Test
    public void testSourceExtraction() {
        Iterator iterator = context.getRegisteredComponents();
        while (iterator.hasNext()) {
            ComponentDefinition compDef = (ComponentDefinition) iterator.next();
            assertNotNull("CompositeComponentDefinition '" + compDef.getName() + "' has no source attachment", compDef.getSource());
            validateComponentDefinition(compDef);
        }
    }

    private void validateComponentDefinition(ComponentDefinition compDef) {
        BeanDefinition[] beanDefs = compDef.getBeanDefinitions();
        for (BeanDefinition beanDef : beanDefs) {
            assertNotNull("BeanDefinition has no source attachment", beanDef.getSource());
        }
    }


    public static class TestMessageListener implements MessageListener {

        public Message message;

        @Override
        public void onMessage(Message message) {
            this.message = message;
        }
    }


    /**
     * Internal extension that registers a {@link ReaderEventListener} to store
     * registered {@link ComponentDefinition}s.
     */
    private static class ToolingTestApplicationContext extends ClassPathXmlApplicationContext {

        private Set<ComponentDefinition> registeredComponents;

        public ToolingTestApplicationContext(String path, Class clazz) {
            super(path, clazz);
        }

        @Override
        protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {
            this.registeredComponents = new HashSet<ComponentDefinition>();
            beanDefinitionReader.setEventListener(new StoringReaderEventListener(this.registeredComponents));
            beanDefinitionReader.setSourceExtractor(new PassThroughSourceExtractor());
        }

        public boolean containsComponentDefinition(String name) {
            for (ComponentDefinition cd : this.registeredComponents) {
                if (cd instanceof CompositeComponentDefinition) {
                    ComponentDefinition[] innerCds = ((CompositeComponentDefinition) cd).getNestedComponents();
                    for (ComponentDefinition innerCd : innerCds) {
                        if (innerCd.getName().equals(name)) {
                            return true;
                        }
                    }
                } else {
                    if (cd.getName().equals(name)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public Iterator<ComponentDefinition> getRegisteredComponents() {
            return this.registeredComponents.iterator();
        }
    }


    private static class StoringReaderEventListener extends EmptyReaderEventListener {

        protected final Set<ComponentDefinition> registeredComponents;

        public StoringReaderEventListener(Set<ComponentDefinition> registeredComponents) {
            this.registeredComponents = registeredComponents;
        }

        @Override
        public void componentRegistered(ComponentDefinition componentDefinition) {
            this.registeredComponents.add(componentDefinition);
        }
    }


    static class TestErrorHandler implements ErrorHandler {

        @Override
        public void handleError(Throwable t) {
        }
    }

}
