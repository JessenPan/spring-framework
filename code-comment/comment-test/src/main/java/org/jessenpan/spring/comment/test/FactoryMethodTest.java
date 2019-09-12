package org.jessenpan.spring.comment.test;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.jessenpan.spring.comment.test.jessenpan.TestObj;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author jessenpan
 * @since 17/5/30
 */
public class FactoryMethodTest {

    @Test
    public void testFactoryMethod() {
        Resource resource = new ClassPathResource("spring-config-test.xml");
        BeanFactory bf = new XmlBeanFactory(resource);
        TestObj testObj = (TestObj) bf.getBean("testByFactory");
    }
}
