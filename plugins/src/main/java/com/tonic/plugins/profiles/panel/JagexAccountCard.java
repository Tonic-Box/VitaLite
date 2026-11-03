package com.tonic.plugins.profiles.panel;

import com.tonic.plugins.profiles.data.JagexAccount;
import com.tonic.plugins.profiles.data.JagexAccountCharacter;
import com.tonic.plugins.profiles.jagex.JagexAccounts;
import com.tonic.plugins.profiles.jagex.JagexAuthException;
import com.tonic.plugins.profiles.jagex.JagexLoginException;
import com.tonic.plugins.profiles.session.ProfilesSession;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * One Jagex account in the profiles list: its characters in a dropdown, Fill and Login for the selected
 * character, Refresh, Edit and Delete, and a status line that offers a new sign-in when the session expired.
 */
public class JagexAccountCard extends JPanel {
    private static final int STATUS_WIDTH = 150;

    private final ProfilesSession store;
    private final JagexAccounts accounts;
    private final Runnable onListChanged;

    @Getter
    private JagexAccount account;

    private final JLabel titleLabel = new JLabel();
    private final JComboBox<JagexAccountCharacter> characterBox = new JComboBox<>();
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton fillButton = new JButton("Fill");
    private final JButton loginButton = new JButton("Login");
    private final JButton editButton = new JButton("Edit");
    private final JButton deleteButton = new JButton("Delete");
    private final JLabel statusLabel = new JLabel();
    private final JButton signInAgainButton = new JButton("Sign in again");
    private boolean busy;
    private boolean showingAccount;

