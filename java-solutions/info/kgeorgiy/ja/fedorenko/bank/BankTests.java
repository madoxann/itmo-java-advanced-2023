package info.kgeorgiy.ja.fedorenko.bank;

import info.kgeorgiy.ja.fedorenko.bank.test.AppTests;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

// скрипты для запуска c junit и без в /scripts
public class BankTests {
    public static void main(String[] args) {
        final long start = System.currentTimeMillis();
        Result result = new JUnitCore().run(AppTests.class);

        if (result.wasSuccessful()) {
            System.out.println("============================");
            final long time = System.currentTimeMillis() - start;
            System.out.printf("OK %s in %dms %n", AppTests.class.getName(), time);
            System.exit(0);
        }

        for (final Failure failure : result.getFailures()) {
            System.err.println("Test " + failure.getDescription().getMethodName() + " failed: " + failure.getMessage());
            if (failure.getException() != null) {
                failure.getException().printStackTrace();
            }
        }
        System.exit(1);
    }
}
