package com.tonic.util.handler.script;

import com.tonic.data.wrappers.PlayerEx;
import com.tonic.util.handler.StepHandler;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Coroutine-style DSL for building StepHandlers.
 * Provides yield/await semantics that compile to step-based execution.
 *
 * <p>Example usage:
 * <pre>{@code
 * Script.run(s -> {
 *     s.action(() -> openBank());
 *     s.await(BankAPI::isOpen);
 *     s.action(() -> depositAll());
 * });
 * }</pre>
 */
public class ScriptBuilder {

    private final ScriptCompiler compiler;
    private int loopCounter = 0;

    ScriptBuilder(ScriptCompiler compiler) {
        this.compiler = compiler;
    }

    // ==================== Basic Actions ====================

    /**
     * Executes an action and continues to the next step.
     */
    public void action(Runnable action) {
        compiler.addStep(new ScriptStep.ActionStep(ctx -> action.run()));
    }

    /**
     * Executes an action with access to the context.
     */
    public void action(Consumer<ScriptContext> action) {
        compiler.addStep(new ScriptStep.ActionStep(action));
    }

    /**
     * Executes an action that produces a value and stores it in a Var.
     */
    public <T> void action(Var<T> var, Supplier<T> action) {
        compiler.addStep(new ScriptStep.ActionStep(ctx -> {
            T result = action.get();
            ctx.set(var, result);
        }));
    }

    // ==================== Yield/Await Primitives ====================

    /**
     * Pauses execution for one tick.
     */
    public void yield() {
        yield(1);
    }

    /**
     * Pauses execution for the specified number of ticks.
     */
    public void yield(int ticks) {
        compiler.addStep(new ScriptStep.YieldStep(ticks));
    }

    /**
     * Pauses until the condition becomes true.
     */
    public void await(BooleanSupplier condition) {
        compiler.addStep(new ScriptStep.AwaitStep(ctx -> condition.getAsBoolean()));
    }

    /**
     * Pauses until the condition becomes true (with context access).
     */
    public void await(Predicate<ScriptContext> condition) {
        compiler.addStep(new ScriptStep.AwaitStep(condition));
    }

    /**
     * Pauses until condition is true, or timeout triggers callback.
     */
    public void await(int timeout, BooleanSupplier condition, Runnable onTimeout) {
        compiler.addStep(new ScriptStep.AwaitWithTimeoutStep(
                ctx -> condition.getAsBoolean(),
                timeout,
                ctx -> onTimeout.run()
        ));
    }

    /**
     * Pauses until condition is true (with context), or timeout triggers callback.
     */
    public void await(int timeout, Predicate<ScriptContext> condition, Consumer<ScriptContext> onTimeout) {
        compiler.addStep(new ScriptStep.AwaitWithTimeoutStep(condition, timeout, onTimeout));
    }

    /**
     * Pauses until the condition becomes true, then yields for additional ticks.
     * Note: Adds 1 internally to offset SPEED_UP cascade from await.
     */
    public void awaitAndYield(BooleanSupplier condition, int yieldTicks) {
        compiler.addStep(new ScriptStep.AwaitStep(ctx -> condition.getAsBoolean()));
        compiler.addStep(new ScriptStep.YieldStep(yieldTicks + 1));
    }

    /**
     * Pauses until the condition becomes true (with context), then yields for additional ticks.
     * Note: Adds 1 internally to offset SPEED_UP cascade from await.
     */
    public void awaitAndYield(Predicate<ScriptContext> condition, int yieldTicks) {
        compiler.addStep(new ScriptStep.AwaitStep(condition));
        compiler.addStep(new ScriptStep.YieldStep(yieldTicks + 1));
    }

    // ==================== Control Flow ====================

    /**
     * Marks a jump target.
     */
    public void label(String name) {
        compiler.addLabel(name);
        compiler.addStep(new ScriptStep.LabelStep(name));
    }

    /**
     * Jumps to a label unconditionally.
     */
    public void jump(String label) {
        compiler.addStep(new ScriptStep.JumpStep(label));
    }

    /**
     * Jumps to a label if condition is true.
     */
    public void jumpIf(BooleanSupplier condition, String label) {
        compiler.addStep(new ScriptStep.JumpIfStep(ctx -> condition.getAsBoolean(), label));
    }

    /**
     * Jumps to a label if condition is true (with context access).
     */
    public void jumpIf(Predicate<ScriptContext> condition, String label) {
        compiler.addStep(new ScriptStep.JumpIfStep(condition, label));
    }

    /**
     * Exits the script immediately.
     */
    public void exit() {
        compiler.addStep(new ScriptStep.ExitStep());
    }

    /**
     * Exits the script if condition is true.
     */
    public void exitIf(BooleanSupplier condition) {
        compiler.addStep(new ScriptStep.ExitIfStep(ctx -> condition.getAsBoolean()));
    }

    /**
     * Exits the script if condition is true (with context access).
     */
    public void exitIf(Predicate<ScriptContext> condition) {
        compiler.addStep(new ScriptStep.ExitIfStep(condition));
    }

    // ==================== Branching ====================

