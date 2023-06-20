package info.kgeorgiy.ja.fedorenko.bank.test;

import info.kgeorgiy.ja.fedorenko.bank.src.*;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.*;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.*;
import java.util.concurrent.*;

import static info.kgeorgiy.ja.fedorenko.bank.test.TestUtils.*;

@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RemoteBankTests {
    protected final Random RANDOM = new Random(4839638478730764374L);

    @BeforeClass
    public static void make_registry() throws RemoteException {
        LocateRegistry.createRegistry(1099);
    }

    protected static Bank bank;
    @Rule
    public ExternalResource bankSetup = new ExternalResource() {
        @Override
        protected void before() {
            bank = TestUtils.newServer();
        }
    };

    protected String testMethodName;
    @Rule
    public final TestRule watcher = new TestWatcher() {
        private long startTime;

        @Override
        protected void starting(final Description description) {
            startTime = System.currentTimeMillis();
            testMethodName = description.getMethodName();
            System.err.println("=== Running " + testMethodName);
        }

        @Override
        protected void finished(final Description description) {
            try {
                long finish = System.currentTimeMillis() - startTime;

                Thread.sleep(SOUT_FINISH_TIME);
                System.err.printf("    %s finished in %dms%n", testMethodName, finish);
            } catch (InterruptedException e) {
                throw new AssertionError("Test thread interrupted", e);
            }
        }
    };

    @Test
    public void test01_create() {
        DISTINCT_PERSONS.forEach((ThrowingConsumer<Person>) p ->
                areSame((LocalPerson) p, (RemotePerson) bank.createPerson(p))
        );
    }

    @Test
    public void test02_absent() throws RemoteException {
        final Person person = new LocalPerson(random(NAMES), random(SURNAMES), random(PASSPORTS), Set.of());

        Assert.assertNull(bank.lookupPerson(person.getPassport(), Bank.LookupOptions.REMOTE));
        bank.createPerson(person);
        Assert.assertEquals(person, bank.lookupPerson(person.getPassport(), Bank.LookupOptions.LOCAL));
    }

    @Test
    public void test03_set() {
        PERSONS.forEach((ThrowingConsumer<Person>) p -> {
            p.getAccounts().forEach(a -> Assertions.assertThatThrownBy(() -> a.setAmount(-1)).isInstanceOf(TransactionDeniedException.class));

            Assertions.assertThatThrownBy(() -> bank.createAccount(p, "test").setAmount(-1)).isInstanceOf(TransactionDeniedException.class);
        });
    }

    @Test
    public void test04_try_modifying() throws RemoteException {
        final RemotePerson person = (RemotePerson) bank.createPerson(random(PERSONS));
        final Account account = random(ACCOUNTS).iterator().next();

        Assertions.assertThatThrownBy(() -> person.getAccounts().add(account)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void test05_local_modification() {
        PERSONS.forEach((ThrowingConsumer<Person>) p -> {
            final int sz = p.getAccounts().size(), balance = randomInt();

            final Account account = bank.createAccount(p, "1");
            account.setAmount(balance);

            Assert.assertEquals(Account.getBankID(p.getPassport(), "1"), account.getId());
            Assert.assertEquals(sz + 1, p.getAccounts().size());
            Assert.assertEquals(balance, account.getAmount());

            Assert.assertNull(bank.getAccount(account.getId()));
        });
    }

    @Test
    public void test06_remote_modification() {
        PERSONS.forEach((ThrowingConsumer<Person>) p -> {
            Person returnedPerson = bank.createPerson(p);
            Set<Account> returned = new HashSet<>(returnedPerson.getAccounts());

            RANDOM.ints(20, 90, 140)
                    .distinct()
                    .forEach(id -> {
                        try {
                            returned.add(bank.createAccount(returnedPerson, String.valueOf(id)));
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                    });

            final Set<Account> accounts = returnedPerson.getAccounts();
            returned.forEach((ThrowingConsumer<Account>) a -> {
                final int balance = randomInt();
                a.setAmount(balance);
            });

            assertEqualsAccountColl(accounts, returned);
            areSame(returnedPerson, bank.createPerson(returnedPerson), Person.class);
        });
    }

    @Test
    public void test07_local_add_coherent() {
        DISTINCT_PERSONS.forEach((ThrowingConsumer<Person>) p -> {
            Person person = bank.createPerson(p);
            final int created = RANDOM.nextInt(1, 20), sz = person.getAccounts().size();

            for (int i = 0; i < created; i++) bank.createAccount(person, String.valueOf(-i));

            LocalPerson local = (LocalPerson) bank.lookupPerson(p.getPassport(), Bank.LookupOptions.LOCAL);

            Assert.assertEquals(created, local.getAccounts().size() - sz);
        });
    }

    @Test
    public void test08_local_account_coherent() {
        DISTINCT_PERSONS.forEach((ThrowingConsumer<Person>) p -> {
            final int const1 = randomInt(), const2 = randomInt();

            final Person person = bank.createPerson(p);
            final Map<String, Integer> amountBefore = accountCollToMap(person.getAccounts());
            amountBefore.keySet().forEach((ThrowingConsumer<String>) id -> bank.getAccount(id).setAmount(amountBefore.get(id) + const1));

            final Set<Account> local = bank.lookupPerson(p.getPassport(), Bank.LookupOptions.LOCAL).getAccounts();
            local.forEach((ThrowingConsumer<Account>) a -> a.setAmount(amountBefore.get(a.getId()) + const2));

            final Map<String, Integer> amountNow = accountCollToMap(local);
            amountBefore.forEach((id, m) -> {
                try {
                    Assert.assertEquals(const1, bank.getAccount(id).getAmount() - m);
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }

                Assert.assertEquals(const2, amountNow.get(id) - m);
            });
        });
    }

    @Test
    public void test09_concurrent() throws RemoteException {
        final int sz = 50;
        ExecutorService es = Executors.newFixedThreadPool(sz);

        final Person p = random(DISTINCT_PERSONS), person = bank.createPerson(p);
        final Account account = person.getAccounts().iterator().next();
        final int oldAmount = account.getAmount();

        final List<Future<?>> promises = new ArrayList<>();
        final Runnable test = () -> {
            try {
                account.setAmount(account.getAmount() + 100);
            } catch (RemoteException e) {
                throw new AssertionError("Remote exception on test thread " + e.getMessage());
            }
        };

        for (int i = 0; i < sz; i++) promises.add(es.submit(test));
        promises.forEach((ThrowingConsumer<Future<?>>) Future::get);

        Assert.assertEquals(sz * 100L, account.getAmount() - oldAmount);
        es.shutdown(); // promises are received
    }

    protected int randomInt() {
        return RANDOM.nextInt(0, 10000000);
    }
}
