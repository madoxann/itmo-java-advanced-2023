package info.kgeorgiy.ja.fedorenko.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.*;

public class HelloUDPNonblockingServer extends AbstractHelloUDPServer {
    private Thread listener;
    private ExecutorService workers;
    private DatagramChannel ch;

    private Queue<ByteBuffer> prepare;
    private Queue<ResponseState> ready;

    public static void main(String[] args) { mainImpl(args, HelloUDPNonblockingServer.class); }

    @Override
    public void start(final int port, final int threads) {
        workers = new ThreadPoolExecutor(
                threads, threads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), new ThreadPoolExecutor.DiscardPolicy()
        );

        ready = new ConcurrentLinkedQueue<>();

        listener = new Thread(() -> {
            try (Selector sel = Selector.open()) {
                ch = DatagramChannel.open();
                ch.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                ch.configureBlocking(false);
                ch.bind(new InetSocketAddress(port));
                ch.register(sel, SelectionKey.OP_READ);

                prepare = new ConcurrentLinkedQueue<>();
                for (int i = 0; i < threads; i++) {
                    final ByteBuffer buff = ByteBuffer.allocate(ch.socket().getReceiveBufferSize());
                    buff.put(RESPONSE_ADDITION.getBytes(StandardCharsets.UTF_8));

                    prepare.add(buff);
                }

                while (!Thread.interrupted() && !ch.socket().isClosed()) {
                    sel.select(200);

                    for (final Iterator<SelectionKey> iter = sel.selectedKeys().iterator(); iter.hasNext(); iter.remove()) {
                        final SelectionKey key = iter.next();

                        if (key.isReadable()) {
                            if (prepare.isEmpty()) {
                                key.interestOpsAnd(~SelectionKey.OP_READ);
                                sel.wakeup();
                            } else {
                                final ByteBuffer request = prepare.poll();

                                request.clear();
                                request.position(RESPONSE_ADDITION.length());
                                final SocketAddress addr = ch.receive(request);
                                request.flip();

                                workers.submit(() -> {
                                    ready.add(new ResponseState(request, addr));

                                    key.interestOpsOr(SelectionKey.OP_WRITE);
                                    sel.wakeup();
                                });
                            }
                        }

                        if (key.isWritable()) {
                            if (ready.isEmpty()) {
                                key.interestOpsAnd(~SelectionKey.OP_WRITE);
                                sel.wakeup();
                            } else  {
                                final ResponseState state = ready.poll();
                                Util.sendNonblockingUntilSuccess(ch, state.buffer, state.address);

                                prepare.add(state.buffer);
                                key.interestOpsOr(SelectionKey.OP_READ);
                                sel.wakeup();
                            }
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                System.err.println("Failed to receive request: " + e.getMessage());
            }
        }) {{ start(); }};
    }

    @Override
    public void close() {
        try {
            ch.close();
        } catch (IOException e) {
            System.err.println("Unable to close datagram chanel");
        }

        listener.interrupt();
        try {
            listener.join();
        } catch (InterruptedException ignored) {}

        workers.close();
    }

    private record ResponseState(ByteBuffer buffer, SocketAddress address) {}
}
