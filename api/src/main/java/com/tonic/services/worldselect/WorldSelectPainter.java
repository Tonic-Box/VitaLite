package com.tonic.services.worldselect;

import com.tonic.ui.VitaTheme;
import net.runelite.client.ui.FontManager;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldRegion;
import net.runelite.http.api.worlds.WorldType;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.function.IntConsumer;

/**
 * Draws the world select in Java2D over the whole canvas, and keeps, for the input, the areas drawn each frame that react to the mouse and the list's scroll.
 */
final class WorldSelectPainter
{
    /**
     * An area drawn this frame and what pressing it does, given the press's click count.
     */
    static final class Hit
    {
        private final Rectangle area;
        private final IntConsumer press;

        Hit(Rectangle area, IntConsumer press)
        {
            this.area = area;
            this.press = press;
        }
    }

    private static final int HEADER_HEIGHT = 44;
    private static final int SIDEBAR_WIDTH = 192;
    private static final int COLUMN_HEADER_HEIGHT = 24;
    private static final int ROW_HEIGHT = 26;
    private static final int DIVIDER_HEIGHT = 22;
    private static final int TILE_WIDTH = 100;
    private static final int TILE_HEIGHT = 30;
    private static final int TILE_GAP = 6;
    private static final int SIDEBAR_ROW = 20;
    private static final int SCROLLBAR_WIDTH = 10;
    private static final int PAD = 10;
    private static final int SEARCH_MAX_WIDTH = 300;
    private static final int SEARCH_MIN_WIDTH = 140;
    private static final int MAX_PLAYERS = 2000;
    private static final int FULL_PLAYERS = 1980;
    private static final int NO_DRAG = Integer.MIN_VALUE;

    private static final Color HEADER = new Color(28, 28, 30);
    private static final Color SIDEBAR = new Color(32, 32, 34);
    private static final Color LIST = new Color(38, 38, 40);
    private static final Color ROW_ALTERNATE = new Color(42, 42, 45);
    private static final Color COLUMNS = new Color(33, 33, 35);
    private static final Color LINE = new Color(55, 55, 60);
    private static final Color FIELD = new Color(22, 22, 24);
    private static final Color TILE = new Color(47, 47, 51);
    private static final Color HOVER = new Color(58, 58, 64);
    private static final Color FAVOURITE = new Color(0xF5C542);
    private static final Color MEMBERS = new Color(0xD4AF37);
    private static final Color FREE = new Color(0x9AA0A6);
    private static final Color GOOD = new Color(0x3CC864);
    private static final Color FAIR = new Color(0xE6B422);
    private static final Color POOR = new Color(0xE05050);

    private static final String[] COLUMN_NAMES = {"World", "Players", "Region", "Activity", "Ping"};
    private static final WorldSorter.Key[] COLUMN_KEYS = {WorldSorter.Key.WORLD, WorldSorter.Key.PLAYERS, WorldSorter.Key.REGION, WorldSorter.Key.ACTIVITY, WorldSorter.Key.PING};

    private final WorldSelectModel model;
    private final WorldPinger pinger;

    private volatile List<Hit> hits = Collections.emptyList();
    private volatile int pointerX = -1;
    private volatile int pointerY = -1;
    private final DoubleAdder wheel = new DoubleAdder();
    private volatile int dragTop = NO_DRAG;
    private volatile Rectangle thumb = new Rectangle();
    private volatile Rectangle track = new Rectangle();
    private volatile boolean revealSelected;
    private double scrollTarget;
    private double scroll;
    private int maxScroll;
    private List<Hit> building;

    /**
     * @param model what is shown
     * @param pinger the pings shown
     */
    WorldSelectPainter(WorldSelectModel model, WorldPinger pinger)
    {
        this.model = model;
        this.pinger = pinger;
    }

    /**
     * Starts a new opening: the list scrolls to the selected world once it is listed.
     */
    void opened()
    {
        scroll = 0;
        scrollTarget = 0;
        wheel.reset();
        dragTop = NO_DRAG;
        revealSelected = true;
    }

    /**
     * @param x the pointer's x on the canvas, or -1 when it left
     * @param y the pointer's y on the canvas, or -1 when it left
     */
    void pointer(int x, int y)
    {
        pointerX = x;
        pointerY = y;
    }

    /**
     * Runs what pressing the topmost area under the pointer does.
     *
     * @param x the press's x on the canvas
     * @param y the press's y on the canvas
     * @param clicks the press's click count
     */
    void press(int x, int y, int clicks)
    {
        List<Hit> current = hits;
        for (int i = current.size() - 1; i >= 0; i--)
        {
            Hit hit = current.get(i);
            if (hit.area.contains(x, y))
            {
                hit.press.accept(clicks);
                return;
            }
        }
    }

    /**
     * Asks for the list to scroll, applied when the next frame is painted.
     *
     * @param pixels how far to scroll the list, down when positive
     */
    void scrollBy(double pixels)
    {
        wheel.add(pixels);
    }

    /**
     * @param x the pointer's x on the canvas
     * @param y the pointer's y on the canvas
     * @return whether the pointer is on the scrollbar's thumb
     */
    boolean onThumb(int x, int y)
    {
        return thumb.contains(x, y);
    }

    /**
     * @param x the pointer's x on the canvas
     * @param y the pointer's y on the canvas
     * @return whether the pointer is on the scrollbar's track
     */
    boolean onTrack(int x, int y)
    {
        return track.contains(x, y);
    }

    /**
     * Asks for the list to scroll so the scrollbar's thumb sits at a height, applied when the next frame is painted.
     *
     * @param thumbTop where the thumb's top should be, on the canvas
     */
    void dragThumb(int thumbTop)
    {
        dragTop = thumbTop;
    }

    /**
     * @return the thumb's top on the canvas
     */
    int thumbTop()
    {
        return thumb.y;
    }

