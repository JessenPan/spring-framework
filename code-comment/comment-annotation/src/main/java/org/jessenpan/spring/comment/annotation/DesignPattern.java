package org.jessenpan.spring.comment.annotation;

import lombok.Getter;
import lombok.ToString;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author jessenpan
 * @date 2019/9/12 下午5:10
 */
@Target({ ElementType.FIELD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface DesignPattern {

    DesignPatternEnum[] value();

    @Getter
    @ToString
    enum DesignPatternEnum {

        TEMPLATE(DesignPatternCategory.STRUCTAL, "模板模式"),
        
        
        SIMPLE_FACTORY(DesignPatternCategory.CREATIONAL,"简单工厂");

        private DesignPatternCategory category;
        private String desc;

        DesignPatternEnum(DesignPatternCategory designPatternCategory, String desc) {
            this.category = designPatternCategory;
            this.desc = desc;
        }

    }

    @Getter
    enum DesignPatternCategory {

        CREATIONAL(1, "创建型"),
        STRUCTAL(2,"结构型");

        private int code;
        private String desc;

        DesignPatternCategory(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

    }
}
