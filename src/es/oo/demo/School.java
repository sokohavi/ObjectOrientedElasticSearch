package es.oo.demo;

import java.util.List;

public class School {
    private List<Student> studentsList;
    private String schoolId;
    private String address;
    private SchoolStaff manager;

    public School() {}

    public School(final List<Student> studentsList, final String schoolId,
                  final SchoolStaff manager, final String address) {
        this.studentsList = studentsList;
        this.schoolId = schoolId;
        this.manager = manager;
        this.address = address;
    }

    public List<Student> getStudentsList() {
        return this.studentsList;
    }

    public String getSchoolId() {
        return this.schoolId;
    }

    public String getAddress() {
        return this.address;
    }

    public SchoolStaff getManager() {
        return this.manager;
    }
}