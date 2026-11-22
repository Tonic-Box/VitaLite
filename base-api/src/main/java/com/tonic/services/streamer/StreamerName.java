package com.tonic.services.streamer;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The name the streamer mirror shows in place of the player's, the matching that finds the player's name in text, and where the client reports drawing such text so the mirror can show the name replaced. Names match whatever their case, with space, no-break space, '-' and '_' alike, and only as whole words.
 */
public final class StreamerName
{
    /**
     * The name shown while the setting is blank.
     */
    public static final String DEFAULT = "StreamerMode";

    /**
     * The longest name the setting takes, as in the game.
     */
    public static final int MAX_LENGTH = 12;

    /**
     * Draw the text as it is, for whichever views the draw is for.
     */
    public static final int DRAW = 0;

    /**
     * Draw the text for the real view only, then the replaced text for the mirror only.
     */
    public static final int SPLIT = 1;

    /**
     * Draw the replaced text only, as the draw is for the mirror only.
     */
    public static final int REPLACED = 2;

    private static final int MIN_LOGIN_TAIL = 3;
    private static final int BOX_PAD = 16;
    private static final int BAND_ABOVE = 24;
    private static final int BAND_HEIGHT = 36;
    private static final int WIDE = Integer.MAX_VALUE / 2;
    private static final String CURSOR = "|";
    private static final String[] NONE = new String[0];

    private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private static volatile String fake = DEFAULT;
    private static volatile String[] names = NONE;
    private static volatile String login;
    private static volatile Handler handler;
    private static String shown;

    private StreamerName()
    {
    }

    /**
     * @param name a name the setting may take
     * @return whether it keeps to the game's rules: at most 12 letters, digits, spaces, '-' or '_'
     */
    public static boolean isValid(String name)
    {
        if (name == null || name.length() > MAX_LENGTH)
        {
            return false;
        }
        for (int i = 0; i < name.length(); i++)
        {
            char c = name.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == ' ' || c == '-' || c == '_'))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Sets the name the mirror shows, ignoring one that breaks the game's rules.
     *
     * @param name the setting's text, blank or null for the default
     */
    public static void setFake(String name)
    {
        String trimmed = name == null ? "" : name.trim();
        if (!isValid(trimmed))
        {
            return;
        }
        String effective = trimmed.isEmpty() ? DEFAULT : trimmed;
        if (!effective.equals(fake))
        {
            fake = effective;
            changed();
        }
    }

    /**
     * @return the name the mirror shows
     */
    public static String fake()
    {
        return fake;
    }

    /**
     * Sets the player's names the mirror replaces.
     *
     * @param loginName the name typed into the login screen, or null
     * @param playerNames the player's names, nulls skipped
     */
    public static void setNames(String loginName, String... playerNames)
    {
        String[] kept = Arrays.stream(playerNames).filter(n -> n != null && !n.isBlank()).distinct().toArray(String[]::new);
        String trimmedLogin = loginName == null || loginName.isBlank() ? null : loginName;
        if (!Arrays.equals(kept, names) || !Objects.equals(trimmedLogin, login))
        {
            names = kept;
            login = trimmedLogin;
            changed();
        }
    }

    /**
     * @return whether any of the player's names are known, so text may need replacing
     */
    public static boolean hasNames()
    {
        return names.length > 0;
    }

    /**
     * Runs a task whenever the shown name or the player's names change.
     *
     * @param listener the task, run on the thread that made the change
     */
    public static void addListener(Runnable listener)
    {
        listeners.add(listener);
    }

    /**
     * Replaces each of the player's names in a text with the shown name.
     *
     * @param text the text
     * @return the text with the names replaced, or null when it holds none
     */
    public static String replace(String text)
    {
        if (text == null || text.isEmpty())
        {
            return null;
        }
        String[] current = names;
        String result = null;
        for (String name : current)
        {
            String replaced = replaceName(result == null ? text : result, name, fake);
            if (replaced != null)
            {
                result = replaced;
            }
        }
        return result;
    }

    /**
     * Replaces the player's names in a login screen text, and the login name too, which the screen cuts from the front while it is too long to fit and may follow with a flashing cursor.
     *
     * @param text the text
     * @return the text with the names replaced, or null when it holds none
     */
    public static String replaceLogin(String text)
    {
        String replaced = replace(text);
        if (replaced != null || text == null)
        {
            return replaced;
        }
        String name = login;
        if (name == null)
        {
            return null;
        }
        boolean cursor = text.endsWith(CURSOR);
        String typed = cursor ? text.substring(0, text.length() - CURSOR.length()) : text;
        if (typed.length() < Math.min(MIN_LOGIN_TAIL, name.length()) || typed.length() > name.length() || !name.regionMatches(true, name.length() - typed.length(), typed, 0, typed.length()))
        {
            return null;
        }
        return cursor ? fake + CURSOR : fake;
    }

