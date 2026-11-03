package com.tonic.plugins.profiles.panel;

import com.tonic.plugins.profiles.data.JagexAccount;
import com.tonic.plugins.profiles.data.JagexAccountCharacter;
import com.tonic.plugins.profiles.data.Profile;
import com.tonic.plugins.profiles.jagex.JagexAccounts;
import com.tonic.plugins.profiles.jagex.JagexAuthClient;
import com.tonic.plugins.profiles.jagex.JagexLoginException;
import com.tonic.plugins.profiles.session.ProfilesSession;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
public class ProfilesRootPanel extends PluginPanel {

    private final JPanel profilesPanel;
    private final JLabel unreadableBanner;

    private final ProfilesSession profiles;
    private final JagexAccounts jagexAccounts;
    private final List<JagexAccountCard> jagexCards = new ArrayList<>();
    private final List<ProfilePanel> legacyPanels = new ArrayList<>();
    private AddJagexAccountPanel addJagexAccountPanel;
    private JTextField searchField;


    public ProfilesRootPanel(ProfilesSession profiles) {
        this.profiles = profiles;
        this.jagexAccounts = new JagexAccounts(profiles, new JagexAuthClient());
        this.profilesPanel = new JPanel();
        this.unreadableBanner = new JLabel();

        this.initialize();

        this.profiles.loadProfilesFromFile();
        loadProfilesList();
    }

    private void initialize() {
        setLayout(new GridBagLayout());
        AddLegacyAccountPanel addProxyPanel = createAddAccountPanel();
        profilesPanel.setLayout(new BoxLayout(profilesPanel, BoxLayout.Y_AXIS));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 0, 2, 0);

        this.add(addProxyPanel, gbc);

        gbc.gridy++;
        addJagexAccountPanel = createJagexAccountPanel();
        this.add(addJagexAccountPanel, gbc);

        gbc.gridy++;
        JPanel profilesHeader = new JPanel(new BorderLayout());
        profilesHeader.setPreferredSize(new Dimension(220, 32));
        profilesHeader.setMinimumSize(new Dimension(220, 32));
        profilesHeader.setMaximumSize(new Dimension(220, 32));
        profilesHeader.setBackground(new Color(30, 30, 30));
        JLabel profilesLabel = new JLabel("Profiles");
        profilesLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        profilesLabel.setForeground(Color.WHITE);
        profilesHeader.add(profilesLabel, BorderLayout.WEST);
        this.add(profilesHeader, gbc);

        gbc.gridy++;
        unreadableBanner.setForeground(new Color(255, 120, 120));
        unreadableBanner.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        unreadableBanner.setVisible(false);
        this.add(unreadableBanner, gbc);

