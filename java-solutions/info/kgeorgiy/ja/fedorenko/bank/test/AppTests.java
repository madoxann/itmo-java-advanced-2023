package info.kgeorgiy.ja.fedorenko.bank.test;

import info.kgeorgiy.ja.fedorenko.bank.app.Client;
import info.kgeorgiy.ja.fedorenko.bank.src.Account;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertionsProvider.*;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;

import static info.kgeorgiy.ja.fedorenko.bank.test.TestUtils.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AppTests extends RemoteBankTests {
    private static ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    private final PrintStream originalOut = System.out;
    public static void setUpStreams() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void test10_correct_inputs() throws RemoteException {
        Account account = random(ACCOUNTS).iterator().next();
        final String name = random(NAMES), surname = random(SURNAMES), passport = random(PASSPORTS), id = Account.getBankID(passport, account.getId());

        gatherInput(() -> Client.main(name, surname, passport, account.getId(), "100"));
        Assert.assertEquals(
                String.format("""
                                Creating account with given credentials...
                                Account with id %s found, current amount: 0
                                Setting new amount: 100...
                                Amount on %s set successfully! Current amount: 100
                                """,
                id, id), outContent.toString(StandardCharsets.UTF_8)
        );

        gatherInput(() -> Client.main(name, surname, passport, account.getId(), "100"));
        Assert.assertEquals(
                String.format("""
                                All data is correct
                                Account with id %s found, current amount: 100
                                New amount is same as old, no update necessary.
                                """,
                        id), outContent.toString(StandardCharsets.UTF_8)
        );

        gatherInput(() -> Client.main(name, surname, passport, account.getId(), "200"));
        Assert.assertEquals(
                String.format("""
                                All data is correct
                                Account with id %s found, current amount: 100
                                Setting new amount: 200...
                                Amount on %s set successfully! Current amount: 200
                                """,
                        id, id), outContent.toString(StandardCharsets.UTF_8)
        );
    }

    @Test
    public void test11_incorrect_inputs() throws RemoteException {
        Account account = random(ACCOUNTS).iterator().next();
        final String name = random(NAMES), surname = random(SURNAMES), passport = random(PASSPORTS);

        setUpStreams();
        Assertions.assertThatThrownBy(() -> Client.main(name, surname, passport, account.getId(), "100", "1000")).isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> Client.main(name, surname, passport, account.getId())).isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> Client.main(name, surname, passport, account.getId(), "kek")).isInstanceOf(IllegalArgumentException.class);

        Client.main(name, surname, passport, account.getId(), "100");
        Assertions.assertThatThrownBy(() -> Client.main(name, "test", passport, account.getId(), "100")).isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> Client.main("test", surname, passport, account.getId(), "100")).isInstanceOf(IllegalArgumentException.class);
        restoreStreams();
    }

    private void gatherInput(ThrowingRunnable r) {
        try {
            setUpStreams();
            r.run();
            restoreStreams();
        } catch (Exception e) {
            if (e instanceof RuntimeException that) throw that;

            throw new AssertionError("Checked exception thrown in main");
        }
    }
}
