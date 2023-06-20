package info.kgeorgiy.ja.fedorenko.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPClient extends AbstractHelloIDPClient {
    public static void main(String[] args) { mainImpl(args, HelloUDPClient.class); }

    private String trySendingAndReceiving(DatagramSocket sock, DatagramPacket p, DatagramPacket r, String check) throws InterruptedException {
        String response;
        while (true) {
            try {
                sock.send(p);
                sock.receive(r);

                response = new String(r.getData(), 0, r.getLength(), StandardCharsets.UTF_8);
                if (checkResponse(response, check)) break;
            } catch (IOException | SecurityException e) {
                Thread.sleep(100); // try again...
            }
        }

        return response;
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        final InetSocketAddress address = new InetSocketAddress(host, port);

        try (final ExecutorService senders = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                final int finalI = i;

                senders.submit(() -> {
                    int successful = 0;

                    try (DatagramSocket sock = new DatagramSocket()) {
                        sock.setSoTimeout(300);
                        final DatagramPacket response = new DatagramPacket(new byte[sock.getReceiveBufferSize()], sock.getReceiveBufferSize());
                        final DatagramPacket request = new DatagramPacket(new byte[0], 0, address);

                        while (!Thread.interrupted() && successful < requests) {
                            final String reqStr = formRequest(prefix, finalI + 1, successful + 1);
                            final byte[] bytes = reqStr.getBytes(StandardCharsets.UTF_8);
                            request.setData(bytes);

                            System.out.println(trySendingAndReceiving(sock, request, response, reqStr));
                            successful++;
                        }
                    } catch (SocketException e) {
                        System.err.println("Socket couldn't be opened by " + Thread.currentThread().getName());
                        System.err.println(e.getMessage());
                    } catch (InterruptedException ignored) {
                    }
                });
            }
        }
    }
}
