package com.tonic.ui.sdn;

import com.tonic.ui.VitaTheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;

/**
 * The Vita Hub tab's own pieces on top of VitaTheme: its icons and RuneLite config switches in the Vita accent.
 */
public final class HubTheme {
    static final int ICON_SIZE = 32;

    private static final String CONFIG = "/net/runelite/client/plugins/config/";
    private static ImageIcon github;
    private static ImageIcon info;
    private static ImageIcon back;
    private static ImageIcon switchOn;
    private static ImageIcon switchOff;

    private HubTheme() {
    }

    static ImageIcon github() {
        if (github == null) {
            github = VitaTheme.icon(load("/net/runelite/client/plugins/info/github_icon.png"), 16, VitaTheme.MUTED);
        }
        return github;
    }

    static ImageIcon info() {
        if (info == null) {
            info = new ImageIcon(new BaseMultiResolutionImage(infoGlyph(1), infoGlyph(2)));
        }
        return info;
    }

    static ImageIcon back() {
        if (back == null) {
            back = new ImageIcon(load(CONFIG + "config_back_icon.png"));
        }
        return back;
    }

    static ImageIcon switchOn() {
        if (switchOn == null) {
            switchOn = new ImageIcon(ImageUtil.recolorImage(load(CONFIG + "switcher_on.png"), VitaTheme.accent()));
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

    public static void styleSwitch(JCheckBox checkbox) {
        checkbox.setIcon(switchOff());
        checkbox.setSelectedIcon(switchOn());
    }

    private static BufferedImage infoGlyph(int scale) {
        BufferedImage image = new BufferedImage(16 * scale, 16 * scale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setColor(VitaTheme.MUTED);
        graphics.setStroke(new BasicStroke(1.5f * scale));
        graphics.draw(new Ellipse2D.Float(1.75f * scale, 1.75f * scale, 12.5f * scale, 12.5f * scale));
        graphics.fill(new Ellipse2D.Float(7f * scale, 4f * scale, 2f * scale, 2f * scale));
        graphics.fill(new RoundRectangle2D.Float(7.1f * scale, 7f * scale, 1.8f * scale, 5f * scale, 0.8f * scale, 0.8f * scale));
        graphics.dispose();
        return image;
    }

    private static BufferedImage load(String path) {
        return ImageUtil.loadImageResource(PluginPanel.class, path);
    }
}
