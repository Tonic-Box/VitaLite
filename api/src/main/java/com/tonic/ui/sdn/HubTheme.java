package com.tonic.ui.sdn;

import com.tonic.services.hotswapper.CycleButton;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;

/**
 * The Vita Hub tab's look: RuneLite's fonts and colours with the Vita accent, its icons, and the buttons it is built from.
 */
final class HubTheme {
    static final Color BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
    static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;
    static final Color CARD_HOVER = ColorScheme.DARKER_GRAY_HOVER_COLOR;
    static final Color TITLE = Color.WHITE;
    static final Color TEXT = ColorScheme.TEXT_COLOR;
    static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;
    static final Color INSTALL = new Color(0x28BE28);
    static final Color WARNING = ColorScheme.PROGRESS_INPROGRESS_COLOR;
    static final Color ERROR = new Color(0xE05050);
    static final Color BUSY = ColorScheme.MEDIUM_GRAY_COLOR;

    static final int GAP = 5;
    static final int PADDING = 8;
    static final int ICON_SIZE = 32;

    private static final String CONFIG = "/net/runelite/client/plugins/config/";
    private static ImageIcon cog;
    private static ImageIcon trash;
    private static ImageIcon github;
    private static ImageIcon back;
    private static ImageIcon reload;
    private static ImageIcon more;
    private static ImageIcon switchOn;
    private static ImageIcon switchOff;

    private HubTheme() {
    }

    /** @return the accent, read at use since VitaLite recolours RuneLite's brand colour at startup */
    static Color accent() {
        return ColorScheme.BRAND_ORANGE;
    }

    /** @return the font for plugin names and titles */
    static Font titleFont() {
        return FontManager.getRunescapeBoldFont();
    }

    /** @return the font for descriptions, details and buttons */
    static Font textFont() {
        return FontManager.getRunescapeSmallFont();
    }

    /** @return the regular RuneLite font */
    static Font regularFont() {
        return FontManager.getRunescapeFont();
    }

    static ImageIcon cog() {
        if (cog == null) {
            cog = icon(load(CONFIG + "config_edit_icon.png"), 16, MUTED);
        }
        return cog;
    }

    static ImageIcon trash() {
        if (trash == null) {
            trash = icon(load(CONFIG + "mdi_delete.png"), 16, MUTED);
        }
        return trash;
    }

    static ImageIcon github() {
        if (github == null) {
            github = icon(load("/net/runelite/client/plugins/info/github_icon.png"), 16, MUTED);
        }
        return github;
    }

    static ImageIcon back() {
        if (back == null) {
            back = new ImageIcon(load(CONFIG + "config_back_icon.png"));
        }
        return back;
    }

    static ImageIcon reload() {
        if (reload == null) {
            reload = icon(ImageUtil.loadImageResource(CycleButton.class, "cycle.png"), 16, MUTED);
        }
        return reload;
    }

    static ImageIcon more() {
        if (more == null) {
            more = new ImageIcon(new BaseMultiResolutionImage(dots(1), dots(2)));
        }
        return more;
    }

    static ImageIcon switchOn() {
        if (switchOn == null) {
            switchOn = new ImageIcon(ImageUtil.recolorImage(load(CONFIG + "switcher_on.png"), accent()));
        }
        return switchOn;
    }

    static ImageIcon switchOff() {
        if (switchOff == null) {
            BufferedImage on = load(CONFIG + "switcher_on.png");
            switchOff = new ImageIcon(ImageUtil.flipImage(ImageUtil.luminanceScale(ImageUtil.grayscaleImage(on), 0.61f), true, false));
        }
        return switchOff;
    }

    /**
     * @param image an image
     * @param size  the size to show it at
     * @param color the colour to paint its shape in, or null to keep its colours
     * @return it at that size, with a twice-as-large variant for scaled displays
     */
    static ImageIcon icon(BufferedImage image, int size, Color color) {
        BufferedImage source = color == null ? image : ImageUtil.recolorImage(image, color);
        return new ImageIcon(new BaseMultiResolutionImage(ImageUtil.resizeImage(source, size, size, true), ImageUtil.resizeImage(source, size * 2, size * 2, true)));
    }

    /**
     * @param icon    its image
     * @param tooltip what it does
     * @param action  what happens on click
     * @return a borderless icon button that brightens under the mouse
     */
    static JButton iconButton(ImageIcon icon, String tooltip, Runnable action) {
        JButton button = new JButton(icon);
        SwingUtil.removeButtonDecorations(button);
        button.setRolloverIcon(brighter(icon));
        button.setToolTipText(tooltip);
        bare(button);
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());
        return button;
    }

    /**
     * @param text   the label
     * @param color  its background
     * @param action what happens on click, or null for a button that cannot be pressed
     * @return a flat text button in RuneLite's Plugin Hub style
     */
    static JButton textButton(String text, Color color, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(textFont());
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setBorder(new CompoundBorder(new LineBorder(color.darker()), new EmptyBorder(2, 8, 2, 8)));
        if (action == null) {
            button.setEnabled(false);
        } else {
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.addActionListener(e -> action.run());
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    button.setBackground(color.brighter());
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    button.setBackground(color);
                }
            });
        }
        return button;
    }

    /**
     * @param component a component that sits on a card
     * @return it, see-through so the card's hover colour shows behind it
     */
    static <T extends JComponent> T clear(T component) {
        component.setOpaque(false);
        return component;
    }

    /** @return a see-through panel for laying out a card's pieces */
    static JPanel row(LayoutManager layout) {
        return clear(new JPanel(layout));
    }

    /** @return the width cards are laid out at, used only before the panel has been shown */
    static int fallbackWidth() {
        return PluginPanel.PANEL_WIDTH;
    }

    /**
     * @param button an icon button
     * @return it with no background, border or focus ring, so only its icon shows
     */
    static <T extends AbstractButton> T bare(T button) {
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setBorder(new EmptyBorder(2, 2, 2, 2));
        return button;
    }

    private static ImageIcon brighter(ImageIcon icon) {
        Image image = icon.getImage();
        if (image instanceof BaseMultiResolutionImage) {
            BaseMultiResolutionImage variants = (BaseMultiResolutionImage) image;
            Image[] brighter = variants.getResolutionVariants().stream().map(variant -> ImageUtil.luminanceScale(variant, 1.6f)).toArray(Image[]::new);
            return new ImageIcon(new BaseMultiResolutionImage(brighter));
        }
        return new ImageIcon(ImageUtil.luminanceScale(image, 1.6f));
    }

    private static BufferedImage dots(int scale) {
        BufferedImage image = new BufferedImage(16 * scale, 16 * scale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(MUTED);
        int size = 3 * scale;
        for (int index = 0; index < 3; index++) {
            graphics.fillOval((2 + index * 5) * scale - 1 + scale, 8 * scale - size / 2, size, size);
        }
        graphics.dispose();
        return image;
    }

    private static BufferedImage load(String path) {
        return ImageUtil.loadImageResource(PluginPanel.class, path);
    }
}
