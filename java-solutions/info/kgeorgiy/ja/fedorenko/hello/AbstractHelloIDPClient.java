package info.kgeorgiy.ja.fedorenko.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class AbstractHelloIDPClient implements HelloClient {
    private static final String USAGE = "Usage: HelloUDP Client ip port prefix [threads [perThread]]";

    protected String formRequest(String prefix, int thread, int response) {
        return String.format("%s%d_%d", prefix, thread, response);
    }

    protected boolean checkResponse(String response, String check) {
        return response.chars()
                .mapToObj(c -> Character.isDigit(c) ? String.valueOf(Character.getNumericValue(c)) : String.valueOf((char) c))
                .collect(Collectors.joining()).contains(check);
    }

    public static void mainImpl(String[] args, Class<? extends HelloClient> token) {
        try {
            if (args.length < 3 || args.length > 5) throw new IllegalArgumentException("Incorrect argument length!");
            if (Arrays.stream(args).anyMatch(Objects::isNull))
                throw new IllegalArgumentException("null arguments are not allowed");

            try {
                token.getDeclaredConstructor().newInstance().run(
                        InetAddress.getByName(args[0]).getHostName(), Integer.parseInt(args[1]), args[2],
                        Util.tryGetArgument(args, 3, 16), Util.tryGetArgument(args, 4, 5)
                );
            } catch (ReflectiveOperationException | InstantiationError e) {
                throw new RuntimeException(e);
            }
        } catch (UnknownHostException e) {
            System.err.println("Specified URL is invalid " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Expected number: " + e.getMessage());
            System.err.println(USAGE);
        } catch (IllegalArgumentException e) {
            System.err.println("Wrong arguments were passed: " + e.getMessage());
            System.err.println(USAGE);
        }
    }
}
