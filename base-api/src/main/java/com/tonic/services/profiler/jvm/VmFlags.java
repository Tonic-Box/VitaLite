package com.tonic.services.profiler.jvm;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the output of the VM.flags -all diagnostic command
 */
public final class VmFlags {
    private static final Pattern LINE = Pattern.compile("^\\s*(\\S+)\\s+(\\S+)\\s+:?=\\s*(.*?)\\s*\\{([^}]*)\\}\\s*(?:\\{([^}]*)\\})?\\s*$");

    private VmFlags() {
    }

    /**
     * One JVM flag
     */
    public static final class Flag {
        public final String type;
        public final String name;
        public final String value;
        public final String kinds;
        public final String origin;

        /**
         * Creates the flag
         * @param type its type, such as bool or intx
         * @param name its name
         * @param value its current value, empty when unset
         * @param kinds what kind of flag it is, such as product or manageable
         * @param origin where its value came from, such as default or command line
         */
        public Flag(String type, String name, String value, String kinds, String origin) {
            this.type = type;
            this.name = name;
            this.value = value;
            this.kinds = kinds;
            this.origin = origin;
        }

        /**
         * Checks whether the flag still has its default value
         * @return true when its origin is default
         */
        public boolean isDefault() {
            return "default".equals(origin);
        }
    }

    /**
     * Parses every flag line, skipping lines that are not flags
     * @param output the command's output
     * @return the flags in output order
     */
    public static List<Flag> parse(String output) {
        List<Flag> flags = new ArrayList<>();
        for (String line : output.split("\\R")) {
            Matcher matcher = LINE.matcher(line);
            if (matcher.matches()) {
                flags.add(new Flag(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4).trim(), matcher.group(5) == null ? "" : matcher.group(5).trim()));
            }
        }
        return flags;
    }
}
