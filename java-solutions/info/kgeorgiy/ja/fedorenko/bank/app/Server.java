package info.kgeorgiy.ja.fedorenko.bank.app;

import info.kgeorgiy.ja.fedorenko.bank.src.Bank;
import info.kgeorgiy.ja.fedorenko.bank.src.RemoteBank;

import java.rmi.*;
import java.rmi.registry.LocateRegistry;

/** A Server, exporting a new instance of {@link Bank} to {@link java.rmi.registry.Registry} opened on a default port */
public final class Server {
    public static final int DEFAULT_PORT = 8888;

    /**
     * Exports a new {@link Bank} instance on a given port
     * @param port where bank resides
     * @return a new {@link Bank} instance
     */
    public static Bank startServer(final int port) {
        final Bank bank;
        try {
            bank = new RemoteBank(port);
            LocateRegistry.getRegistry().rebind("//localhost/bank", bank);
            System.out.println("Server started");
            return bank;
        } catch (final RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    /**
     * Starts server on a {@link #DEFAULT_PORT}
     * @return a  new {@link Bank} instance
     */
    public static Bank startServer() { return startServer(DEFAULT_PORT); }

    public static void main(final String... args) {
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        startServer(port);
    }
}
