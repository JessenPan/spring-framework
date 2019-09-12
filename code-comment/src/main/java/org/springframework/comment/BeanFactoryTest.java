package org.springframework.comment;

import org.junit.Test;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * @author jessenpan
 * @since 17/8/27
 */
public class BeanFactoryTest {

    @Test
    public void testGetBean() {
        DefaultListableBeanFactory defaultListableBeanFactory = new DefaultListableBeanFactory();
        BeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(defaultListableBeanFactory);
        beanDefinitionReader.loadBeanDefinitions("spring-config-test.xml");
        defaultListableBeanFactory.getBean("test");
    }
}