    /**
     * Paints one frame of the world select.
     *
     * @param g the frame's graphics
     * @param width the canvas width
     * @param height the canvas height
     */
    void paint(Graphics2D g, int width, int height)
    {
        Graphics2D graphics = (Graphics2D) g.create();
        building = new ArrayList<>();
        try
        {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            WorldSelectModel.Listing listing = model.listing();
            graphics.setColor(LIST);
            graphics.fillRect(0, 0, width, height);

            Hover hover = new Hover();
            paintList(graphics, listing, new Rectangle(SIDEBAR_WIDTH, HEADER_HEIGHT, width - SIDEBAR_WIDTH, height - HEADER_HEIGHT), hover);
            paintSidebar(graphics, listing, new Rectangle(0, HEADER_HEIGHT, SIDEBAR_WIDTH, height - HEADER_HEIGHT));
            paintHeader(graphics, width);
            if (hover.world != null && model.isCompact())
            {
                paintTooltip(graphics, hover.world, width, height);
            }
        }
        finally
        {
            graphics.dispose();
            hits = building;
        }
    }

    private static final class Hover
    {
        private World world;
    }

    private void paintHeader(Graphics2D graphics, int width)
    {
        graphics.setColor(HEADER);
        graphics.fillRect(0, 0, width, HEADER_HEIGHT);
        graphics.setColor(LINE);
        graphics.drawLine(0, HEADER_HEIGHT - 1, width, HEADER_HEIGHT - 1);

        graphics.setFont(FontManager.getRunescapeBoldFont());
        graphics.setColor(VitaTheme.TITLE);
        text(graphics, "Select a world", 16, 0, HEADER_HEIGHT);

        Rectangle close = new Rectangle(width - 36, (HEADER_HEIGHT - 28) / 2, 28, 28);
        paintClose(graphics, close);
        hit(close, clicks -> WorldSelect.close());

        int selected = model.getSelected();
        int current = model.getCurrent();
        World target = model.world(selected);
        boolean canSwitch = target != null && selected != current;
        graphics.setFont(FontManager.getRunescapeBoldFont());
        String label = canSwitch ? "Switch to " + selected : "Switch";
        int buttonWidth = Math.max(76, graphics.getFontMetrics().stringWidth(label) + 24);
        Rectangle button = new Rectangle(close.x - 8 - buttonWidth, (HEADER_HEIGHT - 28) / 2, buttonWidth, 28);
        paintButton(graphics, button, label, canSwitch);
        if (canSwitch)
        {
            hit(button, clicks -> WorldSelect.confirm());
        }

        int searchLeft = 140;
        World currentWorld = model.world(current);
        int chipRight = button.x - 10;
        int chipWidth = currentWorld == null ? 0 : chipWidth(graphics, currentWorld);
        boolean chip = currentWorld != null && chipRight - chipWidth - 10 - searchLeft >= SEARCH_MIN_WIDTH;
        if (chip)
        {
            paintChip(graphics, currentWorld, new Rectangle(chipRight - chipWidth, (HEADER_HEIGHT - 26) / 2, chipWidth, 26));
        }
        int searchRight = chip ? chipRight - chipWidth - 10 : button.x - 10;
        int searchWidth = Math.min(SEARCH_MAX_WIDTH, searchRight - searchLeft);
        if (searchWidth >= SEARCH_MIN_WIDTH / 2)
        {
            paintSearch(graphics, new Rectangle(searchLeft, (HEADER_HEIGHT - 26) / 2, searchWidth, 26));
        }
    }

    private int chipWidth(Graphics2D graphics, World world)
    {
        graphics.setFont(FontManager.getRunescapeSmallFont());
        int width = graphics.getFontMetrics().stringWidth("Current");
        graphics.setFont(FontManager.getRunescapeBoldFont());
        width += graphics.getFontMetrics().stringWidth(String.valueOf(world.getId()));
        return width + 8 + 6 + 18 + 6 + 8;
    }

    private void paintChip(Graphics2D graphics, World world, Rectangle area)
    {
        graphics.setColor(FIELD);
        graphics.fillRoundRect(area.x, area.y, area.width, area.height, 8, 8);
        graphics.setColor(LINE);
        graphics.drawRoundRect(area.x, area.y, area.width - 1, area.height - 1, 8, 8);
        int x = area.x + 8;
        graphics.setFont(FontManager.getRunescapeSmallFont());
        graphics.setColor(VitaTheme.MUTED);
        x = text(graphics, "Current", x, area.y, area.height) + 6;
        x = flag(graphics, world.getRegion(), x, area.y + area.height / 2) + 6;
        graphics.setFont(FontManager.getRunescapeBoldFont());
        graphics.setColor(GameMode.of(world).isDangerous() ? POOR : VitaTheme.TITLE);
        text(graphics, String.valueOf(world.getId()), x, area.y, area.height);
    }

    private void paintSearch(Graphics2D graphics, Rectangle area)
    {
        String search = model.getSearch();
        graphics.setColor(FIELD);
        graphics.fillRoundRect(area.x, area.y, area.width, area.height, 8, 8);
        graphics.setColor(search.isEmpty() ? LINE : VitaTheme.accent());
        graphics.drawRoundRect(area.x, area.y, area.width - 1, area.height - 1, 8, 8);

        int iconX = area.x + 9;
        int iconY = area.y + area.height / 2 - 4;
        graphics.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.setColor(VitaTheme.MUTED);
        graphics.drawOval(iconX, iconY, 7, 7);
        graphics.drawLine(iconX + 7, iconY + 7, iconX + 10, iconY + 10);

        int textX = area.x + 26;
        int clearSize = 16;
        Rectangle clear = new Rectangle(area.x + area.width - clearSize - 6, area.y + (area.height - clearSize) / 2, clearSize, clearSize);
        Shape clip = graphics.getClip();
        graphics.clipRect(textX, area.y, clear.x - textX - 2, area.height);
        graphics.setFont(FontManager.getRunescapeFont());
        int end;
        if (search.isEmpty())
        {
            graphics.setColor(VitaTheme.MUTED);
            text(graphics, "Search world, activity, region", textX, area.y, area.height);
            end = textX;
        }
        else
        {
            graphics.setColor(VitaTheme.TITLE);
            end = text(graphics, search, textX, area.y, area.height);
        }
        if ((System.currentTimeMillis() / 530) % 2 == 0)
        {
            graphics.setColor(VitaTheme.TITLE);
            graphics.fillRect(end + 1, area.y + 7, 1, area.height - 14);
        }
        graphics.setClip(clip);

        if (!search.isEmpty())
        {
            boolean hovered = hovered(clear);
            graphics.setColor(hovered ? VitaTheme.TITLE : VitaTheme.MUTED);
            cross(graphics, clear, 5);
            hit(clear, clicks -> model.setSearch(""));
        }
    }

