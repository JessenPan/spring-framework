/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.beans;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * javaBeans的一些静态工具方法: 如实例化bean方法,检测bean属性类型工具,以及拷贝bean属性等一些方法.
 * Static convenience methods for JavaBeans: for instantiating beans,
 * checking bean property types, copying bean properties, etc.
 * <p>
 * <p>Mainly for use within the framework, but to some degree also
 * useful for application classes.
 * 此工具类主要用在spring框架里,但是某些程度上也可用在应用程序里
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 */
//工具类。对于不可实例化,用abstract修饰
public abstract class BeanUtils {

    private static final Log logger = LogFactory.getLog(BeanUtils.class);

    // Effectively using a WeakHashMap as a Set
    //TODO 学习下weakHashMap好处
    //pattern decorator 装饰器模式
    //pattern static-factory-method  创建unknownEditorTypes时使用静态工厂方法
    private static final Map<Class<?>, Boolean> unknownEditorTypes =
            Collections.synchronizedMap(new WeakHashMap<Class<?>, Boolean>());


    /**
     * 工具方法，使用类的无参构造函数实例化类.
     * 此方法不会尝试用类名去加载类,它也因此将避免一些classloading的一些问题
     * <br/>
     * Convenience method to instantiate a class using its no-arg constructor.
     * As this method doesn't try to load classes by name, it should avoid
     * class-loading issues.
     *
     * @param clazz class to instantiate
     * @return the new instance
     * @throws BeanInstantiationException if the bean cannot be instantiated
     */
    public static <T> T instantiate(Class<T> clazz) throws BeanInstantiationException {
        //tips 始终校验入参
        Assert.notNull(clazz, "Class must not be null");
        //接口是无法实例化的
        if (clazz.isInterface()) {
            throw new BeanInstantiationException(clazz, "Specified class is an interface");
        }
        try {
            //调用clazz的无参构造函数
            return clazz.newInstance();
        } catch (InstantiationException ex) {
            //tips 异常最好包含详细的描述信息和异常现场信息
            //tips 对于不同层面的异常，可以考虑新建异常类来包装底层异常，达到代码分层及解耦合的作用
            throw new BeanInstantiationException(clazz, "Is it an abstract class?", ex);
        } catch (IllegalAccessException ex) {
            throw new BeanInstantiationException(clazz, "Is the constructor accessible?", ex);
        }
    }

    /**
     * Instantiate a class using its no-arg constructor.
     * As this method doesn't try to load classes by name, it should avoid
     * class-loading issues.
     * <p>Note that this method tries to set the constructor accessible
     * if given a non-accessible (that is, non-public) constructor.
     * <p>需要注意的是，此方法会在类的构造函数不可访问的情况下，设置它为可访问的
     *
     * @param clazz class to instantiate
     * @return the new instance
     * @throws BeanInstantiationException if the bean cannot be instantiated
     */
    public static <T> T instantiateClass(Class<T> clazz) throws BeanInstantiationException {
        Assert.notNull(clazz, "Class must not be null");
        if (clazz.isInterface()) {
            throw new BeanInstantiationException(clazz, "Specified class is an interface");
        }
        try {
            return instantiateClass(clazz.getDeclaredConstructor());
        } catch (NoSuchMethodException ex) {
            throw new BeanInstantiationException(clazz, "No default constructor found", ex);
        }
    }

    /**
     * 通过类的无参构造函数实例化它，同时返回指定的可赋值类型<p>
     * Instantiate a class using its no-arg constructor and return the new instance
     * as the the specified assignable type.
     * <p>在类似spi机制的情况下，即在编译器期知道接口类，但是不知道具体的实现类情况下，此方法是十分有用的.
     * <p>Useful in cases where
     * the type of the class to instantiate (clazz) is not available, but the type
     * desired (assignableTo) is known.
     * <p>As this method doesn't try to load classes by name, it should avoid
     * class-loading issues.
     * <p>Note that this method tries to set the constructor accessible
     * if given a non-accessible (that is, non-public) constructor.
     *
     * @param clazz        class to instantiate
     * @param assignableTo type that clazz must be assignableTo
     * @return the new instance
     * @throws BeanInstantiationException if the bean cannot be instantiated
     */
    @SuppressWarnings("unchecked")
    public static <T> T instantiateClass(Class<?> clazz, Class<T> assignableTo) throws BeanInstantiationException {
        //tips check参数first
        Assert.isAssignable(assignableTo, clazz);
        return (T) instantiateClass(clazz);
    }

