package com.tonic.queries.abstractions;

import com.tonic.Static;
import net.runelite.api.Client;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractQuery<T, Q extends AbstractQuery<T, Q>> {
    protected final Supplier<List<T>> dataSource;
    protected final Client client;
    private final Random random = new Random();
    private final List<Predicate<T>> filters = new ArrayList<>();
    private final List<Comparator<T>> sorters = new ArrayList<>();
    private boolean negate = false;

    public AbstractQuery(List<T> cache) {
        this.dataSource = () -> new ArrayList<>(cache);
        this.client = Static.getClient();
    }

    @SuppressWarnings("unchecked")
    protected final Q self() {
        return (Q) this;
    }

    /**
     * Lazily filter by predicate (removes matching items)
     */
    public Q removeIf(Predicate<T> predicate) {
        filters.add(predicate.negate());
        return self();
    }

    /**
     * Lazily filter by predicate (keeps matching items)
     */
    public Q keepIf(Predicate<T> predicate) {
        filters.add(predicate);
        return self();
    }

    /**
     * Lazily add sorting
     */
    public Q sort(Comparator<T> comparator) {
        sorters.add(comparator);
        return self();
    }

    /**
     * Execute the query and get results
     */
    private List<T> execute() {
        return Static.invoke(() -> {
            Stream<T> stream = dataSource.get().stream();

            // Apply all filters
            for (Predicate<T> filter : filters) {
                stream = stream.filter(filter);
            }

            // Apply sorting (chain comparators if multiple)
            if (!sorters.isEmpty()) {
                Comparator<T> combined = sorters.stream()
                        .reduce(Comparator::thenComparing)
                        .orElse(null);
                stream = stream.sorted(combined);
            }

            return stream.collect(Collectors.toList());
        });
    }

    /**
     * Get the first element from the filtered/sorted list
     */
    public T first() {
        List<T> results = execute();
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Get the last element from the filtered/sorted list
     */
    public T last() {
        List<T> results = execute();
        return results.isEmpty() ? null : results.get(results.size() - 1);
    }

    /**
     * Perform action on the first element, or else run elseAction if no results
     * @param action action to perform on the first element
     * @param elseAction action to perform if no results
     */
    public void firstOrElse(Consumer<T> action, Runnable elseAction) {
        List<T> results = execute();
        if (results.isEmpty()) {
            elseAction.run();
        } else {
            action.accept(results.get(0));
        }
    }

    /**
     * Perform action on the last element, or else run elseAction if no results
     * @param action action to perform on the last element
     * @param elseAction action to perform if no results
     */
    public void lastOrElse(Consumer<T> action, Runnable elseAction) {
        List<T> results = execute();
        if (results.isEmpty()) {
            elseAction.run();
        } else {
            action.accept(results.get(results.size() - 1));
        }
    }

    /**
     * Get a random element from the filtered/sorted list
     */
    public T random() {
        List<T> results = execute();
        return results.isEmpty() ? null : results.get(random.nextInt(results.size()));
    }

    /**
     * Get the filtered/sorted list
     */
    public List<T> collect() {
        return execute();
    }

    /**
     * Get count of filtered results
     */
    public int count() {
        return Static.invoke(() -> {
            Stream<T> stream = dataSource.get().stream();
            for (Predicate<T> filter : filters) {
                stream = stream.filter(filter);
            }
            return (int) stream.count();
        });
    }

    /**
     * Check if filtered results are empty
     */
    public boolean isEmpty() {
        return Static.invoke(() -> {
            Stream<T> stream = dataSource.get().stream();
            for (Predicate<T> filter : filters) {
                stream = stream.filter(filter);
            }
            return stream.findAny().isEmpty();
        });
    }

    /**
     * Generic aggregation method for custom terminal operations
     * Executes filters and allows custom stream processing
     */
    public <R> R aggregate(Function<Stream<T>, R> aggregator) {
        return Static.invoke(() -> {
            Stream<T> stream = dataSource.get().stream();

            for (Predicate<T> filter : filters) {
                stream = stream.filter(filter);
            }

            return aggregator.apply(stream);
        });
    }

    /**
     * Execute filters and process with custom collector
     */
    public <R> R collect(Collector<T, ?, R> collector) {
        return Static.invoke(() -> {
            Stream<T> stream = dataSource.get().stream();

            for (Predicate<T> filter : filters) {
                stream = stream.filter(filter);
            }

            if (!sorters.isEmpty()) {
                Comparator<T> combined = sorters.stream()
                        .reduce(Comparator::thenComparing)
                        .orElse(null);
                if (combined != null) {
                    stream = stream.sorted(combined);
                }
            }

            return stream.collect(collector);
        });
    }
}