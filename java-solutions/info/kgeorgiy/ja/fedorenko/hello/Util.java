package info.kgeorgiy.ja.fedorenko.hello;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/*package-private*/ class Util {
    private Util() {}

    public static int tryGetArgument(String[] args, int pos, int otherwise) {
        if (args.length <= pos) return otherwise;
        return Integer.parseInt(args[pos]);
    }

    public static void sendNonblockingUntilSuccess(DatagramChannel ch, ByteBuffer buff, SocketAddress addr) throws InterruptedException {
        while (ch.isOpen()) {
            try {
                ch.send(buff, addr);
                break;
            } catch (IOException | SecurityException e) {
                Thread.sleep(100);
            }
        }
    }
}
