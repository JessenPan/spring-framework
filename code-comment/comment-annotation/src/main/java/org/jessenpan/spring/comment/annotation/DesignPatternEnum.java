package org.jessenpan.spring.comment.annotation;

import lombok.Getter;
import lombok.ToString;

import static org.jessenpan.spring.comment.annotation.DesignPatternCategory.CREATIONAL;
import static org.jessenpan.spring.comment.annotation.DesignPatternCategory.STRUCTAL;

/**
 * @author jessenpan
 */
@Getter
@ToString
public enum DesignPatternEnum {

    TEMPLATE(STRUCTAL, "模板模式"),

    SIMPLE_FACTORY(CREATIONAL, "简单工厂");

    private DesignPatternCategory category;
    private String desc;

    DesignPatternEnum(DesignPatternCategory designPatternCategory, String desc) {
        this.category = designPatternCategory;
        this.desc = desc;
    }

}
