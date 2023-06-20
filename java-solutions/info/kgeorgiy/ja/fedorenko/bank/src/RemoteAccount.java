package info.kgeorgiy.ja.fedorenko.bank.src;

public class RemoteAccount implements Account {
    private final String id;
    private int amount;
    // :NOTE: просто int плохо, не учитываются копейки
    // согласен... но в домашнем вроде нужно было "добавить возможность работы с физическиме лицами", а не переписать логику банка...
    // этот RemoteAccount, наследуется от Account, который я никак не изменял, и который не требовали изменять в модификации...
    // *...ну, просто int на BigDecimal придется менять ВЕЗДЕ: в банке, в клиент-сервере, в тестах... Это не очень интеллектуально :(*

    public RemoteAccount(final String id) {
        this.id = id;
        amount = 0;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        return amount;
    }

    @Override
    public synchronized void setAmount(final int amount) {
        if (amount < 0) throw new TransactionDeniedException(id, amount);

        this.amount = amount;
    }
}
