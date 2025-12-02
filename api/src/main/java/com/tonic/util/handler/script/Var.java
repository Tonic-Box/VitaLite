package com.tonic.util.handler.script;

import java.util.Objects;

/**
 * Type-safe key for ScriptContext values.
 * Eliminates magic strings and provides compile-time type checking.
 *
 * @param <T> The type of value this key represents
 */
public final class Var<T> {
    private final String name;
    private final Class<T> type;
    private final T defaultValue;

    private Var(String name, Class<T> type, T defaultValue) {
        this.name = Objects.requireNonNull(name, "Var name cannot be null");
        this.type = Objects.requireNonNull(type, "Var type cannot be null");
        this.defaultValue = defaultValue;
    }

    /**
     * Creates a new Var with no default value.
     */
    public static <T> Var<T> of(String name, Class<T> type) {
        return new Var<>(name, type, null);
    }

    /**
     * Creates a new Var with a default value.
     */
    public static <T> Var<T> of(String name, Class<T> type, T defaultValue) {
        return new Var<>(name, type, defaultValue);
    }

    /**
     * Creates a Var for Integer with default 0.
     */
    public static Var<Integer> intVar(String name) {
        return new Var<>(name, Integer.class, 0);
    }

    /**
     * Creates a Var for Boolean with default false.
     */
    public static Var<Boolean> boolVar(String name) {
        return new Var<>(name, Boolean.class, false);
    }

    /**
     * Creates a Var for String with default empty string.
     */
    public static Var<String> stringVar(String name) {
        return new Var<>(name, String.class, "");
    }

    public String name() {
        return name;
    }

    public Class<T> type() {
        return type;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public boolean hasDefault() {
        return defaultValue != null;
    }

    /**
     * Creates a derived Var with a suffix appended to the name.
     * Useful for dynamic keys like COUNT_itemId.
     */
    public Var<T> withSuffix(String suffix) {
        return new Var<>(name + suffix, type, defaultValue);
    }

    /**
     * Creates a derived Var with an integer suffix.
     */
    public Var<T> withSuffix(int suffix) {
        return withSuffix(String.valueOf(suffix));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Var)) return false;
        Var<?> var = (Var<?>) o;
        return name.equals(var.name) && type.equals(var.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return "Var<" + type.getSimpleName() + ">(" + name + ")";
    }
}
