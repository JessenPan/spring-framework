package org.jessenpan.spring.comment.annotation;

import lombok.Getter;
import lombok.ToString;

/**
 * @author jessenpan
 * @date 2019/9/17 下午10:09
 */
@Getter
@ToString
public enum DesignPrincipleEnum {

    SRP(1, "职责单一原则");

    private int code;
    private String desc;

    DesignPrincipleEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
