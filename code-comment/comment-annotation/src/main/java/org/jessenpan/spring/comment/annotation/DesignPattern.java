package org.jessenpan.spring.comment.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author jessenpan
 * @date 2019/9/12 下午5:10
 */
@Target({ ElementType.METHOD,ElementType.FIELD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface DesignPattern {

    DesignPatternEnum[] value();

}
