package com.tonic.ui.sdn;

import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.services.hotswapper.PluginReloader;
import com.tonic.services.hub.HubFetcher;
import com.tonic.services.hub.HubPlugin;
import com.tonic.services.hub.HubRepo;
import com.tonic.services.hub.HubService;
import com.tonic.services.hub.HubState;
import com.tonic.util.ThreadPool;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The Vita Hub tab: Installed lists the plugins you have, from the Hub and sideloaded, and Browse what the enabled
 * repositories offer; repositories are managed on a page of their own.
 */
public class VitaExternalsPanel extends PluginPanel {
    private static VitaExternalsPanel INSTANCE = null;

    /** @return the panel, created the first time */
    public static VitaExternalsPanel get() {
        if (INSTANCE == null) {
            INSTANCE = new VitaExternalsPanel();
        }
        return INSTANCE;
    }

    private static final String MAIN = "main";
    private static final String REPOS = "repos";
    private static final String ALL_REPOS = "All repositories";

    private final HubService hub = HubService.get();
    private final File pluginDirectory = Static.RUNELITE_DIR.resolve("sideloaded-plugins").toFile();
    private final CardLayout pages = new CardLayout();
    private final MaterialTabGroup tabs = new MaterialTabGroup();
    private final MaterialTab installedTab = new MaterialTab("Installed", tabs, null);
    private final MaterialTab browseTab = new MaterialTab("Browse", tabs, null);
    private final IconTextField search = new IconTextField();
    private final JComboBox<String> repoFilter = new JComboBox<>();
    private final Map<String, String> repoUrls = new LinkedHashMap<>();
    private final JPanel list = HubTheme.row(new DynamicGridLayout(0, 1, 0, HubTheme.GAP));
    private final ReposPage reposPage = new ReposPage(() -> showPage(MAIN));
    private final Set<String> expanded = new HashSet<>();
    private boolean browsing;
    private boolean showIncompatible;
    private boolean showingRepos;
    private boolean updatingFilter;
    private boolean rebuildQueued;
    private String notice;
    private JButton moreButton;

