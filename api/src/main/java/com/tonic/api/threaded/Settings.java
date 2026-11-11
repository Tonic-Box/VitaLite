package com.tonic.api.threaded;

import com.tonic.api.game.GameAPI;
import com.tonic.api.game.MovementAPI;
import com.tonic.api.widgets.DialogueAPI;
import com.tonic.api.widgets.SettingsAPI;
import com.tonic.api.widgets.TabsAPI;
import com.tonic.data.Tab;
import com.tonic.data.settings.AbstractSetting;
import com.tonic.data.settings.ActionSetting;
import com.tonic.data.settings.ColourSetting;
import com.tonic.data.settings.DropdownOption;
import com.tonic.data.settings.KeyBindingOption;
import com.tonic.data.settings.KeyBindingSetting;
import com.tonic.data.settings.NumberSetting;
import com.tonic.data.settings.SettingResult;
import com.tonic.data.settings.SliderSetting;
import com.tonic.data.settings.ToggleSetting;

import java.util.function.BooleanSupplier;

/**
 * Changes settings through the settings menu, opening it when needed and waiting until the game shows the new value.
 * The menu is left open. Accept aid and run use their side panel buttons and never open the menu.
 */
public class Settings
{
    private static final int OPEN_TICKS = 5;
    private static final int CONFIRM_TICKS = 7;
    private static final int TRIES = 2;
    private static final String CONFIRM = "Yes";

    /**
     * Opens the settings menu and waits for it.
     * @return true if the settings menu is open
     */
    public static boolean open()
    {
        if (SettingsAPI.isOpen())
        {
            return true;
        }
        if (!TabsAPI.isOpen(Tab.SETTINGS_TAB))
        {
            TabsAPI.open(Tab.SETTINGS_TAB);
            Delays.tick();
        }
        SettingsAPI.open();
        return Delays.waitUntil(SettingsAPI::isOpen, OPEN_TICKS);
    }

    /**
     * Turns a toggle on or off and waits for it.
     * @param setting the toggle
     * @param enabled true to turn it on
     * @return how the change ended
     */
    public static SettingResult set(ToggleSetting setting, boolean enabled)
    {
        return change(setting, () -> SettingsAPI.set(setting, enabled), () -> SettingsAPI.isEnabled(setting) == enabled);
    }

    /**
     * Turns run on or off from the settings side panel and waits for it.
     * @param enabled true to turn run on
     * @return how the change ended
     */
    public static SettingResult setRun(boolean enabled)
    {
        if (!GameAPI.isLoggedIn())
        {
            return SettingResult.NOT_IN_GAME;
        }
        for (int attempt = 0; attempt < TRIES; attempt++)
        {
            if (MovementAPI.isRunEnabled() == enabled)
            {
                return SettingResult.DONE;
            }
            SettingsAPI.setRun(enabled);
            if (Delays.waitUntil(() -> MovementAPI.isRunEnabled() == enabled, CONFIRM_TICKS))
            {
                return SettingResult.DONE;
            }
        }
        return SettingResult.NOT_CONFIRMED;
    }

    /**
     * Sets a stepped slider to a step and waits for it.
     * @param setting the slider
     * @param step the step, from 0
     * @return how the change ended
     */
    public static SettingResult set(SliderSetting setting, int step)
    {
        return change(setting, () -> SettingsAPI.set(setting, step), () -> SettingsAPI.get(setting) == step);
    }

    /**
     * Picks an option of a dropdown and waits for it.
     * @param option the option
     * @return how the change ended
     */
    public static SettingResult select(DropdownOption option)
    {
        return change(option.getSetting(), () -> SettingsAPI.select(option), () -> SettingsAPI.get(option.getSetting()).equals(option));
    }

    /**
     * Sets a key binding to a key and waits for it.
     * @param setting the key binding
     * @param key the key
     * @return how the change ended
     */
    public static SettingResult set(KeyBindingSetting setting, KeyBindingOption key)
    {
        return change(setting, () -> SettingsAPI.set(setting, key), () -> SettingsAPI.get(setting).equals(key));
    }

    /**
     * Enters a number setting's value and waits for it.
     * @param setting the number setting
     * @param value the value
     * @return how the change ended
     */
    public static SettingResult set(NumberSetting setting, int value)
    {
        return change(setting, () -> SettingsAPI.set(setting, value), () -> SettingsAPI.get(setting) == value);
    }

    /**
     * Picks a colour setting's colour and waits for it.
     * @param setting the colour setting
     * @param rgb the colour as 0xRRGGBB
     * @return how the change ended
     */
    public static SettingResult set(ColourSetting setting, int rgb)
    {
        return change(setting, () -> SettingsAPI.set(setting, rgb), () -> SettingsAPI.get(setting) == rgb);
    }

    /**
     * Presses an action button and answers its confirmation, if it asks one.
     * @param setting the action
     * @return how the press ended
     */
    public static SettingResult run(ActionSetting setting)
    {
        SettingResult ready = prepare(setting);
        if (ready != null)
        {
            return ready;
        }
        if (!SettingsAPI.run(setting))
        {
            return refused(setting);
        }
        if (Delays.waitUntil(() -> DialogueAPI.optionPresent(CONFIRM), CONFIRM_TICKS))
        {
            DialogueAPI.selectOption(CONFIRM);
        }
        return SettingResult.DONE;
    }

    private static SettingResult change(AbstractSetting setting, BooleanSupplier send, BooleanSupplier done)
    {
        SettingResult ready = prepare(setting);
        if (ready != null)
        {
            return ready;
        }
        for (int attempt = 0; attempt < TRIES; attempt++)
        {
            if (done.getAsBoolean())
            {
                return SettingResult.DONE;
            }
            if (!send.getAsBoolean())
            {
                return refused(setting);
            }
            if (Delays.waitUntil(done::getAsBoolean, CONFIRM_TICKS))
            {
                return SettingResult.DONE;
            }
        }
        return SettingResult.NOT_CONFIRMED;
    }

    private static SettingResult prepare(AbstractSetting setting)
    {
        if (!GameAPI.isLoggedIn())
        {
            return SettingResult.NOT_IN_GAME;
        }
        if (!SettingsAPI.isOnSidePanel(setting) && !open())
        {
            return SettingResult.NOT_OPEN;
        }
        if (!SettingsAPI.isEditable(setting))
        {
            return SettingResult.BLOCKED;
        }
        if (!SettingsAPI.isAvailable(setting))
        {
            return SettingResult.NOT_AVAILABLE;
        }
        return null;
    }

    private static SettingResult refused(AbstractSetting setting)
    {
        if (!SettingsAPI.isOnSidePanel(setting) && !SettingsAPI.isOpen())
        {
            return SettingResult.NOT_OPEN;
        }
        return SettingsAPI.isEditable(setting) ? SettingResult.NOT_AVAILABLE : SettingResult.BLOCKED;
    }
}
