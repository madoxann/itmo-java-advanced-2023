package info.kgeorgiy.ja.fedorenko.bank.src;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface Bank extends Remote {
    /**
     * {@link Enum} containing {@link Person} retrieval options for a {@link #lookupPerson} method. Available options:
     * <ul>
     *    <li> {@link #REMOTE} for a {@link RemotePerson} </li>
     *    <li> {@link #LOCAL} for a {@link LocalPerson} </li>
     * <ul/>
     */
    enum LookupOptions {
        /**
         * Option indicating that a {@link RemotePerson} must be returned
         */
        REMOTE,

        /**
         * Option indicating that a {@link LocalPerson} must be returned
         */
        LOCAL
    }

    /**
     * Creates a new person with specified credentials if it does not already exist
     * @param name name
     * @param surname surname
     * @param passport ID of a passport
     * @return a new person or an existing instance
     */
    Person createPerson(String name, String surname, String passport) throws RemoteException;

    /**
     * Creates a new person instance in database identical to passed
     * @param person credentials
     * @return a new person instance linked to this bank
     */
    Person createPerson(Person person) throws RemoteException;

    /**
     * Creates a new account with specified identifier if it does not already exist. Balance on a new account is 0
     * @param id account id
     * @return created or existing account.
     */
    Account createAccount(String id) throws RemoteException;

    /**
     * Creates a new account held by given person. Balance on a new account is 0
     * @param person {@link Person} to whom account belongs
     * @param subId id of account given to person
     * @return created or existing account.
     */
    Account createAccount(Person person, String subId) throws RemoteException;

    /**
     * Searches database for a {@link Person} with same passport
     * @param passport passport ID
     * @param options a lookup option
     * @return person with matching passport (a {@code null} can be returned to indicate unregistered person)
     */
    Person lookupPerson(String passport, LookupOptions options) throws RemoteException;

    /**
     * Returns account by identifier.
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exist.
     */
    Account getAccount(String id) throws RemoteException;

    /**
     * Returns {@link Set} of accounts held by a given {@link Person}.
     * @param person a holder of accounts
     * @return set of accounts held by this person.
     */
    Set<Account> getAllAccounts(Person person) throws RemoteException;
}