    private void paintClose(Graphics2D graphics, Rectangle close)
    {
        boolean hovered = hovered(close);
        if (hovered)
        {
            graphics.setColor(HOVER);
            graphics.fillRoundRect(close.x, close.y, close.width, close.height, 8, 8);
        }
        graphics.setColor(hovered ? VitaTheme.ERROR : VitaTheme.MUTED);
        cross(graphics, close, 9);
    }

    private void paintButton(Graphics2D graphics, Rectangle area, String label, boolean enabled)
    {
        Color accent = VitaTheme.accent();
        Color fill = enabled ? (hovered(area) ? accent.brighter() : accent) : new Color(52, 52, 56);
        graphics.setColor(fill);
        graphics.fillRoundRect(area.x, area.y, area.width, area.height, 8, 8);
        graphics.setFont(FontManager.getRunescapeBoldFont());
        graphics.setColor(enabled ? Color.WHITE : VitaTheme.MUTED);
        FontMetrics metrics = graphics.getFontMetrics();
        text(graphics, label, area.x + (area.width - metrics.stringWidth(label)) / 2, area.y, area.height);
    }

    private void paintSidebar(Graphics2D graphics, WorldSelectModel.Listing listing, Rectangle area)
    {
        graphics.setColor(SIDEBAR);
        graphics.fillRect(area.x, area.y, area.width, area.height);
        graphics.setColor(LINE);
        graphics.drawLine(area.x + area.width - 1, area.y, area.x + area.width - 1, area.y + area.height);

        Shape clip = graphics.getClip();
        graphics.clipRect(area.x, area.y, area.width - 1, area.height);
        int x = area.x + PAD;
        int width = area.width - PAD * 2;
        int y = area.y + 8;

        y = section(graphics, "GAME MODES", x, y);
        for (Map.Entry<GameMode, Integer> entry : listing.getModeCounts().entrySet())
        {
            GameMode mode = entry.getKey();
            Rectangle row = new Rectangle(area.x + 4, y, area.width - 8, SIDEBAR_ROW);
            rowHover(graphics, row);
            int cx = checkbox(graphics, x, y, model.isTicked(mode));
            graphics.setColor(mode.getColor());
            graphics.fillOval(cx + 6, y + SIDEBAR_ROW / 2 - 4, 8, 8);
            graphics.setFont(FontManager.getRunescapeSmallFont());
            graphics.setColor(VitaTheme.TEXT);
            text(graphics, mode.getDisplayName(), cx + 20, y, SIDEBAR_ROW);
            count(graphics, entry.getValue(), x + width, y);
            hit(row, clicks -> model.toggleMode(mode));
            y += SIDEBAR_ROW;
        }

        y = section(graphics, "REGIONS", x, y + 8);
        int column = 0;
        int cellWidth = (area.width - 8) / 2;
        for (Map.Entry<WorldRegion, Integer> entry : listing.getRegionCounts().entrySet())
        {
            WorldRegion region = entry.getKey();
            int cellX = area.x + 4 + column * cellWidth;
            Rectangle cell = new Rectangle(cellX, y, cellWidth, SIDEBAR_ROW);
            rowHover(graphics, cell);
            int cx = checkbox(graphics, cellX + PAD - 4, y, model.isTicked(region));
            int fx = flag(graphics, region, cx + 5, y + SIDEBAR_ROW / 2);
            graphics.setFont(FontManager.getRunescapeSmallFont());
            graphics.setColor(VitaTheme.TEXT);
            text(graphics, region.getAlpha2(), fx + 5, y, SIDEBAR_ROW);
            count(graphics, entry.getValue(), cellX + cellWidth - 6, y);
            hit(cell, clicks -> model.toggleRegion(region));
            column++;
            if (column == 2)
            {
                column = 0;
                y += SIDEBAR_ROW;
            }
        }
        if (column != 0)
        {
            y += SIDEBAR_ROW;
        }

        y = section(graphics, "OPTIONS", x, y + 8);
        y = option(graphics, "Members worlds", model.showsMembers(), area, y, model::toggleMembers);
        y = option(graphics, "Free worlds", model.showsFree(), area, y, model::toggleFree);
        y = option(graphics, "Compact view", model.isCompact(), area, y, model::toggleCompact);

        if (model.hasFilters())
        {
            Rectangle reset = new Rectangle(x, y + 10, width, 24);
            boolean hovered = hovered(reset);
            graphics.setColor(hovered ? HOVER : TILE);
            graphics.fillRoundRect(reset.x, reset.y, reset.width, reset.height, 8, 8);
            graphics.setFont(FontManager.getRunescapeSmallFont());
            graphics.setColor(hovered ? VitaTheme.TITLE : VitaTheme.TEXT);
            String label = "Reset filters";
            text(graphics, label, reset.x + (reset.width - graphics.getFontMetrics().stringWidth(label)) / 2, reset.y, reset.height);
            hit(reset, clicks -> model.reset());
        }
        graphics.setClip(clip);
    }

    private int section(Graphics2D graphics, String title, int x, int y)
    {
        graphics.setFont(FontManager.getRunescapeSmallFont());
        graphics.setColor(VitaTheme.MUTED);
        text(graphics, title, x, y, 18);
        return y + 20;
    }

