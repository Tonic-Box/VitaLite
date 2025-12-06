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
    private int limitValue = -1;
    private int skipValue = 0;
    private boolean distinctValue = false;
    private final List<Predicate<T>> filters = new ArrayList<>();
    private final List<Comparator<T>> sorters = new ArrayList<>();
    private final List<Consumer<T>> peekActions = new ArrayList<>();
    private Function<List<T>, List<T>> postProcessor = null;

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
     * @return the query instance
     */
    public Q removeIf(Predicate<T> predicate) {
        filters.add(predicate.negate());
        return self();
    }

    /**
     * Lazily filter by predicate (keeps matching items)
     * @return the query instance
     */
    public Q keepIf(Predicate<T> predicate) {
        filters.add(predicate);
        return self();
    }

    /**
     * Lazily add sorting
     * @return the query instance
     */
    public Q sort(Comparator<T> comparator) {
        sorters.add(comparator);
        return self();
    }

    /**
     * Limit the number of results
     * @return the query instance
     */
    public Q limit(int maxSize) {
        this.limitValue = maxSize;
        return self();
    }

    /**
     * Skip a number of results
     * @return the query instance
     */
    public Q skip(int count) {
        this.skipValue = count;
        return self();
    }

    /**
     * Ensure results are distinct
     * @return the query instance
     */
    public Q distinct() {
        this.distinctValue = true;
        return self();
    }

    /**
     * Peek at each element during processing
     * @param action action to perform on each element
     * @return the query instance
     */
    public Q peek(Consumer<T> action) {
        peekActions.add(action);
        return self();
    }

    /**
     * Execute the query and get results
     */
    private List<T> execute() {
        return Static.invoke(() -> {
            Stream<T> stream = dataSource.get().stream();

            for (Predicate<T> filter : filters) {
                stream = stream.filter(filter);
            }

            for (Consumer<T> peekAction : peekActions) {
                stream = stream.peek(peekAction);
            }

            if (distinctValue) {
                stream = stream.distinct();
            }

            if (!sorters.isEmpty()) {
                Comparator<T> combined = sorters.stream()
                        .reduce(Comparator::thenComparing)
                        .orElse(null);
                stream = stream.sorted(combined);
            }

            if (skipValue > 0) {
                stream = stream.skip(skipValue);
            }

            if (limitValue > 0) {
                stream = stream.limit(limitValue);
            }

            List<T> result = stream.collect(Collectors.toList());

            if (postProcessor != null) {
                result = postProcessor.apply(result);
            }

            return result;
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

    public void firstIfPresent(Consumer<T> action) {
        List<T> results = execute();
        if (!results.isEmpty()) {
            action.accept(results.get(0));
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
     * Executes filters and performs an action on each result.
     */
    public void forEach(Consumer<T> action) {
        Static.invoke(() -> {
            Stream<T> stream = dataSource.get().stream();

            // Apply all filters
            for (Predicate<T> filter : filters) {
                stream = stream.filter(filter);
            }

            // Apply sorting if it exists, as forEach is terminal
            if (!sorters.isEmpty()) {
                Comparator<T> combined = sorters.stream()
                        .reduce(Comparator::thenComparing)
                        .orElse(null);
                stream = stream.sorted(combined);
            }

            stream.forEach(action);
        });
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
     * Check if there are no filtered results
     * @return true if no results after filtering, false otherwise
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
     * Check if any filtered result matches the predicate
     * @param predicate predicate to test
     * @return true if any match, false otherwise
     */
    public boolean any(Predicate<T> predicate) {
        return Static.invoke(() -> {
            Stream<T> stream = dataSource.get().stream();
            for (Predicate<T> filter : filters) {
                stream = stream.filter(filter);
            }
            return stream.anyMatch(predicate);
        });
    }

    /**
     * Check if all filtered results match the predicate
     * @param predicate predicate to test
     * @return true if all match, false otherwise
     */
    public boolean all(Predicate<T> predicate) {
        return Static.invoke(() -> {
            Stream<T> stream = dataSource.get().stream();
            for (Predicate<T> filter : filters) {
                stream = stream.filter(filter);
            }
            return stream.allMatch(predicate);
        });
    }

    /**
     * Check if no filtered results match the predicate
     * @param predicate predicate to test
     * @return true if none match, false otherwise
     */
    public boolean none(Predicate<T> predicate) {
        return Static.invoke(() -> {
            Stream<T> stream = dataSource.get().stream();
            for (Predicate<T> filter : filters) {
                stream = stream.filter(filter);
            }
            return stream.noneMatch(predicate);
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
                stream = stream.sorted(combined);
            }

            return stream.collect(collector);
        });
    }

    /**
     * Apply a post-processing function after sorting.
     * Useful for reordering algorithms that can't be expressed as Comparators.
     * @param processor function to transform the result list
     * @return the query instance
     */
    public Q postProcess(Function<List<T>, List<T>> processor) {
        this.postProcessor = processor;
        return self();
    }
}