package com.tonic.util.handler.script;

import com.tonic.util.handler.StepContext;
import com.tonic.util.handler.StepHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Compiles Script DSL into a StepHandler.
 * Collects steps during builder execution and produces a step map.
 */
public class ScriptCompiler {

    private final List<ScriptStep> steps = new ArrayList<>();
    private final Map<String, Integer> labels = new HashMap<>();
    private final Map<String, Integer> pendingLabels = new HashMap<>();
    private ScriptContext scriptContext;

    /**
     * Compiles a script consumer into a StepHandler.
     */
    public StepHandler compile(Consumer<ScriptBuilder> script) {
        // Create the builder and collect steps
        ScriptBuilder builder = new ScriptBuilder(this);
        script.accept(builder);

        // Create the step handler
        StepHandler handler = new StepHandler(new HashMap<>());
        scriptContext = new ScriptContext(handler.getContext());

        // Register labels in the handler's context
        handler.getContext().getLabels().putAll(labels);

        // Convert steps to handler functions
        Map<Integer, Function<StepContext, Integer>> stepMap = new HashMap<>();
        for (int i = 0; i < steps.size(); i++) {
            ScriptStep step = steps.get(i);
            stepMap.put(i, step.toFunction(i, labels, scriptContext));
        }

        // Add steps to handler via reflection or direct access
        for (Map.Entry<Integer, Function<StepContext, Integer>> entry : stepMap.entrySet()) {
            handler.add(entry.getKey(), entry.getValue());
        }

        return handler;
    }

    /**
     * Adds a step and returns its index.
     */
    int addStep(ScriptStep step) {
        int index = steps.size();
        steps.add(step);
        return index;
    }

    /**
     * Reserves a step slot to be filled later (for forward references).
     */
    int reserveStep() {
        int index = steps.size();
        steps.add(null);  // Placeholder
        return index;
    }

    /**
     * Sets a step at a previously reserved index.
     */
    void setStep(int index, ScriptStep step) {
        steps.set(index, step);
    }

    /**
     * Gets the current step index (next step to be added).
     */
    int getCurrentStep() {
        return steps.size();
    }

    /**
     * Adds a label at the current position.
     */
    void addLabel(String name) {
        labels.put(name, getCurrentStep());
    }

    /**
     * Adds a label at a specific position.
     */
    void addLabelAt(String name, int stepIndex) {
        labels.put(name, stepIndex);
    }

    /**
     * Gets the script context (available after compilation starts).
     */
    ScriptContext getContext() {
        return scriptContext;
    }

    /**
     * Gets the step count.
     */
    int getStepCount() {
        return steps.size();
    }
}
