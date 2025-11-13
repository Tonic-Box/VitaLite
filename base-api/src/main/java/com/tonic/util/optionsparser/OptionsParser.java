package com.tonic.util.optionsparser;

import com.tonic.util.optionsparser.annotations.CLIArgument;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptionsParser {

    /**
     * Parses the command line arguments and sets the fields of this class accordingly.
     * @param args
     * the command line arguments
     */
    public String[] parse(String[] args) {
        Map<String, Field> annotatedFields = new HashMap<>();
        List<String> passThruArgs = new ArrayList<>();

        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields)
        {
            CLIArgument annotation = field.getAnnotation(CLIArgument.class);
            if (annotation != null)
            {
                annotatedFields.put(annotation.name(), field);
            }
        }

        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];

            if (arg.equals("-help"))
            {
                help();
                System.exit(0);
            }

            if (arg.startsWith("-"))
            {
                if ((i + 1) < args.length && !args[i + 1].startsWith("-"))
                {
                    String argName = arg.substring(2);
                    Field field = annotatedFields.get(argName);
                    if(field == null)
                    {
                        passThruArgs.add(args[i]);
                        continue;
                    }
                    String value = args[++i];
                    setFieldValue(field, value);
                }
                else
                {
                    String argName = arg.substring(1);
                    Field field = annotatedFields.get(argName);
                    if (field == null)
                    {
                        passThruArgs.add(args[i]);
                        continue;
                    }
                    setFieldValue(field, "true");
                }
            }
        }
        for (String arg : passThruArgs)
        {
            System.out.println("Passing to RL: " + arg);
        }
        return passThruArgs.toArray(new String[0]);
    }

    public void help()
    {
        System.out.println("Usage: java -jar deobber.jar [options]");
        System.out.println("Options:");
        for (Field field : this.getClass().getDeclaredFields())
        {
            boolean isBooleanField = field.getType().equals(boolean.class) || field.getType().equals(Boolean.class);
            CLIArgument annotation = field.getAnnotation(CLIArgument.class);
            if (annotation != null && !annotation.description().isBlank())
            {
                System.out.println("  " + (isBooleanField ? "-" : "--") + annotation.name() + ": " + annotation.description());
            }
        }
    }

    /**
     * Helper method to set a field's value based on its type.
     */
    private void setFieldValue(Field field, String value) {
        try {
            field.setAccessible(true);
            Class<?> type = field.getType();

            if (type.equals(String.class))
            {
                field.set(this, value);
            }
            else if (type.equals(int.class) || type.equals(Integer.class))
            {
                field.set(this, Integer.valueOf(value));
            }
            else if (type.equals(boolean.class) || type.equals(Boolean.class))
            {
                field.set(this, Boolean.valueOf(value));
            }
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }
}
