package com.tonic.api.widgets;

import com.tonic.Static;
import com.tonic.api.game.ClientScriptAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.game.VarAPI;
import com.tonic.data.settings.AbstractSetting;
import com.tonic.data.settings.ActionSetting;
import com.tonic.data.settings.ColourSetting;
import com.tonic.data.settings.DropdownOption;
import com.tonic.data.settings.DropdownSetting;
import com.tonic.data.settings.KeyBindingOption;
import com.tonic.data.settings.KeyBindingSetting;
import com.tonic.data.settings.NumberSetting;
import com.tonic.data.settings.SettingEntry;
import com.tonic.data.settings.SettingID;
import com.tonic.data.settings.SettingsMenu;
import com.tonic.data.settings.SliderSetting;
import com.tonic.data.settings.ToggleSetting;
import com.tonic.services.GameManager;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings API: reads settings and changes them through the settings menu, sending every step in the same tick.
 * Changes need the settings menu open, except accept aid and run, which use their side panel buttons; use the threaded
 * Settings class to open the menu and wait for the result.
 */
public class SettingsAPI
{
    private static final int SCRIPT_HIDDEN = 3955;
    private static final int SCRIPT_EDITABLE = 3957;
    private static final int OPTION_STRIDE = 3;
    private static final int OPTION_CLICKZONE = 2;
    private static final int VARP_MASTER_VOLUME = 3796;
    private static final int VARP_MUSIC_VOLUME = 168;
    private static final int VARP_SOUND_EFFECTS_VOLUME = 169;
    private static final int VARP_AREA_SOUNDS_VOLUME = 872;
    private static final int VARP_AREA_SOUNDS_SEPARATE = 5588;
    private static final int VARP_AREA_SOUNDS_SEPARATE_VOLUME = 5589;
    private static final int MAX_VOLUME = 100;
    private static int sentCategory = -1;
    private static int sentTick = -1;

    /**
     * Checks if the settings menu is open.
     * @return true if the settings menu is open
     */
    public static boolean isOpen()
    {
        return WidgetAPI.isVisible(InterfaceID.Settings.UNIVERSE);
    }

    /**
     * Opens the settings menu from the settings side panel.
     */
    public static void open()
    {
        WidgetAPI.interact(1, InterfaceID.SettingsSide.SETTINGS_OPEN, -1, -1);
    }

    /**
     * Closes the settings menu.
     */
    public static void close()
    {
        if (isOpen())
        {
            ClientScriptAPI.closeInterface();
        }
    }

    /**
     * Gets the menu tab currently shown.
     * @return the index of the tab
     */
    public static int getCategory()
    {
        return VarAPI.getVar(VarbitID.SETTINGS_CATEGORY);
    }

    /**
     * Gets where a setting is listed and what the cache says about it.
     * @param setting the setting
     * @return the entry
     */
    public static SettingEntry entry(AbstractSetting setting)
    {
        return SettingsMenu.entry(setting);
    }

    /**
     * Checks if the settings menu lists a setting right now. Settings only the official clients have are never listed.
     * @param setting the setting
     * @return true if the setting is listed
     */
    public static boolean isAvailable(AbstractSetting setting)
    {
        return runScript(SCRIPT_HIDDEN, entry(setting).getStructId()) == 0;
    }

    /**
     * Checks if the game allows a setting to be changed right now.
     * @param setting the setting
     * @return true if the setting can be changed
     */
    public static boolean isEditable(AbstractSetting setting)
    {
        return runScript(SCRIPT_EDITABLE, entry(setting).getStructId()) == 1;
    }

    /**
     * Gets the message the game shows when a setting cannot be changed.
     * @param setting the setting
     * @return the message, empty when there is none
     */
    public static String getBlockedMessage(AbstractSetting setting)
    {
        return entry(setting).getBlockedMessage();
    }

    /**
     * Checks if a toggle is on.
     * @param setting the toggle
     * @return true if the toggle is on
     */
    public static boolean isEnabled(ToggleSetting setting)
    {
        SettingEntry entry = entry(setting);
        int stored = stored(entry);
        return entry.isInverted() ? stored == 0 : stored == 1;
    }

    /**
     * Gets a slider's value: its step, or 0 to 100 for volumes.
     * @param setting the slider
     * @return the value
     * @throws UnsupportedOperationException for smooth sliders other than volumes
     */
    public static int get(SliderSetting setting)
    {
        SettingEntry entry = entry(setting);
        if (!entry.isSmooth())
        {
            return stored(entry);
        }
        return Math.max(0, Math.min(MAX_VOLUME, volume(setting)));
    }

    /**
     * Gets the option a dropdown is set to.
     * @param setting the dropdown
     * @return the option
     */
    public static DropdownOption get(DropdownSetting setting)
    {
        return new DropdownOption(setting, Math.max(0, stored(entry(setting))));
    }

