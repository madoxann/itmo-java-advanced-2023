package info.kgeorgiy.ja.fedorenko.student;

import info.kgeorgiy.java.advanced.student.Student;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public enum StudentOrder {
    BY_NAME(Comparator.comparing(Student::getLastName, Collections.reverseOrder(String::compareTo))
            .thenComparing(Student::getFirstName, Collections.reverseOrder(String::compareTo))
            .thenComparingInt(Student::getId)),
    BY_ID(Comparator.comparingInt(Student::getId));

    private final Comparator<Student> cmp;

    StudentOrder(Comparator<Student> cmp) { this.cmp = cmp; }

    public List<Student> getOrder(Collection<Student> coll) { return sortStudentsBy(coll, cmp); }

    private static List<Student> sortStudentsBy(Collection<Student> students, Comparator<Student> cmp) {
        return students.stream().sorted(cmp).toList();
    }
}