        gbc.gridy++;
        searchField = new JTextField();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                showFilteredList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                showFilteredList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                showFilteredList();
            }
        });
        this.add(searchField, gbc);

        gbc.gridy++;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTH;

        this.add(profilesPanel, gbc);
        this.revalidate();
        this.repaint();
    }

    private AddJagexAccountPanel createJagexAccountPanel() {
        AddJagexAccountPanel panel = new AddJagexAccountPanel();
        panel.addJagexAccountActionListener(event -> addJagexAccount());
        panel.addImportJagexAccountActionListener(event -> importJagexSession());
        return panel;
    }

    private void addJagexAccount() {
        String label = askNewName("Name this Jagex account (e.g. Main):", "Add Jagex Account");
        if (label == null) {
            return;
        }

        addJagexAccountPanel.setBusy(true);
        JagexSignInDialog.run(this, "Add Jagex Account: " + label, idToken -> jagexAccounts.add(label, idToken),
                account -> {
                    addJagexAccountPanel.setBusy(false);
                    loadProfilesList();
                },
                failure -> {
                    addJagexAccountPanel.setBusy(false);
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
        addJagexAccountPanel.setBusy(true);
        new SwingWorker<JagexAccount, Void>() {
            @Override
            protected JagexAccount doInBackground() throws Exception {
                return jagexAccounts.importSession(importLabel, importSessionId);
            }

            @Override
            protected void done() {
                addJagexAccountPanel.setBusy(false);
                try {
                    get();
                    loadProfilesList();
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

    private AddLegacyAccountPanel createAddAccountPanel() {
        AddLegacyAccountPanel addProxyPanel = new AddLegacyAccountPanel();

        addProxyPanel.addRsAccountActionListener((event) -> {
            String bankPin = "";
            if (StringUtils.isNumeric(addProxyPanel.getBankPin())) {
                bankPin = addProxyPanel.getBankPin();
            }

            Profile profile = new Profile(addProxyPanel.getIdentifierField().getText().trim(),
                    false,
                    addProxyPanel.getUsername(),
                    addProxyPanel.getPassword(),
                    addProxyPanel.getUsername(),
                    "",
                    "",
                    bankPin);

            try {
                profiles.addLegacy(profile);
            } catch (Exception e) {
                showError("Couldn't add the account", e.getMessage());
                return;
            }
            loadProfilesList();
        });
        return addProxyPanel;
    }

    private void loadProfilesList() {
        unreadableBanner.setVisible(profiles.isUnreadable());
        if (profiles.isUnreadable()) {
            unreadableBanner.setText("<html><body style='width:180px'>Profiles can't be loaded, so nothing is shown or saved. "
                    + escape(profiles.getLoadError()) + "</body></html>");
        }

        jagexCards.clear();
        List<JagexAccount> accounts = profiles.getJagexAccounts();
        accounts.sort(Comparator.comparing(JagexAccount::getLabel, String.CASE_INSENSITIVE_ORDER));
        for (JagexAccount account : accounts) {
            jagexCards.add(new JagexAccountCard(account, profiles, jagexAccounts, this::loadProfilesList));
        }

        legacyPanels.clear();
        List<Profile> legacyProfiles = profiles.getLegacyProfiles();
        legacyProfiles.sort(Comparator.comparing(Profile::getIdentifier, String.CASE_INSENSITIVE_ORDER));
        for (Profile profile : legacyProfiles) {
            ProfilePanel panel = new ProfilePanel(profile);
            panel.getDeleteButton().addActionListener(e -> {
                int result = JOptionPane.showConfirmDialog(null, "Do you want to delete " + profile.getIdentifier() + "?", "Remove account", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null);

                if (result == JOptionPane.YES_OPTION) {
                    removeProfile(profile);
                }
            });
            panel.getEditButton().addActionListener(e -> editLegacyProfile(profile));
            legacyPanels.add(panel);
        }

        showFilteredList();
    }

    private void showFilteredList() {
        String query = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        profilesPanel.removeAll();

        for (JagexAccountCard card : jagexCards) {
            if (matches(card.getAccount(), query)) {
                profilesPanel.add(card);
                profilesPanel.add(Box.createRigidArea(new Dimension(0, 4)));
            }
        }

        for (ProfilePanel panel : legacyPanels) {
            if (query.isEmpty() || panel.getProfile().getIdentifier().toLowerCase().contains(query)) {
                profilesPanel.add(panel);
                profilesPanel.add(Box.createRigidArea(new Dimension(0, 4)));
            }
        }

        profilesPanel.revalidate();
        profilesPanel.repaint();
        revalidate();
        repaint();
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

    private void editLegacyProfile(Profile profile) {
        JTextField tag = new JTextField(profile.getIdentifier());
        JTextField username = new JTextField(profile.getUsername());
        JPasswordField password = new JPasswordField(profile.getPassword());
        JTextField bankPin = new JTextField(profile.getBankPin(), 4);
        JPanel editPanel = createEditPanel(tag, username, password, bankPin);

        while (true) {
            int result = JOptionPane.showConfirmDialog(null,
                    editPanel, "Editing: " + profile.getIdentifier(),
                    JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null);

            if (result != JOptionPane.YES_OPTION) {
                return;
            }
            if (!bankPin.getText().isEmpty() && (!StringUtils.isNumeric(bankPin.getText()) || bankPin.getText().length() != 4)) {
                JOptionPane.showMessageDialog(null, "Invalid bank pin");
                continue;
            }

            Profile updated = new Profile(tag.getText().trim(), false, username.getText(), new String(password.getPassword()),
                    username.getText(), "", "", bankPin.getText());
            try {
                profiles.updateLegacy(profile.getIdentifier(), updated);
            } catch (IllegalArgumentException e) {
                showError("Couldn't save", e.getMessage());
                continue;
            } catch (Exception e) {
                showError("Couldn't save", e.getMessage());
                return;
            }
            loadProfilesList();
            return;
        }
    }

    private void removeProfile(Profile profile) {
        try {
            profiles.removeLegacy(profile.getIdentifier());
        } catch (Exception e) {
            showError("Couldn't delete", e.getMessage());
            return;
        }
        loadProfilesList();
    }

    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private JPanel createEditPanel(JTextField tagField, JTextField usernameField, JTextField passwordField, JTextField bankPinField) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);


        JLabel tagLabel = new JLabel("Profile Tag:");
        tagField.setMinimumSize(new Dimension(186, 32));
        tagField.setMaximumSize(new Dimension(186, 32));
        tagField.setPreferredSize(new Dimension(186, 32));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(tagLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(tagField, gbc);

        JLabel usernameLabel = new JLabel("Username:");
        usernameField.setMinimumSize(new Dimension(186, 32));
        usernameField.setPreferredSize(new Dimension(186, 32));
        usernameField.setMaximumSize(new Dimension(186, 32));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(usernameLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(usernameField, gbc);

        JLabel passwordLabel = new JLabel("Password:");
        passwordField.setMinimumSize(new Dimension(186, 32));
        passwordField.setPreferredSize(new Dimension(186, 32));
        passwordField.setMaximumSize(new Dimension(186, 32));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(passwordField, gbc);

        JLabel bankPinLabel = new JLabel("Bank Pin:");
        bankPinField.setMinimumSize(new Dimension(64, 32));
        bankPinField.setPreferredSize(new Dimension(64, 32));
        bankPinField.setMaximumSize(new Dimension(64, 32));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(bankPinLabel, gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(bankPinField, gbc);
        return panel;
    }
}
