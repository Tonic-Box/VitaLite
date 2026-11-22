package com.tonic.services.streamer;

import com.google.common.collect.MapMaker;
import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.TMenu;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The right-click menu and mouseover text as the mirror shows them, with the streamer name in place of the player's: an entry is not stock when content that is not stock created it, when its action is not, when its action belongs to an overlay, InfoBox or world map point the mirror hides, or when every entry of its submenu is not stock. While the menu holds entries that are not stock, the client's own draw runs a second time for the mirror with only the stock entries, laid out where the client would lay them out, and the menu is put back exactly as it was. Runs on the client thread only.
 */
final class MirrorMenu implements StreamerMenu.Handler
{
    private static final int TEXT_ABOVE = 15;
    private static final int TEXT_HEIGHT = 21;
    private static final String MORE_OPTIONS = "/ ";

    private static final ClassValue<Field[]> CAPTURED = new ClassValue<>()
    {
        @Override
        protected Field[] computeValue(Class<?> type)
        {
            List<Field> fields = new ArrayList<>();
            for (Field field : type.getDeclaredFields())
            {
                if (!Modifier.isStatic(field.getModifiers()) && isOwner(field.getType()))
                {
                    try
                    {
                        field.setAccessible(true);
                        fields.add(field);
                    }
                    catch (RuntimeException ignored)
                    {
                    }
                }
            }
            return fields.toArray(new Field[0]);
        }
    };

    private static final MirrorMenu INSTANCE = new MirrorMenu();
    private static final Set<Object> CREATED_NOT_STOCK = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

    private final Map<TMenu, Point> openedAt = new MapMaker().weakKeys().makeMap();
    private boolean drawFailed;
    private static volatile boolean namesChanged;
    private static Menu differsMenu;
    private static int differsCycle;
    private static boolean differsResult;
    private boolean mirroring;
    private boolean hidingMenu;
    private Snapshot textState;
    private final Rectangle textArea = new Rectangle();
    private int realCount;
    private int mirrorCount;
    private String realTop;
    private String mirrorTop;

    private MirrorMenu()
    {
    }

    /**
     * Starts handling the client's menu draws for the mirror.
     */
    static void install()
    {
        StreamerMenu.setHandler(INSTANCE);
        StreamerName.addListener(() -> namesChanged = true);
        Static.getInjector().getInstance(EventBus.class).register(MenuEntryAdded.class, event -> CREATED_NOT_STOCK.remove(event.getMenuEntry()), 0);
    }

    /**
     * Forgets which entries content that is not stock created, as streamer mode starts or stops.
     */
    static void reset()
    {
        CREATED_NOT_STOCK.clear();
    }

    /**
     * Runs something with the client's menu as the mirror shows it, holding only its stock entries with the streamer name in place of the player's, then puts the menu back exactly as it was.
     *
     * @param action what to run
     */
    static void withStockMenu(Runnable action)
    {
        Menu menu = menu();
        if (differs(menu))
        {
            withStock(menu, action);
        }
    }

    /**
     * @return whether the mirror shows the client's menu differently: without entries that are not stock, or with the streamer name in place of the player's
     */
    static boolean menuDiffers()
    {
        return differs(menu());
    }

    @Override
    public void created(Object entry)
    {
        Class<?> creator = StockContent.caller(type -> type.getPackageName().isEmpty());
        if (creator == null || StockContent.isStock(creator))
        {
            CREATED_NOT_STOCK.remove(entry);
        }
        else
        {
            CREATED_NOT_STOCK.add(entry);
        }
    }

    @Override
    public void drawing(TMenu menu)
    {
        if (mirroring)
        {
            return;
        }
        hidingMenu = differs((Menu) menu);
        if (hidingMenu)
        {
            MirrorHiding.hideFrame(null);
        }
    }

    @Override
    public void drawn(TMenu menu)
    {
        if (mirroring || !hidingMenu)
        {
            return;
        }
        hidingMenu = false;
        MirrorHiding.end();
        Menu real = (Menu) menu;
        Point opened = openedAt.get(menu);
        Point at = opened != null ? opened : new Point(real.getMenuX() + real.getMenuWidth() / 2, real.getMenuY());
        mirroring = true;
        try
        {
            withStock(real, () ->
            {
                menu.layoutMenu(at.x, at.y);
                MirrorHiding.mirrorFrame(null);
                menu.drawMenu();
            });
        }
        catch (RuntimeException e)
        {
            if (!drawFailed)
            {
                drawFailed = true;
                Logger.error("Streamer mirror could not draw the menu for the mirror: " + e);
            }
        }
        finally
        {
            MirrorHiding.end();
            mirroring = false;
        }
    }

