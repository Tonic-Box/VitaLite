package com.tonic.data.settings;

import com.tonic.Static;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.StructComposition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reads the settings menu from the client's cache the way the menu's own scripts lay it out.
 */
public final class SettingsMenu
{
    private static final int CATEGORIES_ENUM = 422;
    private static final int CATEGORY_INDEX = 743;
    private static final int CATEGORY_ENTRIES = 745;
    private static final int SETTING_ID = 1077;
    private static final int KIND = 1078;
    private static final int INVERTED = 1084;
    private static final int NAME = 1086;
    private static final int OPTIONS = 1091;
    private static final int DESCRIPTION = 1096;
    private static final int STEPS = 1101;
    private static final int NOTCHED = 1105;
    private static final int SMOOTH = 1107;
    private static final int BLOCKED_MESSAGE = 1116;
    private static final int DEFAULT_COLOUR = 1230;
    private static final Pattern TAG = Pattern.compile("<[^>]*>");
    private static Map<Integer, SettingEntry> entries = new HashMap<>();

    private SettingsMenu()
    {
    }

    /**
     * Finds where a setting is listed.
     * @param setting the setting
     * @return the entry
     * @throws IllegalArgumentException if the menu does not list the setting as that kind
     */
    public static SettingEntry entry(AbstractSetting setting)
    {
        if (setting == null)
        {
            throw new IllegalArgumentException("setting cannot be null");
        }
        SettingEntry entry = lookup(setting.getStructId());
        if (entry == null || entry.getKind() != setting.getKind())
        {
            throw new IllegalArgumentException("the settings menu does not list " + setting);
        }
        return entry;
    }

    private static synchronized SettingEntry lookup(int structId)
    {
        SettingEntry entry = entries.get(structId);
        if (entry == null)
        {
            entries = Static.invoke(SettingsMenu::read);
            entry = entries.get(structId);
        }
        return entry;
    }

    private static Map<Integer, SettingEntry> read()
    {
        Client client = Static.getClient();
        Map<Integer, SettingEntry> read = new HashMap<>();
        EnumComposition categories = client.getEnum(CATEGORIES_ENUM);
        for (int position = 0; position < categories.size(); position++)
        {
            StructComposition category = client.getStructComposition(categories.getIntValue(position));
            if (category == null)
            {
                continue;
            }

            int index = category.getIntValue(CATEGORY_INDEX);
            EnumComposition rows = client.getEnum(category.getIntValue(CATEGORY_ENTRIES));
            int row = 0;
            int notch = 0;
            for (int key = 0; key < rows.size(); key++)
            {
                int structId = rows.getIntValue(key);
                if (structId == -1)
                {
                    continue;
                }

                StructComposition struct = client.getStructComposition(structId);
                SettingKind kind = struct == null ? null : SettingKind.of(struct.getIntValue(KIND));
                int steps = struct == null ? 0 : struct.getIntValue(STEPS);
                boolean notched = kind == SettingKind.SLIDER && struct.getIntValue(NOTCHED) == 1;
                if (kind != null && !read.containsKey(structId))
                {
                    read.put(structId, new SettingEntry(structId, kind, struct.getIntValue(SETTING_ID), index, row, text(struct, NAME), text(struct, DESCRIPTION), text(struct, BLOCKED_MESSAGE), struct.getIntValue(INVERTED) == 1, options(client, kind, struct), steps, notched ? notch : -1, struct.getIntValue(SMOOTH) == 1, struct.getIntValue(DEFAULT_COLOUR)));
                }
                if (notched)
                {
                    notch += steps;
                }
                row++;
            }
        }
        return read;
    }

    private static List<String> options(Client client, SettingKind kind, StructComposition struct)
    {
        List<String> options = new ArrayList<>();
        if (kind != SettingKind.DROPDOWN && kind != SettingKind.KEY_BINDING)
        {
            return options;
        }
        int optionEnum = struct.getIntValue(OPTIONS);
        if (optionEnum == -1)
        {
            return options;
        }
        EnumComposition values = client.getEnum(optionEnum);
        for (int value = 0; value < values.size(); value++)
        {
            options.add(strip(values.getStringValue(value)));
        }
        return options;
    }

    private static String text(StructComposition struct, int param)
    {
        String value = strip(struct.getStringValue(param));
        return value == null ? "" : value;
    }

    private static String strip(String text)
    {
        return text == null ? null : TAG.matcher(text).replaceAll("");
    }
}
