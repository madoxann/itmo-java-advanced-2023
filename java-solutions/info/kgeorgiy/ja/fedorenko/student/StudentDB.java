package info.kgeorgiy.ja.fedorenko.student;

import info.kgeorgiy.java.advanced.student.*;
import net.java.quickcheck.collection.Pair;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class StudentDB implements AdvancedQuery {
    private final Function<Student, String> fullName = st -> st.getFirstName() + " " + st.getLastName();

    private <T> List<T> getFromStudents(Collection<Student> students, Function<Student, T> mapper) {
        return students.stream().map(mapper).toList();
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getFromStudents(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) { return getFromStudents(students, Student::getLastName); }

    @Override
    public List<GroupName> getGroups(List<Student> students) { return getFromStudents(students, Student::getGroup); }

    @Override
    public List<String> getFullNames(List<Student> students) { return getFromStudents(students, fullName); }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return new TreeSet<>(getFromStudents(students, Student::getFirstName));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.isEmpty() ? "" :
                students.stream().max(Comparator.comparingInt(Student::getId)).get().getFirstName();
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return StudentOrder.BY_ID.getOrder(students);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return StudentOrder.BY_NAME.getOrder(students);
    }

    private <T> Stream<Student> filteredByEquals(Collection<Student> students,
                                                 Function<Student, T> mapper, Object obj) {
        return students.stream().filter(st -> mapper.apply(st).equals(obj));
    }

    private <T> List<Student> sortFilteredByEquals(Collection<Student> students, Function<Student, T> mapper,
                                                   Object obj, StudentOrder ord) {
        return ord.getOrder(filteredByEquals(students, mapper, obj).toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return sortFilteredByEquals(students, Student::getFirstName, name, StudentOrder.BY_NAME);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return sortFilteredByEquals(students, Student::getLastName, name, StudentOrder.BY_NAME);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return sortFilteredByEquals(students, Student::getGroup, group, StudentOrder.BY_NAME);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return filteredByEquals(students, Student::getGroup, group)
                .collect(Collectors.groupingBy(
                        Student::getLastName, Collectors.mapping(
                                Student::getFirstName, Collectors.collectingAndThen(
                                        Collectors.minBy(String::compareTo), opt -> opt.orElse(""))
                        )));
    }

    private List<Group> getOrderedGroups(Collection<Student> students, StudentOrder ord) {
        return getGroups(students.stream().distinct().toList())
                .stream().distinct().map(
                        gr -> new Group(gr, sortFilteredByEquals(students, Student::getGroup, gr, ord))
                ).sorted(Comparator.comparing(Group::getName)).toList();
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getOrderedGroups(students, StudentOrder.BY_NAME);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getOrderedGroups(students, StudentOrder.BY_ID);
    }

    private GroupName getLargestGroupBy(Collection<Student> students, Comparator<Group> cmp) {
        return getGroupsByName(students).stream()
                .map(gr -> new Pair<>(gr, gr.getName()))
                .max(Comparator.comparing(Pair::getFirst, cmp)).orElse(new Pair<>(null, null)).getSecond();
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getLargestGroupBy(students, Comparator.comparingInt((ToIntFunction<Group>) gr -> gr.getStudents().size())
                .thenComparing(gr -> gr.getName().name(), String::compareTo)
        );
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupBy(students, Comparator.comparingLong(
                (ToLongFunction<Group>) gr -> getFirstNames(gr.getStudents()).stream().distinct().count()
        ).thenComparing(gr -> gr.getName().name(), Collections.reverseOrder(String::compareTo)));
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(
                        Student::getFirstName, Collectors.mapping(Student::getGroup, Collectors.toSet())
                )).entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .max(Map.Entry.comparingByValue(Comparator.comparingInt(Set::size)))
                .orElse(Map.entry("", Set.of())).getKey();
    }

    // adv from y2020
    private <T> List<T> getFromIds(Collection<Student> students, int[] ids, Function<Student, T> mapper) {
        return students.stream().collect(Collectors.collectingAndThen(
                Collectors.toList(),
                lst -> Arrays.stream(ids).mapToObj(ind -> mapper.apply(lst.get(ind))).toList()
        ));
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] ids) {
        return getFromIds(students, ids, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] ids) {
        return getFromIds(students, ids, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(Collection<Student> students, int[] ids) {
        return getFromIds(students, ids, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] ids) {
        return getFromIds(students, ids, fullName);
    }
}
