package com.tonic.plugins.profiles.panel;

import com.tonic.plugins.profiles.data.JagexAccount;
import com.tonic.plugins.profiles.data.JagexAccountCharacter;
import com.tonic.plugins.profiles.data.Profile;
import com.tonic.plugins.profiles.jagex.JagexAccounts;
import com.tonic.plugins.profiles.jagex.JagexAuthClient;
import com.tonic.plugins.profiles.jagex.JagexLoginException;
import com.tonic.plugins.profiles.session.ProfilesSession;
import com.tonic.ui.ScrollColumn;
import com.tonic.ui.VitaTheme;
import com.tonic.ui.WrappedText;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * The Profiles tab: a header to add Jagex accounts, a search box, a card per Jagex account and the legacy accounts
 * in a collapsible group. Which cards are collapsed and which sessions expired is kept for the session.
 */
public class ProfilesRootPanel extends PluginPanel {
    private static final Color ERROR_BACKGROUND = new Color(70, 35, 30);

    private final ProfilesSession profiles;
    private final JagexAccounts jagexAccounts;
    private final Set<String> collapsed = new HashSet<>();
    private final Set<String> expired = new HashSet<>();
    private final JPanel list = VitaTheme.row(new DynamicGridLayout(0, 1, 0, VitaTheme.GAP));
    private final IconTextField search = new IconTextField();
    private final JButton addButton = VitaTheme.textButton("+ Add account", VitaTheme.accent(), this::showAddMenu);
    private final WrappedText busyLine = new WrappedText("", VitaTheme.textFont(), VitaTheme.MUTED, 0);
    private final WrappedText errorText = new WrappedText("", VitaTheme.textFont(), JagexAccountCard.EXPIRED_TEXT, 0);
    private final JPanel errorBox = new JPanel(new BorderLayout());
    private final JagexAccountCard.Host host = new CardHost();
    private final JPanel header = VitaTheme.row(new DynamicGridLayout(0, 1, 0, 8));
    private final JPanel titleRow = VitaTheme.row(new BorderLayout(VitaTheme.GAP, 0));
    private boolean legacyOpen;
    private boolean adding;

    /**
     * @param profiles the profile store the tab shows and edits
     */
    public ProfilesRootPanel(ProfilesSession profiles) {
        super(false);
        this.profiles = profiles;
        this.jagexAccounts = new JagexAccounts(profiles, new JagexAuthClient());

        setLayout(new BorderLayout());
        setBackground(VitaTheme.BACKGROUND);
        buildHeader();
        add(header, BorderLayout.NORTH);

        ScrollColumn column = new ScrollColumn();
        column.setLayout(new BorderLayout());
        column.setBorder(new EmptyBorder(0, 10, 10, 10));
        column.add(list, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(column, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(VitaTheme.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        profiles.loadProfilesFromFile();
        rebuild();
    }

    private void buildHeader() {
        header.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Profiles");
        title.setFont(VitaTheme.titleFont());
        title.setForeground(VitaTheme.TITLE);
        titleRow.add(title, BorderLayout.WEST);
        addButton.setToolTipText("Add a Jagex account with all of its characters");
        titleRow.add(addButton, BorderLayout.EAST);

        errorBox.setBackground(ERROR_BACKGROUND);
        errorBox.setBorder(new EmptyBorder(6, 8, 6, 8));
        errorBox.add(errorText, BorderLayout.CENTER);

        search.setIcon(IconTextField.Icon.SEARCH);
        search.setBackground(VitaTheme.CARD);
        search.setHoverBackgroundColor(VitaTheme.CARD_HOVER);
        search.setPreferredSize(new Dimension(0, 30));
        search.setToolTipText("Search accounts or characters");
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                rebuild();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                rebuild();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                rebuild();
            }
        });
        search.addClearListener(this::rebuild);
    }

    private void layoutHeader() {
        header.removeAll();
        header.add(titleRow);
        if (adding) {
            header.add(busyLine);
        }
        if (profiles.isUnreadable()) {
            header.add(errorBox);
        }
        header.add(search);
        header.revalidate();
        header.repaint();
    }

    private void rebuild() {
        boolean unreadable = profiles.isUnreadable();
        if (unreadable) {
            errorText.setText("Profiles can't be loaded, so nothing is shown or saved. " + (profiles.getLoadError() == null ? "" : profiles.getLoadError()));
        }
        layoutHeader();

        String query = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        list.removeAll();

        List<JagexAccount> accounts = profiles.getJagexAccounts();
        accounts.sort(Comparator.comparing(JagexAccount::getLabel, String.CASE_INSENSITIVE_ORDER));
        int shown = 0;
        for (JagexAccount account : accounts) {
            if (matches(account, query)) {
                list.add(new JagexAccountCard(account, profiles, jagexAccounts, host, !query.isEmpty()));
                shown++;
            }
        }

        List<Profile> legacy = new ArrayList<>();
        for (Profile profile : profiles.getLegacyProfiles()) {
            if (matches(profile, query)) {
                legacy.add(profile);
            }
        }
        legacy.sort(Comparator.comparing(Profile::getIdentifier, String.CASE_INSENSITIVE_ORDER));

        if (accounts.isEmpty() && query.isEmpty() && !unreadable) {
            list.add(emptyState());
        } else if (shown == 0 && legacy.isEmpty() && !query.isEmpty()) {
            list.add(new WrappedText("No matches", VitaTheme.textFont(), VitaTheme.MUTED, 0));
        }
        if (!legacy.isEmpty()) {
            list.add(new LegacySection(legacy, profiles, legacyOpen || !query.isEmpty(), this::toggleLegacy, this::rebuild));
        }

        list.revalidate();
        list.repaint();
    }