    private void rowHover(Graphics2D graphics, Rectangle row)
    {
        if (hovered(row))
        {
            graphics.setColor(HOVER);
            graphics.fillRoundRect(row.x, row.y, row.width, row.height, 6, 6);
        }
    }

    private int checkbox(Graphics2D graphics, int x, int y, boolean ticked)
    {
        int size = 12;
        int top = y + (SIDEBAR_ROW - size) / 2;
        if (ticked)
        {
            graphics.setColor(VitaTheme.accent());
            graphics.fillRoundRect(x, top, size, size, 4, 4);
            graphics.setColor(Color.WHITE);
            graphics.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D tick = new Path2D.Double();
            tick.moveTo(x + 3, top + 6.5);
            tick.lineTo(x + 5.2, top + 8.7);
            tick.lineTo(x + 9.2, top + 3.6);
            graphics.draw(tick);
        }
        else
        {
            graphics.setColor(new Color(95, 95, 102));
            graphics.setStroke(new BasicStroke(1.2f));
            graphics.drawRoundRect(x, top, size, size, 4, 4);
        }
        return x + size;
    }

    private void count(Graphics2D graphics, int count, int right, int y)
    {
        graphics.setFont(FontManager.getRunescapeSmallFont());
        graphics.setColor(VitaTheme.MUTED);
        String text = String.valueOf(count);
        text(graphics, text, right - graphics.getFontMetrics().stringWidth(text), y, SIDEBAR_ROW);
    }

    private int option(Graphics2D graphics, String label, boolean on, Rectangle area, int y, Runnable toggle)
    {
        Rectangle row = new Rectangle(area.x + 4, y, area.width - 8, SIDEBAR_ROW + 2);
        rowHover(graphics, row);
        graphics.setFont(FontManager.getRunescapeSmallFont());
        graphics.setColor(VitaTheme.TEXT);
        text(graphics, label, area.x + PAD, y, row.height);
        int switchWidth = 26;
        int switchHeight = 14;
        int sx = area.x + area.width - PAD - switchWidth;
        int sy = y + (row.height - switchHeight) / 2;
        graphics.setColor(on ? VitaTheme.accent() : new Color(70, 70, 76));
        graphics.fillRoundRect(sx, sy, switchWidth, switchHeight, switchHeight, switchHeight);
        graphics.setColor(Color.WHITE);
        int knob = switchHeight - 4;
        graphics.fillOval(on ? sx + switchWidth - knob - 2 : sx + 2, sy + 2, knob, knob);
        hit(row, clicks -> toggle.run());
        return y + row.height;
    }

    private void paintList(Graphics2D graphics, WorldSelectModel.Listing listing, Rectangle area, Hover hover)
    {
        boolean compact = model.isCompact();
        Rectangle columns = new Rectangle(area.x, area.y, area.width, COLUMN_HEADER_HEIGHT);
        Rectangle body = new Rectangle(area.x, area.y + COLUMN_HEADER_HEIGHT, area.width, area.height - COLUMN_HEADER_HEIGHT);
        Layout layout = compact ? tiles(listing, body.width - SCROLLBAR_WIDTH - PAD * 2) : rows(listing);
        maxScroll = Math.max(0, layout.height - body.height);
        if (revealSelected && listing.isLoaded())
        {
            revealSelected = false;
            Placed selected = layout.find(model.getSelected());
            if (selected != null)
            {
                scrollTarget = clamp(selected.y - (body.height - selected.height) / 2.0, 0, maxScroll);
                scroll = scrollTarget;
            }
        }
        scrollTarget += wheel.sumThenReset();
        int drag = dragTop;
        if (drag != NO_DRAG)
        {
            dragTop = NO_DRAG;
            Rectangle bar = track;
            int travel = bar.height - thumb.height;
            if (travel > 0)
            {
                scrollTarget = (drag - bar.y) * (double) maxScroll / travel;
                scroll = clamp(scrollTarget, 0, maxScroll);
            }
        }
        scrollTarget = clamp(scrollTarget, 0, maxScroll);
        scroll += (scrollTarget - scroll) * 0.35;
        if (Math.abs(scrollTarget - scroll) < 0.5)
        {
            scroll = scrollTarget;
        }
        int offset = (int) Math.round(scroll);

        Shape clip = graphics.getClip();
        graphics.clipRect(body.x, body.y, body.width, body.height);
        if (!listing.isLoaded())
        {
            centred(graphics, "Loading worlds…", body);
        }
        else if (listing.size() == 0)
        {
            centred(graphics, "No worlds match your filters", body);
        }
        else
        {
            Columns cells = new Columns(body.x, body.width - SCROLLBAR_WIDTH);
            int index = 0;
            for (Placed placed : layout.items)
            {
                int top = body.y + placed.y - offset;
                if (top + placed.height < body.y || top > body.y + body.height)
                {
                    index++;
                    continue;
                }
                if (placed.world == null)
                {
                    divider(graphics, placed.label, body.x + PAD, top, body.width - PAD * 2 - SCROLLBAR_WIDTH);
                }
                else if (compact)
                {
                    tile(graphics, placed.world, new Rectangle(body.x + PAD + placed.x, top, placed.width, placed.height), body, hover);
                }
                else
                {
                    row(graphics, placed.world, new Rectangle(body.x, top, body.width - SCROLLBAR_WIDTH, placed.height), cells, index, body);
                }
                index++;
            }
        }
        graphics.setClip(clip);
        scrollbar(graphics, body, layout.height, offset);
        columnHeader(graphics, columns, compact);
    }

