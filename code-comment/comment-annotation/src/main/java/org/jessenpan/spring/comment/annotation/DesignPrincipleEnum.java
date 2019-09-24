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

    SRP(1, "职责单一原则"),
    OCP(2, "开闭原则"),
    LSP(3, "里氏替换原则"),
    ISP(4, "接口隔离原则"),
    DIP(5, "依赖注入原则"),

    LKP(6, "迪米特-最少知道原则");

    private int code;
    private String desc;

    DesignPrincipleEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
