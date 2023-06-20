package info.kgeorgiy.ja.fedorenko.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper mapper;

    public IterativeParallelism() { mapper = null; }
    public IterativeParallelism(ParallelMapper m) { mapper = m; }

    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        Function<Stream<? extends T>, T> getReduced = stream -> stream.map(s -> (T) s).reduce(monoid.getIdentity(), monoid.getOperator());
        return splitToThreads(threads, values, getReduced, getReduced);
    }

    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid) throws InterruptedException {
        return reduce(threads, map(threads, values, lift), monoid);
    }

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return splitToThreads(threads, values, stream -> stream.map(String::valueOf).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining()));
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return splitToThreads(threads, values, stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).toList());
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return splitToThreads(threads, values, stream -> stream.map(i -> (U) f.apply(i)).toList(),
                stream -> stream.flatMap(Collection::stream).toList());
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        Function<Stream<? extends T>, T> getMax = stream -> stream.max(comparator).orElseThrow();
        return splitToThreads(threads, values, getMax, getMax);
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        Function<Stream<? extends T>, T> getMin = stream -> stream.min(comparator).orElseThrow();
        return splitToThreads(threads, values, getMin, getMin);
    }

    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return splitToThreads(threads, values, stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return splitToThreads(threads, values, stream -> stream.anyMatch(predicate),
                stream -> stream.anyMatch(Boolean::booleanValue));
    }

    @Override
    public <T> int count(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return splitToThreads(threads, values, stream -> (int) stream.filter(predicate).count(),
                stream -> stream.mapToInt(Integer::intValue).sum());
    }

    private <T, R> R splitToThreads(int threads, List<? extends T> values, Function<Stream<? extends T>, R> accumulate, Function<Stream<? extends R>, R> transform) throws InterruptedException {
        int threadN = Math.min(threads, values.size());
        int sliceSz = values.size() / threadN;
        int remainder = values.size() % threadN;
        List<Stream<? extends T>> tasks = new ArrayList<>(threadN);

        for (int i = 0, prevSlice = 0; i < threadN; i++) {
            final int currSlice = prevSlice + sliceSz + ((remainder-- > 0) ? 1 : 0);

            tasks.add(values.subList(prevSlice, currSlice).stream());
            prevSlice = currSlice;
        }

        if (mapper == null) {
            try (ParallelMapper m = new ParallelMapperImpl(threadN)) {
                return transform.apply(m.map(accumulate, tasks).stream());
            }
        }
        return transform.apply(mapper.map(accumulate, tasks).stream());
    }
}
