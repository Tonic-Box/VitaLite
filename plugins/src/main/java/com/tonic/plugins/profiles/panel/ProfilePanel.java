package com.tonic.plugins.profiles.panel;

import com.tonic.plugins.profiles.ProfilesPlugin;
import com.tonic.plugins.profiles.data.AuthHooks;
import com.tonic.plugins.profiles.data.Profile;
import com.tonic.plugins.profiles.util.GsonUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Slf4j
public class ProfilePanel extends JPanel {

    private final Client client;
    private final ClientThread clientThread;
    @Getter
    private final Profile profile;

    private final JPanel contentPanel;
    private final JButton collapseExpandButton;
    @Getter
    private final JButton editButton;
    @Getter
    private final JButton deleteButton;
    private boolean isExpanded = true;

    private final AuthHooks authHooks;

    public ProfilePanel(ProfilesPlugin plugin, Profile profile) {
        this.client = plugin.getClient();
        this.clientThread = plugin.getClientThread();
        this.profile = profile;
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(200, 80));
        setPreferredSize(new Dimension(200, 80));
        setMaximumSize(new Dimension(200, 80));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setPreferredSize(new Dimension(220, 48));
        titlePanel.setMinimumSize(new Dimension(220, 48));
        titlePanel.setMaximumSize(new Dimension(220, 48));
        titlePanel.setBackground(new Color(30, 30, 30));

        JLabel titleLabel = new JLabel(profile.getIdentifier());
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        JButton loginButton = new JButton(profile.getIdentifier());
        collapseExpandButton = new JButton("+");

        titlePanel.add(loginButton, BorderLayout.CENTER);

        contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayout(1, 2));
        JLabel usernameLabel = new JLabel(profile.getCharacterName());
        usernameLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        editButton = new JButton("Edit");
        editButton.setBackground(new Color(48, 48, 48));
        editButton.setPreferredSize(new Dimension());
        deleteButton = new JButton("Delete");
        deleteButton.setBackground(new Color(48, 48, 48));
        contentPanel.add(editButton);
        contentPanel.add(deleteButton);

        add(titlePanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        loginButton.addActionListener(e -> login());
        collapseExpandButton.addActionListener(e -> toggleContent());

        this.authHooks = GsonUtil.loadJsonResource(ProfilesPlugin.class, "authHooks.json", AuthHooks.class);
    }

    private void toggleContent() {
        isExpanded = !isExpanded;
        contentPanel.setVisible(isExpanded);
        collapseExpandButton.setText(isExpanded ? "-" : "+");
        revalidate();
        repaint();
    }

    private void login() {
        if (profile == null)
            return;

        if (profile.isJagexAccount()) {
            setLoginWithJagexAccount(false);
        } else {
            setLoginWithUsernamePassword(false);
        }
    }

    private void setLoginIndex(int index) {
        try {
            if (this.authHooks.getSetLoginIndexGarbageValue() <= Byte.MAX_VALUE && this.authHooks.getSetLoginIndexGarbageValue() >= Byte.MIN_VALUE) {
                Class<?> paramComposition = Class.forName(this.authHooks.getSetLoginIndexClassName(), true,
                        client.getClass().getClassLoader());
                Method updateLoginIndex = paramComposition.getDeclaredMethod(this.authHooks.getSetLoginIndexMethodName(),
                        int.class, byte.class);
                updateLoginIndex.setAccessible(true);
                updateLoginIndex.invoke(null, index, (byte) this.authHooks.getSetLoginIndexGarbageValue());
                updateLoginIndex.setAccessible(false);
            } else if (this.authHooks.getSetLoginIndexGarbageValue() <= Short.MAX_VALUE && this.authHooks.getSetLoginIndexGarbageValue() >= Short.MIN_VALUE) {
                Class<?> paramComposition = Class.forName(this.authHooks.getSetLoginIndexClassName(), true,
                        client.getClass().getClassLoader());
                Method updateLoginIndex = paramComposition.getDeclaredMethod(this.authHooks.getSetLoginIndexMethodName(),
                        int.class, short.class);
                updateLoginIndex.setAccessible(true);
                updateLoginIndex.invoke(null, index, (short) this.authHooks.getSetLoginIndexGarbageValue());
                updateLoginIndex.setAccessible(false);
            } else {
                Class<?> paramComposition = Class.forName(this.authHooks.getSetLoginIndexClassName(), true,
                        client.getClass().getClassLoader());
                Method updateLoginIndex = paramComposition.getDeclaredMethod(this.authHooks.getSetLoginIndexMethodName(),
                        int.class, int.class);
                updateLoginIndex.setAccessible(true);
                updateLoginIndex.invoke(null, index, this.authHooks.getSetLoginIndexGarbageValue());
                updateLoginIndex.setAccessible(false);
            }
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    public void setLoginWithJagexAccount(boolean login) {
        clientThread.invokeLater(() -> {
            if (client.getGameState() != GameState.LOGIN_SCREEN) {
                return;
            }

            try {
                setLoginIndex(10);

                Class<?> jxSessionClass = Class.forName(this.authHooks.getJxSessionClassName(), true, client.getClass().getClassLoader());
                Field jxSessionField = jxSessionClass.getDeclaredField(this.authHooks.getJxSessionFieldName());
                jxSessionField.setAccessible(true);
                jxSessionField.set(null, profile.getSessionId());
                jxSessionField.setAccessible(false);

                Class<?> jxAccountIdClass = Class.forName(this.authHooks.getJxAccountIdClassName(), true, client.getClass().getClassLoader());
                Field jxAccountIdField = jxAccountIdClass.getDeclaredField(this.authHooks.getJxAccountIdFieldName());
                jxAccountIdField.setAccessible(true);
                jxAccountIdField.set(null, profile.getCharacterId());
                jxAccountIdField.setAccessible(false);

                Class<?> jxDisplayNameClass = Class.forName(this.authHooks.getJxDisplayNameClassName(), true, client.getClass().getClassLoader());
                Field jxDisplayNameField = jxDisplayNameClass.getDeclaredField(this.authHooks.getJxDisplayNameFieldName());
                jxDisplayNameField.setAccessible(true);
                jxDisplayNameField.set(null, profile.getCharacterName());
                jxDisplayNameField.setAccessible(false);
            } catch (Exception e) {
//                e.printStackTrace();
            }

            if (login) {
                client.setGameState(GameState.LOGGING_IN);
            }
        });

    }

    public void setLoginWithUsernamePassword(boolean login) {
        clientThread.invokeLater(() -> {
            if (client.getGameState() != GameState.LOGIN_SCREEN) {
                return;
            }

            try {
                Class<?> jxSessionClass = Class.forName(this.authHooks.getJxSessionClassName(), true, client.getClass().getClassLoader());
                Field jxSessionField = jxSessionClass.getDeclaredField(this.authHooks.getJxSessionFieldName());
                jxSessionField.setAccessible(true);
                jxSessionField.set(null, null);
                jxSessionField.setAccessible(false);

                Class<?> jxAccountIdClass = Class.forName(this.authHooks.getJxAccountIdClassName(), true, client.getClass().getClassLoader());
                Field jxAccountIdField = jxAccountIdClass.getDeclaredField(this.authHooks.getJxAccountIdFieldName());
                jxAccountIdField.setAccessible(true);
                jxAccountIdField.set(null, null);
                jxAccountIdField.setAccessible(false);

                Class<?> jxDisplayNameClass = Class.forName(this.authHooks.getJxDisplayNameClassName(), true, client.getClass().getClassLoader());
                Field jxDisplayNameField = jxDisplayNameClass.getDeclaredField(this.authHooks.getJxDisplayNameFieldName());
                jxDisplayNameField.setAccessible(true);
                jxDisplayNameField.set(null, null);
                jxAccountIdField.setAccessible(false);

                Class<?> jxLegacyAccountValueClass = Class.forName(this.authHooks.getJxLegacyValueClassName(), true, client.getClass().getClassLoader());
                Field jxLegacyAccountValueField = jxLegacyAccountValueClass.getDeclaredField(this.authHooks.getJxLegacyValueFieldName());
                jxLegacyAccountValueField.setAccessible(true);
                Object jxLegacyAccountObject = jxLegacyAccountValueField.get(null);
                jxLegacyAccountValueField.setAccessible(false);

                Class<?> clientClass = client.getClass(); // Class.forName("client", true, client.getClass.getClassLoader());
                Field jxAccountCheckField = clientClass.getDeclaredField(this.authHooks.getJxAccountCheckFieldName());
                jxAccountCheckField.setAccessible(true);
                jxAccountCheckField.set(null, jxLegacyAccountObject);
                jxAccountCheckField.setAccessible(false);
            } catch (Exception e) {
//                e.printStackTrace();
            }

            try {
                setLoginIndex(2);
            } catch (Exception e) {
//                e.printStackTrace();
            }

            client.setUsername(profile.getUsername());
            client.setPassword(profile.getPassword());

            if (login) {
                client.setGameState(GameState.LOGGING_IN);
            }
        });
    }
}
