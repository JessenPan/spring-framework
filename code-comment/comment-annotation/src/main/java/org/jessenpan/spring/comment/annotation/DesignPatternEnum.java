package org.jessenpan.spring.comment.annotation;

import lombok.Getter;
import lombok.ToString;

import static org.jessenpan.spring.comment.annotation.DesignPatternCategory.*;

/**
 * @author jessenpan
 */
@Getter
@ToString
public enum DesignPatternEnum {

    /**
     * 创建型
     */
    SIMPLE_FACTORY(CREATIONAL, "简单工厂"),
    /**
     * 结构型
     */
    DECORATOR(STRUCTURAL, "装饰器模式"),
    /**
     * 行为型
     */
    TEMPLATE(BEHAVIORAL, "模板模式"),
    OBSERVER(BEHAVIORAL, "观察者模式");

    private DesignPatternCategory category;
    private String desc;

    DesignPatternEnum(DesignPatternCategory designPatternCategory, String desc) {
        this.category = designPatternCategory;
        this.desc = desc;
    }

}
