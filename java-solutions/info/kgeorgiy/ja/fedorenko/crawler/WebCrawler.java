package info.kgeorgiy.ja.fedorenko.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler{
    private static final String USAGE = "WebCrawler url [depth [downloads [extractors [perHost]]]]";
    private final Downloader dwn;
    private final int perHost;
    private final ExecutorService extractors, downloaders;
    private final ConcurrentMap<String, HostLoadManager> hosts = new ConcurrentHashMap<>();

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        dwn = downloader;
        this.perHost = perHost;

        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
    }

    @Override
    public Result download(String url, int depth) { return new BFSCrawler().crawl(url, depth); }

    private static int tryGetArgument(String[] args, int pos, int otherwise) {
        if (args.length <= pos) return otherwise;
        return Integer.parseInt(args[pos]);
    }

    public static void main(String[] args) {
        try {
            if (args.length < 2) throw new IllegalArgumentException("Incorrect argument length!");
            if (Arrays.stream(args).anyMatch(Objects::isNull)) throw new IllegalArgumentException("null arguments are not allowed");

            try (WebCrawler crwl = new WebCrawler(new CachingDownloader(1), tryGetArgument(args, 2, 16),
                    tryGetArgument(args, 3, 16), tryGetArgument(args, 4, 10))) {
                crwl.download(args[0], Integer.parseInt(args[1]));
            }
        } catch (IOException e) {
            System.err.println("IOException occurred: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Wrong arguments were passed: " + e.getMessage());
            System.err.println(USAGE);
        }
    }

    @Override
    public void close() {
        extractors.shutdownNow();
        downloaders.shutdownNow();
    }

    private class HostLoadManager {
        private final Queue<Runnable> queue = new ArrayDeque<>();
        private int downloadingNow = 0;

        public synchronized void add(Runnable r) {
            final Runnable submitting = () -> {
                try {
                    r.run();
                } finally {
                    trySubmitRemains();
                }
            };

            if (downloadingNow < perHost) {
                downloaders.submit(submitting);
                downloadingNow++;
            } else queue.add(submitting);
        }

        private synchronized void trySubmitRemains() {
            final Runnable tmp = queue.poll();

            if (tmp == null) downloadingNow--;
            else downloaders.submit(tmp);
        }
    }

    private class BFSCrawler {
        private final Phaser ph = new Phaser(1);
        private final Set<String> enqueued = ConcurrentHashMap.newKeySet();

        private void download(ConcurrentLinkedQueue<Stage> bfs, Set<String> downloaded, ConcurrentMap<String, IOException> errors, Stage now, int depth) throws MalformedURLException {
            final HostLoadManager manager = hosts.computeIfAbsent(URLUtils.getHost(now.url), (str) -> new HostLoadManager());
            ph.register();

            manager.add(() -> {
                try {
                    final Document res = dwn.download(now.url);
                    downloaded.add(now.url);

                    if (now.depth >= depth) return;
                    extract(bfs, res, now.depth);
                } catch (IOException e) {
                    errors.put(now.url, e);
                } finally {
                    ph.arriveAndDeregister();
                }
            });
        }

        private void extract(ConcurrentLinkedQueue<Stage> bfs, Document res, int depth) {
            ph.register();

            extractors.submit(() -> {
                try {
                    res.extractLinks().stream()
                            .filter(enqueued::add)
                            .forEach(newUrl -> bfs.add(new Stage(newUrl, depth + 1)));
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    ph.arriveAndDeregister();
                }
            });
        }

        public Result crawl(String url, int depth) {
            final ConcurrentLinkedQueue<Stage> bfs = new ConcurrentLinkedQueue<>(List.of(new Stage(url, 1)));
            final ConcurrentMap<String, IOException> errors = new ConcurrentHashMap<>();
            final Set<String> downloaded = ConcurrentHashMap.newKeySet();
            int prevDepth = 0;

            enqueued.add(url);
            while (!bfs.isEmpty()) {
                final Stage now = bfs.poll();
                if (now.depth > prevDepth) {
                    ph.arriveAndAwaitAdvance();
                    prevDepth++;
                }

                try {
                    download(bfs, downloaded, errors, now, depth);
                } catch (MalformedURLException e) {
                    errors.put(now.url, e);
                }

                if (bfs.isEmpty()) ph.arriveAndAwaitAdvance();
            }

            return new Result(new ArrayList<>(downloaded), errors);
        }

        private record Stage(String url, int depth) {}
    }
}
