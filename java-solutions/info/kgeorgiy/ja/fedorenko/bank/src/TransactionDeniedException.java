package info.kgeorgiy.ja.fedorenko.bank.src;

/** Thrown when {@link Account} balance is being set below zero */
public class TransactionDeniedException extends RuntimeException {
    private TransactionDeniedException() {}
    public TransactionDeniedException(String account, int balance) {
        super(String.format("A transaction on account №%s was denied: a negative balance is being set. Current balance: %d", account, balance));
    }
}
