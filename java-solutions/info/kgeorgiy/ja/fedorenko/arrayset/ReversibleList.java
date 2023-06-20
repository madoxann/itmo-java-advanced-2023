package info.kgeorgiy.ja.fedorenko.arrayset;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

class ReversibleList<T> extends AbstractList<T> implements RandomAccess {
    private final List<T> c;
    private final boolean rev;

    public ReversibleList(List<T> coll, boolean isReversed) {
        c = coll;
        rev = isReversed;
    }

    @Override
    public T get(int i) { return rev ? c.get(c.size() - i - 1) : c.get(i); }

    @Override
    public int size() { return c.size(); }

    public ReversibleList<T> reverse() { return new ReversibleList<>(c, !rev); }
}