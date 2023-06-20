package info.kgeorgiy.ja.fedorenko.bank.src;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface Person extends Remote {
    /**
     * Returns a person name
     * @return name
     * @throws RemoteException
     */
    String getName() throws RemoteException;

    /**
     * Returns a person surname
     * @return surname
     * @throws RemoteException
     */
    String getSurname() throws RemoteException;

    /**
     * Returns a person passport
     * @return passport
     * @throws RemoteException
     */
    String getPassport() throws RemoteException;

    /**
     * Returns a {@link java.util.Set} of {@link Account} this person holds
     * @return accounts belonging to this person
     * @throws RemoteException
     */
    Set<Account> getAccounts() throws RemoteException;
}
