package com.tonic.services.profiler.jit;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the output of the Compiler.codelist diagnostic command: every method compiled into the code cache
 */
public final class CodeList {
    private static final Pattern LINE = Pattern.compile("^(\\d+) (-?\\d+) (\\d+) (\\S.*?) \\[0x([0-9a-fA-F]+), 0x([0-9a-fA-F]+) - 0x([0-9a-fA-F]+)]\\s*$");
    private static final String[] TIERS = {"Native wrapper", "C1", "C1 limited profile", "C1 full profile", "C2"};
    private static final String[] STATES = {"in use", "not used", "not entrant", "zombie", "unloaded"};

    private CodeList() {
    }

    /**
     * One compiled method
     */
    public static final class Method {
        public final int compileId;
        public final int tier;
        public final int state;
        public final String name;
        public final String signature;
        public final long codeSize;

        /**
         * Creates the entry
         * @param compileId the compilation's id
         * @param tier the compilation tier, 0 for native wrappers to 4 for C2
         * @param state the nmethod state, 0 in use to 4 unloaded
         * @param name the class and method name
         * @param signature the method's descriptor
         * @param codeSize the size of its machine code in bytes
         */
        public Method(int compileId, int tier, int state, String name, String signature, long codeSize) {
            this.compileId = compileId;
            this.tier = tier;
            this.state = state;
            this.name = name;
            this.signature = signature;
            this.codeSize = codeSize;
        }

        /**
         * Gets the tier's name
         * @return the name, such as C1 full profile or C2
         */
        public String tierName() {
            return CodeList.tierName(tier);
        }

        /**
         * Gets the state's name
         * @return the name, such as in use or not entrant
         */
        public String stateName() {
            return state >= 0 && state < STATES.length ? STATES[state] : "state " + state;
        }
    }

    /**
     * Names a compilation tier
     * @param tier the tier, 0 for native wrappers to 4 for C2
     * @return the name, such as C1 full profile or C2
     */
    public static String tierName(int tier) {
        return tier >= 0 && tier < TIERS.length ? TIERS[tier] : "Tier " + tier;
    }

    /**
     * Parses every method line, skipping lines that are not methods
     * @param output the command's output
     * @return the compiled methods in output order
     */
    public static List<Method> parse(String output) {
        List<Method> methods = new ArrayList<>();
        for (String line : output.split("\\R")) {
            Matcher matcher = LINE.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            String method = matcher.group(4);
            int parenthesis = method.indexOf('(');
            String name = parenthesis < 0 ? method : method.substring(0, parenthesis);
            String signature = parenthesis < 0 ? "" : method.substring(parenthesis);
            long codeSize = Long.parseUnsignedLong(matcher.group(7), 16) - Long.parseUnsignedLong(matcher.group(6), 16);
            methods.add(new Method(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)), name, signature, codeSize));
        }
        return methods;
    }
}