    private static final class Placed
    {
        private final World world;
        private final String label;
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        Placed(World world, String label, int x, int y, int width, int height)
        {
            this.world = world;
            this.label = label;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static final class Layout
    {
        private final List<Placed> items = new ArrayList<>();
        private int height;

        Placed find(int world)
        {
            for (Placed placed : items)
            {
                if (placed.world != null && placed.world.getId() == world)
                {
                    return placed;
                }
            }
            return null;
        }
    }

    private static Layout rows(WorldSelectModel.Listing listing)
    {
        Layout layout = new Layout();
        int y = 0;
        boolean dividers = !listing.getFavourites().isEmpty();
        if (dividers)
        {
            layout.items.add(new Placed(null, "FAVOURITES", 0, y, 0, DIVIDER_HEIGHT));
            y += DIVIDER_HEIGHT;
            for (World world : listing.getFavourites())
            {
                layout.items.add(new Placed(world, null, 0, y, 0, ROW_HEIGHT));
                y += ROW_HEIGHT;
            }
            if (!listing.getOthers().isEmpty())
            {
                layout.items.add(new Placed(null, "ALL WORLDS", 0, y, 0, DIVIDER_HEIGHT));
                y += DIVIDER_HEIGHT;
            }
        }
        for (World world : listing.getOthers())
        {
            layout.items.add(new Placed(world, null, 0, y, 0, ROW_HEIGHT));
            y += ROW_HEIGHT;
        }
        layout.height = y + 6;
        return layout;
    }

    private static Layout tiles(WorldSelectModel.Listing listing, int width)
    {
        Layout layout = new Layout();
        int perRow = Math.max(1, (width + TILE_GAP) / (TILE_WIDTH + TILE_GAP));
        int y = 6;
        boolean dividers = !listing.getFavourites().isEmpty();
        if (dividers)
        {
            layout.items.add(new Placed(null, "FAVOURITES", 0, y, 0, DIVIDER_HEIGHT));
            y = grid(layout, listing.getFavourites(), perRow, y + DIVIDER_HEIGHT);
            if (!listing.getOthers().isEmpty())
            {
                layout.items.add(new Placed(null, "ALL WORLDS", 0, y + 4, 0, DIVIDER_HEIGHT));
                y += 4 + DIVIDER_HEIGHT;
            }
        }
        y = grid(layout, listing.getOthers(), perRow, y);
        layout.height = y + 6;
        return layout;
    }

    private static int grid(Layout layout, List<World> worlds, int perRow, int y)
    {
        int column = 0;
        for (World world : worlds)
        {
            layout.items.add(new Placed(world, null, column * (TILE_WIDTH + TILE_GAP), y, TILE_WIDTH, TILE_HEIGHT));
            column++;
            if (column == perRow)
            {
                column = 0;
                y += TILE_HEIGHT + TILE_GAP;
            }
        }
        if (column != 0)
        {
            y += TILE_HEIGHT + TILE_GAP;
        }
        return y;
    }

    /**
     * Where each column of a row starts, worked out from the list's width; the activity takes what the others leave.
     */
    private static final class Columns
    {
        private static final int STAR = 22;
        private static final int WORLD = 50;
        private static final int PLAYERS = 96;
        private static final int REGION = 52;
        private static final int BADGES = 118;
        private static final int PING = 46;

        private final int star;
        private final int world;
        private final int players;
        private final int region;
        private final int badges;
        private final int activity;
        private final int activityWidth;
        private final int ping;

        Columns(int left, int width)
        {
            star = left + 8;
            world = star + STAR;
            players = world + WORLD;
            region = players + PLAYERS;
            badges = region + REGION;
            activity = badges + BADGES;
            ping = left + width - PING - 6;
            activityWidth = Math.max(0, ping - activity - 8);
        }
    }

    private void columnHeader(Graphics2D graphics, Rectangle area, boolean compact)
    {
        graphics.setColor(COLUMNS);
        graphics.fillRect(area.x, area.y, area.width, area.height);
        graphics.setColor(LINE);
        graphics.drawLine(area.x, area.y + area.height - 1, area.x + area.width, area.y + area.height - 1);
        graphics.setFont(FontManager.getRunescapeSmallFont());
        if (compact)
        {
            graphics.setColor(VitaTheme.MUTED);
            int x = text(graphics, "Sort by", area.x + PAD, area.y, area.height) + 12;
            for (int i = 0; i < COLUMN_KEYS.length; i++)
            {
                x = sortLabel(graphics, COLUMN_NAMES[i], COLUMN_KEYS[i], x, area) + 14;
            }
            return;
        }
        Columns cells = new Columns(area.x, area.width - SCROLLBAR_WIDTH);
        int[] starts = {cells.world, cells.players, cells.region, cells.activity, cells.ping};
        for (int i = 0; i < COLUMN_KEYS.length; i++)
        {
            sortLabel(graphics, COLUMN_NAMES[i], COLUMN_KEYS[i], starts[i], area);
        }
        graphics.setColor(VitaTheme.MUTED);
        text(graphics, "Type", cells.badges, area.y, area.height);
    }

    private int sortLabel(Graphics2D graphics, String name, WorldSorter.Key key, int x, Rectangle area)
    {
        boolean primary = model.sortKey() == key;
        int width = graphics.getFontMetrics().stringWidth(name) + (primary ? 12 : 0);
        Rectangle label = new Rectangle(x - 3, area.y + 2, width + 6, area.height - 4);
        boolean hovered = hovered(label);
        if (hovered)
        {
            graphics.setColor(HOVER);
            graphics.fillRoundRect(label.x, label.y, label.width, label.height, 6, 6);
        }
        graphics.setColor(primary ? VitaTheme.TITLE : hovered ? VitaTheme.TEXT : VitaTheme.MUTED);
        int end = text(graphics, name, x, area.y, area.height);
        if (primary)
        {
            graphics.setColor(VitaTheme.accent());
            int cx = end + 6;
            int cy = area.y + area.height / 2;
            Path2D arrow = new Path2D.Double();
            if (model.sortAscending())
            {
                arrow.moveTo(cx - 3.5, cy + 2);
                arrow.lineTo(cx + 3.5, cy + 2);
                arrow.lineTo(cx, cy - 2.5);
            }
            else
            {
                arrow.moveTo(cx - 3.5, cy - 2);
                arrow.lineTo(cx + 3.5, cy - 2);
                arrow.lineTo(cx, cy + 2.5);
            }
            arrow.closePath();
            graphics.fill(arrow);
        }
        hit(label, clicks -> model.pickSort(key));
        return x + width;
    }

    private void divider(Graphics2D graphics, String label, int x, int y, int width)
    {
        graphics.setFont(FontManager.getRunescapeSmallFont());
        graphics.setColor(VitaTheme.MUTED);
        int end = text(graphics, label, x, y, DIVIDER_HEIGHT);
        graphics.setColor(LINE);
        graphics.drawLine(end + 8, y + DIVIDER_HEIGHT / 2, x + width, y + DIVIDER_HEIGHT / 2);
    }

    private void row(Graphics2D graphics, World world, Rectangle area, Columns cells, int index, Rectangle body)
    {
        int id = world.getId();
        boolean selected = id == model.getSelected();
        boolean current = id == model.getCurrent();
        boolean hovered = hovered(area) && body.contains(pointerX, pointerY);
        GameMode mode = GameMode.of(world);

        if (index % 2 == 1)
        {
            graphics.setColor(ROW_ALTERNATE);
            graphics.fillRect(area.x, area.y, area.width, area.height);
        }
        if (hovered)
        {
            graphics.setColor(HOVER);
            graphics.fillRect(area.x, area.y, area.width, area.height);
        }
        if (selected)
        {
            Color accent = VitaTheme.accent();
            graphics.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 48));
            graphics.fillRect(area.x, area.y, area.width, area.height);
            graphics.setColor(accent);
            graphics.setStroke(new BasicStroke(1f));
            graphics.drawRect(area.x + 1, area.y, area.width - 3, area.height - 1);
        }
        if (mode != GameMode.MAIN_GAME)
        {
            graphics.setColor(mode.getColor());
            graphics.fillRect(area.x, area.y, 3, area.height);
        }

