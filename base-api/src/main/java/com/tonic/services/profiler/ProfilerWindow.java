package com.tonic.services.profiler;

import com.tonic.model.ui.components.VitaFrame;
import com.tonic.services.profiler.heap.HeapTab;
import com.tonic.services.profiler.jit.JitTab;
import com.tonic.services.profiler.jvm.JvmTab;
import com.tonic.services.profiler.recording.RecordingTab;
import com.tonic.services.profiler.resource.ResourceMonitorTab;
import com.tonic.services.profiler.server.ProfilerServer;
import com.tonic.services.profiler.threads.ThreadsTab;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import static com.tonic.services.profiler.ProfilerTheme.*;

/**
 * The profiler window: its tabs, the API server controls and the timers that refresh them
 */
public class ProfilerWindow extends VitaFrame {
    private static ProfilerWindow instance;

    private final List<ProfilerTab> tabs = new ArrayList<>();
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final ResourceMonitorTab resourceMonitor = new ResourceMonitorTab();
    private final Timer metricsTimer;
    private final Timer refreshTimer;
    private ProfilerTab selected;
    private boolean closed;

    /**
     * Opens the profiler, brings it back when minimised, or closes it when it is showing
     */
    public static void toggle() {
        SwingUtilities.invokeLater(() -> {
            if (instance != null && instance.isVisible()) {
                if ((instance.getExtendedState() & Frame.ICONIFIED) != 0) {
                    instance.setExtendedState(instance.getExtendedState() & ~Frame.ICONIFIED);
                    instance.toFront();
                    return;
                }
                instance.dispose();
                return;
            }
            if (instance == null) {
                instance = new ProfilerWindow();
            }
            instance.setVisible(true);
            instance.toFront();
            instance.requestFocus();
        });
    }

    private ProfilerWindow() {
        super("VitaLite JVM Profiler");
        setTitle("VitaLite JVM Profiler");
        setSize(900, 700);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });

        add(resourceMonitor);
        add(new RecordingTab());
        add(new HeapTab());
        add(new ThreadsTab());
        add(new JvmTab());
        add(new JitTab());

        tabbedPane.setBackground(PANEL);
        tabbedPane.setForeground(TEXT);
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabbedPane.addChangeListener(e -> select(tabs.get(tabbedPane.getSelectedIndex())));

        getContentPanel().setLayout(new BorderLayout());
        getContentPanel().setBackground(BACKGROUND);
        getContentPanel().add(tabbedPane, BorderLayout.CENTER);
        getContentPanel().add(statusBar(), BorderLayout.SOUTH);

        select(tabs.get(0));
        metricsTimer = new Timer(1000, e -> resourceMonitor.collect());
        metricsTimer.start();
        refreshTimer = new Timer(2000, e -> selected.refresh());
        refreshTimer.start();
    }

    @Override
    public void dispose() {
        cleanup();
        super.dispose();
    }

    private void add(ProfilerTab tab) {
        tabs.add(tab);
        tabbedPane.addTab(tab.title(), tab.component());
    }

    private void select(ProfilerTab tab) {
        if (tab == selected) {
            return;
        }
        if (selected != null) {
            selected.hidden();
        }
        selected = tab;
        tab.shown();
    }

    private void cleanup() {
        if (closed) {
            return;
        }
        closed = true;
        metricsTimer.stop();
        refreshTimer.stop();
        selected.hidden();
        for (ProfilerTab tab : tabs) {
            tab.dispose();
        }
        instance = null;
    }

    private JPanel statusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(PANEL);
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(20, 21, 24)));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        right.setOpaque(false);

        JLabel apiStatus = label("API: Off");
        apiStatus.setFont(UI_SMALL);
        right.add(apiStatus);

        JLabel apiUrl = new JLabel();
        apiUrl.setFont(UI_SMALL);
        apiUrl.setForeground(ACCENT);
        apiUrl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        apiUrl.setToolTipText("Click to copy URL to clipboard");
        apiUrl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String url = url();
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
                apiUrl.setText("Copied!");
                apiUrl.setForeground(SUCCESS);
                Timer restore = new Timer(1000, event -> {
                    apiUrl.setText(url);
                    apiUrl.setForeground(ACCENT);
                });
                restore.setRepeats(false);
                restore.start();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!apiUrl.getText().equals("Copied!")) {
                    apiUrl.setText("<html><u>" + url() + "</u></html>");
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!apiUrl.getText().equals("Copied!")) {
                    apiUrl.setText(url());
                }
            }
        });
        right.add(apiUrl);

        JButton toggle = new JButton();
        toggle.setForeground(Color.WHITE);
        toggle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        toggle.setFocusPainted(false);
        toggle.setBorderPainted(false);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.setPreferredSize(new Dimension(90, 24));
        Runnable showState = () -> {
            boolean running = ProfilerServer.getInstance().isRunning();
            toggle.setText(running ? "Stop API" : "Start API");
            toggle.setBackground(running ? ProfilerTheme.ERROR : ACCENT);
            apiStatus.setText(running ? "API:" : "API: Off");
            apiUrl.setText(url());
            apiUrl.setVisible(running);
        };
        toggle.addActionListener(e -> {
            ProfilerServer server = ProfilerServer.getInstance();
            try {
                if (server.isRunning()) {
                    server.stop();
                } else {
                    server.start();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to start API server: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
            showState.run();
        });
        showState.run();
        right.add(toggle);

        statusBar.add(right, BorderLayout.EAST);
        return statusBar;
    }

    private static String url() {
        return "http://localhost:" + ProfilerServer.getInstance().getPort();
    }
}
