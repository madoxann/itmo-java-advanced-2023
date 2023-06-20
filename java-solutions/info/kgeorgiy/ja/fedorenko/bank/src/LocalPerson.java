package info.kgeorgiy.ja.fedorenko.bank.src;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Set;

/** {@link Person} whose methods do not require connection to {@link Bank} instance */
public class LocalPerson extends BasePerson implements Serializable {
    private final Set<Account> accounts;

    public LocalPerson(String name, String surname, String passport, Set<Account> accounts) {
        super(name, surname, passport);
        this.accounts = accounts;
    }

    @Override
    public Set<Account> getAccounts() {
        return accounts;
    }
}