        Rectangle star = new Rectangle(cells.star - 4, area.y, 22, area.height);
        boolean favourite = model.isFavourite(id);
        star(graphics, cells.star + 7, area.y + area.height / 2.0, 6.5, favourite, hovered(star));

        graphics.setFont(FontManager.getRunescapeBoldFont());
        graphics.setColor(mode.isDangerous() ? POOR : VitaTheme.TITLE);
        text(graphics, String.valueOf(id), cells.world, area.y, area.height);

        players(graphics, world, cells.players, area.y, area.height);

        int fx = flag(graphics, world.getRegion(), cells.region, area.y + area.height / 2);
        if (world.getRegion() != null)
        {
            graphics.setFont(FontManager.getRunescapeSmallFont());
            graphics.setColor(VitaTheme.TEXT);
            text(graphics, world.getRegion().getAlpha2(), fx + 5, area.y, area.height);
        }

        badges(graphics, world, current, cells.badges, area.y + (area.height - 14) / 2, cells.activity - cells.badges - 6);

        graphics.setFont(FontManager.getRunescapeFont());
        if (WorldSorter.hasActivity(world))
        {
            graphics.setColor(VitaTheme.TEXT);
            text(graphics, ellipsis(graphics, world.getActivity(), cells.activityWidth), cells.activity, area.y, area.height);
        }
        else
        {
            graphics.setColor(VitaTheme.MUTED);
            text(graphics, "–", cells.activity, area.y, area.height);
        }

        ping(graphics, pinger.ping(id), cells.ping, area.y, area.height);

        Rectangle visible = area.intersection(body);
        if (!visible.isEmpty())
        {
            hit(visible, clicks -> choose(id, clicks));
            Rectangle starVisible = star.intersection(body);
            if (!starVisible.isEmpty())
            {
                hit(starVisible, clicks -> model.toggleFavourite(id));
            }
        }
    }

    private void tile(Graphics2D graphics, World world, Rectangle area, Rectangle body, Hover hover)
    {
        int id = world.getId();
        boolean selected = id == model.getSelected();
        boolean hovered = hovered(area) && body.contains(pointerX, pointerY);
        GameMode mode = GameMode.of(world);
        if (hovered)
        {
            hover.world = world;
        }

        graphics.setColor(hovered ? HOVER : TILE);
        graphics.fillRoundRect(area.x, area.y, area.width, area.height, 8, 8);
        if (mode != GameMode.MAIN_GAME)
        {
            Shape clip = graphics.getClip();
            graphics.clipRect(area.x, area.y, 4, area.height);
            graphics.setColor(mode.getColor());
            graphics.fillRoundRect(area.x, area.y, 10, area.height, 8, 8);
            graphics.setClip(clip);
        }
        if (selected)
        {
            graphics.setColor(VitaTheme.accent());
            graphics.setStroke(new BasicStroke(1.5f));
            graphics.drawRoundRect(area.x, area.y, area.width - 1, area.height - 1, 8, 8);
        }

        graphics.setFont(FontManager.getRunescapeBoldFont());
        graphics.setColor(mode.isDangerous() ? POOR : VitaTheme.TITLE);
        int x = text(graphics, String.valueOf(id), area.x + 9, area.y, area.height);
        flag(graphics, world.getRegion(), x + 5, area.y + area.height / 2);

        graphics.setFont(FontManager.getRunescapeSmallFont());
        String players = playersText(world);
        graphics.setColor(playersColor(world));
        text(graphics, players, area.x + area.width - 8 - graphics.getFontMetrics().stringWidth(players), area.y + 3, area.height / 2);
        graphics.setColor(world.getTypes().contains(WorldType.MEMBERS) ? MEMBERS : FREE);
        String kind = world.getTypes().contains(WorldType.MEMBERS) ? "P2P" : "F2P";
        text(graphics, kind, area.x + area.width - 8 - graphics.getFontMetrics().stringWidth(kind), area.y + area.height / 2 - 2, area.height / 2);

        if (model.isFavourite(id))
        {
            star(graphics, area.x + area.width - 40, area.y + area.height / 2.0, 4.5, true, false);
        }

        Rectangle visible = area.intersection(body);
        if (!visible.isEmpty())
        {
            hit(visible, clicks -> choose(id, clicks));
        }
    }

