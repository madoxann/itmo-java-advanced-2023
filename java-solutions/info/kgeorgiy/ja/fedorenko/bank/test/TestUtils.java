package info.kgeorgiy.ja.fedorenko.bank.test;

import info.kgeorgiy.ja.fedorenko.bank.app.Server;
import info.kgeorgiy.ja.fedorenko.bank.src.*;
import org.junit.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

/*package-private*/ class TestUtils {
    public static final int SOUT_FINISH_TIME = 500;
    protected static final Random RANDOM = new Random(2350238478730489714L);

    public static final List<String> NAMES = List.of("Айнур", "Александр", "Александра", "Алексей", "Алиссия", "Алмасгали", "Анастасия", "Андрей", "Анна", "Антон", "Арсений", "Арсентий", "Артем", "Артём", "Артемий", "Артур", "Асаддулла", "Вадим", "Василиса", "Вероника", "Виктор", "Виктория", "Виолетта", "Виталий", "Владимир", "Владислав", "Всеволод", "Вячеслав", "Георгий", "Герман", "Глеб", "Григорий", "Дамир", "Даниил", "Данил", "Данила", "Даниэль", "Денис", "Дмитрий", "Евгений", "Евгения", "Егор", "Захар", "Иван", "Игорь", "Ильдар", "Илья", "Камиль", "Кирилл", "Константин", "Кристина", "Ксения", "Лев", "Максим", "Максимилиан", "Маргарита", "Марина", "Мария", "Марк", "Мирзомансурхон", "Михаил", "Надежда", "Наталья", "Никита", "Николай", "Олег", "Омайма", "Павел", "Петр", "Пётр", "Полина", "Роман", "Руслан", "Рустам", "Савелий", "Сафина", "Семён", "Сергей", "Софья", "Станислав", "Степан", "Тарас", "Тестович", "Тимофей", "Тимур", "Федор", "Фёдор", "Эдгар", "Эдуард", "Эльвира", "Эрвин", "Эрнест", "Юлия", "Юрий");
    public static final List<String> SURNAMES = List.of("Абрамов", "Алентьев", "Андосов", "Бактурин", "Бандурин", "Баринов", "Бекиш", "Белозоров", "Белоус", "Белоусов", "Белоцерковченко", "Белуган", "Беркутов", "Блохин", "Богатырев", "Бодрикова", "Бондарев", "Борисов", "Борькин", "Бурунсузян", "Бюргановская", "Ванчугов", "Варламов", "Вереня", "Викулаев", "Власов", "Волков", "Володько", "Выборнов", "Гаврилов", "Гайнанов", "Гельметдинов", "Германова", "Говорина", "Голов", "Григоренко", "Гринь", "Грунский", "Губин", "Гулямова", "Дамасевич", "Дзюбенко", "Дмитриев", "Дмитриева", "Дроботов", "Дудко", "Дягин", "Егоров", "Елисеев", "Жерков", "Живаев", "Жимоедов", "Жуковский", "Журавлев", "Занин", "Засухин", "Захаров", "Зубков", "Иванов", "Ивченков", "Илык", "Ильченко", "Исаева", "Кадочникова", "Кадыров", "Каймакова", "Капелюшок", "Капленков", "Карпов", "Касатов", "Качмар", "Ким", "Кинзин", "Кислов", "Ковалев", "Коган", "Кольченко", "Кондрюков", "Корнеева", "Корнильев", "Коробко", "Королев", "Коротков", "Котов", "Кузнецов", "Куксо", "Купцов", "Куркин", "Кутасин", "Ларский", "Латанов", "Латыпов", "Левицкий", "Лежень", "Леонтьев", "Лихачев", "Мавлютов", "Макаревич", "Мартынов", "Маслаков", "Медведев", "Мельникова", "Меркулов", "Минько", "Миронов", "Михайлов", "Морозов", "Муфтиев", "Мухтаров", "Мясников", "Надеждин", "Назаров", "Новичков", "Нотфуллин", "Ночевкина", "Окорочкова", "Олангаев", "Оратовский", "Осипов", "Пальченков", "Панов", "Пантелеев", "Петров", "Петрова", "Плетнев", "Плешанов", "Подкопаев", "Пономаренко", "Попов", "Прасолов", "Преснов", "Пульникова", "Пушкарев", "Пьянков", "Рассадников", "Рахмани", "Рожко", "Рожков", "Рыбин", "Рынк", "Рябов", "Рябчун", "Салахов", "Салятов", "Самоделов", "Сандовин", "Санников", "Свириденко", "Селезнев", "Семенидов", "Сентемов", "Сенькин", "Симаков", "Синицин", "Смирнов", "Сотников", "Стафеев", "Степанов", "Султанов", "Тайбинов", "Тарчевский", "Тест", "Трещёв", "Трофимов", "Трощий", "Тяпкин", "Упчер", "Файзиева", "Федоренко", "Филиппович", "Фролов", "Хадыров", "Хайруллин", "Хасанов", "Хренов", "Хритоненко", "Хусаинов", "Целиков", "Чанышев", "Чеканова", "Черкашин", "Черников", "Чернышев", "Чечеватов", "Чистяков", "Чулков", "Чуракова", "Шаньшин", "Швалова", "Швецов", "Шевчук", "Шеметов", "Шилкин", "Шириков", "Шпилева", "Шпильков", "Шпрайдун", "Щербаков", "Щербина", "Этталби", "Юзеев", "Юренков", "Яганова", "Ярунина");
    public static final List<String> PASSPORTS = List.of("1Wqhd46cz8BhEV", "IREAMjDRngEAHK", "9wVIx9wHZQZbY8", "yKuWYz29eMP57A", "T4OARcUWFP5BOw", "70b5OfuLXueb1K", "zBtZfxXiCg9pbX", "TV7ytrvYaLLfWv", "sYxGQpz7WWObbt", "wZekVhx6PCTaRZ", "3GI5D51yb1fzOu", "qePvZDx9VMwBbA", "uFG3dzEKGOkEpA", "vPwZVFnZkc8cuh", "05bGqKNcxfWRxA", "PMGMa32VX0uuWn", "XPRAUaoigdfPaF", "vD3l1Dgi72Dft6", "PQjbA06CXiio2x", "j94SSJ4bepSx5E");
    public static final List<Set<Account>> ACCOUNTS = RANDOM.ints(300, 5, 9)
            .mapToObj(
                    sz -> RANDOM.ints(sz, 0, 10000000)
                            .distinct()
                            .mapToObj(acc -> (Account) new RemoteAccount(String.valueOf(acc)))
                            .collect(Collectors.toCollection(ConcurrentHashMap::newKeySet))
            ).collect(Collectors.toCollection(CopyOnWriteArrayList::new));

    public static final List<Person> PERSONS = RANDOM.ints(300, 0, 10000)
            .mapToObj(id -> {
                String passport = random(PASSPORTS);
                return new LocalPerson(random(NAMES), random(SURNAMES), passport, impersonate(random(ACCOUNTS), passport));
            }).collect(Collectors.toUnmodifiableList());

    private static final Iterator<Set<Account>> init = ACCOUNTS.stream().iterator();
    public static final List<Person> DISTINCT_PERSONS = PASSPORTS.stream()
            .map(p -> new LocalPerson(random(NAMES), random(SURNAMES), p, impersonate(init.next(), p)))
            .collect(Collectors.toUnmodifiableList());

    private TestUtils() {}

    public static Bank newServer() { return Server.startServer(); }

    public static <T> T random(final List<T> values) { return values.get(RANDOM.nextInt(values.size())); }

    public static Map<String, Integer> accountCollToMap(Collection<Account> coll) {
        return coll.stream().collect(
                Collectors.toMap(
                        (RemoteFunction<Account, String>) (Account::getId),
                        (RemoteFunction<Account, Integer>) (Account::getAmount)
                ));
    }

    public static void assertEqualsAccountColl(Collection<Account> c1, Collection<Account> c2) {
        Assert.assertEquals(
                accountCollToMap(c1),
                accountCollToMap(c2)
        );
    }

    @SuppressWarnings("unchecked")
    public static void areSame(LocalPerson p1, RemotePerson p2) {
        try {
            for (Method m : Person.class.getMethods()) {
                if (m.equals(Person.class.getMethod("getAccounts"))) {
                    assertEqualsAccountColl(
                            impersonate((Set<Account>) m.invoke(p1), p1.getPassport()),
                            (Set<Account>) m.invoke(p2)
                    );
                } else
                    Assert.assertEquals(String.format("Objects %s, %s are not equal by call of %s()", p1, p2, m.getName()), m.invoke(p1), m.invoke(p2));
            }
        } catch (IllegalAccessException | NoSuchMethodException ignored) {
        } catch (InvocationTargetException e) {
            throw new AssertionError("A person method threw exception" + e.getMessage());
        }
    }

    private final static TypeReference<Set<Account>> setAccountToken = new TypeReference<Set<Account>>() {};
    @SuppressWarnings("unchecked")
    public static <T> void areSame(T p1, T p2, Class<T> token) {
        try {
            for (Method m : token.getMethods()) {
                if (m.getGenericReturnType().equals(setAccountToken.getType())) {
                    assertEqualsAccountColl((Set<Account>) m.invoke(p1), (Set<Account>) m.invoke(p2));
                }
                if (m.getParameters().length == 0) {
                    Assert.assertEquals(m.invoke(p1), m.invoke(p2));
                }
            }
        } catch (IllegalAccessException ignored) {
        } catch (InvocationTargetException e) {
            throw new AssertionError("A" + token.getName() + "method threw exception" + e.getMessage());
        }
    }

    private static Set<Account> impersonate(Set<Account> accounts, String passport) {
        return accounts.stream()
                .map(account -> {
                    try {
                        return new RemoteAccount(Account.getBankID(passport, account.getId()));
                    } catch (RemoteException ignored) {
                    }
                    throw new AssertionError("Unable to get ID of account");
                })
                .collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
    }

    public abstract static class TypeReference<T> {
        private final Type type;

        public TypeReference() {
            Type superclass = getClass().getGenericSuperclass();
            type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
        }

        public Type getType() {
            return type;
        }
    }

    @FunctionalInterface
    public interface RemoteFunction<T, R> extends Function<T, R> {
        @Override
        default R apply(T a) {
            try {
                return applyThrowing(a);
            } catch (RemoteException e) {
                throw new AssertionError("A remote exception was thrown: " + e.getMessage());
            }
        }

        R applyThrowing(T a) throws RemoteException;
    }
}
