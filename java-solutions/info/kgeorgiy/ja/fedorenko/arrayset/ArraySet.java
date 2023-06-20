package info.kgeorgiy.ja.fedorenko.arrayset;

import java.util.*;
import java.util.function.Predicate;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final ReversibleList<T> coll;
    private final Comparator<? super T> cmp;
    private final int sz;
    private final Predicate<Integer> isOutOfBounds = (i) -> i < 0 || i >= size();

    private enum Strictness { LESS, LESS_OR_EQUAL, GREATER, GREATER_OR_EQUAL, }

    public ArraySet() { this(List.of(), null); }

    public ArraySet(Comparator<? super T> comparator) { this(List.of(), comparator); }

    public ArraySet(Collection<T> collection) { this(collection, null); }

    public ArraySet(Collection<T> collection, Comparator<? super T> comparator) {
        this(List.copyOf(
                (collection instanceof SortedSet<T> && ((SortedSet<T>) collection).comparator() == comparator)
                        ? collection
                        : new TreeSet<>(comparator) {{ this.addAll(collection); }}
                ), comparator);
    }

    private ArraySet(List<T> collection, Comparator<? super T> comparator) {
        this(new ReversibleList<>(collection, false), comparator);
    }

    private ArraySet(ReversibleList<T> collection, Comparator<? super T> comparator) {
        coll = collection;
        cmp = comparator;
        sz = collection.size();
    }

    @Override
    public Comparator<? super T> comparator() { return cmp; }

    @Override
    public T lower(T t) { return getExactElement(t, Strictness.LESS); }

    @Override
    public T floor(T t) { return getExactElement(t, Strictness.LESS_OR_EQUAL); }

    @Override
    public T ceiling(T t) { return getExactElement(t, Strictness.GREATER_OR_EQUAL); }

    @Override
    public T higher(T t) { return getExactElement(t, Strictness.GREATER); }

    private int getExactPosition(T element, Strictness s) {
        int p = Collections.binarySearch(coll, element, cmp);

        return switch (s) {
            case LESS -> p >= 0 ? (p - 1) : (-p - 2);
            case LESS_OR_EQUAL -> p >= 0 ? p : (-p - 2);
            case GREATER -> p >= 0 ? p + 1 : (-p - 1);
            case GREATER_OR_EQUAL -> p >= 0 ? p : (-p - 1);
        };
    }

    private T getExactElement(T element, Strictness s) {
        int p = getExactPosition(element, s);
        return isOutOfBounds.test(p) ? null : coll.get(p);
    }

    @Override
    public T pollFirst() { throw new UnsupportedOperationException("Set is immutable"); }

    @Override
    public T pollLast() { throw new UnsupportedOperationException("Set is immutable"); }

    @Override
    public void clear() { throw new UnsupportedOperationException("Set is immutable"); }

    @Override
    public Iterator<T> iterator() { return coll.iterator(); }

    @Override
    public NavigableSet<T> descendingSet() { return new ArraySet<>(coll.reverse(), Collections.reverseOrder(cmp)); }

    @Override
    public Iterator<T> descendingIterator() { return coll.reverse().iterator(); }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        if (o != null) {
            return Collections.binarySearch(coll, (T) o, cmp) >= 0;
        }

        return false;
    }

    private NavigableSet<T> subSetOrEmpty(T t, boolean b, T e1, boolean b1) {
        int p1 = getExactPosition(t, b ? Strictness.GREATER_OR_EQUAL : Strictness.GREATER);
        int p2 = getExactPosition(e1, b1 ? Strictness.LESS_OR_EQUAL : Strictness.LESS);

        if (isOutOfBounds.test(p1) || isOutOfBounds.test(p2)) return new ArraySet<>(cmp);
        return new ArraySet<>(coll.subList(p1, p2 + 1), cmp);
    }

    @SuppressWarnings("unchecked")
    @Override
    public NavigableSet<T> subSet(T t, boolean b, T e1, boolean b1) {
        int test = (cmp == null) ? ((Comparable<T>) t).compareTo(e1) : cmp.compare(t, e1);
        if (test > 0) throw new IllegalArgumentException("fromKey > toKey");
        if (test == 0 && !b && !b1) return new ArraySet<>(cmp);

        return subSetOrEmpty(t, b, e1, b1);
    }

    @Override
    public NavigableSet<T> headSet(T t, boolean b) {
        return coll.isEmpty() ? new ArraySet<>(cmp) : subSetOrEmpty(first(), true, t, b);
    }

    @Override
    public NavigableSet<T> tailSet(T t, boolean b) {
        return coll.isEmpty() ? new ArraySet<>(cmp) : subSetOrEmpty(t, b, last(), true);
    }

    @Override
    public SortedSet<T> subSet(T t, T e1) { return subSet(t, true, e1, false); }

    @Override
    public SortedSet<T> headSet(T t) { return headSet(t, false); }

    @Override
    public SortedSet<T> tailSet(T t) { return tailSet(t, true); }

    @Override
    public T first() {
        checkIfEmpty();
        return coll.get(0);
    }

    @Override
    public T last() {
        checkIfEmpty();
        return coll.get(size() - 1);
    }

    @Override
    public int size() { return sz; }

    private void checkIfEmpty() { if (coll.isEmpty()) throw new NoSuchElementException("Collection is empty"); }
}