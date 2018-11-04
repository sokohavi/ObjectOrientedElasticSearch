package es.oo.demo;

public class Person {
    private String id;
    private String name;

    public Person() {}

    public Person(final String id, final String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }
}
