package es.oo.model.attributes;

public class SomeObject2 {
    private String name;
    private Object value;

    public SomeObject2() {}

    public SomeObject2(String name, Object value) {
        this.name = name;
        this.value = value;
    }


    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }
}
