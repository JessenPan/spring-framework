package org.jessenpan.spring.comment.annotation;

import lombok.Getter;

/**
 * @author jessenpan
 */
@Getter
public enum DesignPatternCategory {

    CREATIONAL(1, "创建型"),
    STRUCTURAL(2, "结构型"),
    BEHAVIORAL(3, "行为型");

    private int code;
    private String desc;

    DesignPatternCategory(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
