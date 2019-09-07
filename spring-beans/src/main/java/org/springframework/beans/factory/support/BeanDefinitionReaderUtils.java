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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 工具方法类,主要在bean definition读取时使用.<br/>
 * 主要在内部使用
 * <p>
 * Utility methods that are useful for bean definition reader implementations.
 * Mainly intended for internal use.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see PropertiesBeanDefinitionReader
 * @see org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader
 * @since 1.1
 */
//review jessenpan 不可实例化工具类用abstract修饰
public abstract class BeanDefinitionReaderUtils {

    /**
     * 生成的beanName的分隔符.如果生成的bean名称不唯一,则在名称后添加#1,#2,#3等等,直到名称唯一
     * Separator for generated bean names. If a class name or parent name is not
     * unique, "#1", "#2" etc will be appended, until the name becomes unique.
     */
    public static final String GENERATED_BEAN_NAME_SEPARATOR = BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;


    /**
     * Create a new GenericBeanDefinition for the given parent name and class name,
     * eagerly loading the bean class if a ClassLoader has been specified.
     * <p>
     * 根据指定的parent名称和类名称,创建一个新的GenericBeanDefinition,
     * 如果指定了classloader,则迫切的将此bean class装载到jvm里.
     * <br/>
     * 算是一个简单的静态工厂方法模式
     *
     * @param parentName  the name of the parent bean, if any
     * @param className   the name of the bean class, if any
     * @param classLoader the ClassLoader to use for loading bean classes
     *                    (can be {@code null} to just register bean classes by name)
     * @return the bean definition
     * @throws ClassNotFoundException if the bean class could not be loaded
     */
    public static AbstractBeanDefinition createBeanDefinition(
            String parentName, String className, ClassLoader classLoader) throws ClassNotFoundException {


        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setParentName(parentName);
        if (className != null) {
            //类名称和classloader不为空时,加载类
            if (classLoader != null) {
                bd.setBeanClass(ClassUtils.forName(className, classLoader));
            } else {
                //classloader为空时，只设置类名称文本
                bd.setBeanClassName(className);
            }
        }
        return bd;
    }

    /**
     * Generate a bean name for the given bean definition, unique within the
     * given bean factory.
     *
     * @param definition  the bean definition to generate a bean name for
     * @param registry    the bean factory that the definition is going to be
     *                    registered with (to check for existing bean names)
     * @param isInnerBean whether the given bean definition will be registered
     *                    as inner bean or as top-level bean (allowing for special name generation
     *                    for inner beans versus top-level beans)
     * @return the generated bean name
     * @throws BeanDefinitionStoreException if no unique name can be generated
     *                                      for the given bean definition
     */
    public static String generateBeanName(
            BeanDefinition definition, BeanDefinitionRegistry registry, boolean isInnerBean)
            throws BeanDefinitionStoreException {

        String generatedBeanName = definition.getBeanClassName();
        if (generatedBeanName == null) {
            if (definition.getParentName() != null) {
                generatedBeanName = definition.getParentName() + "$child";
            } else if (definition.getFactoryBeanName() != null) {
                generatedBeanName = definition.getFactoryBeanName() + "$created";
            }
        }
        if (!StringUtils.hasText(generatedBeanName)) {
            throw new BeanDefinitionStoreException("Unnamed bean definition specifies neither " +
                    "'class' nor 'parent' nor 'factory-bean' - can't generate bean name");
        }

        String id = generatedBeanName;
        if (isInnerBean) {
            // Inner bean: generate identity hashcode suffix.
            id = generatedBeanName + GENERATED_BEAN_NAME_SEPARATOR + ObjectUtils.getIdentityHexString(definition);
        } else {
            // Top-level bean: use plain class name.
            // Increase counter until the id is unique.
            int counter = -1;
            while (counter == -1 || registry.containsBeanDefinition(id)) {
                counter++;
                id = generatedBeanName + GENERATED_BEAN_NAME_SEPARATOR + counter;
            }
        }
        return id;
    }

    /**
     * Generate a bean name for the given top-level bean definition,
     * unique within the given bean factory.
     *
     * @param beanDefinition the bean definition to generate a bean name for
     * @param registry       the bean factory that the definition is going to be
     *                       registered with (to check for existing bean names)
     * @return the generated bean name
     * @throws BeanDefinitionStoreException if no unique name can be generated
     *                                      for the given bean definition
     */
    public static String generateBeanName(BeanDefinition beanDefinition, BeanDefinitionRegistry registry)
            throws BeanDefinitionStoreException {

        return generateBeanName(beanDefinition, registry, false);
    }

    /**
     * Register the given bean definition with the given bean factory.
     * <br/>
     * 注册指定的beanDefinition到指定的bean factory中去
     *
     * @param definitionHolder the bean definition including name and aliases
     * @param registry         the bean factory to register with
     * @throws BeanDefinitionStoreException if registration failed
     */
    public static void registerBeanDefinition(BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
            throws BeanDefinitionStoreException {

        // Register bean definition under primary name.
        String beanName = definitionHolder.getBeanName();
        //根据beanName注册beanDefinition
        registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());

        // Register aliases for bean name, if any.
        //注册所有的别名
        String[] aliases = definitionHolder.getAliases();
        if (aliases != null) {
            for (String aliase : aliases) {
                registry.registerAlias(beanName, aliase);
            }
        }
    }

    /**
     * Register the given bean definition with a generated name,
     * unique within the given bean factory.
     *
     * @param definition the bean definition to generate a bean name for
     * @param registry   the bean factory to register with
     * @return the generated bean name
     * @throws BeanDefinitionStoreException if no unique name can be generated
     *                                      for the given bean definition or the definition cannot be registered
     */
    public static String registerWithGeneratedName(
            AbstractBeanDefinition definition, BeanDefinitionRegistry registry)
            throws BeanDefinitionStoreException {

        String generatedName = generateBeanName(definition, registry, false);
        registry.registerBeanDefinition(generatedName, definition);
        return generatedName;
    }

}
