package com.tonic.services.profiler;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Runs HotSpot diagnostic commands (the jcmd set) in-process through the DiagnosticCommand bean, which needs no flags
 */
public final class DiagnosticCommands {
    private static final ObjectName BEAN = beanName();
    private static final String[] SIGNATURE = {String[].class.getName()};

    private DiagnosticCommands() {
    }

    /**
     * Runs a diagnostic command
     * @param operation the bean operation, such as compilerCodelist or gcClassHistogram
     * @param arguments the command's arguments, such as -all
     * @return the command's text output
     * @throws IllegalStateException if the command is unknown or fails
     */
    public static String run(String operation, String... arguments) {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            return (String) server.invoke(BEAN, operation, new Object[]{arguments}, SIGNATURE);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException(operation + " failed: " + cause.getMessage(), e);
        }
    }

    private static ObjectName beanName() {
        try {
            return new ObjectName("com.sun.management:type=DiagnosticCommand");
        } catch (MalformedObjectNameException e) {
            throw new IllegalStateException(e);
        }
    }
}
