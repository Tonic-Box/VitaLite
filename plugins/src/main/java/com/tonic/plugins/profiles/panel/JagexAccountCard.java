package com.tonic.plugins.profiles.panel;

import com.tonic.plugins.profiles.data.JagexAccount;
import com.tonic.plugins.profiles.data.JagexAccountCharacter;
import com.tonic.plugins.profiles.jagex.JagexAccounts;
import com.tonic.plugins.profiles.jagex.JagexAuthException;
import com.tonic.plugins.profiles.jagex.JagexLoginException;
import com.tonic.plugins.profiles.session.ProfilesSession;
import com.tonic.ui.VitaTheme;
import com.tonic.ui.WrappedText;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutionException;

/**
 * One Jagex account in the profiles list: a collapsible card with its characters in a dropdown to fill in or log in
 * with, Refresh, inline rename and per-character bank PINs, and a banner offering a new sign-in when the session expired.
 */
public class JagexAccountCard extends JPanel {
    static final Color NEUTRAL = ColorScheme.MEDIUM_GRAY_COLOR;
    static final Color EXPIRED_BACKGROUND = new Color(70, 35, 30);
    static final Color EXPIRED_TEXT = new Color(255, 150, 130);

    /**
     * What a card needs from the list it sits in: the state that outlives a rebuild of the list, and the rebuild itself.
     */
    public interface Host {
        /**
         * @param label an account label
         * @return whether that account's card is collapsed
         */
        boolean isCollapsed(String label);

        /**
         * @param label     an account label
         * @param collapsed whether its card is now collapsed
         */
        void setCollapsed(String label, boolean collapsed);

        /**
         * @param label an account label
         * @return whether that account's session was found expired
         */
        boolean isExpired(String label);

        /**
         * @param label   an account label
         * @param expired whether its session is now known to be expired
         */
        void setExpired(String label, boolean expired);

        /**
         * Moves the remembered state of an account to its new label and rebuilds the list.
         *
         * @param from the old label
         * @param to   the new label
         */
        void renamed(String from, String to);

        /** Rebuilds the list from the store, after an account was deleted. */
        void listChanged();
    }

    private final ProfilesSession store;
    private final JagexAccounts accounts;
    private final Host host;

    @Getter
    private JagexAccount account;
    private boolean open;
    private boolean busy;
    private boolean renaming;
    private String pinCharacterId;
    private JTextField renameField;
    private String status = "";
    private Color statusColor = VitaTheme.MUTED;

    /**
     * @param account  the account to show
     * @param store    the profile store the card saves to
     * @param accounts the network operations on Jagex accounts
     * @param host     the list the card sits in
     * @param forceOpen whether to show the card open whatever its remembered state, as while searching
     */
    public JagexAccountCard(JagexAccount account, ProfilesSession store, JagexAccounts accounts, Host host, boolean forceOpen) {
        super(new DynamicGridLayout(0, 1, 0, VitaTheme.GAP));
        this.account = account;
        this.store = store;
        this.accounts = accounts;
        this.host = host;
        this.open = forceOpen || !host.isCollapsed(account.getLabel());
        setBackground(VitaTheme.CARD);
        render();
    }

    private void render() {
        removeAll();
        boolean expired = host.isExpired(account.getLabel());
        setBorder(new CompoundBorder(new MatteBorder(0, 3, 0, 0, expired ? VitaTheme.ERROR : VitaTheme.accent()), new EmptyBorder(6, 6, 8, 6)));

        add(header());
        if (renaming) {
            add(renameButtons());
        }
        if (open) {
            if (expired) {
                add(expiredBanner());
            }
            JagexAccountCharacter selected = account.getSelectedCharacter();
            if (selected != null) {
                add(characterPicker(selected));
                add(characterActions(selected, expired));
                if (selected.getAccountId().equals(pinCharacterId)) {
                    add(pinEditor(selected));
                }
            }
        }
        if (!status.isEmpty()) {
            add(new WrappedText(status, VitaTheme.textFont(), statusColor, 0));
        }
        revalidate();
        repaint();
    }