    public VitaExternalsPanel() {
        super(false);
        setLayout(pages);
        setBackground(HubTheme.BACKGROUND);
        if (!pluginDirectory.exists()) {
            pluginDirectory.mkdirs();
        }

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(HubTheme.BACKGROUND);
        main.add(header(), BorderLayout.NORTH);

        ReposPage.ScrollColumn column = new ReposPage.ScrollColumn();
        column.setLayout(new BorderLayout());
        column.setBorder(new EmptyBorder(0, 10, 10, 10));
        column.add(list, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(column, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(HubTheme.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        main.add(scroll, BorderLayout.CENTER);

        add(main, MAIN);
        add(reposPage, REPOS);

        hub.addListener(this::queueRebuild);
        HubIcons.addListener(this::queueRebuild);
        try {
            PluginControls.onPluginChanged(this::queueRebuild);
        } catch (RuntimeException e) {
            Logger.error("Vita Hub: could not follow plugin changes: " + e.getMessage());
        }

        tabs.select(installedTab);
        rebuild();
    }

    @Override
    public void onActivate() {
        if (!hub.hasFetched() && !hub.isBusy()) {
            hub.refresh();
        }
        rebuild();
    }

    private JPanel header() {
        JPanel header = HubTheme.row(new DynamicGridLayout(0, 1, 0, 8));
        header.setBorder(new EmptyBorder(10, 10, 10, 10));

        installedTab.setOnSelectEvent(() -> {
            browsing = false;
            queueRebuild();
            return true;
        });
        browseTab.setOnSelectEvent(() -> {
            browsing = true;
            if (!hub.hasFetched() && !hub.isBusy()) {
                hub.refresh();
            }
            queueRebuild();
            return true;
        });
        installedTab.setFont(HubTheme.regularFont());
        browseTab.setFont(HubTheme.regularFont());
        tabs.addTab(installedTab);
        tabs.addTab(browseTab);
        header.add(tabs);

        search.setIcon(IconTextField.Icon.SEARCH);
        search.setBackground(HubTheme.CARD);
        search.setHoverBackgroundColor(HubTheme.CARD_HOVER);
        search.setPreferredSize(new Dimension(0, 30));
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { queueRebuild(); }
            public void removeUpdate(DocumentEvent e) { queueRebuild(); }
            public void changedUpdate(DocumentEvent e) { queueRebuild(); }
        });
        search.addClearListener(this::queueRebuild);
        header.add(search);

        repoFilter.setFont(HubTheme.textFont());
        repoFilter.setFocusable(false);
        repoFilter.setToolTipText("Show one repository's plugins");
        repoFilter.addActionListener(e -> {
            if (!updatingFilter) {
                queueRebuild();
            }
        });

        JPanel buttons = HubTheme.row(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        buttons.add(HubTheme.iconButton(HubTheme.reload(), "Check repositories for new plugins and updates", hub::refresh));
        moreButton = HubTheme.iconButton(HubTheme.more(), "More", this::showMenu);
        buttons.add(moreButton);

        JPanel toolbar = HubTheme.row(new BorderLayout(HubTheme.GAP, 0));
        toolbar.add(repoFilter, BorderLayout.CENTER);
        toolbar.add(buttons, BorderLayout.EAST);
        header.add(toolbar);
        return header;
    }

    private void showMenu() {
        menu().show(moreButton, 0, moreButton.getHeight());
    }

    private JPopupMenu menu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem repos = new JMenuItem("Repositories…");
        repos.addActionListener(e -> showPage(REPOS));
        menu.add(repos);
        JCheckBoxMenuItem incompatible = new JCheckBoxMenuItem("Show incompatible plugins", showIncompatible);
        incompatible.addActionListener(e -> {
            showIncompatible = incompatible.isSelected();
            queueRebuild();
        });
        menu.add(incompatible);
        return menu;
    }

    private void showPage(String page) {
        showingRepos = REPOS.equals(page);
        pages.show(this, page);
        rebuild();
    }

    private void queueRebuild() {
        if (rebuildQueued) {
            return;
        }
        rebuildQueued = true;
        SwingUtilities.invokeLater(() -> {
            rebuildQueued = false;
            rebuild();
        });
    }

    private void rebuild() {
        if (showingRepos) {
            reposPage.rebuild();
            return;
        }
        int updates = hub.updateCount();
        installedTab.setText(updates > 0 ? "Installed (" + updates + ")" : "Installed");
        repoFilter.setVisible(browsing);
        updateRepoFilter();

        list.removeAll();
        for (JComponent banner : banners(updates)) {
            list.add(banner);
        }
        List<PluginCard.Model> models = browsing ? browseModels() : installedModels();
        for (PluginCard.Model model : models) {
            list.add(new PluginCard(model, expanded.contains(model.key), open -> {
                if (open) {
                    expanded.add(model.key);
                } else {
                    expanded.remove(model.key);
                }
            }));
        }
        if (models.isEmpty()) {
            list.add(empty());
        }
        list.revalidate();
        list.repaint();
    }

    private List<JComponent> banners(int updates) {
        List<JComponent> banners = new ArrayList<>();
        String error = hub.error();
        if (error != null) {
            banners.add(banner(error, HubTheme.ERROR, "Dismiss", HubTheme.BUSY, hub::clearError));
        }
        if (notice != null) {
            banners.add(banner(notice, HubTheme.TEXT, "Dismiss", HubTheme.BUSY, () -> {
                notice = null;
                queueRebuild();
            }));
        }
        for (HubFetcher.Result result : hub.results()) {
            if (result.error != null) {
                String text = "Couldn't load " + repoName(result.repo) + ": " + result.error + ".";
                banners.add(banner(text, result.manifest == null ? HubTheme.ERROR : HubTheme.WARNING, "Retry", HubTheme.BUSY, hub::refresh));
            }
        }
        if (!browsing && updates > 0) {
            String text = updates == 1 ? "1 update available" : updates + " updates available";
            banners.add(banner(text, HubTheme.TITLE, "Update all", HubTheme.accent(), hub::updateAll));
        }
        return banners;
    }

    private JComponent banner(String text, Color color, String action, Color actionColor, Runnable onAction) {
        JPanel banner = new JPanel(new BorderLayout(HubTheme.GAP, 0));
        banner.setBackground(HubTheme.CARD);
        banner.setBorder(new EmptyBorder(6, HubTheme.PADDING, 6, HubTheme.PADDING));
        banner.add(new WrappedText(text, HubTheme.textFont(), color, 0), BorderLayout.CENTER);
        JPanel button = HubTheme.row(new GridBagLayout());
        button.add(HubTheme.textButton(action, actionColor, onAction));
        banner.add(button, BorderLayout.EAST);
        return banner;
    }

    private JComponent empty() {
        String query = query();
        String text;
        if (!query.isEmpty()) {
            text = "No plugins match “" + search.getText().trim() + "”.";
        } else if (browsing) {
            text = hub.isBusy() || !hub.hasFetched() ? "Loading plugins…" : "No repository offers any plugins yet.";
        } else {
            text = "Nothing installed yet. Open Browse to find plugins.";
        }
        WrappedText label = new WrappedText(text, HubTheme.textFont(), HubTheme.MUTED, 0);
        label.setCentered(true);
        label.setBorder(new EmptyBorder(30, 0, 0, 0));
        return label;
    }

    private void updateRepoFilter() {
        String selected = selectedRepo();
        updatingFilter = true;
        repoUrls.clear();
        repoFilter.removeAllItems();
        repoFilter.addItem(ALL_REPOS);
        for (HubFetcher.Result result : hub.results()) {
            String label = repoName(result.repo);
            repoUrls.put(label, result.repo.url);
            repoFilter.addItem(label);
        }
        repoFilter.setSelectedItem(selected != null && repoUrls.containsKey(selected) ? selected : ALL_REPOS);
        updatingFilter = false;
    }

    private String selectedRepo() {
        return (String) repoFilter.getSelectedItem();
    }

    private List<PluginCard.Model> browseModels() {
        String selected = selectedRepo();
        String repoUrl = selected == null || ALL_REPOS.equals(selected) ? null : repoUrls.get(selected);
        List<PluginCard.Model> models = new ArrayList<>();
        for (HubService.Entry entry : hub.entries()) {
            if (repoUrl != null && !repoUrl.equals(entry.repo.url) || !matches(entry.plugin)) {
                continue;
            }
            if (!entry.isCompatible() && !entry.isInstalled() && !showIncompatible) {
                continue;
            }
            models.add(hubModel(entry));
        }
        return models;
    }

    private List<PluginCard.Model> installedModels() {
        List<PluginCard.Model> models = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (HubService.Entry entry : hub.entries()) {
            if (entry.isInstalled() && seen.add(entry.plugin.id) && matches(entry.plugin)) {
                models.add(hubModel(entry));
            }
        }
        for (HubState.Installed installed : hub.unlisted()) {
            PluginCard.Model model = unlistedModel(installed);
            if (matches(model.name, installed.id)) {
                models.add(model);
            }
        }
        File[] jars = pluginDirectory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars != null) {
            for (File jar : jars) {
                PluginCard.Model model = localModel(jar);
                if (matches(model.name, model.description)) {
                    models.add(model);
                }
            }
        }
        models.sort(Comparator.comparing((PluginCard.Model model) -> model.primary == null).thenComparing(model -> model.name.toLowerCase(Locale.ROOT)));
        return models;
    }

