package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

/** Tests for the generic repository. */
class RepositoryTest {

    private static final MonthlyMembership OLEH =
            new MonthlyMembership("Олег Савчук", "Monthly", 1, 12, 650.00, LocalDate.of(2026, 10, 20));
    private static final AnnualMembership IVAN =
            new AnnualMembership("Іван Федорчук", "Безліміт", 12, 45, 1500.50, LocalDate.of(2026, 1, 15));

    /* Records come back in the order they were added. */
    @Test
    void returnsRecordsInAddOrder() {
        Repository<String> repository = new Repository<>();
        repository.add("Олег");
        repository.add("Анна");
        repository.add("Іван");

        assertEquals(List.of("Олег", "Анна", "Іван"), repository.all());
        assertEquals(3, repository.size());
    }

    /* A new repository is empty, and so are its snapshot and every search. */
    @Test
    void newRepositoryIsEmpty() {
        Repository<Membership> repository = new Repository<>();

        assertEquals(List.of(), repository.all());
        assertEquals(List.of(), repository.find(membership -> true));
        assertEquals(0, repository.size());
    }

    /* null is not a record. */
    @Test
    void rejectsNull() {
        Repository<String> repository = new Repository<>();

        assertThrows(NullPointerException.class, () -> repository.add(null));
        assertEquals(0, repository.size());
    }

    /* The snapshot cannot be changed: the internal list stays private. */
    @Test
    void snapshotIsUnmodifiable() {
        Repository<String> repository = new Repository<>();
        repository.add("Олег");

        List<String> snapshot = repository.all();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.add("Анна"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.remove(0));
        assertEquals(List.of("Олег"), repository.all());
    }

    /* A snapshot is a copy: records added later do not appear in it. */
    @Test
    void oldSnapshotDoesNotChange() {
        Repository<String> repository = new Repository<>();
        repository.add("Олег");
        List<String> before = repository.all();

        repository.add("Анна");

        assertEquals(List.of("Олег"), before);
        assertEquals(List.of("Олег", "Анна"), repository.all());
    }

    /* find keeps only the records that satisfy the condition, in add order. */
    @Test
    void findsByCondition() {
        Repository<Membership> repository = new Repository<>();
        repository.add(OLEH);
        repository.add(IVAN);

        assertEquals(List.of(IVAN), repository.find(membership -> membership.getVisits() > 20));
        assertEquals(List.of(OLEH), repository.find(membership -> membership.getKind() == MembershipKind.MONTHLY));
        assertEquals(List.of(), repository.find(membership -> membership.getVisits() > 100));
        assertThrows(UnsupportedOperationException.class,
                () -> repository.find(membership -> true).clear());
    }

    /* ? super T: a condition written for a supertype of T can be used. */
    @Test
    void findAcceptsConditionOnSupertype() {
        Repository<Membership> repository = new Repository<>();
        repository.add(OLEH);
        repository.add(IVAN);
        Predicate<Object> describedAsMonthly = item -> item.toString().startsWith("Monthly");

        assertEquals(List.of(OLEH), repository.find(describedAsMonthly));
        assertThrows(NullPointerException.class, () -> repository.find(null));
    }

    /* ? extends T: a list of a subtype can be added to a repository of the base type. */
    @Test
    void addAllAcceptsListOfSubtype() {
        Repository<Membership> repository = new Repository<>();
        List<MonthlyMembership> monthly = List.of(OLEH);

        repository.addAll(monthly);
        repository.add(IVAN);

        assertEquals(List.of(OLEH, IVAN), repository.all());
    }

    /* A collection with null is rejected as a whole: no record of it is added. */
    @Test
    void addAllWithNullAddsNothing() {
        Repository<String> repository = new Repository<>();
        List<String> withNull = new ArrayList<>(Arrays.asList("Олег", null));

        assertThrows(NullPointerException.class, () -> repository.addAll(withNull));
        assertTrue(repository.all().isEmpty(), repository.all().toString());
    }
}
