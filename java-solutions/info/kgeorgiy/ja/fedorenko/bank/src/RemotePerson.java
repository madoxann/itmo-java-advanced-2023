package info.kgeorgiy.ja.fedorenko.bank.src;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

/** {@link Person} whose instance accessed wia RMI */
public class RemotePerson extends BasePerson implements Remote {
    private final Bank bank;

    public RemotePerson(Bank bank, String name, String surname, String passport) throws RemoteException {
        super(name, surname, passport);

        this.bank = bank;
    }

    @Override
    public Set<Account> getAccounts() throws RemoteException { return bank.getAllAccounts(this); }
}