    private PluginCard.Model hubModel(HubService.Entry entry) {
        HubPlugin plugin = entry.plugin;
        HubState.Installed installed = entry.installed;
        PluginCard.Model model = new PluginCard.Model();
        model.key = entry.repo.url + "|" + plugin.id;
        model.icon = HubIcons.of(plugin);
        model.name = plugin.displayName();
        model.meta = join(plugin.author, repoName(entry.repo));
        model.description = plugin.description;
        model.requires = plugin.required();
        model.source = plugin.source;

        if (installed != null && !entry.isInstalled()) {
            model.version = "Installed from another repository";
        } else if (installed != null && entry.hasUpdate()) {
            model.version = installed.version + " → " + plugin.version;
            model.versionColor = HubTheme.accent();
            primary(model, "Update", HubTheme.accent(), () -> hub.update(plugin.id));
        } else if (installed != null) {
            model.version = browsing ? "Installed " + installed.version : installed.version;
            model.versionColor = browsing ? HubTheme.INSTALL : null;
        } else if (!entry.isCompatible()) {
            model.version = "Needs VitaLite " + plugin.minVita + " or newer";
            model.versionColor = HubTheme.ERROR;
            primary(model, "Unavailable", HubTheme.BUSY, null);
        } else {
            model.version = plugin.version;
            primary(model, "Install", HubTheme.INSTALL, () -> hub.install(entry.repo.url, plugin.id));
        }

        if (entry.isInstalled()) {
            controls(model, new File(installed.file));
            model.trash = () -> uninstall(plugin.id, plugin.displayName());
        }
        busy(model, plugin.id);
        return model;
    }

