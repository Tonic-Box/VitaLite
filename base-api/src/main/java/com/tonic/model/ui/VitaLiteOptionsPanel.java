package com.tonic.model.ui;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.events.PacketSent;
import com.tonic.model.ui.componants.OptionPanel;
import com.tonic.model.ui.componants.ToggleSlider;
import com.tonic.model.ui.componants.VPluginPanel;
import com.tonic.util.ReflectBuilder;
import com.tonic.util.ReflectUtil;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class VitaLiteOptionsPanel extends VPluginPanel {

    private static VitaLiteOptionsPanel INSTANCE;

    public static VitaLiteOptionsPanel getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VitaLiteOptionsPanel();
        }
        return INSTANCE;
    }

    private static final Color BACKGROUND_GRADIENT_START = new Color(45, 45, 50);
    private static final Color BACKGROUND_GRADIENT_END = new Color(35, 35, 40);
    private static final Color ACCENT_GLOW = new Color(64, 169, 211, 30);
    private static final Color HEADER_COLOR  = new Color(245, 245, 250);
    private static final Color SEPARATOR_COLOR = new Color(70, 70, 75);
    private static final Color CARD_BACKGROUND = new Color(55, 55, 60);
    private static final Color ACCENT_COLOR = new Color(64, 169, 211);
    private final ToggleSlider headlessToggle;
    private final ToggleSlider logPacketsToggle;
    private final ToggleSlider logMenuActionsToggle;

    private VitaLiteOptionsPanel() {
        super(true);

        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gradient = new GradientPaint(
                        0, 0, BACKGROUND_GRADIENT_START,
                        0, getHeight(), BACKGROUND_GRADIENT_END
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.add(Box.createVerticalStrut(10));

        JPanel titlePanel = createGlowPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Settings");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(HEADER_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(Box.createVerticalStrut(10));
        titlePanel.add(titleLabel);

        JLabel taglineLabel = new JLabel("Enhanced RuneLite Experience");
        taglineLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        taglineLabel.setForeground(ACCENT_COLOR);
        taglineLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(taglineLabel);
        titlePanel.add(Box.createVerticalStrut(10));

        contentPanel.add(titlePanel);
        contentPanel.add(Box.createVerticalStrut(10));

        headlessToggle = new ToggleSlider();
        contentPanel.add(createToggleOption(
                "Headless Mode",
                "Run without rendering",
                headlessToggle,
                () -> Static.setHeadless(headlessToggle.isSelected())
        ));
        contentPanel.add(Box.createVerticalStrut(12));

        logPacketsToggle = new ToggleSlider();
        contentPanel.add(createToggleOption(
                "Log Packets",
                "Enable packet logging",
                logPacketsToggle,
                () -> {}
        ));
        contentPanel.add(Box.createVerticalStrut(12));

        logMenuActionsToggle = new ToggleSlider();
        contentPanel.add(createToggleOption(
                "Log Menu Actions",
                "Enable menu action logging",
                logMenuActionsToggle,
                () -> {}
        ));

        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(createSeparator());
        contentPanel.add(Box.createVerticalStrut(20));

        JButton checkButton = new JButton("Check Platform Info");
        checkButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        checkButton.addActionListener(e -> {
            checkShit();
            Logger.info("Platform info checked and logged.");
        });
        checkButton.setMaximumSize(new Dimension(PANEL_WIDTH - 40, 30));
        checkButton.setBackground(ACCENT_COLOR);
        checkButton.setForeground(Color.WHITE);
        checkButton.setFocusPainted(false);
        checkButton.setBorderPainted(false);
        checkButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contentPanel.add(checkButton);

        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(createSeparator());
        contentPanel.add(Box.createVerticalStrut(20));

        JButton mouseButton = new JButton("Check Mouse Values");
        mouseButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        mouseButton.addActionListener(e -> checkMouseValues());
        mouseButton.setMaximumSize(new Dimension(PANEL_WIDTH - 40, 30));
        mouseButton.setBackground(ACCENT_COLOR);
        mouseButton.setForeground(Color.WHITE);
        mouseButton.setFocusPainted(false);
        mouseButton.setBorderPainted(false);
        mouseButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mouseButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        contentPanel.add(mouseButton);

        add(contentPanel);
    }

    private void checkMouseValues()
    {
        Object client = Static.getClient();
        long client_latsPressed = ReflectBuilder.of(client)
                .method("getClientMouseLastPressedMillis", null, null)
                .get();

        long mh_lastPressed = ReflectBuilder.of(client)
                .method("getMouseHandler", null, null)
                .method("getMouseLastPressedMillis", null, null)
                .get();

        long ms = System.currentTimeMillis();

        int time = (int) (ms - mh_lastPressed);

        short info = (short)(time << 1);

        Logger.info("Client last pressed: " + client_latsPressed + ", MouseHandler last pressed: " + mh_lastPressed + ", Diff: " + time + ", Info: " + info);
    }

    private void checkShit()
    {
        Object platInfo = ReflectBuilder.ofClass("le")
                .staticField("wc")
                .get();

        ReflectUtil.inspectNonStaticFields(platInfo);
    }

    private JPanel createToggleOption(String title, String description, ToggleSlider toggle, Runnable onClick) {
        OptionPanel optionPanel = new OptionPanel();
        optionPanel.init(title, description, toggle, onClick);
        return optionPanel;
    }

    private JPanel createGlowPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(ACCENT_GLOW);
                int glowRadius = 20;
                for (int i = glowRadius; i > 0; i--) {
                    float alpha = (float)(glowRadius - i) / glowRadius * 0.3f;
                    g2d.setColor(new Color(64, 169, 211, (int)(alpha * 255)));
                    g2d.fillRoundRect(i/2, i/2, getWidth() - i, getHeight() - i, 15, 15);
                }

                g2d.setColor(CARD_BACKGROUND);
                g2d.fillRoundRect(5, 5, getWidth() - 10, getHeight() - 10, 10, 10);
            }
        };
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(PANEL_WIDTH - 20, 65));
        return panel;
    }

    public void onMenuAction(String option, String target, int identifier, int opcode, int param0, int param1, int itemId)
    {
        if(!logMenuActionsToggle.isSelected())
            return;

        String actionInfo = String.format("MenuAction: option='%s', target='%s', id=%d, opcode=%d, param0=%d, param1=%d, itemId=%d",
                option, target, identifier, opcode, param0, param1, itemId);

        Logger.info(actionInfo);
    }

    private JSeparator createSeparator() {
        JSeparator separator = new JSeparator();
        separator.setForeground(SEPARATOR_COLOR);
        separator.setMaximumSize(new Dimension(PANEL_WIDTH - 60, 1));
        return separator;
    }

    public void onPacketSent(PacketSent event)
    {
        if(!logPacketsToggle.isSelected())
            return;

        String packetInfo = event.toString();

        if(packetInfo.startsWith("[UNKNOWN("))
            return;

        Logger.info(packetInfo);
    }
}