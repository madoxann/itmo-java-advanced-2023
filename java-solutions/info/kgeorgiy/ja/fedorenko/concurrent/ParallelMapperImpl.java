package info.kgeorgiy.ja.fedorenko.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> workers;
    private final JobQueue jobs;

    public ParallelMapperImpl(int threadN) {
        jobs = new JobQueue(new ArrayDeque<>());
        workers = new ArrayList<>(threadN);

        final Runnable th = () -> {
            try {
                while (!Thread.interrupted()) jobs.pollJob().run();
            } catch (InterruptedException e) {
                // return immediately
            }
        };

        for (int i = 0; i < threadN; i++) {
            workers.add(new Thread(th) {{ start(); }});
        }
    }

    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        final List<R> answer = new ArrayList<>(Collections.nCopies(args.size(), null));
        final JobCountdown cnt = new JobCountdown(args.size());
        final RuntimeException thrown = new RuntimeException("Exception is thrown during map");

        for (int i = 0; i < args.size(); i++) {
            final int finalI = i;

            jobs.addJob(() -> {
                try {
                    answer.set(finalI, f.apply(args.get(finalI)));
                    cnt.doStage();
                } catch (RuntimeException e) {
                    thrown.addSuppressed(e);
                }
            });
        }

        cnt.awaitCompletion();
        if (thrown.getSuppressed().length > 0) throw thrown;
        return answer;
    }

    @Override
    public void close() {
        final RuntimeException thrown = new RuntimeException("Exception is thrown during thread closing");
        for (Thread th : workers) {
            th.interrupt();

            try {
                th.join();
            } catch (RuntimeException | InterruptedException e) {
                thrown.addSuppressed(e);
            }
        }

        if (thrown.getSuppressed().length > 0) throw thrown;
    }

    private record JobQueue(Queue<Runnable> jobs) {
        public synchronized void addJob(Runnable r) {
            jobs.add(r);
            notify();
        }

        public synchronized Runnable pollJob() throws InterruptedException {
            while (jobs.isEmpty()) {
                wait();
            }

            return jobs.poll();
        }
    }

    private static class JobCountdown {
        private int stages;

        JobCountdown(int s) { stages = s; }

        public synchronized void doStage() {
            stages--;

            if (ready()) notify();
        }

        public synchronized void awaitCompletion() throws InterruptedException {
            while (!ready()) {
                wait();
            }
        }

        public boolean ready() { return stages == 0; }
    }
}
