package com.tonic.plugins.profiles.panel;

import com.tonic.plugins.profiles.data.Profile;
import com.tonic.plugins.profiles.jagex.JagexAccountService;
import com.tonic.plugins.profiles.jagex.model.JagCharacter;
import com.tonic.plugins.profiles.jagex.model.JagLoginToken;
import com.tonic.plugins.profiles.session.ProfilesSession;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class ProfilesRootPanel extends PluginPanel {

    private final JPanel profilesPanel;

    private final ProfilesSession profiles;
    private final Set<ProfilePanel> profilePanelSet;
    private JTextField searchField;


    public ProfilesRootPanel(ProfilesSession profiles) {
        this.profiles = profiles;
        this.profilePanelSet = new HashSet<>();
        this.profilesPanel = new JPanel();

        this.initialize();

        this.profiles.loadProfilesFromFile();
        loadProfilesList(this.profiles.getProfiles());
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
        AddJagexAccountPanel addJagexAccountPanel = createJagexAccountPanel();
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
        searchField = new JTextField();
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateProfilesList(searchField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateProfilesList(searchField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateProfilesList(searchField.getText());
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

    private void updateProfilesList(String query) {
        List<ProfilePanel> filteredList = profilePanelSet.stream()
                .filter(panel -> {
                    if (query.isEmpty()) {
                        return true;
                    } else {
                        return panel.getProfile().getIdentifier().toLowerCase().contains(query.toLowerCase());
                    }
                })
                .sorted((p1, p2) -> p1.getProfile().getIdentifier().compareToIgnoreCase(p2.getProfile().getIdentifier()))
                .collect(Collectors.toList());
        refreshPanelList(filteredList);
    }

    private void refreshPanelList(List<ProfilePanel> panels) {
        profilesPanel.removeAll();
        for (ProfilePanel panel : panels) {
            profilesPanel.add(panel);
        }
        profilesPanel.revalidate();
        profilesPanel.repaint();
    }

    private void onTokenReceived(JagLoginToken token, JagexAccountService service) {
        if (token == null)
            return;

        //acc has 0 characters
        if (token.getCharacters().length == 0) {
            JOptionPane.showMessageDialog(
                    null,
                    "This jag account has 0 characters, idk how to handle this, stopping process.",
                    "Error with Logging in",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        SelectCharacterPanel characterSelectPanel = new SelectCharacterPanel(Arrays.asList(token.getCharacters()));

        profiles.loadProfilesFromFile();

        int selectCharacterReponse = JOptionPane.showOptionDialog(null,
                characterSelectPanel,
                "Select characters",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                null);

        if (selectCharacterReponse != JOptionPane.YES_OPTION) {
            return;
        }

        List<JagCharacter> characters = new ArrayList<>(characterSelectPanel.getSelectedCharacters().values());

        while (!characters.isEmpty()) {
            JagCharacter jagCharacter = characters.get(0);
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);


            JLabel nameLabel = new JLabel("Identifier:");
            JLabel bankPinLabel = new JLabel("Bank Pin:");
            JTextField nameField = new JTextField(32);
            JTextField bankPin = new JTextField(4);

            JLabel label = new JLabel("Type an identifier and bank pin for this account: " + jagCharacter.getDisplayName() + ", optionally leave pin empty");
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.NORTH;
            panel.add(label, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.anchor = GridBagConstraints.EAST;
            panel.add(nameLabel, gbc);

            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            panel.add(nameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.anchor = GridBagConstraints.EAST;
            panel.add(bankPinLabel, gbc);

            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            panel.add(bankPin, gbc);


            bankPin.setColumns(4);
            int response = JOptionPane.showConfirmDialog(
                    null,
                    panel
            );
            if (response == JOptionPane.YES_OPTION)
            {

                if (nameField.getText().isEmpty()) {
                    characters.remove(0);
                    continue;
                }

                if (profiles.getProfiles().stream().anyMatch(p -> p.getIdentifier().equals(nameField.getText()))) {
                    characters.remove(0);
                    continue;
                }

                String bankPinText = "";

                if (!bankPin.getText().isEmpty() && StringUtils.isNumeric(bankPin.getText())) {
                    bankPinText = bankPin.getText();
                }

                String displayName = nameField.getText();

                if (jagCharacter.getDisplayName() != null && !jagCharacter.getDisplayName().isEmpty()) {
                    displayName = jagCharacter.getDisplayName();
                }

                Profile profile = new Profile(nameField.getText(), true,
                        "", "",
                        displayName, token.getSessionId(),
                        jagCharacter.getAccountId(), bankPinText);

                profiles.getProfiles().add(profile);
                characters.remove(0);
            }
        }

        profiles.saveProfilesToFile();
        loadProfilesList(profiles.getProfiles());
        service.shutdownServer();
    }

    private AddJagexAccountPanel createJagexAccountPanel() {
        AddJagexAccountPanel addJagexAccountPanel = new AddJagexAccountPanel();
        addJagexAccountPanel.addJagexAccountActionListener((event) -> {

            new Thread(() -> {
                try {
                    JagexAccountService auth = new JagexAccountService();
                    auth.startServer();
                    CompletableFuture<JagLoginToken> loginToken = auth.requestLoginToken();
                    onTokenReceived(loginToken.get(2, TimeUnit.MINUTES), auth);
                } catch (Exception e) {
//                    e.printStackTrace();
                }
            }).start();
        });

        addJagexAccountPanel.addImportJagexAccountActionListener((event) -> {
            importJagexAccountDialog();
        });
        return addJagexAccountPanel;
    }

    private void importJagexAccountDialog() {
        JTextField identifierField = new JTextField(32);
        JTextField accountIdField = new JTextField(32);
        JTextField sessionIdField = new JTextField(32);
        JTextField displayNameField = new JTextField(32);
        JTextField bankPinField = new JTextField(4);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Identifier (Nickname):"));
        panel.add(identifierField);
        panel.add(new JLabel("Account ID (Character ID):"));
        panel.add(accountIdField);
        panel.add(new JLabel("Session ID:"));
        panel.add(sessionIdField);
        panel.add(new JLabel("Display Name (Character Name):"));
        panel.add(displayNameField);
        panel.add(new JLabel("Bank PIN (Optional):"));
        panel.add(bankPinField);

        int result = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Import Jagex Account",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String identifier = identifierField.getText().trim();
            String accountId = accountIdField.getText().trim();
            String sessionId = sessionIdField.getText().trim();
            String displayName = displayNameField.getText().trim();
            String bankPin = bankPinField.getText().trim();

            if (identifier.isEmpty() || accountId.isEmpty() || sessionId.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Identifier, Account ID, and Session ID are required.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            profiles.loadProfilesFromFile();

            boolean alreadyExists = profiles.getProfiles().stream()
                    .anyMatch(p -> p != null && p.getIdentifier().equals(identifier));

            if (alreadyExists) {
                JOptionPane.showMessageDialog(null, "A profile with that identifier already exists.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!bankPin.isEmpty() && !bankPin.matches("\\d{4}")) {
                JOptionPane.showMessageDialog(null, "Bank PIN must be 4 digits.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Profile profile = new Profile(
                    identifier,
                    true,
                    "",
                    "",
                    displayName,
                    sessionId,
                    accountId,
                    bankPin
            );

            profiles.getProfiles().add(profile);
            profiles.saveProfilesToFile();
            loadProfilesList(profiles.getProfiles());

            JOptionPane.showMessageDialog(null, "Jagex account added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private AddLegacyAccountPanel createAddAccountPanel() {
        AddLegacyAccountPanel addProxyPanel = new AddLegacyAccountPanel();

        addProxyPanel.addRsAccountActionListener((event) -> {
            profiles.loadProfilesFromFile();

            if (!profiles.getProfiles().isEmpty() && profiles.getProfiles().stream().anyMatch(p -> p != null && p.getIdentifier().equals(addProxyPanel.getIdentifierField().getText()))) {
                return;
            }

            String bankPin = "";
            if (StringUtils.isNumeric(addProxyPanel.getBankPin())) {
                bankPin = addProxyPanel.getBankPin();
            }

            Profile profile = new Profile(addProxyPanel.getIdentifierField().getText(),
                    false,
                    addProxyPanel.getUsername(),
                    addProxyPanel.getPassword(),
                    addProxyPanel.getUsername(),
                    "",
                    "",
                    bankPin);


            profiles.getProfiles().add(profile);
            profiles.saveProfilesToFile();
            loadProfilesList(profiles.getProfiles());
        });
        return addProxyPanel;
    }

    private void loadProfilesList(Set<Profile> profiles) {
        List<Profile> profileList = new ArrayList<>(profiles);
        profileList.sort(Comparator.comparing(Profile::getIdentifier));
        profilesPanel.removeAll();
        profilePanelSet.clear();

        if (profileList.isEmpty()) {
            return;
        }

        for (Profile profile : profileList) {
            ProfilePanel panel = new ProfilePanel(profile);
            panel.getDeleteButton().addActionListener(e -> {
                int result = JOptionPane.showConfirmDialog(null, "Do you want to delete " + profile.getIdentifier() + "?", "Remove account", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null);

                if (result == JOptionPane.YES_OPTION) {
                    removeProfile(profile);
                }
            });


            if (profile.isJagexAccount()) {
                panel.getEditButton().addActionListener(e -> {
                    JTextField tag = new JTextField("Profile Tag");
                    JTextField bankPin = new JTextField(4);
                    JPanel editPanel = new JPanel(new GridLayout(2,1));
                    editPanel.add(tag);
                    editPanel.add(bankPin);

                    int result = JOptionPane.showConfirmDialog(null, editPanel, "Editing: " + profile.getIdentifier(),
                            JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null);

                    if (result == JOptionPane.YES_OPTION) {
                        if (!bankPin.getText().isEmpty() && (!StringUtils.isNumeric(bankPin.getText()) || bankPin.getText().length() != 4)) {
                            JOptionPane.showMessageDialog(null, "Invalid bank pin");
                            return;
                        }

                        if (tag.getText().isEmpty()) {
                            JOptionPane.showMessageDialog(null, "Invalid profile tag");
                            return;
                        }

                        editProfile(profile, tag.getText(), bankPin.getText());
                    }
                });
            } else {
                panel.getEditButton().addActionListener(e -> {
                    JTextField tag = new JTextField("Profile Tag");
                    JTextField username = new JTextField("username");
                    JPasswordField password = new JPasswordField();
                    JTextField bankPin = new JTextField(4);
                    JPanel editPanel = createEditPanel(tag, username, password, bankPin);

                    int result = JOptionPane.showConfirmDialog(null,
                            editPanel, "Editing: " + profile.getIdentifier(),
                            JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null);

                    if (result == JOptionPane.YES_OPTION) {
                        if (!bankPin.getText().isEmpty() && (!StringUtils.isNumeric(bankPin.getText()) || bankPin.getText().length() != 4)) {
                            JOptionPane.showMessageDialog(null, "Invalid bank pin");
                            return;
                        }
                        editProfile(profile, tag.getText(), username.getText(), new String(password.getPassword()), bankPin.getText());
                    }
                });
            }
            profilesPanel.add(panel);
            profilesPanel.add(Box.createRigidArea(new Dimension(0, 4)));
            profilePanelSet.add(panel);
        }

        profilesPanel.revalidate();
        profilesPanel.repaint();
        revalidate();
        repaint();
    }

    private void removeProfile(Profile profile) {
        profiles.getProfiles().remove(profile);
        profiles.saveProfilesToFile();
        loadProfilesList(profiles.getProfiles());
    }

    private void editProfile(Profile profile, String tag, String bankPin) {
        profiles.getProfiles().remove(profile);
        profile.setIdentifier(tag);
        profile.setBankPin(bankPin);
        profiles.getProfiles().add(profile);
        profiles.saveProfilesToFile();
        loadProfilesList(profiles.getProfiles());
    }

    private void editProfile(Profile profile, String tag, String user, String pass, String pin) {
        profiles.getProfiles().remove(profile);
        profile.setIdentifier(tag);
        profile.setUsername(user);
        profile.setCharacterName(user);
        profile.setPassword(pass);
        profile.setBankPin(pin);
        profiles.getProfiles().add(profile);
        profiles.saveProfilesToFile();
        loadProfilesList(profiles.getProfiles());
    }

    private JPanel createEditPanel(JTextField tagField, JTextField usernameField, JTextField passwordField, JTextField bankPinField) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Add some padding


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