    /**
     * @param account the account to show
     * @param store the profile store the card saves to
     * @param accounts the network operations on Jagex accounts
     * @param onListChanged called after the account was renamed or deleted, so the list is rebuilt
     */
    public JagexAccountCard(JagexAccount account, ProfilesSession store, JagexAccounts accounts, Runnable onListChanged) {
        this.store = store;
        this.accounts = accounts;
        this.onListChanged = onListChanged;

        setLayout(new BorderLayout());

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setPreferredSize(new Dimension(200, 32));
        titlePanel.setBackground(new Color(30, 30, 30));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(refreshButton, BorderLayout.EAST);

        characterBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Object shown = value instanceof JagexAccountCharacter ? ((JagexAccountCharacter) value).shownName() : value;
                return super.getListCellRendererComponent(list, shown, index, isSelected, cellHasFocus);
            }
        });

        JPanel loginRow = new JPanel(new GridLayout(1, 2));
        loginRow.add(fillButton);
        loginRow.add(loginButton);

        JPanel manageRow = new JPanel(new GridLayout(1, 2));
        manageRow.add(editButton);
        manageRow.add(deleteButton);

        JPanel statusRow = new JPanel(new BorderLayout(4, 0));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 0));
        statusRow.add(statusLabel, BorderLayout.CENTER);
        statusRow.add(signInAgainButton, BorderLayout.EAST);

        JPanel contentPanel = new JPanel(new GridLayout(0, 1));
        contentPanel.add(characterBox);
        contentPanel.add(loginRow);
        contentPanel.add(manageRow);

        JPanel body = new JPanel(new BorderLayout());
        body.add(contentPanel, BorderLayout.NORTH);
        body.add(statusRow, BorderLayout.SOUTH);

        add(titlePanel, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        characterBox.addActionListener(e -> onCharacterSelected());
        fillButton.addActionListener(e -> fill(false));
        loginButton.addActionListener(e -> fill(true));
        refreshButton.addActionListener(e -> refresh());
        signInAgainButton.addActionListener(e -> signInAgain());
        editButton.addActionListener(e -> edit());
        deleteButton.addActionListener(e -> delete());

        showAccount(account);
        setStatus(characterCount(account), false);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(super.getMaximumSize().width, getPreferredSize().height);
    }

    private void showAccount(JagexAccount shown) {
        account = shown;
        titleLabel.setText(shown.getLabel());
        showingAccount = true;
        characterBox.removeAllItems();
        for (JagexAccountCharacter character : shown.getCharacters()) {
            characterBox.addItem(character);
        }
        JagexAccountCharacter selected = shown.getSelectedCharacter();
        if (selected != null) {
            characterBox.setSelectedItem(selected);
        }
        showingAccount = false;
        updateButtons();
    }

    private void updateButtons() {
        boolean hasCharacter = characterBox.getSelectedItem() != null;
        characterBox.setEnabled(!busy && hasCharacter);
        fillButton.setEnabled(!busy && hasCharacter);
        loginButton.setEnabled(!busy && hasCharacter);
        refreshButton.setEnabled(!busy);
        editButton.setEnabled(!busy);
        deleteButton.setEnabled(!busy);
        signInAgainButton.setEnabled(!busy);
    }

    private void setBusy(boolean nowBusy, String status) {
        busy = nowBusy;
        updateButtons();
        if (status != null) {
            setStatus(status, signInAgainButton.isVisible());
        }
    }

    private void setStatus(String text, boolean expired) {
        statusLabel.setText("<html><body style='width:" + STATUS_WIDTH + "px'>" + escape(text) + "</body></html>");
        signInAgainButton.setVisible(expired);
        revalidate();
        repaint();
    }

    private void onCharacterSelected() {
        JagexAccountCharacter selected = (JagexAccountCharacter) characterBox.getSelectedItem();
        if (showingAccount || selected == null || selected.getAccountId().equals(account.getSelectedCharacterId())) {
            return;
        }
        JagexAccount updated = account.copy();
        updated.setSelectedCharacterId(selected.getAccountId());
        try {
            store.updateJagex(account.getLabel(), updated);
            account = updated;
        } catch (Exception e) {
            setStatus("Couldn't save the selection: " + e.getMessage(), false);
        }
    }

    private void fill(boolean login) {
        JagexAccountCharacter character = (JagexAccountCharacter) characterBox.getSelectedItem();
        if (character == null) {
            return;
        }
        JagexAccount target = account.copy();
        setBusy(true, null);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return store.login(target, character, login);
            }

            @Override
            protected void done() {
                setBusy(false, null);
                try {
                    boolean applied = get();
                    if (!applied) {
                        setStatus("Not on the login screen", false);
                    } else {
                        setStatus((login ? "Logging in as " : "Filled in ") + character.shownName(), false);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    setStatus("Couldn't fill the login screen: " + messageOf(e), false);
                }
            }
        }.execute();
    }

    private void refresh() {
        String label = account.getLabel();
        JagexAccount before = account.copy();
        setBusy(true, "Refreshing...");
        new SwingWorker<JagexAccount, Void>() {
            @Override
            protected JagexAccount doInBackground() throws Exception {
                return accounts.refresh(label);
            }

            @Override
            protected void done() {
                setBusy(false, null);
                try {
                    JagexAccount refreshed = get();
                    showAccount(refreshed);
                    int removed = removedCount(before, refreshed);
                    setStatus(characterCount(refreshed) + (removed > 0 ? ", " + removed + " no longer listed and removed" : ""), false);
                } catch (InterruptedException | ExecutionException e) {
                    Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
                    if (cause instanceof JagexAuthException && ((JagexAuthException) cause).isSessionExpired()) {
                        setStatus("Session expired", true);
                    } else {
                        setStatus("Refresh failed: " + messageOf(e), false);
                    }
                }
            }
        }.execute();
    }

    private void signInAgain() {
        String label = account.getLabel();
        setBusy(true, "Waiting for the browser sign-in...");
        JagexSignInDialog.run(this, "Sign in again: " + label, idToken -> accounts.signInAgain(label, idToken),
                refreshed -> {
                    setBusy(false, null);
                    showAccount(refreshed);
                    setStatus("Signed in again, " + characterCount(refreshed), false);
                },
                failure -> {
                    setBusy(false, null);
                    boolean cancelled = failure instanceof JagexLoginException && ((JagexLoginException) failure).getKind() == JagexLoginException.Kind.CANCELLED;
                    setStatus(cancelled ? "Session expired" : "Sign-in failed: " + failure.getMessage(), true);
                });
    }

    private void edit() {
        JTextField labelField = new JTextField(account.getLabel(), 16);
        List<JTextField> pinFields = new ArrayList<>();

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Label:"), gbc);
        gbc.gridx = 1;
        panel.add(labelField, gbc);
        for (JagexAccountCharacter character : account.getCharacters()) {
            JTextField pinField = new JTextField(character.getBankPin(), 4);
            pinFields.add(pinField);
            gbc.gridy++;
            gbc.gridx = 0;
            panel.add(new JLabel(character.shownName() + " bank PIN:"), gbc);
            gbc.gridx = 1;
            panel.add(pinField, gbc);
        }

        while (true) {
            int result = JOptionPane.showConfirmDialog(this, panel, "Editing: " + account.getLabel(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            JagexAccount updated = account.copy();
            updated.setLabel(labelField.getText().trim());
            String invalidPin = null;
            for (int i = 0; i < pinFields.size(); i++) {
                String pin = pinFields.get(i).getText().trim();
                JagexAccountCharacter character = updated.getCharacters().get(i);
                if (!pin.isEmpty() && !pin.matches("\\d{4}")) {
                    invalidPin = character.shownName();
                    break;
                }
                character.setBankPin(pin);
            }
            if (invalidPin != null) {
                JOptionPane.showMessageDialog(this, "The bank PIN of " + invalidPin + " must be 4 digits, or empty for none.", "Invalid bank PIN", JOptionPane.ERROR_MESSAGE);
                continue;
            }

            try {
                store.updateJagex(account.getLabel(), updated);
            } catch (IllegalArgumentException | IllegalStateException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Couldn't save", JOptionPane.ERROR_MESSAGE);
                continue;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Couldn't save profiles.db: " + e.getMessage(), "Couldn't save", JOptionPane.ERROR_MESSAGE);
                return;
            }
            boolean renamed = !updated.getLabel().equals(account.getLabel());
            showAccount(updated);
            setStatus("Saved", false);
            if (renamed) {
                onListChanged.run();
            }
            return;
        }
    }

    private void delete() {
        int result = JOptionPane.showConfirmDialog(this, "Do you want to delete " + account.getLabel() + " (" + characterCount(account).toLowerCase() + ")?", "Remove account", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            store.removeJagex(account.getLabel());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Couldn't delete: " + e.getMessage(), "Couldn't delete", JOptionPane.ERROR_MESSAGE);
            return;
        }
        onListChanged.run();
    }

    private static int removedCount(JagexAccount before, JagexAccount after) {
        int removed = 0;
        for (JagexAccountCharacter character : before.getCharacters()) {
            if (after.getCharacter(character.getAccountId()) == null) {
                removed++;
            }
        }
        return removed;
    }

    private static String characterCount(JagexAccount shown) {
        int count = shown.getCharacters().size();
        if (count == 0) {
            return "No characters";
        }
        return count + (count == 1 ? " character" : " characters");
    }

    private static String messageOf(Exception e) {
        Throwable cause = e instanceof ExecutionException && e.getCause() != null ? e.getCause() : e;
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
