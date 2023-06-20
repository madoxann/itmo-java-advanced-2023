package info.kgeorgiy.ja.fedorenko.bank.app;

import info.kgeorgiy.ja.fedorenko.bank.src.Account;
import info.kgeorgiy.ja.fedorenko.bank.src.Bank;
import info.kgeorgiy.ja.fedorenko.bank.src.Person;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

/** A Client, able to check persons and accounts, also able create new ones */
public final class Client {
    private static final String USAGE = "Usage: Client name surname passport subID amount";
    private Client() {}

    public static void main(final String... args) throws RemoteException {
        try {
            if (args.length != 5)
                throw new IllegalArgumentException("Expected 5 arguments, got " + args.length);

            final Bank bank = (Bank) LocateRegistry.getRegistry().lookup("//localhost/bank");

            final String name = args[0], surname = args[1], passport = args[2], subID = args[3];
            final int amount = Integer.parseInt(args[4]);

            Person person;
            final String id = Account.getBankID(passport, subID);
            if ((person = bank.lookupPerson(passport, Bank.LookupOptions.REMOTE)) != null){
                checkData(bank, person, name, surname, passport, id, amount);
                return;
            }

            System.out.println("Creating account with given credentials...");
            person = bank.createPerson(name, surname, passport);

            updateAmount(bank.createAccount(person, subID), amount);
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
        } catch (final IllegalArgumentException e) {
            System.out.println("Incorrect data was passed");
            System.out.println(USAGE);
            throw e;
        }
    }

    private static void checkData(Bank bank, final Person person, String name, String surname, String passport, String id, int amount) throws RemoteException {
        if (!person.getName().equals(name))
            throw new IllegalArgumentException("Incorrect name!");

        if (!person.getSurname().equals(surname))
            throw new IllegalArgumentException("Incorrect surname");

        System.out.println("All data is correct");

        Account account = bank.getAccount(id);
        if (account == null) {
            System.out.println("Account not found. Creating...");
            account = bank.createAccount(person, id);
        }

        updateAmount(account, amount);
    }

    private static void updateAmount(Account account, int newAmount) throws RemoteException {
        final int amount = account.getAmount();
        System.out.printf("Account with id %s found, current amount: %d%n", account.getId(), amount);

        if (amount == newAmount || newAmount == 0) {
            System.out.println("New amount is same as old, no update necessary.");
            return;
        }

        System.out.println("Setting new amount: " + newAmount + "...");
        account.setAmount(newAmount);
        System.out.printf("Amount on %s set successfully! Current amount: %d%n", account.getId(), newAmount);
    }
}
