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
    SINGLETON(CREATIONAL,"单例模式"),
    BUILDER(CREATIONAL,"建造器模式"),
    /**
     * 结构型
     */
    DECORATOR(STRUCTURAL, "装饰器模式"),
    ADAPTER(STRUCTURAL, "适配器模式"),  //确定下那个是适配者、那个是被适配者
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DesignPatternEnum{");
        sb.append("category=").append(category);
        sb.append(", desc='").append(desc).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
