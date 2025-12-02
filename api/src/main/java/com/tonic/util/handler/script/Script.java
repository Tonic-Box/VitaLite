package com.tonic.util.handler.script;

import com.tonic.util.ReflectUtil;
import com.tonic.util.handler.StepHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Entry point for the coroutine-style Script DSL.
 *
 * <p>Script provides a yield/await-style syntax that compiles to step-based
 * StepHandler execution. This eliminates manual step numbering and magic
 * string context keys while providing natural sequential code flow.
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // Build a handler without executing
 * StepHandler handler = Script.build(s -> {
 *     s.action(() -> openBank());
 *     s.await(BankAPI::isOpen);
 *     s.action(() -> depositAll());
 * });
 *
 * // Or execute immediately
 * Script.execute(s -> {
 *     s.action(() -> openBank());
 *     s.await(BankAPI::isOpen);
 *     s.action(() -> depositAll());
 * });
 * }</pre>
 *
 * <h2>Type-Safe Context</h2>
 * <pre>{@code
 * static final Var<Integer> COUNT = Var.of("count", Integer.class, 0);
 * static final Var<NpcEx> TARGET = Var.of("target", NpcEx.class);
 *
 * Script.execute(s -> {
 *     s.action(TARGET, () -> NpcAPI.search().withName("Banker").first());
 *     s.exitIf(() -> s.get(TARGET) == null);
 *     s.action(() -> NpcAPI.interact(s.get(TARGET), "Bank"));
 *     s.await(BankAPI::isOpen);
 * });
 * }</pre>
 *
 * <h2>Control Flow</h2>
 * <pre>{@code
 * Script.execute(s -> {
 *     // Conditional
 *     s.when(() -> someCondition(), body -> {
 *         body.action(() -> doSomething());
 *     });
 *
 *     // Loops
 *     s.loopUntil(() -> inventoryFull(), loop -> {
 *         loop.action(() -> mine());
 *         loop.yield();
 *     });
 *
 *     // Labels and jumps
 *     s.label("retry");
 *     s.action(() -> tryAction());
 *     s.jumpIf(() -> failed(), "retry");
 * });
 * }</pre>
 *
 * <h2>Composition</h2>
 * <pre>{@code
 * // Include other scripts
 * Script.execute(s -> {
 *     s.include(bankingScript);
 *     s.include(existingStepHandler);
 * });
 * }</pre>
 */
public final class Script {

    private Script() {}

    /**
     * Builds a StepHandler from the script definition.
     * The handler can be executed later with handler.execute() or handler.step().
     *
     * @param script The script definition
     * @return A compiled StepHandler
     */
    public static StepHandler build(Consumer<ScriptBuilder> script) {
        ScriptCompiler compiler = new ScriptCompiler();
        return compiler.compile(script);
    }

    public static StepHandler build(Class<? extends IScript> script)
    {
        IScript instance;
        try {
            instance = (IScript) ReflectUtil.newInstance(script, null, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return build(s -> {
            for (Method method : findScriptBuilderMethods(script)) {
                try {
                    s.sub(method.getName(), toConsumer(method, instance));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            instance.main(s);
        });
    }

    /**
     * Builds and immediately executes the script.
     * Blocks until the script completes.
     *
     * @param script The script definition
     */
    public static void execute(Consumer<ScriptBuilder> script) {
        build(script).execute();
    }

    /**
     * Alias for build() - creates a StepHandler from the script.
     *
     * @param script The script definition
     * @return A compiled StepHandler
     */
    public static StepHandler run(Consumer<ScriptBuilder> script) {
        return build(script);
    }

    private static Consumer<ScriptBuilder> toConsumer(Method method, Object instance) {
        return (sb) -> {
            try {
                method.setAccessible(true);
                method.invoke(instance, sb);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static List<Method> findScriptBuilderMethods(Class<?> clazz) {
        List<Method> result = new ArrayList<>();

        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            if (method.getReturnType() != void.class) {
                continue;
            }

            if (method.getName().equals("main")) {
                continue;
            }

            Class<?>[] params = method.getParameterTypes();
            if (params.length == 1 && params[0] == ScriptBuilder.class) {
                result.add(method);
            }
        }

        return result;
    }
}