    private JPanel emptyState() {
        JPanel card = new JPanel(new DynamicGridLayout(0, 1, 0, 8));
        card.setBackground(VitaTheme.CARD);
        card.setBorder(new EmptyBorder(VitaTheme.PADDING, VitaTheme.PADDING, VitaTheme.PADDING, VitaTheme.PADDING));
        card.add(new WrappedText("No Jagex accounts yet", VitaTheme.titleFont(), VitaTheme.TITLE, 0));
        card.add(new WrappedText("Sign in on Jagex's own page in your browser and every character on the account is added, ready to log in from here.", VitaTheme.textFont(), VitaTheme.TEXT, 0));
        card.add(VitaTheme.textButton("Add Jagex account", VitaTheme.accent(), this::addJagexAccount));
        card.add(new WrappedText("Have a session id? Use + Add account, then Import session id.", VitaTheme.textFont(), VitaTheme.MUTED, 0));
        return card;
    }

    private void toggleLegacy() {
        legacyOpen = !legacyOpen;
        rebuild();
    }

    private void showAddMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem signIn = new JMenuItem("Sign in with browser...");
        signIn.addActionListener(e -> addJagexAccount());
        menu.add(signIn);
        JMenuItem importSession = new JMenuItem("Import session id...");
        importSession.addActionListener(e -> importJagexSession());
        menu.add(importSession);
        menu.show(addButton, 0, addButton.getHeight());
    }

    private void setAdding(String status) {
        adding = status != null;
        addButton.setEnabled(!adding);
        addButton.setBackground(adding ? VitaTheme.BUSY : VitaTheme.accent());
        busyLine.setText(adding ? status : "");
        layoutHeader();
    }

    private void addJagexAccount() {
        String label = askNewName("Name this Jagex account (e.g. Main):", "Add Jagex Account");
        if (label == null) {
            return;
        }

        setAdding("Waiting for the browser sign-in for " + label + "...");
        JagexSignInDialog.run(this, "Add Jagex Account: " + label, idToken -> jagexAccounts.add(label, idToken),
                account -> {
                    setAdding(null);
                    rebuild();
                },
                failure -> {
                    setAdding(null);
                    boolean cancelled = failure instanceof JagexLoginException && ((JagexLoginException) failure).getKind() == JagexLoginException.Kind.CANCELLED;
                    if (!cancelled) {
                        showError("Couldn't add the Jagex account", failure.getMessage());
                    }
                });
    }

    private void importJagexSession() {
        JTextField labelField = new JTextField(24);
        JTextField sessionIdField = new JTextField(24);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Label (e.g. Main):"));
        panel.add(labelField);
        panel.add(new JLabel("Session ID:"));
        panel.add(sessionIdField);

        String label;
        String sessionId;
        while (true) {
            int result = JOptionPane.showConfirmDialog(this, panel, "Import Jagex Session", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }
            label = labelField.getText().trim();
            sessionId = sessionIdField.getText().trim();
            if (sessionId.isEmpty()) {
                showError("Couldn't import the session", "Enter the session id.");
                continue;
            }
            try {
                profiles.checkNewName(label);
                break;
            } catch (IllegalArgumentException | IllegalStateException e) {
                showError("Couldn't import the session", e.getMessage());
            }
        }

        String importLabel = label;
        String importSessionId = sessionId;
        setAdding("Importing " + importLabel + "...");
        new SwingWorker<JagexAccount, Void>() {
            @Override
            protected JagexAccount doInBackground() throws Exception {
                return jagexAccounts.importSession(importLabel, importSessionId);
            }

            @Override
            protected void done() {
                setAdding(null);
                try {
                    get();
                    rebuild();
                } catch (InterruptedException | ExecutionException e) {
                    Throwable cause = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
                    showError("Couldn't import the session", cause.getMessage());
                }
            }
        }.execute();
    }

    private String askNewName(String prompt, String title) {
        String initial = "";
        while (true) {
            Object answer = JOptionPane.showInputDialog(this, prompt, title, JOptionPane.PLAIN_MESSAGE, null, null, initial);
            if (answer == null) {
                return null;
            }
            String name = answer.toString().trim();
            try {
                profiles.checkNewName(name);
                return name;
            } catch (IllegalArgumentException | IllegalStateException e) {
                showError(title, e.getMessage());
                initial = name;
            }
        }
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private static boolean matches(JagexAccount account, String query) {
        if (query.isEmpty() || account.getLabel().toLowerCase().contains(query)) {
            return true;
        }
        for (JagexAccountCharacter character : account.getCharacters()) {
            if (character.shownName().toLowerCase().contains(query)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(Profile profile, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String username = profile.getUsername() == null ? "" : profile.getUsername().toLowerCase();
        return profile.getIdentifier().toLowerCase().contains(query) || username.contains(query);
    }

    private final class CardHost implements JagexAccountCard.Host {
        @Override
        public boolean isCollapsed(String label) {
            return collapsed.contains(label);
        }

        @Override
        public void setCollapsed(String label, boolean nowCollapsed) {
            if (nowCollapsed) {
                collapsed.add(label);
            } else {
                collapsed.remove(label);
            }
        }

        @Override
        public boolean isExpired(String label) {
            return expired.contains(label);
        }

        @Override
        public void setExpired(String label, boolean nowExpired) {
            if (nowExpired) {
                expired.add(label);
            } else {
                expired.remove(label);
            }
        }

        @Override
        public void renamed(String from, String to) {
            if (collapsed.remove(from)) {
                collapsed.add(to);
            }
            if (expired.remove(from)) {
                expired.add(to);
            }
            rebuild();
        }

        @Override
        public void listChanged() {
            rebuild();
        }
    }
}