    @Override
    public void laidOut(TMenu menu, int x, int y)
    {
        if (!mirroring)
        {
            openedAt.put(menu, new Point(x, y));
        }
    }

    @Override
    public boolean textBegin(String text, int x, int y)
    {
        if (mirroring || textState != null)
        {
            return false;
        }
        if (!differs(menu()))
        {
            return false;
        }
        textArea.setBounds(x - 2, y - TEXT_ABOVE, Integer.MAX_VALUE / 2, TEXT_HEIGHT);
        MirrorHiding.hideFrame(textArea);
        return true;
    }

    @Override
    public boolean textSwap()
    {
        MirrorHiding.end();
        Menu menu = menu();
        MenuEntry[] real = menu.getMenuEntries();
        realCount = real.length;
        realTop = topText(real);
        textState = Snapshot.of(menu);
        if (!keepStock(menu))
        {
            return false;
        }
        nameEntries(menu);
        MenuEntry[] mirror = menu.getMenuEntries();
        mirrorCount = mirror.length;
        mirrorTop = topText(mirror);
        return true;
    }

    @Override
    public String textMirror(String text)
    {
        String mirror = mirrorText(text, realTop, mirrorTop, realCount, mirrorCount);
        if (mirror != null)
        {
            String named = StreamerName.replace(mirror);
            if (named != null)
            {
                mirror = named;
            }
            MirrorHiding.mirrorFrame(textArea);
        }
        return mirror;
    }

    @Override
    public void textDone()
    {
        MirrorHiding.end();
        if (textState != null)
        {
            textState.restore();
            textState = null;
        }
    }

    /**
     * Works out the mouseover text a menu of only stock entries gives, from the text the real menu gave.
     *
     * @param text the real text
     * @param realTop the real top entry's text
     * @param mirrorTop the stock top entry's text
     * @param realEntries how many entries the real menu holds
     * @param mirrorEntries how many entries the stock menu holds
     * @return the mirror's text, the real text when it is not one the menu built, or null for none
     */
    private static String mirrorText(String text, String realTop, String mirrorTop, int realEntries, int mirrorEntries)
    {
        if (mirrorEntries < 2)
        {
            return null;
        }
        if (realTop == null || !text.startsWith(realTop))
        {
            return text;
        }
        String suffix = text.substring(realTop.length());
        if (mirrorEntries <= 2)
        {
            return mirrorTop;
        }
        String count = MORE_OPTIONS + (realEntries - 2);
        int at = suffix.lastIndexOf(count);
        if (realEntries <= 2 || at < 0)
        {
            return text;
        }
        return mirrorTop + suffix.substring(0, at) + MORE_OPTIONS + (mirrorEntries - 2) + suffix.substring(at + count.length());
    }

    private static Menu menu()
    {
        return Static.<Client>getClient().getMenu();
    }

    private static void withStock(Menu menu, Runnable action)
    {
        Snapshot state = Snapshot.of(menu);
        try
        {
            if (keepStock(menu))
            {
                nameEntries(menu);
                action.run();
            }
        }
        finally
        {
            state.restore();
        }
    }

    private static String topText(MenuEntry[] entries)
    {
        if (entries.length == 0)
        {
            return "";
        }
        MenuEntry top = entries[entries.length - 1];
        String target = top.getTarget();
        return target == null || target.isEmpty() ? top.getOption() : top.getOption() + " " + target;
    }

    private static boolean differs(Menu menu)
    {
        int cycle = Static.<Client>getClient().getGameCycle();
        if (menu != differsMenu || cycle != differsCycle || namesChanged)
        {
            namesChanged = false;
            differsMenu = menu;
            differsCycle = cycle;
            differsResult = differsFromStock(menu, StreamerName.hasNames());
        }
        return differsResult;
    }

    private static boolean differsFromStock(Menu menu, boolean names)
    {
        for (MenuEntry entry : menu.getMenuEntries())
        {
            if (!isStock(entry, menu) || names && (StreamerName.replace(entry.getOption()) != null || StreamerName.replace(entry.getTarget()) != null))
            {
                return true;
            }
            Menu sub = entry.getSubMenu();
            if (sub != null && sub != menu && differsFromStock(sub, names))
            {
                return true;
            }
        }
        return false;
    }

    private static void nameEntries(Menu menu)
    {
        if (!StreamerName.hasNames())
        {
            return;
        }
        for (MenuEntry entry : menu.getMenuEntries())
        {
            String option = StreamerName.replace(entry.getOption());
            if (option != null)
            {
                entry.setOption(option);
            }
            String target = StreamerName.replace(entry.getTarget());
            if (target != null)
            {
                entry.setTarget(target);
            }
            Menu sub = entry.getSubMenu();
            if (sub != null && sub != menu)
            {
                nameEntries(sub);
            }
        }
    }

