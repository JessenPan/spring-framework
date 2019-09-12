package org.jessenpan.spring.comment.test.context;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author jessenpan
 * @since 17/8/26
 */
public class CreateContextTest {

    @Test
    public void testBeanPostProc() {
        ApplicationContext appContext = new ClassPathXmlApplicationContext("spring-config-test.xml");
        appContext.getBean("test");
    }
}
