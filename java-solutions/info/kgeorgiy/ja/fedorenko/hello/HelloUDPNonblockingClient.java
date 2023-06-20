package info.kgeorgiy.ja.fedorenko.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class HelloUDPNonblockingClient extends AbstractHelloIDPClient {

    public static void main(String[] args) { mainImpl(args, HelloUDPNonblockingClient.class); }

    private String formRequest(RequestState s, String p) { return formRequest(p, s.thread, s.request); }

    private void trySending(SelectionKey key, String prefix) throws IOException, InterruptedException {
        final DatagramChannel ch = (DatagramChannel) key.channel();
        final ByteBuffer buff = ByteBuffer.wrap(
                formRequest((RequestState) key.attachment(), prefix).getBytes(StandardCharsets.UTF_8)
        );

        Util.sendNonblockingUntilSuccess(ch, buff, ch.getRemoteAddress());

        key.interestOps(SelectionKey.OP_READ);
    }

    private boolean tryReceiving(SelectionKey key, String prefix, int requests) throws IOException {
        final DatagramChannel ch = (DatagramChannel) key.channel();
        final ByteBuffer buff = ByteBuffer.allocate(ch.socket().getReceiveBufferSize());
        final RequestState state = (RequestState) key.attachment();

        buff.clear();
        try {
            ch.receive(buff);
        } catch (IOException e) {
            System.err.println("Error while receiving");
            return false;
        }
        buff.flip();

        String check = formRequest(state, prefix);
        String response = new String(buff.array(), 0, buff.limit(), StandardCharsets.UTF_8);

        if (checkResponse(response, check)) {
            System.out.println(response);
            if (state.request == requests) return true;

            key.attach(new RequestState(state.thread, state.request + 1));
        }
        key.interestOps(SelectionKey.OP_WRITE);

        return false;
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try (final Selector sel = Selector.open()) {
            final InetSocketAddress address = new InetSocketAddress(host, port);

            for (int i = 0; i < threads; i++) {
                final DatagramChannel ch = DatagramChannel.open();
                ch.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                ch.configureBlocking(false);
                ch.connect(address);
                ch.register(sel, SelectionKey.OP_WRITE, new RequestState(i + 1, 1));
            }

            while (!Thread.interrupted() && !sel.keys().isEmpty()) {
                sel.select(100);

                if (sel.selectedKeys().isEmpty())
                    for (SelectionKey key : sel.keys())
                        trySending(key, prefix);

                for (Iterator<SelectionKey> iter = sel.selectedKeys().iterator(); iter.hasNext(); iter.remove()) {
                    SelectionKey key = iter.next();

                    if (key.isWritable()) trySending(key, prefix);
                    else if (key.isReadable() && tryReceiving(key, prefix, requests)) {
                        key.channel().close();
                        key.cancel();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Exception occurred - unable to proceed: \n" + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("A thread was interrupted - unable to process requests");
        }
    }

    private record RequestState(int thread, int request) {}
}
