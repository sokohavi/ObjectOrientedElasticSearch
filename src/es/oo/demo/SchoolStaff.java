package es.oo.demo;

public class SchoolStaff extends Person {
    private SchoolStaff supervisor;

    public SchoolStaff() {}

    public SchoolStaff(final String name, final String id, final SchoolStaff supervisor) {
        super(id, name);
        this.supervisor = supervisor;
    }

    public SchoolStaff getSupervisor() {
        return this.supervisor;
    }
}