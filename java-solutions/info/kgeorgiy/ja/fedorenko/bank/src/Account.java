package info.kgeorgiy.ja.fedorenko.bank.src;

import java.rmi.*;

public interface Account extends Remote {
    /**
     * Returns account identifier
     * @return unique ID
     * @throws RemoteException
     */
    String getId() throws RemoteException;

    /**
     * Returns amount of money in the account.
     * @return current balance
     * @throws RemoteException
     */
    int getAmount() throws RemoteException;

    /**
     * Sets amount of money in the account.
     * @param amount a sum to be set
     * @throws RemoteException
     */
    void setAmount(int amount) throws RemoteException;

    /**
     * Gets account ID formed as "passport:accountID"
     * @param passport passport of account holder
     * @param id account ID
     * @return a ID formed by "passport:accountID"
     * @throws RemoteException
     */
    static String getBankID(String passport, String id) throws RemoteException { return String.format("%s:%s", passport, id); }
}
