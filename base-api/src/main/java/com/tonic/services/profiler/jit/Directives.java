package com.tonic.services.profiler.jit;

import com.tonic.services.profiler.DiagnosticCommands;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compiler directive rules that change how matching methods compile from then on. A compilation uses only the newest
 * directive matching its method, so the rules are pushed as one file: exclusions first, then a single catch-all
 * entry carrying every inline rule, so no rule hides another.
 */
public final class Directives {
    private static final List<Rule> RULES = new ArrayList<>();

    private Directives() {
    }

    /**
     * What a rule does to the methods it matches
     */
    public enum Action {
        EXCLUDE("Don't compile"),
        INLINE("Always inline"),
        DONT_INLINE("Never inline");

        private final String label;

        Action(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * One rule: a method pattern and what to do to it
     */
    public static final class Rule {
        public final String pattern;
        public final Action action;

        /**
         * Creates the rule
         * @param pattern a pattern accepted by toHotSpotPattern
         * @param action what to do to matching methods
         * @throws IllegalArgumentException if the pattern is not usable
         */
        public Rule(String pattern, Action action) {
            this.pattern = toHotSpotPattern(pattern);
            this.action = Objects.requireNonNull(action, "action");
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Rule && ((Rule) other).pattern.equals(pattern) && ((Rule) other).action == action;
        }

        @Override
        public int hashCode() {
            return Objects.hash(pattern, action);
        }

        @Override
        public String toString() {
            return action + ": " + pattern;
        }
    }

    /**
     * Turns a method pattern into HotSpot's form: dotted class names become slashed and the method follows the last dot
     * @param pattern a pattern such as net.runelite.client.Foo.bar or java.lang.String.*, with * wildcards
     * @return the pattern as directives expect it, such as net/runelite/client/Foo.bar
     * @throws IllegalArgumentException if the pattern is empty, has no method part or contains a quote
     */
    public static String toHotSpotPattern(String pattern) {
        String trimmed = pattern.trim();
        if (trimmed.contains("\"")) {
            throw new IllegalArgumentException("pattern cannot contain quotes");
        }
        if (trimmed.contains("/") || trimmed.contains("::")) {
            return trimmed.replace("::", ".");
        }
        int lastDot = trimmed.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == trimmed.length() - 1) {
            throw new IllegalArgumentException("pattern needs a class and a method, such as java.lang.String.indexOf");
        }
        return trimmed.substring(0, lastDot).replace('.', '/') + trimmed.substring(lastDot);
    }

    /**
     * Gets the rules in force
     * @return the rules, oldest first
     */
    public static synchronized List<Rule> rules() {
        return new ArrayList<>(RULES);
    }

    /**
     * Adds a rule and pushes the rule set
     * @param rule the rule to add; adding a rule already in force changes nothing
     * @return the command's reply
     * @throws IllegalStateException if the rules could not be pushed
     */
    public static synchronized String add(Rule rule) {
        if (!RULES.contains(rule)) {
            RULES.add(rule);
        }
        return push();
    }

    /**
     * Removes a rule and pushes the rule set
     * @param rule the rule to remove
     * @return the command's reply
     * @throws IllegalStateException if the rules could not be pushed
     */
    public static synchronized String remove(Rule rule) {
        RULES.remove(rule);
        return push();
    }

    /**
     * Removes every rule, and any directive added by other tools, leaving the default directive
     * @return the command's reply
     * @throws IllegalStateException if the command fails
     */
    public static synchronized String clear() {
        RULES.clear();
        return DiagnosticCommands.run("compilerDirectivesClear").trim();
    }

    /**
     * Lists the directive stack
     * @return the command's full listing, newest first
     * @throws IllegalStateException if the command fails
     */
    public static String print() {
        return DiagnosticCommands.run("compilerDirectivesPrint");
    }

    /**
     * Builds the directive file for a rule set: one entry per exclusion, then one catch-all entry with the inline rules
     * @param rules the rules
     * @return the JSON directive array
     */
    public static String json(List<Rule> rules) {
        List<String> entries = new ArrayList<>();
        List<String> inline = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule.action == Action.EXCLUDE) {
                entries.add("{ match: \"" + rule.pattern + "\", Exclude: true }");
            } else {
                inline.add("\"" + (rule.action == Action.INLINE ? "+" : "-") + rule.pattern + "\"");
            }
        }
        if (!inline.isEmpty()) {
            entries.add("{ match: \"*.*\", inline: [" + String.join(", ", inline) + "] }");
        }
        return "[" + String.join(", ", entries) + "]";
    }

    private static String push() {
        String cleared = DiagnosticCommands.run("compilerDirectivesClear").trim();
        if (RULES.isEmpty()) {
            return cleared;
        }
        Path file = null;
        try {
            file = Files.createTempFile("vitalite-directives", ".json");
            Files.write(file, json(RULES).getBytes(StandardCharsets.UTF_8));
            return DiagnosticCommands.run("compilerDirectivesAdd", file.toString()).trim();
        } catch (IOException e) {
            throw new IllegalStateException("could not write the directive file: " + e.getMessage(), e);
        } finally {
            if (file != null) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException ignored) {
                }
            }
        }
    }
}
