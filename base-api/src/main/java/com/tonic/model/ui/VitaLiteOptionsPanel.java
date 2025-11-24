package com.tonic.model.ui;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.events.PacketReceived;
import com.tonic.events.PacketSent;
import com.tonic.model.ui.components.*;
import com.tonic.packets.PacketBuffer;
import com.tonic.services.ClickManager;
import com.tonic.services.ClickStrategy;
import com.tonic.services.mouserecorder.DecodedMousePacket;
import com.tonic.services.mouserecorder.MousePacketDecoder;
import com.tonic.services.mouserecorder.trajectory.TrajectoryService;
import com.tonic.services.mouserecorder.trajectory.ui.TrajectoryTrainerMonitor;
import com.tonic.services.mouserecorder.trajectory.ui.TrajectorySettingsPanel;
import com.tonic.services.pathfinder.PathfinderAlgo;
import com.tonic.services.mouserecorder.MovementVisualization;
import com.tonic.services.profiler.ProfilerWindow;
import com.tonic.util.Profiler;
import com.tonic.util.ReflectBuilder;
import com.tonic.util.StaticIntFinder;
import com.tonic.util.ThreadPool;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;

import javax.swing.*;
import java.awt.*;

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
    private final ToggleSlider logMousePacketsToggle;
    private final ToggleSlider nameLogging;
    private final ToggleSlider logServerPacketsToggle;
    private final ToggleSlider logMenuActionsToggle;
    private final ToggleSlider hideLoggerToggle;
    private final ToggleSlider bankCacheToggle;
    private JFrame transportsEditor;

    private final ToggleSlider sendMouseMovement;
    private final ToggleSlider recordTrajectory;
    private Timer trajectoryCheckTimer;

    private VitaLiteOptionsPanel() {
        super(false);

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
        contentPanel.add(Box.createVerticalStrut(15));

        // General Settings
        CollapsiblePanel generalPanel = new CollapsiblePanel("General");
        headlessToggle = new ToggleSlider();
        generalPanel.addContent(createToggleOption(
                "Headless Mode",
                "Run without rendering",
                headlessToggle,
                () -> Static.setHeadless(headlessToggle.isSelected())
        ));
        generalPanel.addVerticalStrut(12);
        ToggleSlider neverLogToggle = new ToggleSlider();
        neverLogToggle.setSelected(Static.getVitaConfig().shouldNeverLog());
        generalPanel.addContent(createToggleOption(
                "Never Log",
                "Prevent the AFK logout",
                neverLogToggle,
                () -> Static.getVitaConfig().setNeverLog(neverLogToggle.isSelected())
        ));
        contentPanel.add(generalPanel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Logging Settings
        CollapsiblePanel loggingPanel = new CollapsiblePanel("Logging");

        logPacketsToggle = new ToggleSlider();
        loggingPanel.addContent(createToggleOption(
                "Log Packets",
                "Enable packet logging",
                logPacketsToggle,
                () -> {}
        ));
        loggingPanel.addVerticalStrut(12);

        logMousePacketsToggle = new ToggleSlider();
        loggingPanel.addContent(createToggleOption(
                "Log Mouse Packets",
                "Enable mouse packet logging",
                logMousePacketsToggle,
                () -> {}
        ));
        loggingPanel.addVerticalStrut(12);

        logServerPacketsToggle = new ToggleSlider();
        loggingPanel.addContent(createToggleOption(
                "Log Server Packets",
                "Enable server packet logging",
                logServerPacketsToggle,
                () -> {}
        ));
        loggingPanel.addVerticalStrut(12);

        logMenuActionsToggle = new ToggleSlider();
        loggingPanel.addContent(createToggleOption(
                "Log Menu Actions",
                "Enable menu action logging",
                logMenuActionsToggle,
                () -> {}
        ));
        loggingPanel.addVerticalStrut(12);

        nameLogging = new ToggleSlider();
        nameLogging.setSelected(Static.getVitaConfig().shouldLogNames());
        loggingPanel.addContent(createToggleOption(
                "Logger Names",
                "Show gameval names in logging",
                nameLogging,
                () -> Static.getVitaConfig().setShouldLogNames(nameLogging.isSelected())
        ));
        loggingPanel.addVerticalStrut(12);

        hideLoggerToggle = new ToggleSlider();
        loggingPanel.addContent(createToggleOption(
                "Hide Logger",
                "Hide the logger panel",
                hideLoggerToggle,
                () -> Logger.setLoggerVisible(!hideLoggerToggle.isSelected())
        ));
        loggingPanel.addVerticalStrut(12);

        // Log history limit spinner
        FancySpinner logHistorySpinner = new FancySpinner(
                "Log History Limit",
                1, 1000, Static.getVitaConfig().getLogHistoryLimit()
        );
        // Set initial Logger max messages from config
        Logger.setMaxMessages(Static.getVitaConfig().getLogHistoryLimit());
        Static.getVitaConfig().setLogHistoryLimit(Static.getVitaConfig().getLogHistoryLimit());
        logHistorySpinner.addChangeListener(e -> {
            int limit = logHistorySpinner.getValue().intValue();
            Static.getVitaConfig().setLogHistoryLimit(limit);
            Logger.setMaxMessages(limit);
        });
        loggingPanel.addContent(logHistorySpinner);

        contentPanel.add(loggingPanel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Caching Settings
        CollapsiblePanel cachingPanel = new CollapsiblePanel("Caching");

        ToggleSlider cachedRandomDat = new ToggleSlider();
        cachedRandomDat.setSelected(Static.getVitaConfig().shouldCacheRandomDat());
        cachingPanel.addContent(createToggleOption(
                "Cached RandomDat",
                "Spoof and cache per-account Random dat data",
                cachedRandomDat,
                () -> Static.getVitaConfig().setShouldCacheRandomDat(cachedRandomDat.isSelected())
        ));
        cachingPanel.addVerticalStrut(12);

        ToggleSlider cachedDeviceID = new ToggleSlider();
        cachedDeviceID.setSelected(Static.getVitaConfig().shouldCacheDeviceId());
        cachingPanel.addContent(createToggleOption(
                "Cached DeviceID",
                "Spoof and cache per-account DeviceID",
                cachedDeviceID,
                () -> Static.getVitaConfig().setShouldCacheDeviceId(cachedDeviceID.isSelected())
        ));
        cachingPanel.addVerticalStrut(12);

        bankCacheToggle = new ToggleSlider();
        bankCacheToggle.setSelected(Static.getVitaConfig().shouldCacheBank());
        cachingPanel.addContent(createToggleOption(
                "Persist Bank Cache",
                "Save the bank caching for reuse between sessions",
                bankCacheToggle,
                () -> Static.getVitaConfig().setShouldCacheBank(bankCacheToggle.isSelected())
        ));

        contentPanel.add(cachingPanel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Walker Settings
        CollapsiblePanel walkerPanel = new CollapsiblePanel("Walker");

        FancyDropdown<PathfinderAlgo> pathfinderAlgo = new FancyDropdown<>("Pathfinder Algo", PathfinderAlgo.class);
        PathfinderAlgo algo = Static.getVitaConfig().getPathfinderImpl();
        pathfinderAlgo.setSelectedItem(algo);

        pathfinderAlgo.addSelectionListener(event -> {
            Static.getVitaConfig().setPathfinderImpl(pathfinderAlgo.getSelectedItem());
        });

        walkerPanel.addContent(pathfinderAlgo);
        walkerPanel.addVerticalStrut(12);

        ToggleSlider drawPath = new ToggleSlider();
        drawPath.setSelected(Static.getVitaConfig().shouldDrawWalkerPath());
        walkerPanel.addContent(createToggleOption(
                "Draw Walker Path",
                "Draw the walker path on the floating and mini maps",
                drawPath,
                () -> Static.getVitaConfig().setShouldDrawWalkerPath(drawPath.isSelected())
        ));

        if(!Static.isRunningFromShadedJar())
        {
            walkerPanel.addVerticalStrut(12);
            FancyButton transportButton = new FancyButton("Transport Editor");
            transportButton.addActionListener(e -> toggleTransportsEditor());
            walkerPanel.addContent(transportButton);
        }

        contentPanel.add(walkerPanel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Scene Settings
        CollapsiblePanel scenePanel = new CollapsiblePanel("Scene");

        ToggleSlider drawCollision = new ToggleSlider();
        drawCollision.setSelected(Static.getVitaConfig().shouldDrawCollision());
        scenePanel.addContent(createToggleOption(
                "Draw Tile Collision",
                "Draw tile collision on the floating and mini maps",
                drawCollision,
                () -> Static.getVitaConfig().setShouldDrawCollision(drawCollision.isSelected())
        ));
        scenePanel.addVerticalStrut(12);

        ToggleSlider drawInteractable = new ToggleSlider();
        drawInteractable.setSelected(Static.getVitaConfig().shouldDrawInteractable());
        scenePanel.addContent(createToggleOption(
                "Draw Interactable Faces",
                "Draw lines showing where objects are interactable from.",
                drawInteractable,
                () -> Static.getVitaConfig().setShouldDrawInteractable(drawInteractable.isSelected())
        ));

        scenePanel.addVerticalStrut(12);

        ToggleSlider debugStratPathing = new ToggleSlider();
        Static.getVitaConfig().setDrawStratPath(false);
        scenePanel.addContent(createToggleOption(
                "Debug Strat Pathing",
                "Define warning and impassible tiles for strategic pathfinding with overlay.",
                debugStratPathing,
                () -> Static.getVitaConfig().setDrawStratPath(debugStratPathing.isSelected())
        ));

        scenePanel.addVerticalStrut(12);

        FancyButton distanceDebug = new FancyButton("Distance Debugger");
        distanceDebug.addActionListener(e -> {
            DistanceDebugger window = DistanceDebugger.getInstance();
            if (window.isVisible())
            {
                window.setVisible(false);
            }
            else
            {
                window.setVisible(true);
                window.toFront();
            }
        });
        scenePanel.addContent(distanceDebug);

        contentPanel.add(scenePanel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Input Settings
        CollapsiblePanel inputPanel = new CollapsiblePanel("Input");

        sendMouseMovement = new ToggleSlider();
        sendMouseMovement.setSelected(Static.getVitaConfig().shouldSpoofMouseMovemnt());
        inputPanel.addContent(createToggleOption(
                "Spoof Mouse Movement",
                "Spoof mouse movement data.",
                sendMouseMovement,
                () -> {
                    if(!Static.getVitaConfig().getHasAcceptedWarning())
                    {
                        int result = JOptionPane.showConfirmDialog(
                                this,
                                "Spoofing mouse movements is experimental and not guaranteed currently to be safe. Use at your own risk. Do you wish to proceed?",
                                "Warning",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE
                        );
                        if (result != JOptionPane.YES_OPTION) {
                            sendMouseMovement.setSelected(false);
                            return;
                        }
                        Static.getVitaConfig().setHasAcceptedWarning(true);
                    }
                    Static.getVitaConfig().setSpoofMouseMovement(sendMouseMovement.isSelected());

                    // Start/stop continuous movement sampling to match client behavior
                    if (sendMouseMovement.isSelected())
                    {
                        ClickManager.startMovementSampling();
                    }
                    else
                    {
                        ClickManager.stopMovementSampling();
                    }
                }
        ));
        inputPanel.addVerticalStrut(12);

        recordTrajectory = new ToggleSlider();
        inputPanel.addContent(createToggleOption(
                "Train Mouse Data",
                "Train a data set on your manual play for use in automated mouse movements.",
                recordTrajectory,
                () -> {
                    if (recordTrajectory.isSelected()) {
                        TrajectoryService.startRecording();
                    } else {
                        TrajectoryService.stopRecording();
                    }
                }
        ));
        inputPanel.addVerticalStrut(12);

        FancyButton monitor = new FancyButton("Training Monitor");
        monitor.addActionListener(e -> {
            TrajectoryTrainerMonitor window = TrajectoryTrainerMonitor.getInstance();
            if (window.isVisible())
            {
                window.setVisible(false);
            }
            else
            {
                window.setVisible(true);
                window.toFront();
            }
        });
        inputPanel.addContent(monitor);
        inputPanel.addVerticalStrut(6);

        FancyButton settings = new FancyButton("Movement Settings");
        settings.addActionListener(e -> {
            TrajectorySettingsPanel window = TrajectorySettingsPanel.getInstance();
            if (window.isVisible())
            {
                window.setVisible(false);
            }
            else
            {
                window.setVisible(true);
                window.toFront();
            }
        });
        inputPanel.addContent(settings);
        inputPanel.addVerticalStrut(12);

        ToggleSlider visualizeMovements = new ToggleSlider();
        visualizeMovements.setSelected(Static.getVitaConfig().shouldVisualizeMovements());
        // Sync initial state
        MovementVisualization.setEnabled(Static.getVitaConfig().shouldVisualizeMovements());
        inputPanel.addContent(createToggleOption(
                "Visualize Movements",
                "Show generated mouse movement paths on the game canvas.",
                visualizeMovements,
                () -> {
                    Static.getVitaConfig().setVisualizeMovements(visualizeMovements.isSelected());
                    MovementVisualization.setEnabled(visualizeMovements.isSelected());
                }
        ));
        inputPanel.addVerticalStrut(12);

        ToggleSlider visualizeClicks = new ToggleSlider();
        visualizeClicks.setSelected(Static.getVitaConfig().shouldVisualizeClicks());
        // Sync initial state
        inputPanel.addContent(createToggleOption(
                "Visualize Clicks",
                "Show generated mouse clicks on the game canvas.",
                visualizeClicks,
                () -> Static.getVitaConfig().setVisualizeClicks(visualizeClicks.isSelected())
        ));
        inputPanel.addVerticalStrut(12);

        FancyDropdown<ClickStrategy> clickStrategyDropdown = new FancyDropdown<>("Click Strategy", ClickStrategy.class);
        ClickStrategy strat = Static.getVitaConfig().getClickStrategy();
        clickStrategyDropdown.setSelectedItem(strat);

        FancyDualSpinner pointSpinner = new FancyDualSpinner(
                "Static Click Point",
                Integer.MIN_VALUE, Integer.MAX_VALUE, Static.getVitaConfig().getClickPointX(),
                Integer.MIN_VALUE, Integer.MAX_VALUE, Static.getVitaConfig().getClickPointY()
        );
        ClickManager.setPoint(pointSpinner.getLeftValue().intValue(), pointSpinner.getRightValue().intValue());
        pointSpinner.setVisible(strat == ClickStrategy.STATIC);
        pointSpinner.addChangeListener(e -> {
            Static.getVitaConfig().setClickPointX(pointSpinner.getLeftValue().intValue());
            Static.getVitaConfig().setClickPointY(pointSpinner.getRightValue().intValue());
            ClickManager.setPoint(pointSpinner.getLeftValue().intValue(), pointSpinner.getRightValue().intValue());
        });

        clickStrategyDropdown.addSelectionListener(event -> {
            Static.getVitaConfig().setClickStrategy(clickStrategyDropdown.getSelectedItem());
            pointSpinner.setVisible(clickStrategyDropdown.getSelectedItem() == ClickStrategy.STATIC);
        });

        inputPanel.addContent(clickStrategyDropdown);
        inputPanel.addVerticalStrut(12);
        inputPanel.addContent(pointSpinner);

        contentPanel.add(inputPanel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Debug Settings
        CollapsiblePanel debugPanel = new CollapsiblePanel("Debug");

        FancyButton profilerButton = new FancyButton("Profiler");
        profilerButton.addActionListener(e -> ProfilerWindow.toggle());
        debugPanel.addContent(profilerButton);
        debugPanel.addVerticalStrut(12);

        FancyButton checkButton = new FancyButton("Check Platform Info");
        checkButton.addActionListener(e -> PlatformInfoViewer.toggle());
        debugPanel.addContent(checkButton);
        debugPanel.addVerticalStrut(12);

        FancyButton mouseButton = new FancyButton("Check Mouse Values");
        mouseButton.addActionListener(e -> checkMouseValues());
        debugPanel.addContent(mouseButton);

        contentPanel.add(debugPanel);
        contentPanel.add(Box.createVerticalStrut(15));

        // Now set the proper dimensions after all components are added
        contentPanel.setMaximumSize(new Dimension(PANEL_WIDTH, Integer.MAX_VALUE));
        contentPanel.revalidate();
        contentPanel.repaint();

        // Create custom styled scroll pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        customizeScrollPane(scrollPane);

        add(scrollPane, BorderLayout.CENTER);

        startTrajectoryCheckTimer();
        updateTrajectoryQualityState();

        // Auto-start movement sampling if spoofing is already enabled
        if (Static.getVitaConfig().shouldSpoofMouseMovemnt())
        {
            ClickManager.startMovementSampling();
        }
    }

    private void customizeScrollPane(JScrollPane scrollPane) {
        // Configure scroll pane for dark theme
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

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

    public void onPacketSent(PacketSent event)
    {
        int isMouse = event.isMouse();
        if(isMouse != 0)
        {
            if(logMousePacketsToggle.isSelected() && isMouse == 1)
            {
                Logger.info(event.toString());
            }

            if(logMousePacketsToggle.isSelected() || recordTrajectory.isSelected() && isMouse == 2)
            {
                DecodedMousePacket decodedInfo = MousePacketDecoder.decode(event.getFreshBuffer());
                if(recordTrajectory.isSelected())
                {
                    TrajectoryService.getPacketCapture().submitDecodedPacket(decodedInfo);
                }
                if(logMousePacketsToggle.isSelected())
                {
                    Logger.info("[OP_MOUSE_MOVEMENT(" + event.getId() + ")] " + decodedInfo);
                }
            }
            return;
        }

        if(!logPacketsToggle.isSelected())
        {
            return;
        }

        String packetInfo = event.toString();

        if(packetInfo.startsWith("[UNKNOWN("))
        {
            return;
        }

        Logger.info(packetInfo);
    }

    public void onPacketReceived(PacketReceived event)
    {
        if(!logServerPacketsToggle.isSelected())
            return;

        String packetInfo = event.toHex();
        int id = event.getId();
        int len = event.getLength();
        Logger.info("[ServerPacket(" + id + ":" + len + ")] " + packetInfo);
    }

    private void startTrajectoryCheckTimer()
    {
        if (trajectoryCheckTimer != null)
        {
            trajectoryCheckTimer.stop();
        }

        trajectoryCheckTimer = new Timer(5000, e -> updateTrajectoryQualityState());
        trajectoryCheckTimer.setRepeats(true);
        trajectoryCheckTimer.start();
    }

    private void updateTrajectoryQualityState()
    {
        SwingUtilities.invokeLater(() -> {
            try
            {
                int trajectoryCount = TrajectoryService.getDatabase().getTrajectoryCount();
                boolean shouldEnable = trajectoryCount >= 50;

                if (sendMouseMovement != null)
                {
                    sendMouseMovement.setEnabled(shouldEnable);
                    if (!shouldEnable)
                    {
                        sendMouseMovement.setToolTipText(String.format(
                            "Training data insufficient: %d trajectories (requires 50)", trajectoryCount));
                        sendMouseMovement.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                    else
                    {
                        sendMouseMovement.setToolTipText("Spoof mouse movement data.");
                        sendMouseMovement.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
                }
            }
            catch (Exception ex)
            {
                Logger.warn("Failed to update trajectory quality state: " + ex.getMessage());
            }
        });
    }
}