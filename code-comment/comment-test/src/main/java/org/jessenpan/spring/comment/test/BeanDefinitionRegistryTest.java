package org.jessenpan.spring.comment.test;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author jessenpan
 * @since 17/6/25
 */
public class BeanDefinitionRegistryTest {

    /**
     * 使用单一的beanDefinition注册中心来简单实现beanDefinition注册的功能
     */
    @Test
    public void testSimpleBeanDefinitionRegistry() {

        BeanDefinitionRegistry bdRegistry = new SimpleBeanDefinitionRegistry();
        Resource resource = new ClassPathResource("spring-config-test.xml");
        BeanDefinitionReader bdReader = new XmlBeanDefinitionReader(bdRegistry);
        bdReader.loadBeanDefinitions(resource);
        Assert.assertTrue(bdRegistry.getBeanDefinitionCount() > 0);
    }

    
}
