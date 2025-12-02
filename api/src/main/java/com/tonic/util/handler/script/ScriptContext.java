package com.tonic.util.handler.script;

import com.tonic.services.pathfinder.model.WalkerPath;
import com.tonic.util.handler.StepContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Type-safe wrapper around StepContext for use in Script DSL.
 * Provides type-safe get/set operations via Var keys.
 */
public class ScriptContext {
    private final StepContext underlying;
    private final Map<Var<?>, Object> typedValues = new HashMap<>();
    private final Deque<Integer> returnStack = new ArrayDeque<>();

    public ScriptContext(StepContext underlying) {
        this.underlying = underlying;
    }

    /**
     * Sets a typed value in the context.
     */
    public <T> void set(Var<T> var, T value) {
        typedValues.put(var, value);
        // Also store in underlying for compatibility with existing step handler features
        underlying.put(var.name(), value);
    }

    /**
     * Gets a typed value from the context.
     * Returns the default value if not set and var has a default.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Var<T> var) {
        Object value = typedValues.get(var);
        if (value == null) {
            // Try underlying context (for compatibility)
            value = underlying.get(var.name());
        }
        if (value == null && var.hasDefault()) {
            return var.defaultValue();
        }
        return (T) value;
    }

    /**
     * Gets a value or computes and stores it if absent.
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrCompute(Var<T> var, Supplier<T> supplier) {
        T value = get(var);
        if (value == null) {
            value = supplier.get();
            set(var, value);
        }
        return value;
    }

    /**
     * Gets a value or returns the fallback if not present.
     */
    public <T> T getOrDefault(Var<T> var, T fallback) {
        T value = get(var);
        return value != null ? value : fallback;
    }

    /**
     * Checks if a var is set in the context.
     */
    public boolean has(Var<?> var) {
        return typedValues.containsKey(var) || underlying.contains(var.name());
    }

    /**
     * Removes a var from the context.
     */
    @SuppressWarnings("unchecked")
    public <T> T remove(Var<T> var) {
        underlying.remove(var.name());
        Object removed = typedValues.remove(var);
        cleanupValue(removed);
        return (T) removed;
    }

    /**
     * Clears multiple vars from the context.
     */
    public void clear(Var<?>... vars) {
        for (Var<?> var : vars) {
            remove(var);
        }
    }

    /**
     * Clears all typed values and cleans up resources.
     */
    public void clearAll() {
        for (Object value : typedValues.values()) {
            cleanupValue(value);
        }
        typedValues.clear();
    }

    /**
     * Gets the underlying StepContext for compatibility.
     */
    public StepContext getUnderlying() {
        return underlying;
    }

    /**
     * Provides access to labels map for jump functionality.
     */
    public Map<String, Integer> getLabels() {
        return underlying.getLabels();
    }

    // ==================== Subroutine Return Stack ====================

    /**
     * Pushes a return address onto the stack for subroutine calls.
     */
    public void pushReturn(int stepNum) {
        returnStack.push(stepNum);
    }

    /**
     * Pops a return address from the stack.
     * Returns -1 (end execution) if stack is empty.
     */
    public int popReturn() {
        if (returnStack.isEmpty()) {
            return -1;
        }
        return returnStack.pop();
    }

    /**
     * Checks if there are pending return addresses.
     */
    public boolean hasReturnAddress() {
        return !returnStack.isEmpty();
    }

    private void cleanupValue(Object value) {
        if (value instanceof WalkerPath) {
            ((WalkerPath) value).shutdown();
        }
    }
}