    private PluginCard.Model unlistedModel(HubState.Installed installed) {
        File jar = new File(installed.file);
        List<Plugin> plugins = PluginControls.plugins(jar);
        PluginCard.Model model = new PluginCard.Model();
        model.key = "unlisted|" + installed.id;
        model.icon = HubIcons.fallback();
        model.name = plugins.isEmpty() ? installed.id : PluginControls.name(plugins.get(0));
        model.meta = "No enabled repository lists it";
        model.description = plugins.isEmpty() ? null : PluginControls.description(plugins.get(0));
        model.version = installed.version;
        controls(model, jar);
        model.trash = () -> uninstall(installed.id, model.name);
        busy(model, installed.id);
        return model;
    }

    private PluginCard.Model localModel(File jar) {
        List<Plugin> plugins = PluginControls.plugins(jar);
        PluginCard.Model model = new PluginCard.Model();
        model.key = "local|" + jar.getAbsolutePath();
        model.icon = HubIcons.fallback();
        model.name = plugins.isEmpty() ? jar.getName().replaceFirst("\\.jar$", "") : PluginControls.name(plugins.get(0));
        model.meta = plugins.isEmpty() ? "Local · not loaded" : "Local";
        model.description = plugins.isEmpty() ? null : PluginControls.description(plugins.get(0));
        controls(model, jar);
        model.reload = () -> ThreadPool.submit(() -> reloadLocal(jar));
        model.trash = () -> removeLocal(jar, model.name);
        return model;
    }

    private void controls(PluginCard.Model model, File jar) {
        List<Plugin> plugins = PluginControls.plugins(jar);
        if (plugins.isEmpty()) {
            return;
        }
        if (PluginControls.hasConfig(plugins)) {
            model.openConfig = () -> PluginControls.openConfig(plugins);
        }
        model.active = PluginControls.isActive(plugins);
        model.setActive = active -> {
            PluginControls.setActive(plugins, active);
            queueRebuild();
        };
    }

    private void busy(PluginCard.Model model, String id) {
        String operation = hub.operation(id);
        if (operation != null) {
            primary(model, operation + "…", HubTheme.BUSY, null);
            model.trash = null;
        }
    }

    private static void primary(PluginCard.Model model, String label, Color color, Runnable action) {
        model.primary = label;
        model.primaryColor = color;
        model.primaryAction = action;
    }

    private void uninstall(String id, String name) {
        List<String> dependents = hub.dependentsOf(id);
        String message = dependents.isEmpty() ? "Uninstall " + name + "?" : name + " is required by " + String.join(", ", dependents) + ".\nUninstall them too?";
        int choice = JOptionPane.showConfirmDialog(this, message, "Vita Hub", JOptionPane.OK_CANCEL_OPTION, dependents.isEmpty() ? JOptionPane.QUESTION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            hub.uninstall(id, !dependents.isEmpty());
        }
    }

    private void reloadLocal(File jar) {
        boolean reloaded = PluginReloader.reloadPlugin(jar);
        PluginReloader.forceRebuildPluginList();
        notice = reloaded ? null : "Couldn't reload " + jar.getName() + ".";
        queueRebuild();
    }

    private void removeLocal(File jar, String name) {
        int choice = JOptionPane.showConfirmDialog(this, "Remove " + name + "?\nThis deletes " + jar.getName() + " from sideloaded-plugins.", "Vita Hub", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        ThreadPool.submit(() -> {
            if (!PluginReloader.unloadPlugin(jar)) {
                notice = name + " would not stop, so it was not removed.";
            } else if (jar.delete() || !jar.exists()) {
                notice = null;
            } else {
                jar.deleteOnExit();
                notice = name + " is turned off and is deleted when the client closes.";
            }
            PluginReloader.forceRebuildPluginList();
            queueRebuild();
        });
    }

    private boolean matches(HubPlugin plugin) {
        return matches(plugin.displayName(), plugin.author, plugin.description, plugin.id, plugin.tags == null ? null : String.join(" ", plugin.tags));
    }

    private boolean matches(String... fields) {
        String query = query();
        if (query.isEmpty()) {
            return true;
        }
        for (String field : fields) {
            if (field != null && field.toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private String query() {
        return search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
    }

    private static String repoName(HubRepo repo) {
        return repo.official ? "Official" : repo.label();
    }

    private static String join(String first, String second) {
        if (first == null || first.isEmpty()) {
            return second;
        }
        return first + " · " + second;
    }
}
