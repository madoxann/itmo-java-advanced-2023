package info.kgeorgiy.ja.fedorenko.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class HelloUDPServer extends AbstractHelloUDPServer {
    private Thread listener;
    private ExecutorService workers;
    private DatagramSocket sock;

    private Map<Long, ByteBuffer> buffMap;
    private final Function<Long, ByteBuffer> allocateNewFilled = id -> {
        final ByteBuffer buff;
        try {
            buff = ByteBuffer.allocate(sock.getReceiveBufferSize());
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        buff.put(RESPONSE_ADDITION.getBytes(StandardCharsets.UTF_8));
        return buff;
    };

    public static void main(String[] args) { mainImpl(args, HelloUDPServer.class); }

    private ByteBuffer getFilledBuffer(DatagramPacket data) {
        final ByteBuffer hello = buffMap.computeIfAbsent(Thread.currentThread().threadId(), allocateNewFilled);

        hello.clear();
        hello.position(RESPONSE_ADDITION.length());
        hello.put(data.getData(), data.getOffset(), data.getLength());
        hello.flip();
        return hello;
    }

    private void trySending(DatagramPacket r) throws InterruptedException {
        while (!sock.isClosed()) {
            try {
                sock.send(r);
                break;
            } catch (IOException | SecurityException e) {
                Thread.sleep(100); // keep sending...
            }
        }
    }

    @Override
    public void start(final int port, final int threads) {
        buffMap = new HashMap<>(threads);
        workers = new ThreadPoolExecutor(threads, threads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), new ThreadPoolExecutor.DiscardPolicy());

        try {
            sock = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("Failed to create socket on a given port: " + e.getMessage());
            return;
        }

        listener = new Thread(() -> {
            while (!Thread.interrupted() && !sock.isClosed()) {
                try {
                    final DatagramPacket request = new DatagramPacket(new byte[sock.getReceiveBufferSize()], sock.getReceiveBufferSize());

                    sock.receive(request);
                    workers.submit(() -> {
                        final ByteBuffer hello = getFilledBuffer(request);

                        try {
                            trySending(new DatagramPacket(hello.array(), hello.limit(), request.getSocketAddress()));
                        } catch (InterruptedException ignored) {}
                    });
                } catch (IOException | SecurityException e) {
                    System.err.println("Failed to receive request: " + e.getMessage());
                }
            }
        }) {{ start(); }};
    }

    @Override
    public void close() {
        sock.close();

        listener.interrupt();
        try {
            listener.join();
        } catch (InterruptedException e) {
            workers.shutdownNow();
        }

        workers.close();
    }
}
