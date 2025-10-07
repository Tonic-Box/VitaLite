package com.tonic.ui.sdn;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.services.hotswapper.PluginReloader;
import com.tonic.util.ThreadPool;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VitaExternalsPanel extends PluginPanel {

    private static VitaExternalsPanel INSTANCE = null;

    public static VitaExternalsPanel get() {
        if (INSTANCE == null) {
            INSTANCE = new VitaExternalsPanel();
        }
        return INSTANCE;
    }

    private static final Color BACKGROUND = new Color(24, 26, 31);
    private static final Color SURFACE = new Color(32, 34, 42);
    private static final Color SURFACE_LIGHT = new Color(42, 44, 54);
    private static final Color ACCENT_BLUE = new Color(59, 142, 255);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 245);
    private static final Color TEXT_SECONDARY = new Color(160, 165, 180);
    private static final Color SUCCESS = new Color(46, 204, 113);
    private static final Color DANGER = new Color(231, 76, 60);

    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 10);

    private final File pluginDirectory;
    private final List<PluginEntry> localPlugins = new ArrayList<>();
    private final List<PluginEntry> sdnPlugins = new ArrayList<>();

    private JPanel cardContainer;
    private JTextField searchField;
    private JLabel statusLabel;
    private boolean showingLocal = true;

    public VitaExternalsPanel() {
        this.pluginDirectory = Static.RUNELITE_DIR.resolve("sideloaded-plugins").toFile();
        if (!pluginDirectory.exists()) {
            pluginDirectory.mkdirs();
        }

        setLayout(new BorderLayout());
        setBackground(BACKGROUND);

        add(createHeaderPanel(), BorderLayout.NORTH);

        cardContainer = new JPanel();
        cardContainer.setLayout(new GridBagLayout());
        cardContainer.setBackground(BACKGROUND);

        JScrollPane scrollPane = new JScrollPane(cardContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BACKGROUND);

        add(scrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(TEXT_SECONDARY);
        statusLabel.setFont(NORMAL_FONT);
        statusLabel.setBorder(new EmptyBorder(8, 12, 8, 12));
        statusLabel.setBackground(SURFACE);
        statusLabel.setOpaque(true);
        add(statusLabel, BorderLayout.SOUTH);

        loadLocalPlugins();
        displayPlugins();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(SURFACE);

        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setBackground(SURFACE);
        tabPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JButton localTab = createTabButton("Local", true);
        JButton sdnTab = createTabButton("SDN", false);

        localTab.addActionListener(e -> {
            showingLocal = true;
            localTab.setForeground(TEXT_PRIMARY);
            sdnTab.setForeground(TEXT_SECONDARY);
            displayPlugins();
            refresh();
        });

        sdnTab.addActionListener(e -> {
            showingLocal = false;
            sdnTab.setForeground(TEXT_PRIMARY);
            localTab.setForeground(TEXT_SECONDARY);
            displayPlugins();
            refresh();
        });

        tabPanel.add(localTab);
        tabPanel.add(sdnTab);
        header.add(tabPanel);

        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchPanel.setBackground(SURFACE);
        searchPanel.setBorder(new EmptyBorder(0, 0, 12, 0));
        searchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        searchField = new JTextField();
        searchField.setBackground(SURFACE_LIGHT);
        searchField.setForeground(TEXT_PRIMARY);
        searchField.setCaretColor(ACCENT_BLUE);
        searchField.setFont(NORMAL_FONT);
        searchField.setBorder(new EmptyBorder(8, 12, 8, 12));

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterPlugins(); }
            public void removeUpdate(DocumentEvent e) { filterPlugins(); }
            public void changedUpdate(DocumentEvent e) { filterPlugins(); }
        });

        JPanel buttonPanel = new JPanel(new BorderLayout(10, 0));
        buttonPanel.setBackground(SURFACE);

        JButton addRepoButton = new JButton("Add Repo");
        addRepoButton.setBackground(ACCENT_BLUE);
        addRepoButton.setForeground(Color.WHITE);
        addRepoButton.setFocusPainted(false);
        addRepoButton.setBorderPainted(false);
        addRepoButton.setFont(NORMAL_FONT.deriveFont(Font.BOLD));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setBackground(ACCENT_BLUE);
        refreshButton.setForeground(TEXT_PRIMARY);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(false);
        refreshButton.setFont(NORMAL_FONT.deriveFont(Font.BOLD));
        refreshButton.addActionListener(e -> refresh());

        buttonPanel.add(addRepoButton, BorderLayout.WEST);
        buttonPanel.add(refreshButton, BorderLayout.CENTER);

        searchPanel.add(searchField, BorderLayout.NORTH);
        searchPanel.add(buttonPanel, BorderLayout.SOUTH);
        header.add(searchPanel);

        return header;
    }

    private JButton createTabButton(String text, boolean active) {
        JButton button = new JButton(text);
        button.setBackground(SURFACE);
        button.setForeground(active ? TEXT_PRIMARY : TEXT_SECONDARY);
        button.setFont(NORMAL_FONT.deriveFont(Font.BOLD));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(100, 36));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void loadLocalPlugins() {
        localPlugins.clear();
        File[] files = pluginDirectory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName().replace(".jar", "");
                localPlugins.add(new PluginEntry(name, file));
            }
        }
    }

    public void addSDNPlugin(String name, String version, String author) {
        sdnPlugins.add(new PluginEntry(name, null));
        if (!showingLocal) {
            displayPlugins();
        }
    }

    private void displayPlugins() {
        cardContainer.removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 8, 4, 8);

        List<PluginEntry> plugins = showingLocal ? localPlugins : sdnPlugins;

        for (int i = 0; i < plugins.size(); i++) {
            gbc.gridy = i;
            cardContainer.add(createPluginCard(plugins.get(i)), gbc);
        }

        gbc.gridy = plugins.size();
        gbc.weighty = 1.0;
        cardContainer.add(Box.createVerticalGlue(), gbc);

        cardContainer.revalidate();
        cardContainer.repaint();
    }

    private JPanel createPluginCard(PluginEntry plugin) {
        JPanel card = new JPanel();
        card.setLayout(new GridBagLayout());
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel nameLabel = new JLabel(plugin.name);
        nameLabel.setFont(TITLE_FONT);
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        gbc.gridy = 0;
        card.add(nameLabel, gbc);

        JButton actionButton = new JButton(showingLocal ? "Reload" : "Install");
        actionButton.setBackground(showingLocal ? ACCENT_BLUE : SUCCESS);
        actionButton.setForeground(Color.WHITE);
        actionButton.setFont(NORMAL_FONT.deriveFont(Font.BOLD));
        actionButton.setFocusPainted(false);
        actionButton.setBorderPainted(false);
        actionButton.setOpaque(true);
        actionButton.setPreferredSize(new Dimension(0, 16));
        actionButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (showingLocal) {
            actionButton.addActionListener(e -> ThreadPool.submit(() -> {
                if(!PluginReloader.reloadPlugin(plugin.file))
                {
                    PluginReloader.forceRebuildPluginList();
                    Logger.error("Failed to reload plugin: " + plugin.file.getName());
                    return;
                }
                PluginReloader.forceRebuildPluginList();
                Logger.info("Reloaded plugin: " + plugin.file.getName());
            }));
        }

        gbc.gridy = 1;
        card.add(actionButton, gbc);

        return card;
    }

    private void removePlugin(PluginEntry plugin) {
        if (plugin.file != null && plugin.file.exists()) {
            plugin.file.delete();
            localPlugins.remove(plugin);
            displayPlugins();
            statusLabel.setText("Removed " + plugin.name);
        }
    }

    private void filterPlugins() {
        String search = searchField.getText().toLowerCase();
        displayPlugins();
    }

    private void refresh() {
        loadLocalPlugins();
        displayPlugins();
        statusLabel.setText("Refreshed");
        revalidate();
        repaint();
    }

    private static class PluginEntry {
        final String name;
        final File file;

        PluginEntry(String name, File file) {
            this.name = name;
            this.file = file;
        }
    }
}