    private static boolean keepStock(Menu menu)
    {
        MenuEntry[] entries = menu.getMenuEntries();
        List<MenuEntry> stock = new ArrayList<>(entries.length);
        for (MenuEntry entry : entries)
        {
            if (isStock(entry, menu))
            {
                stock.add(entry);
                Menu sub = entry.getSubMenu();
                if (sub != null && sub != menu)
                {
                    keepStock(sub);
                }
            }
        }
        if (stock.isEmpty())
        {
            return false;
        }
        if (stock.size() != entries.length)
        {
            menu.setMenuEntries(stock.toArray(new MenuEntry[0]));
        }
        return true;
    }

    private static boolean isStock(MenuEntry entry, Menu owner)
    {
        if (CREATED_NOT_STOCK.contains(entry))
        {
            return false;
        }
        Consumer<MenuEntry> onClick = entry.onClick();
        if (onClick != null && (!StockContent.isStock(onClick.getClass()) || holdsHidden(onClick)))
        {
            return false;
        }
        Menu sub = entry.getSubMenu();
        if (sub == null || sub == owner)
        {
            return true;
        }
        MenuEntry[] subEntries = sub.getMenuEntries();
        for (MenuEntry subEntry : subEntries)
        {
            if (isStock(subEntry, sub))
            {
                return true;
            }
        }
        return subEntries.length == 0;
    }

    private static boolean holdsHidden(Consumer<MenuEntry> onClick)
    {
        for (Field field : CAPTURED.get(onClick.getClass()))
        {
            Object value;
            try
            {
                value = field.get(onClick);
            }
            catch (IllegalAccessException | RuntimeException e)
            {
                continue;
            }
            if (value instanceof Overlay && !StockContent.isStock((Overlay) value)
                    || value instanceof WorldMapPoint && MirrorWorldMap.isHidden((WorldMapPoint) value)
                    || value instanceof OverlayMenuEntry && MirrorInfoBoxes.ownsHidden((OverlayMenuEntry) value))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isOwner(Class<?> type)
    {
        return Overlay.class.isAssignableFrom(type) || WorldMapPoint.class.isAssignableFrom(type) || OverlayMenuEntry.class.isAssignableFrom(type);
    }

    /**
     * Every field of a menu and the entries and submenus it reaches, with copies of their arrays, to put back after the menu is changed.
     */
    private static final class Snapshot
    {
        private final List<Object> owners = new ArrayList<>();
        private final List<Object[]> values = new ArrayList<>();
        private final List<Object[]> arrays = new ArrayList<>();

        private static Snapshot of(Menu menu)
        {
            Snapshot snapshot = new Snapshot();
            snapshot.visit(menu, Collections.newSetFromMap(new IdentityHashMap<>()));
            return snapshot;
        }

        private void visit(Object owner, Set<Object> seen)
        {
            if (owner == null || !seen.add(owner))
            {
                return;
            }
            Field[] fields = FieldCopy.fields(owner.getClass());
            Object[] saved = new Object[fields.length];
            Object[] copies = new Object[fields.length];
            owners.add(owner);
            values.add(saved);
            arrays.add(copies);
            for (int i = 0; i < fields.length; i++)
            {
                Object value;
                try
                {
                    value = fields[i].get(owner);
                }
                catch (IllegalAccessException e)
                {
                    continue;
                }
                saved[i] = value;
                if (value != null && value.getClass().isArray())
                {
                    copies[i] = FieldCopy.copyArray(value);
                    if (value instanceof Object[])
                    {
                        for (Object element : (Object[]) value)
                        {
                            if (element instanceof Menu || element instanceof MenuEntry)
                            {
                                visit(element, seen);
                            }
                        }
                    }
                }
                else if (value instanceof Menu || value instanceof MenuEntry)
                {
                    visit(value, seen);
                }
            }
        }

        private void restore()
        {
            for (int o = 0; o < owners.size(); o++)
            {
                Object owner = owners.get(o);
                Field[] fields = FieldCopy.fields(owner.getClass());
                Object[] saved = values.get(o);
                Object[] copies = arrays.get(o);
                for (int i = 0; i < fields.length; i++)
                {
                    try
                    {
                        if (copies[i] != null)
                        {
                            System.arraycopy(copies[i], 0, saved[i], 0, Array.getLength(copies[i]));
                        }
                        if (!Modifier.isFinal(fields[i].getModifiers()))
                        {
                            fields[i].set(owner, saved[i]);
                        }
                    }
                    catch (IllegalAccessException | RuntimeException ignored)
                    {
                    }
                }
            }
        }
    }
}