    /**
     * @param textHandler the handler that keeps the views apart while the client draws text, or null to stop
     */
    public static void setHandler(Handler textHandler)
    {
        handler = textHandler;
    }

    /**
     * Called by the client before it draws a line of text at a baseline, while streamer mode is on.
     *
     * @param text the text
     * @param y the baseline, down the canvas
     * @return how to draw it: DRAW, SPLIT or REPLACED
     */
    public static int begin(String text, int y)
    {
        return begin(replace(text), 0, y - BAND_ABOVE, WIDE, BAND_HEIGHT);
    }

    /**
     * Called by the client before it draws an interface component's text in its box, while streamer mode is on.
     *
     * @param text the text
     * @param x the box's left, across the canvas
     * @param y the box's top, down the canvas
     * @param width the box's width
     * @param height the box's height
     * @return how to draw it: DRAW, SPLIT or REPLACED
     */
    public static int beginBox(String text, int x, int y, int width, int height)
    {
        return begin(replace(text), x - BOX_PAD, y - BOX_PAD, width + 2 * BOX_PAD, height + 2 * BOX_PAD);
    }

    /**
     * Called by the client before it draws a line of the login screen at a baseline, while streamer mode is on. VitaLite's welcome line is replaced by the client's own.
     *
     * @param text the text
     * @param y the baseline, down the canvas
     * @return how to draw it: DRAW, SPLIT or REPLACED
     */
    public static int beginLogin(String text, int y)
    {
        String welcome = Streamer.stockWelcome(text);
        return begin(welcome != null ? welcome : replaceLogin(text), 0, y - BAND_ABOVE, WIDE, BAND_HEIGHT);
    }

    /**
     * @return the replaced text of the draw begun last
     */
    public static String text()
    {
        return shown;
    }

    /**
     * Called by the client after it draws the text for the real view, before it draws the replaced text for the mirror.
     */
    public static void mirror()
    {
        Handler current = handler;
        if (current != null)
        {
            current.mirror();
        }
    }

    /**
     * Called by the client once both draws are done.
     */
    public static void done()
    {
        Handler current = handler;
        if (current != null)
        {
            current.done();
        }
    }

    private static int begin(String replaced, int x, int y, int width, int height)
    {
        Handler current = handler;
        if (replaced == null || current == null)
        {
            return DRAW;
        }
        shown = replaced;
        int how = current.openDraw();
        if (how == SPLIT)
        {
            current.real(x, y, width, height);
        }
        return how;
    }

    static String replaceName(String text, String name, String with)
    {
        int length = name.length();
        if (length == 0 || text.length() < length)
        {
            return null;
        }
        StringBuilder built = null;
        int copied = 0;
        int at = 0;
        while (at <= text.length() - length)
        {
            if (matchesAt(text, at, name) && wordEdge(text, at - 1) && wordEdge(text, at + length))
            {
                if (built == null)
                {
                    built = new StringBuilder(text.length() + with.length());
                }
                built.append(text, copied, at).append(with);
                at += length;
                copied = at;
            }
            else
            {
                at++;
            }
        }
        if (built == null)
        {
            return null;
        }
        return built.append(text, copied, text.length()).toString();
    }

    private static boolean matchesAt(String text, int at, String name)
    {
        for (int i = 0; i < name.length(); i++)
        {
            char a = text.charAt(at + i);
            char b = name.charAt(i);
            if (a != b && !(spaceLike(a) && spaceLike(b)) && Character.toLowerCase(a) != Character.toLowerCase(b))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean wordEdge(String text, int index)
    {
        return index < 0 || index >= text.length() || !Character.isLetterOrDigit(text.charAt(index));
    }

    private static boolean spaceLike(char c)
    {
        return c == ' ' || c == '\u00A0' || c == '-' || c == '_';
    }

    private static void changed()
    {
        for (Runnable listener : listeners)
        {
            listener.run();
        }
    }

    /**
     * What the streamer mirror does as the client draws text with the player's name.
     */
    public interface Handler
    {
        /**
         * @return how text is drawn now: DRAW while a draw for the real view only is open, REPLACED while one for the mirror only is open, else SPLIT
         */
        int openDraw();

        /**
         * Starts the text's draw for the real view only.
         *
         * @param x the left of the area it may cover
         * @param y the top of the area it may cover
         * @param width the width of the area
         * @param height the height of the area
         */
        void real(int x, int y, int width, int height);

        /**
         * Starts the replaced text's draw for the mirror only.
         */
        void mirror();

        /**
         * Ends the mirror's draw.
         */
        void done();
    }
}