    private void choose(int world, int clicks)
    {
        model.select(world);
        if (clicks >= 2)
        {
            WorldSelect.confirm();
        }
    }

    private void paintTooltip(Graphics2D graphics, World world, int width, int height)
    {
        String activity = WorldSorter.hasActivity(world) ? world.getActivity() : GameMode.of(world).getDisplayName();
        String detail = (world.getRegion() == null ? "" : Regions.name(world.getRegion()) + "  ·  ") + playersText(world) + " players" + pingText(pinger.ping(world.getId()));
        graphics.setFont(FontManager.getRunescapeBoldFont());
        int titleWidth = graphics.getFontMetrics().stringWidth(activity);
        graphics.setFont(FontManager.getRunescapeSmallFont());
        int detailWidth = graphics.getFontMetrics().stringWidth(detail);
        int boxWidth = Math.max(titleWidth, detailWidth) + 20;
        int boxHeight = 42;
        int x = Math.min(pointerX + 14, width - boxWidth - 6);
        int y = Math.min(pointerY + 18, height - boxHeight - 6);
        graphics.setColor(new Color(18, 18, 20, 240));
        graphics.fillRoundRect(x, y, boxWidth, boxHeight, 8, 8);
        graphics.setColor(LINE);
        graphics.drawRoundRect(x, y, boxWidth - 1, boxHeight - 1, 8, 8);
        graphics.setFont(FontManager.getRunescapeBoldFont());
        graphics.setColor(VitaTheme.TITLE);
        text(graphics, activity, x + 10, y + 4, 18);
        graphics.setFont(FontManager.getRunescapeSmallFont());
        graphics.setColor(VitaTheme.MUTED);
        text(graphics, detail, x + 10, y + 21, 16);
    }

    private static String pingText(int ping)
    {
        return ping < 0 ? "" : "  ·  " + ping + " ms";
    }

    private void players(Graphics2D graphics, World world, int x, int y, int height)
    {
        int barWidth = 44;
        int barHeight = 5;
        int barY = y + (height - barHeight) / 2;
        graphics.setColor(new Color(62, 62, 68));
        graphics.fillRoundRect(x, barY, barWidth, barHeight, barHeight, barHeight);
        int players = world.getPlayers();
        if (players > 0)
        {
            int fill = Math.max(barHeight, (int) Math.round(barWidth * Math.min(players, MAX_PLAYERS) / (double) MAX_PLAYERS));
            graphics.setColor(playersColor(world));
            graphics.fillRoundRect(x, barY, fill, barHeight, barHeight, barHeight);
        }
        graphics.setFont(FontManager.getRunescapeSmallFont());
        graphics.setColor(players < 0 ? VitaTheme.MUTED : players > FULL_PLAYERS ? POOR : VitaTheme.TEXT);
        text(graphics, playersText(world), x + barWidth + 6, y, height);
    }

    private static String playersText(World world)
    {
        int players = world.getPlayers();
        if (players < 0)
        {
            return "OFF";
        }
        return players > FULL_PLAYERS ? "FULL" : String.valueOf(players);
    }

    private static Color playersColor(World world)
    {
        int players = world.getPlayers();
        if (players < 0)
        {
            return VitaTheme.MUTED;
        }
        if (players > 1700)
        {
            return POOR;
        }
        return players > 1000 ? FAIR : GOOD;
    }

    private void ping(Graphics2D graphics, int ping, int x, int y, int height)
    {
        graphics.setFont(FontManager.getRunescapeSmallFont());
        if (ping < 0)
        {
            graphics.setColor(VitaTheme.MUTED);
            text(graphics, "–", x, y, height);
            return;
        }
        graphics.setColor(ping < 80 ? GOOD : ping < 160 ? FAIR : POOR);
        text(graphics, ping + " ms", x, y, height);
    }

    private void badges(Graphics2D graphics, World world, boolean current, int x, int y, int width)
    {
        List<Badge> badges = Badge.of(world, current);
        graphics.setFont(FontManager.getRunescapeSmallFont());
        FontMetrics metrics = graphics.getFontMetrics();
        int right = x + width;
        for (int i = 0; i < badges.size(); i++)
        {
            Badge badge = badges.get(i);
            int pill = metrics.stringWidth(badge.text) + 8;
            int remaining = badges.size() - i - 1;
            String more = "+" + remaining;
            int reserve = remaining > 0 ? metrics.stringWidth(more) + 12 : 0;
            if (x + pill + reserve > right)
            {
                more = "+" + (badges.size() - i);
                graphics.setColor(VitaTheme.MUTED);
                text(graphics, more, x, y, 14);
                return;
            }
            Color color = badge.color;
            graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
            graphics.fillRoundRect(x, y, pill, 14, 6, 6);
            graphics.setColor(color);
            text(graphics, badge.text, x + 4, y, 14);
            x += pill + 4;
        }
    }

    /**
     * A short tag for something about a world, drawn as a coloured pill.
     */
    private static final class Badge
    {
        private final String text;
        private final Color color;

        Badge(String text, Color color)
        {
            this.text = text;
            this.color = color;
        }

        static List<Badge> of(World world, boolean current)
        {
            List<Badge> badges = new ArrayList<>();
            if (current)
            {
                badges.add(new Badge("CURRENT", VitaTheme.accent()));
            }
            boolean members = world.getTypes().contains(WorldType.MEMBERS);
            badges.add(new Badge(members ? "P2P" : "F2P", members ? MEMBERS : FREE));
            for (WorldType type : world.getTypes())
            {
                Badge badge = of(type, world);
                if (badge != null)
                {
                    badges.add(badge);
                }
            }
            return badges;
        }

