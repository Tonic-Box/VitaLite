package com.tonic.model.ui;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.events.PacketSent;
import com.tonic.model.DeviceID;
import com.tonic.model.RandomDat;
import com.tonic.model.ui.components.*;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickStrategy;
import com.tonic.services.ConfigManager;
import com.tonic.util.ReflectBuilder;
import com.tonic.util.ReflectUtil;
import com.tonic.util.ThreadPool;

import javax.swing.*;
import java.awt.*;
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
    private final ToggleSlider hideLoggerToggle;
    private JFrame transportsEditor;
    private JFrame jShell;
    private final ConfigManager config = new ConfigManager("VitaLiteOptions");

    private VitaLiteOptionsPanel() {
        super(false); // Don't use default wrapping, we'll handle scrolling ourselves

        Map<String,Object> defaults = Map.of(
                "clickStrategy", ClickStrategy.STATIC.name(),
                "clickPointX", -1,
                "clickPointY", -1,
                "cachedRandomDat", true,
                "cachedDeviceID", true
        );
        config.ensure(defaults);

        // Set up the main panel layout
        setLayout(new BorderLayout());
        setOpaque(false);

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
        contentPanel.add(Box.createVerticalStrut(12));

        hideLoggerToggle = new ToggleSlider();
        contentPanel.add(createToggleOption(
                "Hide Logger",
                "Hide the logger panel",
                hideLoggerToggle,
                () -> Logger.setLoggerVisible(!hideLoggerToggle.isSelected())
        ));
        contentPanel.add(Box.createVerticalStrut(12));

        ToggleSlider cachedRandomDat = new ToggleSlider();
        contentPanel.add(createToggleOption(
                "Cached RandomDat",
                "Spoof and cache per-account Random dat data",
                cachedRandomDat,
                () -> {
                    RandomDat.setUseCachedRandomDat(cachedRandomDat.isSelected());
                    config.setProperty("cachedRandomDat", cachedRandomDat.isSelected());
                }
        ));
        cachedRandomDat.setSelected(config.getBoolean("cachedRandomDat"));
        RandomDat.setUseCachedRandomDat(cachedRandomDat.isSelected());
        contentPanel.add(Box.createVerticalStrut(12));

        ToggleSlider cachedDeviceID = new ToggleSlider();
        contentPanel.add(createToggleOption(
                "Cached DeviceID",
                "Spoof and cache per-account DeviceID",
                cachedDeviceID,
                () -> {
                    DeviceID.setUseCachedUUID(cachedDeviceID.isSelected());
                    config.setProperty("cachedDeviceID", cachedDeviceID.isSelected());
                }
        ));
        cachedDeviceID.setSelected(config.getBoolean("cachedDeviceID"));
        DeviceID.setUseCachedUUID(cachedDeviceID.isSelected());
        contentPanel.add(Box.createVerticalStrut(12));

        FancyDualSpinner pointSpinner = new FancyDualSpinner(
                "Static Click Point",
                Integer.MIN_VALUE, Integer.MAX_VALUE, config.getInt("clickPointX"),
                Integer.MIN_VALUE, Integer.MAX_VALUE, config.getInt("clickPointY")
        );
        ClickManager.setPoint(pointSpinner.getLeftValue().intValue(), pointSpinner.getRightValue().intValue());
        pointSpinner.addChangeListener(e -> {
            config.setProperty("clickPointX", pointSpinner.getLeftValue().intValue());
            config.setProperty("clickPointY", pointSpinner.getRightValue().intValue());
            ClickManager.setPoint(pointSpinner.getLeftValue().intValue(), pointSpinner.getRightValue().intValue());
        });
        pointSpinner.setVisible(ClickManager.getStrategy() == ClickStrategy.STATIC);

        FancyDropdown<ClickStrategy> clickStrategyDropdown = new FancyDropdown<>("Click Strategy", ClickStrategy.class);
        String name = config.getString("clickStrategy");
        clickStrategyDropdown.setSelectedItem(ClickStrategy.valueOf(name));
        ClickManager.setStrategy(clickStrategyDropdown.getSelectedItem());
        pointSpinner.setVisible(ClickManager.getStrategy() == ClickStrategy.STATIC);
        clickStrategyDropdown.addSelectionListener(event -> {
            config.setProperty("clickStrategy", clickStrategyDropdown.getSelectedItem().name());
            ClickManager.setStrategy(clickStrategyDropdown.getSelectedItem());
            pointSpinner.setVisible(ClickManager.getStrategy() == ClickStrategy.STATIC);
        });
        contentPanel.add(clickStrategyDropdown);
        contentPanel.add(Box.createVerticalStrut(12));
        contentPanel.add(pointSpinner);

        contentPanel.add(Box.createVerticalStrut(12));

        FancyButton transportButton = new FancyButton("Transport Editor");
        transportButton.addActionListener(e -> toggleTransportsEditor());
        contentPanel.add(transportButton);

        contentPanel.add(Box.createVerticalStrut(20));
        JLabel debugLabel = new JLabel("Debug");
        debugLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        debugLabel.setForeground(HEADER_COLOR);
        debugLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        debugLabel.add(Box.createVerticalStrut(10));
        contentPanel.add(debugLabel);
        contentPanel.add(Box.createVerticalStrut(12));

        FancyButton mouseButton = new FancyButton("Check Mouse Values");
        mouseButton.addActionListener(e -> checkMouseValues());
        contentPanel.add(mouseButton);

        // Now set the proper dimensions after all components are added
        contentPanel.setPreferredSize(new Dimension(PANEL_WIDTH, contentPanel.getPreferredSize().height));
        contentPanel.setMaximumSize(new Dimension(PANEL_WIDTH, Integer.MAX_VALUE));

        // Create custom styled scroll pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        customizeScrollPane(scrollPane);

        add(scrollPane, BorderLayout.CENTER);
    }

    private void customizeScrollPane(JScrollPane scrollPane) {
        // Configure scroll pane for dark theme
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Style the vertical scrollbar
        scrollPane.getVerticalScrollBar().setOpaque(false);
        scrollPane.getVerticalScrollBar().setBackground(new Color(40, 40, 40));
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(80, 80, 85);
                this.thumbDarkShadowColor = new Color(60, 60, 65);
                this.thumbHighlightColor = new Color(100, 100, 105);
                this.thumbLightShadowColor = new Color(70, 70, 75);
                this.trackColor = new Color(40, 40, 40);
                this.trackHighlightColor = new Color(50, 50, 55);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(thumbColor);
                g2d.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                                thumbBounds.width - 4, thumbBounds.height - 4, 6, 6);
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(trackColor);
                g2d.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            }
        });

        // Smooth scrolling
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setBlockIncrement(64);
    }

    private JFrame getTransportsEditor()
    {
        try
        {
            Class<?> clazz = Static.getClient().getClass().getClassLoader().loadClass("com.tonic.services.pathfinder.ui.TransportEditorFrame");
            return (JFrame) clazz.getDeclaredConstructor().newInstance();
        }
        catch (Exception e)
        {
            Logger.error("Failed to open Transports Editor: " + e.getMessage());
        }
        return null;
    }

    public void toggleTransportsEditor()
    {
        if(transportsEditor == null)
        {
            transportsEditor = ThreadPool.submit(this::getTransportsEditor);
        }
        SwingUtilities.invokeLater(() -> transportsEditor.setVisible(!transportsEditor.isVisible()));
    }

    private void openCodeEvaluator()
    {
        try
        {
            Class<?> codeEvalFrameClass = Static.getClient().getClass().getClassLoader().loadClass("com.tonic.services.codeeval.CodeEvalFrame");
            java.lang.reflect.Method getMethod = codeEvalFrameClass.getMethod("get");
            getMethod.invoke(null);
        }
        catch (Exception e)
        {
            Logger.error("Failed to open Code Evaluator: " + e.getMessage());
        }
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