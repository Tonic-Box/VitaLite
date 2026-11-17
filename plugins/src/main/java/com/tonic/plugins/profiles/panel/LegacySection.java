package com.tonic.plugins.profiles.panel;

import com.tonic.plugins.profiles.data.Profile;
import com.tonic.plugins.profiles.session.ProfilesSession;
import com.tonic.ui.VitaTheme;
import com.tonic.ui.WrappedText;
import net.runelite.client.ui.DynamicGridLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * The legacy accounts at the bottom of the profiles list: a collapsible group of rows that can be edited inline or
 * deleted but no longer log in, since Jagex stopped accepting them.
 */
public class LegacySection extends JPanel {
    private final ProfilesSession store;
    private final Runnable onChanged;

    /**
     * @param profiles  the legacy profiles to show
     * @param store     the profile store rows save to
     * @param open      whether the group is open
     * @param onToggle  called when the group header is clicked
     * @param onChanged called after a profile was saved or deleted, so the list is rebuilt
     */
    public LegacySection(List<Profile> profiles, ProfilesSession store, boolean open, Runnable onToggle, Runnable onChanged) {
        super(new DynamicGridLayout(0, 1, 0, VitaTheme.GAP));
        this.store = store;
        this.onChanged = onChanged;
        setOpaque(false);
        setBorder(new EmptyBorder(6, 0, 0, 0));

        JPanel header = VitaTheme.row(new BorderLayout(4, 0));
        header.add(VitaTheme.iconButton(VitaTheme.chevron(open), open ? "Hide legacy accounts" : "Show legacy accounts", onToggle), BorderLayout.WEST);
        JLabel title = new JLabel("Legacy accounts (" + profiles.size() + ")");
        title.setFont(VitaTheme.regularFont());
        title.setForeground(VitaTheme.MUTED);
        header.add(title, BorderLayout.CENTER);
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    onToggle.run();
                }
            }
        });
        add(header);

        if (!open) {
            return;
        }
        add(new WrappedText("Jagex no longer allows logging in with these. Kept for reference.", VitaTheme.textFont(), VitaTheme.MUTED, 0));
        for (Profile profile : profiles) {
            add(new LegacyRow(profile));
        }
    }

    private final class LegacyRow extends JPanel {
        private final Profile profile;
        private final WrappedText error = new WrappedText("", VitaTheme.textFont(), VitaTheme.ERROR, 0);

        LegacyRow(Profile profile) {
            super(new DynamicGridLayout(0, 1, 0, 3));
            this.profile = profile;
            setBackground(VitaTheme.CARD);
            setBorder(new EmptyBorder(5, 8, 5, 4));
            showSummary();
        }

        private void showSummary() {
            removeAll();
            JPanel line = VitaTheme.row(new BorderLayout(4, 0));
            JPanel names = VitaTheme.row(new DynamicGridLayout(0, 1, 0, 1));
            JLabel tag = new JLabel(profile.getIdentifier());
            tag.setFont(VitaTheme.regularFont());
            tag.setForeground(VitaTheme.MUTED);
            names.add(tag);
            if (!StringUtils.isBlank(profile.getUsername())) {
                JLabel username = new JLabel(profile.getUsername());
                username.setFont(VitaTheme.textFont());
                username.setForeground(VitaTheme.MUTED.darker());
                names.add(username);
            }
            line.add(names, BorderLayout.CENTER);
            line.add(VitaTheme.menuButton("More", this::menu), BorderLayout.EAST);
            add(line);
            revalidate();
            repaint();
        }

        private JPopupMenu menu() {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem edit = new JMenuItem("Edit");
            edit.addActionListener(e -> showEditor());
            menu.add(edit);
            JMenuItem delete = new JMenuItem("Delete");
            delete.addActionListener(e -> delete());
            menu.add(delete);
            return menu;
        }

        private void showEditor() {
            removeAll();
            JTextField tag = new JTextField(profile.getIdentifier());
            JTextField username = new JTextField(profile.getUsername());
            JPasswordField password = new JPasswordField(profile.getPassword());
            JTextField bankPin = new JTextField(profile.getBankPin(), 4);
            Runnable save = () -> save(tag.getText(), username.getText(), new String(password.getPassword()), bankPin.getText());
            Runnable cancel = this::showSummary;

            add(field("Tag", tag, save, cancel));
            add(field("Username", username, save, cancel));
            add(field("Password", password, save, cancel));
            add(field("Bank PIN", bankPin, save, cancel));

            JPanel buttons = VitaTheme.row(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            buttons.add(VitaTheme.textButton("Cancel", JagexAccountCard.NEUTRAL, cancel));
            buttons.add(VitaTheme.textButton("Save", VitaTheme.accent(), save));
            add(buttons);
            revalidate();
            repaint();
            SwingUtilities.invokeLater(tag::requestFocusInWindow);
        }

        private JPanel field(String label, JTextField field, Runnable save, Runnable cancel) {
            JPanel row = VitaTheme.row(new DynamicGridLayout(0, 1, 0, 1));
            JLabel name = new JLabel(label);
            name.setFont(VitaTheme.textFont());
            name.setForeground(VitaTheme.MUTED);
            row.add(name);
            field.setFont(VitaTheme.regularFont());
            JagexAccountCard.bindKeys(field, save, cancel);
            row.add(field);
            return row;
        }

        private void save(String tag, String username, String password, String bankPin) {
            String pin = bankPin.trim();
            if (!pin.isEmpty() && !pin.matches("\\d{4}")) {
                showError("A bank PIN is 4 digits, or empty for none.");
                return;
            }
            Profile updated = new Profile(tag.trim(), false, username, password, username, "", "", pin);
            try {
                store.updateLegacy(profile.getIdentifier(), updated);
            } catch (IllegalArgumentException | IllegalStateException e) {
                showError(e.getMessage());
                return;
            } catch (Exception e) {
                showError("Couldn't save profiles.db: " + e.getMessage());
                return;
            }
            onChanged.run();
        }

        private void showError(String message) {
            error.setText(message);
            if (error.getParent() == null) {
                add(error, getComponentCount() - 1);
            }
            revalidate();
            repaint();
        }

        private void delete() {
            int result = JOptionPane.showConfirmDialog(this, "Do you want to delete " + profile.getIdentifier() + "?", "Remove account", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                store.removeLegacy(profile.getIdentifier());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Couldn't delete: " + e.getMessage(), "Couldn't delete", JOptionPane.ERROR_MESSAGE);
                return;
            }
            onChanged.run();
        }
    }
}