    /**
     * Convenience method to instantiate a class using the given constructor.
     * As this method doesn't try to load classes by name, it should avoid
     * class-loading issues.
     * <p>Note that this method tries to set the constructor accessible
     * if given a non-accessible (that is, non-public) constructor.
     *
     * @param ctor the constructor to instantiate
     * @param args the constructor arguments to apply
     * @return the new instance
     * @throws BeanInstantiationException if the bean cannot be instantiated
     */
    public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws BeanInstantiationException {
        Assert.notNull(ctor, "Constructor must not be null");
        try {
            ReflectionUtils.makeAccessible(ctor);
            return ctor.newInstance(args);
        } catch (InstantiationException ex) {
            throw new BeanInstantiationException(ctor.getDeclaringClass(),
                    "Is it an abstract class?", ex);
        } catch (IllegalAccessException ex) {
            throw new BeanInstantiationException(ctor.getDeclaringClass(),
                    "Is the constructor accessible?", ex);
        } catch (IllegalArgumentException ex) {
            throw new BeanInstantiationException(ctor.getDeclaringClass(),
                    "Illegal arguments for constructor", ex);
        } catch (InvocationTargetException ex) {
            throw new BeanInstantiationException(ctor.getDeclaringClass(),
                    "Constructor threw exception", ex.getTargetException());
        }
    }