    private JPanel header() {
        JPanel header = VitaTheme.row(new BorderLayout(4, 0));

        JButton chevron = VitaTheme.iconButton(VitaTheme.chevron(open), open ? "Collapse" : "Expand", this::toggle);
        header.add(chevron, BorderLayout.WEST);

        JPanel names = VitaTheme.row(new DynamicGridLayout(0, 1, 0, 1));
        if (renaming) {
            JTextField field = new JTextField(account.getLabel());
            field.setFont(VitaTheme.regularFont());
            bindKeys(field, () -> rename(field.getText()), this::stopRenaming);
            renameField = field;
            names.add(field);
            SwingUtilities.invokeLater(() -> {
                field.requestFocusInWindow();
                field.selectAll();
            });
        } else {
            JLabel title = new JLabel(account.getLabel());
            title.setFont(VitaTheme.titleFont());
            title.setForeground(VitaTheme.TITLE);
            names.add(title);
        }
        JLabel subtitle = new JLabel(characterCount(account));
        subtitle.setFont(VitaTheme.textFont());
        subtitle.setForeground(VitaTheme.MUTED);
        names.add(subtitle);
        header.add(names, BorderLayout.CENTER);

        JPanel actions = VitaTheme.row(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton refresh = VitaTheme.iconButton(VitaTheme.reload(), "Refresh the character list", this::refresh);
        refresh.setEnabled(!busy);
        actions.add(refresh);
        JButton more = VitaTheme.menuButton("More", this::accountMenu);
        more.setEnabled(!busy);
        actions.add(more);
        header.add(actions, BorderLayout.EAST);

        MouseAdapter toggleOnClick = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && !renaming) {
                    toggle();
                }
            }
        };
        header.addMouseListener(toggleOnClick);
        names.addMouseListener(toggleOnClick);
        return header;
    }

    private JPopupMenu accountMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem rename = new JMenuItem("Rename");
        rename.addActionListener(e -> {
            renaming = true;
            render();
        });
        menu.add(rename);
        JMenuItem delete = new JMenuItem("Delete");
        delete.addActionListener(e -> delete());
        menu.add(delete);
        return menu;
    }

    private JPanel renameButtons() {
        JPanel buttons = VitaTheme.row(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        JTextField field = renameField;
        buttons.add(VitaTheme.textButton("Cancel", NEUTRAL, this::stopRenaming));
        buttons.add(VitaTheme.textButton("Save", VitaTheme.accent(), () -> rename(field.getText())));
        return buttons;
    }

    private JPanel expiredBanner() {
        JPanel banner = new JPanel(new BorderLayout(4, 0));
        banner.setBackground(EXPIRED_BACKGROUND);
        banner.setBorder(new EmptyBorder(4, 6, 4, 4));
        JLabel text = new JLabel("Session expired");
        text.setFont(VitaTheme.textFont());
        text.setForeground(EXPIRED_TEXT);
        banner.add(text, BorderLayout.CENTER);
        JButton signIn = VitaTheme.textButton("Sign in", busy ? VitaTheme.BUSY : VitaTheme.accent(), this::signInAgain);
        signIn.setEnabled(!busy);
        banner.add(signIn, BorderLayout.EAST);
        return banner;
    }

    private void toggle() {
        open = !open;
        host.setCollapsed(account.getLabel(), !open);
        render();
    }

    private void setStatus(String text, Color color) {
        status = text;
        statusColor = color;
        render();
    }

    private void setBusy(boolean nowBusy, String text) {
        busy = nowBusy;
        if (text != null) {
            status = text;
            statusColor = VitaTheme.MUTED;
        }
        render();
    }

    private void stopRenaming() {
        renaming = false;
        render();
    }

    private void rename(String text) {
        String label = text.trim();
        if (label.equals(account.getLabel())) {
            stopRenaming();
            return;
        }
        JagexAccount updated = account.copy();
        updated.setLabel(label);
        try {
            store.updateJagex(account.getLabel(), updated);
        } catch (IllegalArgumentException | IllegalStateException e) {
            setStatus(e.getMessage(), VitaTheme.ERROR);
            return;
        } catch (Exception e) {
            setStatus("Couldn't save profiles.db: " + e.getMessage(), VitaTheme.ERROR);
            return;
        }
        host.renamed(account.getLabel(), label);
    }

    private void fill(JagexAccountCharacter character, boolean login) {
        select(character);
        JagexAccount target = account.copy();
        setBusy(true, null);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return store.login(target, character, login);
            }

            @Override
            protected void done() {
                busy = false;
                try {
                    if (!get()) {
                        setStatus("Not on the login screen", VitaTheme.ERROR);
                    } else {
                        setStatus((login ? "Logging in as " : "Filled in ") + character.shownName(), VitaTheme.SUCCESS);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    setStatus("Couldn't fill the login screen: " + messageOf(e), VitaTheme.ERROR);
                }
            }
        }.execute();
    }

    private void select(JagexAccountCharacter character) {
        if (character.getAccountId().equals(account.getSelectedCharacterId())) {
            return;
        }
        JagexAccount updated = account.copy();
        updated.setSelectedCharacterId(character.getAccountId());
        try {
            store.updateJagex(account.getLabel(), updated);
            account = updated;
        } catch (Exception e) {
            status = "Couldn't save the selection: " + e.getMessage();
            statusColor = VitaTheme.ERROR;
        }
    }

    private void savePin(JagexAccountCharacter character, String text) {
        String pin = text.trim();
        if (!pin.isEmpty() && !pin.matches("\\d{4}")) {
            setStatus("A bank PIN is 4 digits, or empty for none.", VitaTheme.ERROR);
            return;
        }
        JagexAccount updated = account.copy();
        updated.getCharacter(character.getAccountId()).setBankPin(pin);
        try {
            store.updateJagex(account.getLabel(), updated);
        } catch (Exception e) {
            setStatus("Couldn't save profiles.db: " + e.getMessage(), VitaTheme.ERROR);
            return;
        }
        account = updated;
        pinCharacterId = null;
        setStatus(pin.isEmpty() ? "Bank PIN of " + character.shownName() + " cleared" : "Bank PIN of " + character.shownName() + " saved", VitaTheme.SUCCESS);
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
                busy = false;
                try {
                    JagexAccount refreshed = get();
                    account = refreshed;
                    host.setExpired(label, false);
                    int removed = removedCount(before, refreshed);
                    setStatus(characterCount(refreshed) + (removed > 0 ? ", " + removed + " no longer listed and removed" : ""), VitaTheme.MUTED);
                } catch (InterruptedException | ExecutionException e) {
                    Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
                    if (cause instanceof JagexAuthException && ((JagexAuthException) cause).isSessionExpired()) {
                        host.setExpired(label, true);
                        setStatus("", VitaTheme.MUTED);
                    } else {
                        setStatus("Refresh failed: " + messageOf(e), VitaTheme.ERROR);
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
                    busy = false;
                    account = refreshed;
                    host.setExpired(label, false);
                    setStatus("Signed in again, " + characterCount(refreshed).toLowerCase(), VitaTheme.SUCCESS);
                },
                failure -> {
                    busy = false;
                    boolean cancelled = failure instanceof JagexLoginException && ((JagexLoginException) failure).getKind() == JagexLoginException.Kind.CANCELLED;
                    setStatus(cancelled ? "" : "Sign-in failed: " + failure.getMessage(), VitaTheme.ERROR);
                });
    }

    private void delete() {
        int result = JOptionPane.showConfirmDialog(this, "Do you want to delete " + account.getLabel() + " (" + characterCount(account).toLowerCase() + ")?", "Remove account", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            store.removeJagex(account.getLabel());
        } catch (Exception e) {
            setStatus("Couldn't delete: " + e.getMessage(), VitaTheme.ERROR);
            return;
        }
        host.listChanged();
    }

    /**
     * Saves on Enter and cancels on Escape.
     *
     * @param field  the field
     * @param save   what Enter does
     * @param cancel what Escape does
     */
    static void bindKeys(JTextField field, Runnable save, Runnable cancel) {
        field.addActionListener(e -> save.run());
        field.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        field.getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cancel.run();
            }
        });
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

    private JComboBox<JagexAccountCharacter> characterPicker(JagexAccountCharacter selected) {
        JComboBox<JagexAccountCharacter> picker = new JComboBox<>(account.getCharacters().toArray(new JagexAccountCharacter[0]));
        picker.setSelectedItem(selected);
        picker.setFont(VitaTheme.regularFont());
        picker.setBackground(VitaTheme.BACKGROUND);
        picker.setFocusable(false);
        picker.setMaximumRowCount(12);
        picker.setEnabled(!busy);
        picker.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JagexAccountCharacter character = (JagexAccountCharacter) value;
                String shown = character == null ? "" : character.isNamed() ? character.getDisplayName() : "No name yet";
                Component cell = super.getListCellRendererComponent(list, shown, index, isSelected, cellHasFocus);
                if (character != null && !character.isNamed() && !isSelected) {
                    cell.setForeground(VitaTheme.MUTED);
                }
                return cell;
            }
        });
        picker.addActionListener(e -> {
            JagexAccountCharacter chosen = (JagexAccountCharacter) picker.getSelectedItem();
            if (chosen != null && !chosen.getAccountId().equals(account.getSelectedCharacterId())) {
                select(chosen);
                pinCharacterId = null;
                render();
            }
        });
        return picker;
    }

    private JPanel characterActions(JagexAccountCharacter character, boolean expired) {
        boolean enabled = !busy && !expired;
        JPanel actions = VitaTheme.row(new BorderLayout(4, 0));
        JPanel buttons = VitaTheme.row(new GridLayout(1, 2, 4, 0));
        JButton fill = VitaTheme.textButton("Fill", enabled ? NEUTRAL : VitaTheme.BUSY, () -> fill(character, false));
        fill.setToolTipText("Fill in the login screen without logging in");
        fill.setEnabled(enabled);
        buttons.add(fill);
        JButton login = VitaTheme.textButton("Login", enabled ? VitaTheme.accent() : VitaTheme.BUSY, () -> fill(character, true));
        login.setToolTipText("Log in as " + character.shownName());
        login.setEnabled(enabled);
        buttons.add(login);
        actions.add(buttons, BorderLayout.CENTER);
        JButton more = VitaTheme.menuButton("More", () -> characterMenu(character));
        more.setEnabled(!busy);
        actions.add(more, BorderLayout.EAST);
        return actions;
    }

    private JPopupMenu characterMenu(JagexAccountCharacter character) {
        JPopupMenu menu = new JPopupMenu();
        boolean hasPin = character.getBankPin() != null && !character.getBankPin().isEmpty();
        JMenuItem pin = new JMenuItem((hasPin ? "Change bank PIN of " : "Set bank PIN of ") + character.shownName() + "...");
        pin.addActionListener(e -> {
            pinCharacterId = character.getAccountId();
            render();
        });
        menu.add(pin);
        return menu;
    }

    private JPanel pinEditor(JagexAccountCharacter character) {
        JPanel editor = VitaTheme.row(new BorderLayout(4, 0));
        JLabel label = new JLabel("Bank PIN");
        label.setFont(VitaTheme.textFont());
        label.setForeground(VitaTheme.MUTED);
        editor.add(label, BorderLayout.WEST);
        JTextField field = new JTextField(character.getBankPin() == null ? "" : character.getBankPin(), 4);
        field.setFont(VitaTheme.regularFont());
        Runnable cancel = () -> {
            pinCharacterId = null;
            render();
        };
        bindKeys(field, () -> savePin(character, field.getText()), cancel);
        editor.add(field, BorderLayout.CENTER);
        JPanel buttons = VitaTheme.row(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        buttons.add(VitaTheme.textButton("Cancel", NEUTRAL, cancel));
        buttons.add(VitaTheme.textButton("Save", VitaTheme.accent(), () -> savePin(character, field.getText())));
        editor.add(buttons, BorderLayout.EAST);
        SwingUtilities.invokeLater(field::requestFocusInWindow);
        return editor;
    }
}