    /**
     * Conditionally executes the body if condition is true.
     */
    public void when(BooleanSupplier condition, Consumer<ScriptBuilder> body) {
        ifThenElse(condition, body, b -> {});
    }

    /**
     * Conditionally executes the body if condition is true (with context).
     */
    public void when(Predicate<ScriptContext> condition, Consumer<ScriptBuilder> body) {
        ifThenElse(condition, body, b -> {});
    }

    /**
     * If-then without else.
     */
    public void ifThen(BooleanSupplier condition, Consumer<ScriptBuilder> thenBranch) {
        ifThenElse(condition, thenBranch, b -> {});
    }

    /**
     * If-then without else (with context).
     */
    public void ifThen(Predicate<ScriptContext> condition, Consumer<ScriptBuilder> thenBranch) {
        ifThenElse(condition, thenBranch, b -> {});
    }

    /**
     * If-then-else branching.
     */
    public void ifThenElse(BooleanSupplier condition, Consumer<ScriptBuilder> thenBranch, Consumer<ScriptBuilder> elseBranch) {
        ifThenElse(ctx -> condition.getAsBoolean(), thenBranch, elseBranch);
    }

    /**
     * If-then-else branching with context access.
     */
    public void ifThenElse(Predicate<ScriptContext> condition, Consumer<ScriptBuilder> thenBranch, Consumer<ScriptBuilder> elseBranch) {
        // Record position for branch step
        int branchStepIndex = compiler.reserveStep();

        // Compile then branch
        int thenStart = compiler.getCurrentStep();
        thenBranch.accept(this);
        int afterThen = compiler.reserveStep();  // Jump to skip else

        // Compile else branch
        int elseStart = compiler.getCurrentStep();
        elseBranch.accept(this);
        int afterElse = compiler.getCurrentStep();

        // Patch the branch step
        compiler.setStep(branchStepIndex, new ScriptStep.BranchStep(condition, thenStart, elseStart));

        // Patch the jump after then to skip else
        compiler.setStep(afterThen, new ScriptStep.JumpStep("__ifEnd_" + branchStepIndex));
        compiler.addLabelAt("__ifEnd_" + branchStepIndex, afterElse);
    }

    // ==================== Loops ====================

    /**
     * Infinite loop - use exit() to break.
     */
    public void loop(Consumer<ScriptBuilder> body) {
        loopWhile(() -> true, body);
    }

    /**
     * Loop N times.
     */
    public void loop(int times, Consumer<ScriptBuilder> body) {
        String loopId = "__loopCount_" + (loopCounter++);
        Var<Integer> counterVar = Var.intVar(loopId);

        int loopStartIndex = compiler.reserveStep();  // Reserve for loop counter check
        int bodyStart = compiler.getCurrentStep();

        // Compile body
        body.accept(this);

        // Jump back to start
        int loopEndIndex = compiler.reserveStep();

        // Patch loop start - checks counter
        compiler.setStep(loopStartIndex, new ScriptStep.LoopCountedStartStep(counterVar, times, compiler.getCurrentStep()));

        // Patch loop end - jumps to start
        compiler.setStep(loopEndIndex, new ScriptStep.LoopEndStep(loopStartIndex));
    }

    /**
     * Loop while condition is true.
     */
    public void loopWhile(BooleanSupplier condition, Consumer<ScriptBuilder> body) {
        loopWhile(ctx -> condition.getAsBoolean(), body);
    }

    /**
     * Loop while condition is true (with context).
     */
    public void loopWhile(Predicate<ScriptContext> condition, Consumer<ScriptBuilder> body) {
        loopInternal(condition, body, false);
    }

    /**
     * Loop until condition becomes true.
     */
    public void loopUntil(BooleanSupplier condition, Consumer<ScriptBuilder> body) {
        loopUntil(ctx -> condition.getAsBoolean(), body);
    }

    /**
     * Loop until condition becomes true (with context).
     */
    public void loopUntil(Predicate<ScriptContext> condition, Consumer<ScriptBuilder> body) {
        loopInternal(condition, body, true);
    }

    private void loopInternal(Predicate<ScriptContext> condition, Consumer<ScriptBuilder> body, boolean isUntil) {
        int loopStartIndex = compiler.reserveStep();  // Reserve for condition check
        int bodyStart = compiler.getCurrentStep();

        // Compile body
        body.accept(this);

        // Jump back to start
        int loopEndIndex = compiler.reserveStep();
        int afterLoop = compiler.getCurrentStep();

        // Patch loop start - checks condition
        compiler.setStep(loopStartIndex, new ScriptStep.LoopStartStep(condition, afterLoop, isUntil));

        // Patch loop end - jumps to start
        compiler.setStep(loopEndIndex, new ScriptStep.LoopEndStep(loopStartIndex));
    }

    // ==================== Composition ====================

    /**
     * Includes another script inline.
     */
    public void include(Consumer<ScriptBuilder> subscript) {
        subscript.accept(this);
    }

    /**
     * Includes an existing StepHandler, running until completion.
     */
    public void include(StepHandler handler) {
        compiler.addStep(new ScriptStep.IncludeHandlerStep(handler));
    }

