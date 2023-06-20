package info.kgeorgiy.ja.fedorenko.bank.src;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class RemoteBank extends UnicastRemoteObject implements Bank {
    private final int port;

    /**
     * {@link Account} ID's mapped to their {@link Account} instances
     */
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    /**
     * Passport ID's mapped to {@link Person} instances
     */
    private final ConcurrentMap<String, Person> clients = new ConcurrentHashMap<>();
    /**
     * {@link Person} mapped to {@link Set} to their {@link Account} ID
     */
    private final ConcurrentMap<Person, Set<String>> individualAccounts = new ConcurrentHashMap<>();

    public RemoteBank(final int port) throws RemoteException {
        super(port);
        this.port = port;
    }

    @Override
    public synchronized Person createPerson(String name, String surname, String passport) throws RemoteException {
        final RemotePerson person = new RemotePerson(this, name, surname, passport);

        if (clients.putIfAbsent(passport, person) == null) {
            individualAccounts.putIfAbsent(person, ConcurrentHashMap.newKeySet());
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            return lookupPerson(passport, LookupOptions.REMOTE);
        }
    }

    @Override
    public synchronized Person createPerson(Person person) throws RemoteException {
        Person newPerson = createPerson(person.getName(), person.getSurname(), person.getPassport());

        for (Account account : person.getAccounts()) {
            Account tmp = createAccount(newPerson, account.getId());
            int amount = account.getAmount();

            if (amount != 0) tmp.setAmount(account.getAmount());
        }

        return newPerson;
    }

    @Override
    public synchronized Account createAccount(final String id) throws RemoteException {
        final Account account = new RemoteAccount(id);
        if (accounts.putIfAbsent(id, account) == null) {
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return getAccount(id);
        }
    }

    @Override
    public synchronized Account createAccount(Person person, final String subId) throws RemoteException {
        final String id = Account.getBankID(person.getPassport(), subId);
        if (person instanceof LocalPerson that) {
            return new RemoteAccount(id) {{
                that.getAccounts().add(this);
            }};
        }

        Account account = accounts.get(id);
        if (account != null) return account;

        individualAccounts.computeIfAbsent(person, key -> ConcurrentHashMap.newKeySet()).add(id);
        return createAccount(id);
    }

    @Override
    public synchronized Person lookupPerson(String passport, LookupOptions options) throws RemoteException {
        final Person person = clients.get(passport);

        return switch (options) {
            case REMOTE -> person;
            case LOCAL -> person == null ? null : new LocalPerson(
                    person.getName(),
                    person.getSurname(),
                    passport,
                    getAllAccounts(person)
                            .stream()
                            .map(account -> {
                                try {
                                    return copyAccount(account);
                                } catch (RemoteException e) {
                                    throw new RuntimeException(e);
                                }
                            }).collect(Collectors.toSet())
            );
        };
    }

    @Override
    public Account getAccount(final String id) {
        return accounts.get(id);
    }

    @Override
    public Set<Account> getAllAccounts(Person person) throws RemoteException {
        if (person instanceof LocalPerson) return ((LocalPerson) person).getAccounts();

        final Set<String> accounts = individualAccounts.get(person);
        return accounts == null ? null : accounts.stream().map(this::getAccount).collect(Collectors.toUnmodifiableSet());
    }

    private Account copyAccount(Account account) throws RemoteException {
        Account newAccount = new RemoteAccount(account.getId());
        newAccount.setAmount(account.getAmount());

        return newAccount;
    }
}
