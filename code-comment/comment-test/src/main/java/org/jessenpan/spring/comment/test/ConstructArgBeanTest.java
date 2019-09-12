package org.jessenpan.spring.comment.test;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author jessenpan
 * @since  17/5/29
 */
public class ConstructArgBeanTest {

    @Test
    public void testConstructArgBeanDefinition() {
        Resource springConfig = new ClassPathResource("spring-config-test.xml");
        BeanFactory bf = new XmlBeanFactory(springConfig);
    }
}
