package org.jessenpan.spring.comment.test;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author jessenpan
 * @since 17/7/23
 */
public class CircularRefTest {

    @Test
    public void testCircularRef() {
        Resource springConfig = new ClassPathResource("spring-circular-ref-test.xml");
        BeanFactory bf = new XmlBeanFactory(springConfig);
        bf.getBean("test");
    }
}
