package info.kgeorgiy.ja.fedorenko.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

/*package-private*/ abstract class AbstractHelloUDPServer implements HelloServer {
    private static final String USAGE = "Usage: HelloUDPServer port [workers]";
    protected final String RESPONSE_ADDITION = "Hello, ";

    protected static void mainImpl(String[] args, Class<? extends HelloServer> token) {
        try {
            if (args.length < 1 || args.length > 2) throw new IllegalArgumentException("Incorrect argument length!");
            if (Arrays.stream(args).anyMatch(Objects::isNull))
                throw new IllegalArgumentException("null arguments are not allowed");

            try (HelloServer srv = token.getDeclaredConstructor().newInstance()) {
                srv.start(Integer.parseInt(args[0]), Util.tryGetArgument(args, 2, 16));
                while (true) {
                    // server loop
                    Thread.sleep(Duration.ofDays(1));
                }
            } catch (InterruptedException | ReflectiveOperationException | InstantiationError e) {
                throw new RuntimeException(e);
            }
        } catch (NumberFormatException e) {
            System.err.println("Expected number: " + e.getMessage());
            System.err.println(USAGE);


        } catch (IllegalArgumentException e) {
            System.err.println("Wrong arguments were passed: " + e.getMessage());
            System.err.println(USAGE);
        }
    }
}
