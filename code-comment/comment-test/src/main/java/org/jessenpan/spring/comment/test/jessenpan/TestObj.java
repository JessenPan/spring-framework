package org.jessenpan.spring.comment.test.jessenpan;

/**
 * @author jessenpan
 * @sine 17/5/29
 */
public class TestObj {

    private Object object;

    private String name;

    public TestObj(Object object) {
        this.object = object;
    }

    public TestObj() {

    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