    /**
     * Gets the key a key binding is set to.
     * @param setting the key binding
     * @return the key
     */
    public static KeyBindingOption get(KeyBindingSetting setting)
    {
        return new KeyBindingOption(Math.max(0, stored(entry(setting))));
    }

    /**
     * Gets a number setting's value.
     * @param setting the number setting
     * @return the value
     */
    public static int get(NumberSetting setting)
    {
        return stored(entry(setting));
    }

    /**
     * Gets a colour setting's colour.
     * @param setting the colour setting
     * @return the colour as 0xRRGGBB
     */
    public static int get(ColourSetting setting)
    {
        SettingEntry entry = entry(setting);
        int stored = stored(entry);
        return stored == -1 ? entry.getDefaultColour() : stored;
    }

    /**
     * Gets every option of a dropdown.
     * @param setting the dropdown
     * @return the options in dropdown order
     */
    public static List<DropdownOption> getOptions(DropdownSetting setting)
    {
        List<DropdownOption> options = new ArrayList<>();
        for (int value = 0; value < entry(setting).getOptions().size(); value++)
        {
            options.add(new DropdownOption(setting, value));
        }
        return options;
    }

    /**
     * Gets the label of a dropdown option.
     * @param option the option
     * @return the label, or null when the dropdown has no such option
     */
    public static String getLabel(DropdownOption option)
    {
        List<String> labels = entry(option.getSetting()).getOptions();
        return option.getValue() < labels.size() ? labels.get(option.getValue()) : null;
    }

    /**
     * Finds a dropdown option by its label, ignoring case.
     * @param setting the dropdown
     * @param label the label
     * @return the option, or null when there is none
     */
    public static DropdownOption getOption(DropdownSetting setting, String label)
    {
        List<String> labels = entry(setting).getOptions();
        for (int value = 0; value < labels.size(); value++)
        {
            if (labels.get(value).equalsIgnoreCase(label.trim()))
            {
                return new DropdownOption(setting, value);
            }
        }
        return null;
    }

    /**
     * Checks if a setting is changed from the settings side panel, without the settings menu.
     * @param setting the setting
     * @return true if the setting has its own side panel button
     */
    public static boolean isOnSidePanel(AbstractSetting setting)
    {
        return SettingID.Gameplay.ACCEPT_AID.equals(setting);
    }

    /**
     * Turns a toggle on or off. Accept aid uses its side panel button, every other toggle the settings menu.
     * @param setting the toggle
     * @param enabled true to turn it on
     * @return true if the change was sent or the toggle already was so
     */
    public static boolean set(ToggleSetting setting, boolean enabled)
    {
        SettingEntry entry = entry(setting);
        boolean sidePanel = isOnSidePanel(setting);
        if (sidePanel ? !(isEditable(setting) && isAvailable(setting)) : !canChange(setting))
        {
            return false;
        }
        if (isEnabled(setting) == enabled)
        {
            return true;
        }
        if (sidePanel)
        {
            WidgetAPI.interact(1, InterfaceID.SettingsSide.ACCEPTAID, -1, -1);
        }
        else
        {
            clickRow(entry);
        }
        return true;
    }

    /**
     * Turns run on or off from the settings side panel.
     * @param enabled true to turn run on
     * @return true if the change was sent or run already was so
     */
    public static boolean setRun(boolean enabled)
    {
        if (MovementAPI.isRunEnabled() == enabled)
        {
            return true;
        }
        WidgetAPI.interact(1, InterfaceID.SettingsSide.RUNMODE, -1, -1);
        return true;
    }

    /**
     * Sets a stepped slider to a step.
     * @param setting the slider
     * @param step the step, from 0
     * @return true if the change was sent or the slider already was there
     * @throws UnsupportedOperationException for smooth sliders, volumes included
     */
    public static boolean set(SliderSetting setting, int step)
    {
        SettingEntry entry = entry(setting);
        if (entry.isSmooth() || entry.getNotchStart() < 0)
        {
            throw new UnsupportedOperationException(setting + " is not a stepped slider");
        }
        if (step < 0 || step >= entry.getSteps())
        {
            throw new IllegalArgumentException("step must be between 0 and " + (entry.getSteps() - 1));
        }
        if (!canChange(setting))
        {
            return false;
        }
        if (get(setting) == step)
        {
            return true;
        }
        showCategory(entry);
        WidgetAPI.interact(1, InterfaceID.Settings.SETTINGS_SLIDERCLICKZONE, entry.getNotchStart() + step, -1);
        return true;
    }

    /**
     * Picks an option of a dropdown.
     * @param option the option
     * @return true if the change was sent or the dropdown already was so
     */
    public static boolean select(DropdownOption option)
    {
        DropdownSetting setting = option.getSetting();
        SettingEntry entry = entry(setting);
        if (option.getValue() >= entry.getOptions().size())
        {
            throw new IllegalArgumentException("option " + option.getValue() + " is not in " + setting);
        }
        if (!canChange(setting))
        {
            return false;
        }
        if (get(setting).equals(option))
        {
            return true;
        }
        clickRow(entry);
        clickOption(option.getValue());
        return true;
    }

