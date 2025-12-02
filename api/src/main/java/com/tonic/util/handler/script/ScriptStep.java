package com.tonic.util.handler.script;

import com.tonic.util.handler.HandlerBuilder;
import com.tonic.util.handler.StepContext;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a single step in a compiled script.
 * Each implementation knows how to convert itself to a StepHandler function.
 */
public interface ScriptStep {

    /**
     * Converts this step to a StepHandler function.
     * @param stepNum The step number this will be assigned to
     * @param labels Map of label names to step numbers
     * @param scriptContext The shared script context wrapper
     * @return Function that takes StepContext and returns next step number
     */
    Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext);

    /**
     * An action step - runs code and advances to next step.
     */
    class ActionStep implements ScriptStep {
        private final Consumer<ScriptContext> action;

        public ActionStep(Consumer<ScriptContext> action) {
            this.action = action;
        }

        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            return ctx -> {
                action.accept(scriptContext);
                ctx.put("SPEED_UP", true);  // Immediately proceed to next step
                return stepNum + 1;
            };
        }
    }

    /**
     * A yield step - pauses execution for N ticks.
     */
    class YieldStep implements ScriptStep {
        private final int ticks;

        public YieldStep(int ticks) {
            this.ticks = ticks;
        }

        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            AtomicInteger remaining = new AtomicInteger(ticks);
            return ctx -> {
                if (remaining.decrementAndGet() <= 0) {
                    remaining.set(ticks);  // Reset for potential re-entry
                    // No SPEED_UP - yield must consume a tick, never cascade
                    return stepNum + 1;
                }
                return stepNum;  // Stay on this step
            };
        }
    }

    /**
     * An await step - waits until condition is true.
     * Yields 1 tick before first check to allow prior action to take effect.
     * Uses context-based state tracking so it resets properly on handler.reset().
     */
    class AwaitStep implements ScriptStep {
        private final Predicate<ScriptContext> condition;

        public AwaitStep(Predicate<ScriptContext> condition) {
            this.condition = condition;
        }

        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            final String initKey = "__await_" + stepNum;
            return ctx -> {
                // First call this run - yield 1 tick to let prior action take effect
                if (!ctx.getOrDefault(initKey, false)) {
                    ctx.put(initKey, true);
                    return stepNum;
                }
                if (condition.test(scriptContext)) {
                    ctx.remove(initKey);  // Clear for next handler run
                    ctx.put("SPEED_UP", true);
                    return stepNum + 1;
                }
                return stepNum;  // Stay on this step until condition met
            };
        }
    }

    /**
     * An await step with timeout.
     * Yields 1 tick before first check to allow prior action to take effect.
     * Uses context-based state tracking so it resets properly on handler.reset().
     */
    class AwaitWithTimeoutStep implements ScriptStep {
        private final Predicate<ScriptContext> condition;
        private final int timeout;
        private final Consumer<ScriptContext> onTimeout;

        public AwaitWithTimeoutStep(Predicate<ScriptContext> condition, int timeout, Consumer<ScriptContext> onTimeout) {
            this.condition = condition;
            this.timeout = timeout;
            this.onTimeout = onTimeout;
        }

        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            final String initKey = "__awaitTimeout_" + stepNum;
            final String remainingKey = "__awaitTimeout_remaining_" + stepNum;
            return ctx -> {
                // First call this run - yield 1 tick and init remaining
                if (!ctx.getOrDefault(initKey, false)) {
                    ctx.put(initKey, true);
                    ctx.put(remainingKey, timeout);
                    return stepNum;
                }
                if (condition.test(scriptContext)) {
                    ctx.remove(initKey);
                    ctx.remove(remainingKey);
                    ctx.put("SPEED_UP", true);
                    return stepNum + 1;
                }
                int remaining = ctx.getOrDefault(remainingKey, timeout);
                if (remaining <= 1) {
                    ctx.remove(initKey);
                    ctx.remove(remainingKey);
                    onTimeout.accept(scriptContext);
                    return HandlerBuilder.END_EXECUTION;
                }
                ctx.put(remainingKey, remaining - 1);
                return stepNum;
            };
        }
    }

    /**
     * A label step - just a marker, immediately proceeds to next step.
     */
    class LabelStep implements ScriptStep {
        private final String name;

        public LabelStep(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            return ctx -> {
                ctx.put("SPEED_UP", true);
                return stepNum + 1;
            };
        }
    }

    /**
     * An unconditional jump step.
     */
    class JumpStep implements ScriptStep {
        private final String label;

        public JumpStep(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            return ctx -> {
                Integer target = labels.get(label);
                if (target == null) {
                    throw new IllegalStateException("Unknown label: " + label);
                }
                ctx.put("SPEED_UP", true);
                return target;
            };
        }
    }

    /**
     * A conditional jump step.
     */
    class JumpIfStep implements ScriptStep {
        private final Predicate<ScriptContext> condition;
        private final String label;

        public JumpIfStep(Predicate<ScriptContext> condition, String label) {
            this.condition = condition;
            this.label = label;
        }

        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            return ctx -> {
                if (condition.test(scriptContext)) {
                    Integer target = labels.get(label);
                    if (target == null) {
                        throw new IllegalStateException("Unknown label: " + label);
                    }
                    ctx.put("SPEED_UP", true);
                    return target;
                }
                ctx.put("SPEED_UP", true);
                return stepNum + 1;
            };
        }
    }

    /**
     * An exit step - terminates execution.
     */
    class ExitStep implements ScriptStep {
        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            return ctx -> HandlerBuilder.END_EXECUTION;
        }
    }

    /**
     * A conditional exit step.
     */
    class ExitIfStep implements ScriptStep {
        private final Predicate<ScriptContext> condition;

        public ExitIfStep(Predicate<ScriptContext> condition) {
            this.condition = condition;
        }

        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            return ctx -> {
                if (condition.test(scriptContext)) {
                    return HandlerBuilder.END_EXECUTION;
                }
                ctx.put("SPEED_UP", true);
                return stepNum + 1;
            };
        }
    }

    /**
     * A branch step - conditionally executes one of two step sequences.
     * The targets are step numbers resolved during compilation.
     */
    class BranchStep implements ScriptStep {
        private final Predicate<ScriptContext> condition;
        private final int thenTarget;
        private final int elseTarget;

        public BranchStep(Predicate<ScriptContext> condition, int thenTarget, int elseTarget) {
            this.condition = condition;
            this.thenTarget = thenTarget;
            this.elseTarget = elseTarget;
        }

        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            return ctx -> {
                ctx.put("SPEED_UP", true);
                return condition.test(scriptContext) ? thenTarget : elseTarget;
            };
        }
    }

    /**
     * Loop start marker - evaluates condition, jumps to end if false.
     */
    class LoopStartStep implements ScriptStep {
        private final Predicate<ScriptContext> condition;
        private final int loopEndStep;
        private final boolean isUntil;

        public LoopStartStep(Predicate<ScriptContext> condition, int loopEndStep, boolean isUntil) {
            this.condition = condition;
            this.loopEndStep = loopEndStep;
            this.isUntil = isUntil;
        }

        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            return ctx -> {
                boolean conditionMet = condition.test(scriptContext);
                // For loopUntil: exit when condition becomes true
                // For loopWhile: exit when condition becomes false
                boolean shouldExit = isUntil ? conditionMet : !conditionMet;
                ctx.put("SPEED_UP", true);
                return shouldExit ? loopEndStep : stepNum + 1;
            };
        }
    }

    /**
     * Loop end marker - jumps back to loop start.
     */
    class LoopEndStep implements ScriptStep {
        private final int loopStartStep;

        public LoopEndStep(int loopStartStep) {
            this.loopStartStep = loopStartStep;
        }

        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            return ctx -> {
                ctx.put("SPEED_UP", true);
                return loopStartStep;
            };
        }
    }

    /**
     * A counted loop step - executes body N times.
     */
    class LoopCountedStartStep implements ScriptStep {
        private final Var<Integer> counterVar;
        private final int times;
        private final int loopEndStep;

        public LoopCountedStartStep(Var<Integer> counterVar, int times, int loopEndStep) {
            this.counterVar = counterVar;
            this.times = times;
            this.loopEndStep = loopEndStep;
        }

        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            return ctx -> {
                int count = scriptContext.getOrDefault(counterVar, 0);
                if (count >= times) {
                    scriptContext.set(counterVar, 0);  // Reset for potential re-entry
                    ctx.put("SPEED_UP", true);
                    return loopEndStep;
                }
                scriptContext.set(counterVar, count + 1);
                ctx.put("SPEED_UP", true);
                return stepNum + 1;
            };
        }
    }

    /**
     * Include step - runs another StepHandler inline until completion.
     * Resets the included handler on fresh entry so it works after parent handler.reset().
     */
    class IncludeHandlerStep implements ScriptStep {
        private final com.tonic.util.handler.StepHandler handler;

        public IncludeHandlerStep(com.tonic.util.handler.StepHandler handler) {
            this.handler = handler;
        }

        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            final String initKey = "__include_" + stepNum;
            return ctx -> {
                // Reset included handler on fresh entry
                if (!ctx.getOrDefault(initKey, false)) {
                    ctx.put(initKey, true);
                    handler.reset();
                }
                boolean hasMore = handler.step();
                if (!hasMore) {
                    ctx.remove(initKey);
                    ctx.put("SPEED_UP", true);
                    return stepNum + 1;
                }
                return stepNum;  // Stay until handler completes
            };
        }
    }

    // ==================== Subroutine Steps ====================

    /**
     * Subroutine start marker - skips over the subroutine body during normal execution.
     */
    class SubStartStep implements ScriptStep {
        private final String name;
        private final int skipToStep;

        public SubStartStep(String name, int skipToStep) {
            this.name = name;
            this.skipToStep = skipToStep;
        }

        public String getName() {
            return name;
        }

        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            return ctx -> {
                ctx.put("SPEED_UP", true);
                return skipToStep;  // Skip past the subroutine body
            };
        }
    }

    /**
     * Call step - pushes return address and jumps to subroutine.
     */
    class CallStep implements ScriptStep {
        private final String subName;

        public CallStep(String subName) {
            this.subName = subName;
        }

        public String getSubName() {
            return subName;
        }

        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            return ctx -> {
                scriptContext.pushReturn(stepNum + 1);  // Push return address
                Integer target = labels.get("__sub_" + subName);
                if (target == null) {
                    throw new IllegalStateException("Unknown subroutine: " + subName);
                }
                ctx.put("SPEED_UP", true);
                return target;
            };
        }
    }

    /**
     * Return step - pops return address and jumps back to caller.
     */
    class RetStep implements ScriptStep {
        @Override
        public Function<StepContext, Integer> toFunction(int stepNum, Map<String, Integer> labels, ScriptContext scriptContext) {
            return ctx -> {
                int returnAddr = scriptContext.popReturn();
                ctx.put("SPEED_UP", true);
                return returnAddr;  // -1 will end execution if stack was empty
            };
        }
    }
}
