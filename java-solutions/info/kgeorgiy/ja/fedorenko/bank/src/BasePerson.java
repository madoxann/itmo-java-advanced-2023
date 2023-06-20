package info.kgeorgiy.ja.fedorenko.bank.src;

import java.util.Objects;

public abstract class BasePerson implements Person {
    private final String name;
    private final String surname;
    private final String passport;

    public BasePerson(String name, String surname, String passport) {
        this.name = name;
        this.passport = passport;
        this.surname = surname;
    }

    public synchronized String getName() { return name; }

    public synchronized String getSurname() { return surname; }

    public synchronized String getPassport() { return passport; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        if (o instanceof BasePerson that && getClass() == o.getClass()) {
            return Objects.equals(getName(), that.getName()) && Objects.equals(getSurname(), that.getSurname()) && Objects.equals(getPassport(), that.getPassport());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, surname, passport);
    }
}