        private static Badge of(WorldType type, World world)
        {
            switch (type)
            {
                case PVP:
                    return new Badge("PVP", GameMode.PVP.getColor());
                case HIGH_RISK:
                    return new Badge("HIGH RISK", POOR);
                case BOUNTY:
                    return new Badge("BOUNTY", POOR);
                case PVP_ARENA:
                    return new Badge("ARENA", GameMode.PVP.getColor());
                case SKILL_TOTAL:
                    return new Badge(skillTotal(world), new Color(0xA78BFA));
                case QUEST_SPEEDRUNNING:
                    return new Badge("SPEEDRUN", GameMode.SPEEDRUNNING.getColor());
                case HARDCORE_MAIN:
                    return new Badge("HARDCORE", GameMode.HARDCORE.getColor());
                case LAST_MAN_STANDING:
                    return new Badge("LMS", new Color(0xE67E22));
                case BETA_WORLD:
                    return new Badge("BETA", GameMode.PUBLIC_BETA.getColor());
                case NOSAVE_MODE:
                    return new Badge("SPECIAL", GameMode.SPECIAL.getColor());
                case TOURNAMENT:
                    return new Badge("TOURNAMENT", GameMode.DEADMAN_SEASON.getColor());
                case FRESH_START_WORLD:
                    return new Badge("FRESH", GameMode.FRESH_START.getColor());
                case DEADMAN:
                    return new Badge("DEADMAN", GameMode.DEADMAN_PERMANENT.getColor());
                case SEASONAL:
                    return new Badge("LEAGUES", GameMode.LEAGUES.getColor());
                default:
                    return null;
            }
        }

        private static String skillTotal(World world)
        {
            String activity = world.getActivity();
            if (activity == null)
            {
                return "TOTAL";
            }
            int end = 0;
            while (end < activity.length() && Character.isDigit(activity.charAt(end)))
            {
                end++;
            }
            return end == 0 ? "TOTAL" : "TOTAL " + activity.substring(0, end);
        }
    }

    private void scrollbar(Graphics2D graphics, Rectangle body, int contentHeight, int offset)
    {
        if (contentHeight <= body.height)
        {
            track = new Rectangle();
            thumb = new Rectangle();
            return;
        }
        Rectangle bar = new Rectangle(body.x + body.width - SCROLLBAR_WIDTH, body.y + 2, SCROLLBAR_WIDTH, body.height - 4);
        int thumbHeight = Math.max(28, (int) ((long) bar.height * body.height / contentHeight));
        int travel = bar.height - thumbHeight;
        int thumbY = bar.y + (maxScroll == 0 ? 0 : (int) Math.round(travel * (offset / (double) maxScroll)));
        Rectangle handle = new Rectangle(bar.x + 2, thumbY, SCROLLBAR_WIDTH - 4, thumbHeight);
        track = bar;
        thumb = handle;
        boolean hovered = bar.contains(pointerX, pointerY);
        graphics.setColor(hovered ? new Color(120, 120, 128) : new Color(85, 85, 92));
        int round = handle.width;
        graphics.fillRoundRect(handle.x, handle.y, handle.width, handle.height, round, round);
    }

    private void star(Graphics2D graphics, double cx, double cy, double radius, boolean filled, boolean hovered)
    {
        Path2D star = new Path2D.Double();
        for (int i = 0; i < 10; i++)
        {
            double r = i % 2 == 0 ? radius : radius * 0.45;
            double angle = Math.PI / 5 * i - Math.PI / 2;
            double px = cx + Math.cos(angle) * r;
            double py = cy + Math.sin(angle) * r;
            if (i == 0)
            {
                star.moveTo(px, py);
            }
            else
            {
                star.lineTo(px, py);
            }
        }
        star.closePath();
        if (filled)
        {
            graphics.setColor(FAVOURITE);
            graphics.fill(star);
            return;
        }
        graphics.setColor(hovered ? FAVOURITE : new Color(90, 90, 97));
        graphics.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(star);
    }

    private static int flag(Graphics2D graphics, WorldRegion region, int x, int centreY)
    {
        BufferedImage flag = region == null ? null : Regions.flag(region);
        if (flag == null)
        {
            return x;
        }
        graphics.drawImage(flag, x, centreY - flag.getHeight() / 2, null);
        return x + flag.getWidth();
    }

    private void cross(Graphics2D graphics, Rectangle area, int inset)
    {
        graphics.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(area.x + inset, area.y + inset, area.x + area.width - inset, area.y + area.height - inset);
        graphics.drawLine(area.x + area.width - inset, area.y + inset, area.x + inset, area.y + area.height - inset);
    }

    private static void centred(Graphics2D graphics, String message, Rectangle area)
    {
        graphics.setFont(FontManager.getRunescapeFont());
        graphics.setColor(VitaTheme.MUTED);
        FontMetrics metrics = graphics.getFontMetrics();
        text(graphics, message, area.x + (area.width - metrics.stringWidth(message)) / 2, area.y, Math.min(area.height, 120));
    }

    private static String ellipsis(Graphics2D graphics, String text, int width)
    {
        FontMetrics metrics = graphics.getFontMetrics();
        if (metrics.stringWidth(text) <= width)
        {
            return text;
        }
        String dots = "…";
        int end = text.length();
        while (end > 0 && metrics.stringWidth(text.substring(0, end)) + metrics.stringWidth(dots) > width)
        {
            end--;
        }
        return end == 0 ? "" : text.substring(0, end).trim() + dots;
    }

    /**
     * Draws text centred down a band.
     *
     * @return where the text ends across
     */
    private static int text(Graphics2D graphics, String text, int x, int top, int height)
    {
        FontMetrics metrics = graphics.getFontMetrics();
        int baseline = top + (height + metrics.getAscent() - metrics.getDescent()) / 2;
        graphics.drawString(text, x, baseline);
        return x + metrics.stringWidth(text);
    }

    private boolean hovered(Rectangle area)
    {
        return area.contains(pointerX, pointerY);
    }

    private void hit(Rectangle area, IntConsumer press)
    {
        building.add(new Hit(area, press));
    }

    private static double clamp(double value, double min, double max)
    {
        return Math.max(min, Math.min(max, value));
    }
}