    /**
     * Sets a key binding to a key.
     * @param setting the key binding
     * @param key the key
     * @return true if the change was sent or the binding already was so
     */
    public static boolean set(KeyBindingSetting setting, KeyBindingOption key)
    {
        SettingEntry entry = entry(setting);
        if (key.getValue() >= entry.getOptions().size())
        {
            throw new IllegalArgumentException("key " + key.getValue() + " is not in " + setting);
        }
        if (!canChange(setting))
        {
            return false;
        }
        if (get(setting).equals(key))
        {
            return true;
        }
        clickRow(entry);
        clickOption(key.getValue());
        return true;
    }

    /**
     * Enters a number setting's value.
     * @param setting the number setting
     * @param value the value
     * @return true if the change was sent or the setting already was so
     */
    public static boolean set(NumberSetting setting, int value)
    {
        if (value < 0)
        {
            throw new IllegalArgumentException("value cannot be negative");
        }
        SettingEntry entry = entry(setting);
        if (!canChange(setting))
        {
            return false;
        }
        if (get(setting) == value)
        {
            return true;
        }
        clickRow(entry);
        DialogueAPI.resumeNumericDialogue(value);
        return true;
    }

    /**
     * Picks a colour setting's colour.
     * @param setting the colour setting
     * @param rgb the colour as 0xRRGGBB
     * @return true if the change was sent or the setting already was so
     */
    public static boolean set(ColourSetting setting, int rgb)
    {
        if ((rgb & ~0xFFFFFF) != 0)
        {
            throw new IllegalArgumentException("rgb must be 0xRRGGBB");
        }
        SettingEntry entry = entry(setting);
        if (!canChange(setting))
        {
            return false;
        }
        if (get(setting) == rgb)
        {
            return true;
        }
        clickRow(entry);
        DialogueAPI.resumeNumericDialogue(rgb);
        return true;
    }

    /**
     * Presses an action button. A confirmation it asks for is left open; the threaded Settings class answers it.
     * @param setting the action
     * @return true if the press was sent
     */
    public static boolean run(ActionSetting setting)
    {
        SettingEntry entry = entry(setting);
        if (!canChange(setting))
        {
            return false;
        }
        clickRow(entry);
        return true;
    }

    private static boolean canChange(AbstractSetting setting)
    {
        return isOpen() && isEditable(setting) && isAvailable(setting);
    }

    private static synchronized void showCategory(SettingEntry entry)
    {
        int tick = GameManager.getTickCount();
        int shown = sentTick == tick ? sentCategory : getCategory();
        if (shown != entry.getCategory())
        {
            WidgetAPI.interact(1, InterfaceID.Settings.CATEGORIES_CLICKZONE, entry.getCategory(), -1);
        }
        sentCategory = entry.getCategory();
        sentTick = tick;
    }

    private static void clickRow(SettingEntry entry)
    {
        showCategory(entry);
        WidgetAPI.interact(1, InterfaceID.Settings.SETTINGS_CLICKZONE, entry.getRow(), -1);
    }

    private static void clickOption(int value)
    {
        WidgetAPI.interact(1, InterfaceID.Settings.DROPDOWN_BUTTONS, value * OPTION_STRIDE + OPTION_CLICKZONE, -1);
    }

    private static int stored(SettingEntry entry)
    {
        return runScript(entry.getKind().getGetterScript(), entry.getSettingId());
    }

    private static int volume(SliderSetting setting)
    {
        if (setting.equals(SettingID.Audio.MASTER_VOLUME))
        {
            return VarAPI.getVarp(VARP_MASTER_VOLUME);
        }
        if (setting.equals(SettingID.Audio.MUSIC_VOLUME))
        {
            return VarAPI.getVarp(VARP_MUSIC_VOLUME);
        }
        if (setting.equals(SettingID.Audio.SOUND_EFFECTS_VOLUME))
        {
            return VarAPI.getVarp(VARP_SOUND_EFFECTS_VOLUME);
        }
        if (setting.equals(SettingID.Audio.AREA_SOUNDS_VOLUME))
        {
            return VarAPI.getVarp(VARP_AREA_SOUNDS_SEPARATE) == 1 ? VarAPI.getVarp(VARP_AREA_SOUNDS_SEPARATE_VOLUME) : VarAPI.getVarp(VARP_AREA_SOUNDS_VOLUME);
        }
        throw new UnsupportedOperationException(setting + " is a smooth slider that cannot be read");
    }

    private static int runScript(int script, int argument)
    {
        Client client = Static.getClient();
        return Static.invoke(() -> {
            ClientScriptAPI.runScript(script, argument);
            return client.getIntStack()[client.getIntStackSize() - 1];
        });
    }
}