    // ==================== Context Shortcuts ====================

    /**
     * Gets a value from the context.
     * Note: For use within action lambdas that have context access.
     */
    public <T> T get(Var<T> var) {
        return compiler.getContext().get(var);
    }

    /**
     * Sets a value in the context.
     * Note: For use within action lambdas that have context access.
     */
    public <T> void set(Var<T> var, T value) {
        compiler.getContext().set(var, value);
    }

    // ==================== Script-Local Variable Declarations ====================

    private int varCounter = 0;

    /**
     * Creates a script-local Var for an object type.
     * The variable is scoped to this script instance.
     *
     * @param name The variable name
     * @param type The variable type class
     * @return A new Var scoped to this script
     */
    public <T> Var<T> var(String name, Class<T> type) {
        return Var.of("__local_" + (varCounter++) + "_" + name, type);
    }

    /**
     * Creates a script-local Var for an object type with a default value.
     *
     * @param name The variable name
     * @param type The variable type class
     * @param defaultValue The default value
     * @return A new Var scoped to this script
     */
    public <T> Var<T> var(String name, Class<T> type, T defaultValue) {
        return Var.of("__local_" + (varCounter++) + "_" + name, type, defaultValue);
    }

    /**
     * Creates a script-local integer variable with default 0.
     */
    public Var<Integer> varInt(String name) {
        return Var.of("__local_" + (varCounter++) + "_" + name, Integer.class, 0);
    }

    /**
     * Creates a script-local integer variable with specified default.
     */
    public Var<Integer> varInt(String name, int defaultValue) {
        return Var.of("__local_" + (varCounter++) + "_" + name, Integer.class, defaultValue);
    }

    /**
     * Creates a script-local boolean variable with default false.
     */
    public Var<Boolean> varBool(String name) {
        return Var.of("__local_" + (varCounter++) + "_" + name, Boolean.class, false);
    }

    /**
     * Creates a script-local boolean variable with specified default.
     */
    public Var<Boolean> varBool(String name, boolean defaultValue) {
        return Var.of("__local_" + (varCounter++) + "_" + name, Boolean.class, defaultValue);
    }

    /**
     * Creates a script-local long variable with default 0.
     */
    public Var<Long> varLong(String name) {
        return Var.of("__local_" + (varCounter++) + "_" + name, Long.class, 0L);
    }

    /**
     * Creates a script-local long variable with specified default.
     */
    public Var<Long> varLong(String name, long defaultValue) {
        return Var.of("__local_" + (varCounter++) + "_" + name, Long.class, defaultValue);
    }

    /**
     * Creates a script-local double variable with default 0.0.
     */
    public Var<Double> varDouble(String name) {
        return Var.of("__local_" + (varCounter++) + "_" + name, Double.class, 0.0);
    }

    /**
     * Creates a script-local double variable with specified default.
     */
    public Var<Double> varDouble(String name, double defaultValue) {
        return Var.of("__local_" + (varCounter++) + "_" + name, Double.class, defaultValue);
    }

    /**
     * Creates a script-local string variable with default empty string.
     */
    public Var<String> varString(String name) {
        return Var.of("__local_" + (varCounter++) + "_" + name, String.class, "");
    }

    /**
     * Creates a script-local string variable with specified default.
     */
    public Var<String> varString(String name, String defaultValue) {
        return Var.of("__local_" + (varCounter++) + "_" + name, String.class, defaultValue);
    }

    // ==================== Subroutines ====================

    /**
     * Defines a subroutine. The body is skipped during normal linear execution
     * and only runs when called via call(). Subroutines share the same context
     * as the main script and can access/modify all variables.
     *
     * @param name The subroutine name (used with call())
     * @param body The subroutine body - steps to execute when called
     */
    public void sub(String name, Consumer<ScriptBuilder> body) {
        // Reserve step for jump-over (will skip the subroutine body during normal execution)
        int jumpOverIndex = compiler.reserveStep();

        // Mark subroutine start for call() to jump to
        compiler.addLabel("__sub_" + name);

        // Compile subroutine body
        body.accept(this);

        // Add implicit return at end of subroutine
        compiler.addStep(new ScriptStep.RetStep());

        // Patch jump-over step to skip past subroutine body
        int afterSub = compiler.getCurrentStep();
        compiler.setStep(jumpOverIndex, new ScriptStep.SubStartStep(name, afterSub));
    }

    /**
     * Calls a subroutine. Pushes the return address and jumps to the subroutine.
     * Execution resumes at the next step after the subroutine completes.
     *
     * @param name The name of the subroutine to call (defined with sub())
     */
    public void call(String name) {
        compiler.addStep(new ScriptStep.CallStep(name));
    }

    /**
     * Explicit return from a subroutine. This is optional since an implicit
     * return is added at the end of each subroutine. Use this for early returns.
     */
    public void ret() {
        compiler.addStep(new ScriptStep.RetStep());
    }

    //Domain specific helpers

    public void awaitIdle()
    {
        await(() -> !PlayerEx.getLocal().isIdle());
        await(() -> PlayerEx.getLocal().isIdle());
    }

    //from class


}
