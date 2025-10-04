package com.tonic.plugins.profiles.panel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tonic.Static;
import com.tonic.model.DeviceID;
import com.tonic.plugins.profiles.ProfilesPlugin;
import com.tonic.plugins.profiles.data.Profile;
import com.tonic.plugins.profiles.jagex.JagexAccountService;
import com.tonic.plugins.profiles.jagex.model.JagCharacter;
import com.tonic.plugins.profiles.jagex.model.JagLoginToken;
import com.tonic.plugins.profiles.util.GsonUtil;
import com.tonic.plugins.profiles.util.OS;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.PluginPanel;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class ProfilesRootPanel extends PluginPanel {
    private static final String BASE64_KEY = generateAESKey();
    private static final String DIRECTORY = Static.VITA_DIR + "\\profiles.db";

    private final ProfilesPlugin plugin;

    private final JPanel profilesPanel;

    private final Set<Profile> profiles;
    private final Set<ProfilePanel> profilePanelSet;
    private JTextField searchField;


    public ProfilesRootPanel(ProfilesPlugin plugin) {
        this.plugin = plugin;
        profiles = new HashSet<>();
        this.profilePanelSet = new HashSet<>();
        this.profilesPanel = new JPanel();

        this.initialize();

        loadProfilesFromFile(profiles);
        loadProfilesList(profiles);
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
        AddJagexAccountPanel addJagexAccountPanel = crateJagexAccountPanel();
        this.add(addJagexAccountPanel, gbc);

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

        loadProfilesFromFile(profiles);

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

                if (profiles.stream().anyMatch(p -> p.getIdentifier().equals(nameField.getText()))) {
                    characters.remove(0);
                    continue;
                }

                String bankPinText = "";

                if (!bankPin.getText().isEmpty() && StringUtils.isNumeric(bankPin.getText())) {
                    bankPinText = bankPin.getText();
                }

                String displayName = nameField.getText();

                if (jagCharacter.getDisplayName() != null & !jagCharacter.getDisplayName().isEmpty()) {
                    displayName = jagCharacter.getDisplayName();
                }

                Profile profile = new Profile(nameField.getText(), true,
                        "", "",
                        displayName, token.getSessionId(),
                        jagCharacter.getAccountId(), bankPinText);

                profiles.add(profile);
                characters.remove(0);
            }
        }

        saveProfilesToFile(profiles);
        loadProfilesList(profiles);
        service.shutdownServer();
    }

    private AddJagexAccountPanel crateJagexAccountPanel() {
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
        return addJagexAccountPanel;
    }

    private AddLegacyAccountPanel createAddAccountPanel() {
        AddLegacyAccountPanel addProxyPanel = new AddLegacyAccountPanel();

        addProxyPanel.addRsAccountActionListener((event) -> {
            loadProfilesFromFile(profiles);

            if (!profiles.isEmpty() && profiles.stream().anyMatch(p -> p != null && p.getIdentifier().equals(addProxyPanel.getIdentifierField().getText()))) {
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


            profiles.add(profile);
            saveProfilesToFile(profiles);
            loadProfilesList(profiles);
        });
        return addProxyPanel;
    }

    public static String encrypt(String plaintext) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(BASE64_KEY);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(String base64IvAndCiphertext) {
        try {
            byte[] combined = Base64.getDecoder().decode(base64IvAndCiphertext);
            byte[] keyBytes = Base64.getDecoder().decode(BASE64_KEY);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[16];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            int ctLen = combined.length - iv.length;
            byte[] ct = new byte[ctLen];
            System.arraycopy(combined, iv.length, ct, 0, ctLen);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] plainBytes = cipher.doFinal(ct);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private static String generateAESKey() {
        try {
            String deviceId = DeviceID.vanillaGetDeviceID(OS.detect().getValue());

            // Hash the device ID to get exactly 32 bytes (256-bit) for AES-256
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha.digest(deviceId.getBytes(StandardCharsets.UTF_8));

            // Encode the 32-byte hash to base64
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            // Fallback to a default key if something fails
            return Base64.getEncoder().encodeToString(
                    "DEFAULT_KEY_FALLBACK_1234567890".getBytes()
            );
        }
    }

    public static void saveProfilesToFile(Set<Profile> profiles) {
        try {
            if (!Files.exists(Paths.get(DIRECTORY))) {
                Files.createFile(Paths.get(DIRECTORY));
            }
        } catch (IOException e) {
        }
        try (Writer writer = new FileWriter(DIRECTORY)) {
            String json = GsonUtil.GSON.toJson(profiles);
            String enc = encrypt(json);
            writer.write(enc);
        } catch (IOException e) {
        }
    }

    public static void loadProfilesFromFile(Set<Profile> profiles) {
        try {
            if (!Files.exists(Paths.get(DIRECTORY))) {
                Files.createFile(Paths.get(DIRECTORY));
            }
        } catch (IOException e) {
        }
        try (Reader reader = new FileReader(DIRECTORY)) {
            StringBuilder builder = new StringBuilder();
            int ch;
            while ((ch = reader.read()) != -1) {
                builder.append((char) ch);
            }
            String enc = builder.toString();

            if (enc.isEmpty()) {
                return;
            }

            String json = decrypt(enc);
            Type setType = new TypeToken<Set<Profile>>(){}.getType();
            Set<Profile> loadedProfiles = GsonUtil.GSON.fromJson(json, setType);

            if (loadedProfiles == null) {
                profiles.clear();
            } else {
                profiles.addAll(loadedProfiles);
            }
        } catch (IOException e) {
        }
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
            ProfilePanel panel = new ProfilePanel(plugin, profile);
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
        profiles.remove(profile);
        saveProfilesToFile(profiles);
        loadProfilesList(profiles);
    }

    private void editProfile(Profile profile, String tag, String bankPin) {
        profiles.remove(profile);
        profile.setIdentifier(tag);
        profile.setBankPin(bankPin);
        profiles.add(profile);
        saveProfilesToFile(profiles);
        loadProfilesList(profiles);
    }

    private void editProfile(Profile profile, String tag, String user, String pass, String pin) {
        profiles.remove(profile);
        profile.setIdentifier(tag);
        profile.setUsername(user);
        profile.setCharacterName(user);
        profile.setPassword(pass);
        profile.setBankPin(pin);
        profiles.add(profile);
        saveProfilesToFile(profiles);
        loadProfilesList(profiles);
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
