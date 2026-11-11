package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Type-safe in-memory storage of records.
 *
 * <p>The type parameter is fixed when the repository is created: a {@code Repository<Membership>}
 * accepts only memberships and returns only memberships, so callers never cast. The repository
 * never gives out its internal list: {@link #all()} and {@link #find(Predicate)} return
 * unmodifiable copies.</p>
 *
 * @param <T> type of the stored records
 */
public final class Repository<T> {

    private final List<T> items = new ArrayList<>();

    /** Creates an empty repository. */
    public Repository() {
        // Nothing to initialise: the list is created with the field.
    }

    /**
     * Adds one record to the end of the repository.
     *
     * @param item record to add
     * @throws NullPointerException if {@code item} is null
     */
    public void add(T item) {
        items.add(Objects.requireNonNull(item, "item must not be null"));
    }

    /**
     * Adds all records in their order.
     *
     * <p>{@code ? extends T} lets a {@code Repository<Membership>} take a
     * {@code List<MonthlyMembership>}: the collection only gives records out.</p>
     *
     * @param newItems records to add
     * @throws NullPointerException if the collection or one of its records is null;
     *                              in that case nothing is added
     */
    public void addAll(Collection<? extends T> newItems) {
        // List.copyOf rejects null records before the first one is added.
        items.addAll(List.copyOf(newItems));
    }

    /**
     * Returns a snapshot of all records in the order they were added.
     *
     * @return unmodifiable copy; later changes of the repository do not affect it
     */
    public List<T> all() {
        return List.copyOf(items);
    }

    /**
     * Returns the records that satisfy a condition, in the order they were added.
     *
     * <p>{@code ? super T} accepts a condition on {@code T} or on any supertype of {@code T},
     * for example a {@code Predicate<Object>} for a {@code Repository<String>}.</p>
     *
     * @param condition condition to test every record with
     * @return unmodifiable list of matching records, empty if none match
     * @throws NullPointerException if {@code condition} is null
     */
    public List<T> find(Predicate<? super T> condition) {
        Objects.requireNonNull(condition, "condition must not be null");
        return items.stream()
                .filter(condition)
                .toList();
    }

    /**
     * Returns the number of stored records.
     *
     * @return number of records, not negative
     */
    public int size() {
        return items.size();
    }
}