    /**
     * 从指定的类以及它的父类中,查找指定名称和参数类型的方法,
     * 优先选择匹配的public方法,但同时也可能返回protected,package级别以及私有的方法.
     * Find a method with the given method name and the given parameter types,
     * declared on the given class or one of its superclasses. Prefers public methods,
     * but will return a protected, package access, or private method too.
     * <p>Checks {@code Class.getMethod} first, falling back to
     * {@code findDeclaredMethod}. This allows to find public methods
     * without issues even in environments with restricted Java security settings.
     * <p>首先使用class的getMethod方法去查找，当查找失败后，再使用findDeclaredMethod.
     * <p>这使得查找public方法时不用处理，在比较严格的jvm安全设置下的一些问题。
     * <p>//TODO 注意findMethod和findDeclaredMethod的区别
     *
     * @param clazz      the class to check
     * @param methodName the name of the method to find
     * @param paramTypes the parameter types of the method to find
     * @return the Method object, or {@code null} if not found
     * @see Class#getMethod
     * @see #findDeclaredMethod
     */
    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException ex) {
            return findDeclaredMethod(clazz, methodName, paramTypes);
        }
    }

    /**
     * Find a method with the given method name and the given parameter types,
     * declared on the given class or one of its superclasses. Will return a public,
     * protected, package access, or private method.
     * <p>Checks {@code Class.getDeclaredMethod}, cascading upwards to all superclasses.
     *
     * @param clazz      the class to check
     * @param methodName the name of the method to find
     * @param paramTypes the parameter types of the method to find
     * @return the Method object, or {@code null} if not found
     * @see Class#getDeclaredMethod
     */
    public static Method findDeclaredMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            return clazz.getDeclaredMethod(methodName, paramTypes);
        } catch (NoSuchMethodException ex) {
            if (clazz.getSuperclass() != null) {
                return findDeclaredMethod(clazz.getSuperclass(), methodName, paramTypes);
            }
            return null;
        }
    }

    /**
     * 在指定类中查找指定名称和最少参数的方法。（最少参数可以为零个）
     * <p>
     * Find a method with the given method name and minimal parameters (best case: none),
     * declared on the given class or one of its superclasses. Prefers public methods,
     * but will return a protected, package access, or private method too.
     * <p>Checks {@code Class.getMethods} first, falling back to
     * {@code findDeclaredMethodWithMinimalParameters}. This allows for finding public
     * methods without issues even in environments with restricted Java security settings.
     *
     * @param clazz      the class to check
     * @param methodName the name of the method to find
     * @return the Method object, or {@code null} if not found
     * @throws IllegalArgumentException if methods of the given name were found but
     *                                  could not be resolved to a unique method with minimal parameters
     * @see Class#getMethods
     * @see #findDeclaredMethodWithMinimalParameters
     */
    public static Method findMethodWithMinimalParameters(Class<?> clazz, String methodName)
            throws IllegalArgumentException {

        Method targetMethod = findMethodWithMinimalParameters(clazz.getMethods(), methodName);
        if (targetMethod == null) {
            targetMethod = findDeclaredMethodWithMinimalParameters(clazz, methodName);
        }
        return targetMethod;
    }

    /**
     * Find a method with the given method name and minimal parameters (best case: none),
     * declared on the given class or one of its superclasses. Will return a public,
     * protected, package access, or private method.
     * <p>Checks {@code Class.getDeclaredMethods}, cascading upwards to all superclasses.
     *
     * @param clazz      the class to check
     * @param methodName the name of the method to find
     * @return the Method object, or {@code null} if not found
     * @throws IllegalArgumentException if methods of the given name were found but
     *                                  could not be resolved to a unique method with minimal parameters
     * @see Class#getDeclaredMethods
     */
    public static Method findDeclaredMethodWithMinimalParameters(Class<?> clazz, String methodName)
            throws IllegalArgumentException {

        Method targetMethod = findMethodWithMinimalParameters(clazz.getDeclaredMethods(), methodName);
        if (targetMethod == null && clazz.getSuperclass() != null) {
            //递归查找,在使用递归时，需要稍微留意最大递归数，防止出现线程栈的OOM
            targetMethod = findDeclaredMethodWithMinimalParameters(clazz.getSuperclass(), methodName);
        }
        return targetMethod;
    }

    /**
     * Find a method with the given method name and minimal parameters (best case: none)
     * in the given list of methods.
     *
     * @param methods    the methods to check
     * @param methodName the name of the method to find
     * @return the Method object, or {@code null} if not found
     * @throws IllegalArgumentException if methods of the given name were found but
     *                                  could not be resolved to a unique method with minimal parameters
     */
    public static Method findMethodWithMinimalParameters(Method[] methods, String methodName)
            throws IllegalArgumentException {
        //review by jessenpan 这里可以check参数first
        Method targetMethod = null;
        //所有匹配的方法计数器
        int numMethodsFoundWithCurrentMinimumArgs = 0;
        for (Method method : methods) {
            //方法名称才判断，不相同跳过
            if (method.getName().equals(methodName)) {
                //参数数量
                int numParams = method.getParameterTypes().length;
                //targetMethod为空，表示之前还没有找到名称相同方法
                //当前查找的方法参数数量小于之前找到方法的数量
                if (targetMethod == null || numParams < targetMethod.getParameterTypes().length) {
                    //当前方法设置为targetMethod
                    targetMethod = method;
                    //找到的方法计数器
                    numMethodsFoundWithCurrentMinimumArgs = 1;
                } else {
                    //如果targetMethod不为空,同时新匹配方法的参数长度和之前的相同,只增加方法计数器值
                    if (targetMethod.getParameterTypes().length == numParams) {
                        // Additional candidate with same length
                        numMethodsFoundWithCurrentMinimumArgs++;
                    }
                }
            }
        }
        //匹配的方法计数器大于1，说明有方法重载，无法唯一确定，抛出异常
        if (numMethodsFoundWithCurrentMinimumArgs > 1) {
            throw new IllegalArgumentException("Cannot resolve method '" + methodName +
                    "' to a unique method. Attempted to resolve to overloaded method with " +
                    "the least number of parameters, but there were " +
                    numMethodsFoundWithCurrentMinimumArgs + " candidates.");
        }
        return targetMethod;
    }

    /**
     * 从指定的类中解析文本格式的方法签名,格式为:methodName[([arg_list])],<br/>
     * 此处arg_list不是强制要求的.但如果指定,以逗号分隔的全路径参数类型是必须的.<br/>
     * <p>
     * 当指定查找的方法名时没有添加参数列表，类中包含指定方法名同时参数最少的方法会被返回.
     * <p>
     * 当指定参数列表时，只有参数完全匹配的方法才会返回
     * Parse a method signature in the form {@code methodName[([arg_list])]},
     * where {@code arg_list} is an optional, comma-separated list of fully-qualified
     * type names, and attempts to resolve that signature against the supplied {@code Class}.
     * <p>When not supplying an argument list ({@code methodName}) the method whose name
     * matches and has the least number of parameters will be returned. When supplying an
     * argument type list, only the method whose name and argument types match will be returned.
     * <p>Note then that {@code methodName} and {@code methodName()} are <strong>not</strong>
     * resolved in the same way. The signature {@code methodName} means the method called
     * {@code methodName} with the least number of arguments, whereas {@code methodName()}
     * means the method called {@code methodName} with exactly 0 arguments.
     * <p>If no method can be found, then {@code null} is returned.
     * <p>
     * <p>注意methodName和methodName()的区别,前者返回名称匹配且参数数量最少的方法.而后者返回名称相同且不包含参数
     * 的方法.
     * <p>如果没有方法被找到,null值将会返回.
     *
     * @param signature the method signature as String representation
     * @param clazz     the class to resolve the method signature against
     * @return the resolved Method
     * @see #findMethod
     * @see #findMethodWithMinimalParameters
     */
    public static Method resolveSignature(String signature, Class<?> clazz) {
        Assert.hasText(signature, "'signature' must not be empty");
        Assert.notNull(clazz, "Class must not be null");

        //获取左括号位置
        int firstParen = signature.indexOf("(");
        //获取右括号位置
        int lastParen = signature.indexOf(")");
        //四种情况,要么括号存在和不存在是正常的
        //review jessenpan 是不是可以搞个判断器,判断括号是不是成对出现,代码会清晰些
        if (firstParen > -1 && lastParen == -1) {
            //参数不正确,直接使用jdk原有表示参数不正确的类,没有重新写一个
            throw new IllegalArgumentException("Invalid method signature '" + signature +
                    "': expected closing ')' for args list");
        } else if (lastParen > -1 && firstParen == -1) {
            throw new IllegalArgumentException("Invalid method signature '" + signature +
                    "': expected opening '(' for args list");
        } else if (firstParen == -1 && lastParen == -1) {
            //括号不存在的场景,即方法名匹配即可.使用单独的方法，这个方法也是对外暴露的
            return findMethodWithMinimalParameters(clazz, signature);
        } else {
            //括号存在,指定参数或者参数必须为0的情况,这里可以对methodName加上trim操作
            String methodName = signature.substring(0, firstParen);
            //stringUtils工具类,分隔逗号字符串为数组
            String[] parameterTypeNames =
                    StringUtils.commaDelimitedListToStringArray(signature.substring(firstParen + 1, lastParen));
            Class<?>[] parameterTypes = new Class<?>[parameterTypeNames.length];
            for (int i = 0; i < parameterTypeNames.length; i++) {
                //获取全路径full-qualified参数类型
                String parameterTypeName = parameterTypeNames[i].trim();
                try {
                    //使用classLoader加载类
                    parameterTypes[i] = ClassUtils.forName(parameterTypeName, clazz.getClassLoader());
                } catch (Throwable ex) {
                    throw new IllegalArgumentException("Invalid method signature: unable to resolve type [" +
                            parameterTypeName + "] for argument " + i + ". Root cause: " + ex);
                }
            }
            return findMethod(clazz, methodName, parameterTypes);
        }
    }


    /**
     * 获取指定类的javaBeans规范里的propertyDescriptor集合
     * Retrieve the JavaBeans {@code PropertyDescriptor}s of a given class.
     *
     * @param clazz the Class to retrieve the PropertyDescriptors for
     * @return an array of {@code PropertyDescriptors} for the given class
     * @throws BeansException if PropertyDescriptor look fails
     */
    public static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) throws BeansException {
        //review jessenpan 检查参数first
        //CachedIntrospectionResults身兼两个职位,一个是静态工厂模式的工厂,同时又是CachedIntrospectionResults本身
        //pattern static-factory-method
        CachedIntrospectionResults cr = CachedIntrospectionResults.forClass(clazz);
        //获取instrospection结果包含的propertyDescriptors
        return cr.getPropertyDescriptors();
    }

    /**
     * 获取指定属性名称的propertyDescriptor
     * Retrieve the JavaBeans {@code PropertyDescriptors} for the given property.
     *
     * @param clazz        the Class to retrieve the PropertyDescriptor for
     * @param propertyName the name of the property
     * @return the corresponding PropertyDescriptor, or {@code null} if none
     * @throws BeansException if PropertyDescriptor lookup fails
     */
    public static PropertyDescriptor getPropertyDescriptor(Class<?> clazz, String propertyName)
            throws BeansException {

        CachedIntrospectionResults cr = CachedIntrospectionResults.forClass(clazz);
        return cr.getPropertyDescriptor(propertyName);
    }

    /**
     * 找到指定方法的propertyDescriptor<br/>
     * 这个方法要么是java的pojo里的读方法或者写方法
     * Find a JavaBeans {@code PropertyDescriptor} for the given method,
     * with the method either being the read method or the write method for
     * that bean property.
     *
     * @param method the method to find a corresponding PropertyDescriptor for,
     *               introspecting its declaring class
     * @return the corresponding PropertyDescriptor, or {@code null} if none
     * @throws BeansException if PropertyDescriptor lookup fails
     */
    public static PropertyDescriptor findPropertyForMethod(Method method) throws BeansException {
        return findPropertyForMethod(method, method.getDeclaringClass());
    }

    /**
     * Find a JavaBeans {@code PropertyDescriptor} for the given method,
     * with the method either being the read method or the write method for
     * that bean property.
     *
     * @param method the method to find a corresponding PropertyDescriptor for
     * @param clazz  the (most specific) class to introspect for descriptors
     * @return the corresponding PropertyDescriptor, or {@code null} if none
     * @throws BeansException if PropertyDescriptor lookup fails
     * @since 3.2.13
     */
    public static PropertyDescriptor findPropertyForMethod(Method method, Class<?> clazz) throws BeansException {
        Assert.notNull(method, "Method must not be null");
        PropertyDescriptor[] pds = getPropertyDescriptors(clazz);
        for (PropertyDescriptor pd : pds) {
            //只要指定的方法和读方法(getter)或者写方法(setter)其中一个相同即可
            if (method.equals(pd.getReadMethod()) || method.equals(pd.getWriteMethod())) {
                return pd;
            }
        }
        return null;
    }

    /**
     * 通过javaBean约定的propertyEditor命名方式查找类对应的propertyEditor<br/>
     * Find a JavaBeans PropertyEditor following the 'Editor' suffix convention
     * 比如:"mypackage.MyDomainClass"对应的propertyEditor为"mypackage.MyDomainClassEditor"
     * (e.g. "mypackage.MyDomainClass" -> "mypackage.MyDomainClassEditor").
     * <p>Compatible to the standard JavaBeans convention as implemented by
     * {@link java.beans.PropertyEditorManager} but isolated from the latter's
     * registered default editors for primitive types.
     *
     * @param targetType the type to find an editor for
     * @return the corresponding editor, or {@code null} if none found
     */
    public static PropertyEditor findEditorByConvention(Class<?> targetType) {
        if (targetType == null || targetType.isArray() || unknownEditorTypes.containsKey(targetType)) {
            return null;
        }
        //默认使用查找类的classloader
        ClassLoader cl = targetType.getClassLoader();
        if (cl == null) {
            try {
                //找不到使用系统classloader
                cl = ClassLoader.getSystemClassLoader();
                if (cl == null) {
                    return null;
                }
            } catch (Throwable ex) {
                // e.g. AccessControlException on Google App Engine
                //在google app engine里是无法获取systemClassLoader的
                if (logger.isDebugEnabled()) {
                    logger.debug("Could not access system ClassLoader: " + ex);
                }
                return null;
            }
        }
        //使用约定的命名方式,构建propertyEditor类名称
        String editorName = targetType.getName() + "Editor";
        try {
            //加载对应的propertyEditor类
            Class<?> editorClass = cl.loadClass(editorName);
            //如果找到的类，不是继承自PropertyEditor，标记此类为到unknownEditor中去
            if (!PropertyEditor.class.isAssignableFrom(editorClass)) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Editor class [" + editorName +
                            "] does not implement [java.beans.PropertyEditor] interface");
                }
                unknownEditorTypes.put(targetType, Boolean.TRUE);
                return null;
            }
            //实例化找到的对应propertyEditor
            return (PropertyEditor) instantiateClass(editorClass);
        } catch (ClassNotFoundException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("No property editor [" + editorName + "] found for type " +
                        targetType.getName() + " according to 'Editor' suffix convention");
            }
            //没有对应的类，也标记此类作为缓冲，下次可以直接跳出，不用进行逻辑判断了
            unknownEditorTypes.put(targetType, Boolean.TRUE);
            return null;
        }
    }

    /**
     * Determine the bean property type for the given property from the
     * given classes/interfaces, if possible.
     *
     * @param propertyName the name of the bean property
     * @param beanClasses  the classes to check against
     * @return the property type, or {@code Object.class} as fallback
     */
    public static Class<?> findPropertyType(String propertyName, Class<?>... beanClasses) {
        if (beanClasses != null) {
            for (Class<?> beanClass : beanClasses) {
                PropertyDescriptor pd = getPropertyDescriptor(beanClass, propertyName);
                if (pd != null) {
                    return pd.getPropertyType();
                }
            }
        }
        return Object.class;
    }

    /**
     * Obtain a new MethodParameter object for the write method of the
     * specified property.
     *
     * @param pd the PropertyDescriptor for the property
     * @return a corresponding MethodParameter object
     */
    public static MethodParameter getWriteMethodParameter(PropertyDescriptor pd) {
        if (pd instanceof GenericTypeAwarePropertyDescriptor) {
            return new MethodParameter(((GenericTypeAwarePropertyDescriptor) pd).getWriteMethodParameter());
        } else {
            return new MethodParameter(pd.getWriteMethod(), 0);
        }
    }

    /**
     * Check if the given type represents a "simple" property:
     * a primitive, a String or other CharSequence, a Number, a Date,
     * a URI, a URL, a Locale, a Class, or a corresponding array.
     * <p>Used to determine properties to check for a "simple" dependency-check.
     *
     * @param clazz the type to check
     * @return whether the given type represents a "simple" property
     * @see org.springframework.beans.factory.support.RootBeanDefinition#DEPENDENCY_CHECK_SIMPLE
     * @see org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#checkDependencies
     */
    public static boolean isSimpleProperty(Class<?> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        return isSimpleValueType(clazz) || (clazz.isArray() && isSimpleValueType(clazz.getComponentType()));
    }

    /**
     * Check if the given type represents a "simple" value type:
     * a primitive, a String or other CharSequence, a Number, a Date,
     * a URI, a URL, a Locale or a Class.
     *
     * @param clazz the type to check
     * @return whether the given type represents a "simple" value type
     */
    public static boolean isSimpleValueType(Class<?> clazz) {
        return ClassUtils.isPrimitiveOrWrapper(clazz) || clazz.isEnum() ||
                CharSequence.class.isAssignableFrom(clazz) ||
                Number.class.isAssignableFrom(clazz) ||
                Date.class.isAssignableFrom(clazz) ||
                clazz.equals(URI.class) || clazz.equals(URL.class) ||
                clazz.equals(Locale.class) || clazz.equals(Class.class);
    }


    /**
     * Copy the property values of the given source bean into the target bean.
     * <p>Note: The source and target classes do not have to match or even be derived
     * from each other, as long as the properties match. Any bean properties that the
     * source bean exposes but the target bean does not will silently be ignored.
     * <p>This is just a convenience method. For more complex transfer needs,
     * consider using a full BeanWrapper.
     *
     * @param source the source bean
     * @param target the target bean
     * @throws BeansException if the copying failed
     * @see BeanWrapper
     */
    public static void copyProperties(Object source, Object target) throws BeansException {
        copyProperties(source, target, null, (String[]) null);
    }

    /**
     * Copy the property values of the given source bean into the given target bean,
     * only setting properties defined in the given "editable" class (or interface).
     * <p>Note: The source and target classes do not have to match or even be derived
     * from each other, as long as the properties match. Any bean properties that the
     * source bean exposes but the target bean does not will silently be ignored.
     * <p>This is just a convenience method. For more complex transfer needs,
     * consider using a full BeanWrapper.
     *
     * @param source   the source bean
     * @param target   the target bean
     * @param editable the class (or interface) to restrict property setting to
     * @throws BeansException if the copying failed
     * @see BeanWrapper
     */
    public static void copyProperties(Object source, Object target, Class<?> editable) throws BeansException {
        copyProperties(source, target, editable, (String[]) null);
    }

    /**
     * Copy the property values of the given source bean into the given target bean,
     * ignoring the given "ignoreProperties".
     * <p>Note: The source and target classes do not have to match or even be derived
     * from each other, as long as the properties match. Any bean properties that the
     * source bean exposes but the target bean does not will silently be ignored.
     * <p>This is just a convenience method. For more complex transfer needs,
     * consider using a full BeanWrapper.
     *
     * @param source           the source bean
     * @param target           the target bean
     * @param ignoreProperties array of property names to ignore
     * @throws BeansException if the copying failed
     * @see BeanWrapper
     */
    public static void copyProperties(Object source, Object target, String... ignoreProperties) throws BeansException {
        copyProperties(source, target, null, ignoreProperties);
    }

    /**
     * Copy the property values of the given source bean into the given target bean.
     * <p>Note: The source and target classes do not have to match or even be derived
     * from each other, as long as the properties match. Any bean properties that the
     * source bean exposes but the target bean does not will silently be ignored.
     *
     * @param source           the source bean
     * @param target           the target bean
     * @param editable         the class (or interface) to restrict property setting to
     * @param ignoreProperties array of property names to ignore
     * @throws BeansException if the copying failed
     * @see BeanWrapper
     */
    private static void copyProperties(Object source, Object target, Class<?> editable, String... ignoreProperties)
            throws BeansException {

        Assert.notNull(source, "Source must not be null");
        Assert.notNull(target, "Target must not be null");

        Class<?> actualEditable = target.getClass();
        if (editable != null) {
            if (!editable.isInstance(target)) {
                throw new IllegalArgumentException("Target class [" + target.getClass().getName() +
                        "] not assignable to Editable class [" + editable.getName() + "]");
            }
            actualEditable = editable;
        }
        PropertyDescriptor[] targetPds = getPropertyDescriptors(actualEditable);
        List<String> ignoreList = (ignoreProperties != null ? Arrays.asList(ignoreProperties) : null);

        for (PropertyDescriptor targetPd : targetPds) {
            Method writeMethod = targetPd.getWriteMethod();
            if (writeMethod != null && (ignoreList == null || !ignoreList.contains(targetPd.getName()))) {
                PropertyDescriptor sourcePd = getPropertyDescriptor(source.getClass(), targetPd.getName());
                if (sourcePd != null) {
                    Method readMethod = sourcePd.getReadMethod();
                    if (readMethod != null &&
                            ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType())) {
                        try {
                            if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
                                readMethod.setAccessible(true);
                            }
                            Object value = readMethod.invoke(source);
                            if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
                                writeMethod.setAccessible(true);
                            }
                            writeMethod.invoke(target, value);
                        } catch (Throwable ex) {
                            throw new FatalBeanException(
                                    "Could not copy property '" + targetPd.getName() + "' from source to target", ex);
                        }
                    }
                }
            }
        }
    }

}
