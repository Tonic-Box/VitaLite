package com.tonic.ui.sdn;

import com.tonic.model.ui.components.ToggleSlider;
import com.tonic.services.hub.HubFetcher;
import com.tonic.services.hub.HubRepo;
import com.tonic.services.hub.HubService;
import com.tonic.services.hub.HubState;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.components.FlatTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The Hub's repositories, as a page of the Vita Hub tab: switch each on or off, remove the ones that are not official,
 * and add new ones by owner/repo or address.
 */
final class ReposPage extends JPanel {
    private static final String THIRD_PARTY_WARNING = "This repository is not official. VitaLite does not check its plugins.\n\n"
            + "Plugins can do anything on your computer. Fake plugins are a common way to steal accounts and infect computers.\n\n"
            + "Only add it if you trust the person who made it.";
    private static final int CONFIRM_SECONDS = 10;
    private static final String THIRD_PARTY_CONFIRM = "Are you sure? Anything you install from this repository can access your accounts and files.";

    private final HubService hub = HubService.get();
    private final JPanel repos = HubTheme.row(new DynamicGridLayout(0, 1, 0, HubTheme.GAP));
    private final FlatTextField input = new FlatTextField();
    private final WrappedText status = new WrappedText("", HubTheme.textFont(), HubTheme.MUTED, 0);
    private String localStatus;

    /**
     * @param back called when the back arrow is pressed
     */
    ReposPage(Runnable back) {
        super(new BorderLayout());
        setBackground(HubTheme.BACKGROUND);

        JPanel title = HubTheme.row(new BorderLayout(HubTheme.GAP, 0));
        title.setBorder(new EmptyBorder(10, 10, 10, 10));
        title.add(HubTheme.iconButton(HubTheme.back(), "Back", back), BorderLayout.WEST);
        title.add(new WrappedText("Repositories", HubTheme.titleFont(), HubTheme.TITLE, 0), BorderLayout.CENTER);
        add(title, BorderLayout.NORTH);

        JPanel column = new ScrollColumn();
        column.setLayout(new DynamicGridLayout(0, 1, 0, 10));
        column.setBorder(new EmptyBorder(0, 10, 10, 10));
        column.add(repos);
        column.add(addSection());

        JPanel top = new ScrollColumn();
        top.setLayout(new BorderLayout());
        top.add(column, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(top, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(HubTheme.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    /** Shows the repositories as they are now. */
    void rebuild() {
        repos.removeAll();
        List<HubRepo> all;
        HubState state = HubState.get();
        synchronized (state) {
            all = new ArrayList<>(state.repos);
        }
        boolean busy = hub.isBusy();
        for (HubRepo repo : all) {
            repos.add(card(repo, busy));
        }
        String error = hub.error();
        if (localStatus != null) {
            status.setForeground(HubTheme.ERROR);
            status.setText(localStatus);
        } else if (error != null) {
            status.setForeground(HubTheme.ERROR);
            status.setText(error);
        } else {
            status.setForeground(HubTheme.MUTED);
            status.setText(busy ? hub.message() : "");
        }
        revalidate();
        repaint();
    }

    private JComponent addSection() {
        JPanel section = HubTheme.row(new DynamicGridLayout(0, 1, 0, HubTheme.GAP));
        section.add(new WrappedText("Add a repository", HubTheme.titleFont(), HubTheme.TITLE, 0));
        section.add(new WrappedText("Enter owner/repo, a github.com link, or the https address of a plugins.json.", HubTheme.textFont(), HubTheme.MUTED, 0));

        input.setBackground(HubTheme.CARD);
        input.setHoverBackgroundColor(HubTheme.CARD_HOVER);
        input.setBorder(new EmptyBorder(0, 6, 0, 6));
        input.setPreferredSize(new Dimension(0, 28));
        input.addActionListener(e -> add());

        JPanel row = HubTheme.row(new BorderLayout(HubTheme.GAP, 0));
        row.add(input, BorderLayout.CENTER);
        row.add(HubTheme.textButton("Add", HubTheme.accent(), this::add), BorderLayout.EAST);
        section.add(row);
        section.add(status);
        return section;
    }

    private JComponent card(HubRepo repo, boolean busy) {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(HubTheme.CARD);
        card.setBorder(new EmptyBorder(HubTheme.PADDING, HubTheme.PADDING, HubTheme.PADDING, HubTheme.PADDING));

        JPanel text = HubTheme.row(new DynamicGridLayout(0, 1, 0, 2));
        text.add(new WrappedText(repo.official ? "Official" : repo.label(), HubTheme.titleFont(), HubTheme.TITLE, 0));
        String[] detail = detail(repo);
        text.add(new WrappedText(detail[0], HubTheme.textFont(), detail[1] == null ? HubTheme.MUTED : HubTheme.ERROR, 0));
        if (detail[1] != null && !detail[1].isEmpty()) {
            text.add(new WrappedText(detail[1], HubTheme.textFont(), HubTheme.ERROR, 0));
        }
        text.setToolTipText(repo.url);

        GridBagConstraints cell = new GridBagConstraints();
        cell.gridx = 0;
        cell.weightx = 1;
        cell.fill = GridBagConstraints.HORIZONTAL;
        cell.anchor = GridBagConstraints.NORTHWEST;
        card.add(text, cell);

        ToggleSlider enabled = new ToggleSlider();
        enabled.setSelected(repo.enabled);
        enabled.setEnabled(!busy);
        enabled.setToolTipText(repo.enabled ? "Stop reading this repository" : "Read this repository");
        enabled.addActionListener(e -> hub.setEnabled(repo.url, enabled.isSelected()));
        cell = new GridBagConstraints();
        cell.gridx = 1;
        cell.anchor = GridBagConstraints.NORTHEAST;
        cell.insets = new Insets(0, HubTheme.GAP, 0, 0);
        card.add(enabled, cell);

        if (!repo.official) {
            JButton remove = HubTheme.iconButton(HubTheme.trash(), "Remove this repository", () -> remove(repo));
            remove.setEnabled(!busy);
            cell = new GridBagConstraints();
            cell.gridx = 2;
            cell.anchor = GridBagConstraints.NORTHEAST;
            card.add(remove, cell);
        }
        return card;
    }

    private String[] detail(HubRepo repo) {
        if (!repo.enabled) {
            return new String[]{"Off", null};
        }
        for (HubFetcher.Result result : hub.results()) {
            if (!result.repo.url.equals(repo.url)) {
                continue;
            }
            if (result.manifest == null) {
                return new String[]{result.error == null ? "No plugins loaded" : capitalize(result.error), result.error == null ? null : ""};
            }
            String count = plural(result.manifest.plugins.size(), "plugin");
            String summary = repo.official ? count + " · signed" : count;
            return new String[]{summary, result.error};
        }
        return new String[]{hub.isBusy() ? "Loading…" : "Not loaded yet", null};
    }

    private void remove(HubRepo repo) {
        int choice = JOptionPane.showConfirmDialog(this, "Remove " + repo.label() + "?\nPlugins installed from it stay installed.", "Vita Hub", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            hub.removeRepo(repo.url);
        }
    }

    private void add() {
        String text = input.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        try {
            HubRepo.resolve(text);
        } catch (IllegalArgumentException e) {
            localStatus = e.getMessage();
            rebuild();
            return;
        }
        localStatus = null;
        HubState state = HubState.get();
        if (!state.thirdPartyWarned) {
            if (!confirm(THIRD_PARTY_WARNING) || !confirmAfterCountdown(THIRD_PARTY_CONFIRM, "Yes, add it", CONFIRM_SECONDS)) {
                return;
            }
            synchronized (state) {
                state.thirdPartyWarned = true;
                state.save();
            }
        }
        input.setText("");
        hub.addRepo(text);
    }

    private boolean confirm(String message) {
        Object[] options = {"Add anyway", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, message, "Unofficial repository", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        return choice == 0;
    }

    private boolean confirmAfterCountdown(String message, String accept, int seconds) {
        JButton cancel = new JButton("Cancel");
        JButton confirm = new JButton(accept + " (" + seconds + ")");
        confirm.setPreferredSize(confirm.getPreferredSize());
        confirm.setEnabled(false);
        JOptionPane pane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{cancel, confirm}, cancel);
        cancel.addActionListener(e -> pane.setValue(cancel));
        confirm.addActionListener(e -> pane.setValue(confirm));

        int[] remaining = {seconds};
        Timer countdown = new Timer(1000, e -> {
            remaining[0]--;
            if (remaining[0] > 0) {
                confirm.setText(accept + " (" + remaining[0] + ")");
                return;
            }
            confirm.setText(accept);
            confirm.setEnabled(true);
            ((Timer) e.getSource()).stop();
        });

        JDialog dialog = pane.createDialog(this, "Unofficial repository");
        countdown.start();
        dialog.setVisible(true);
        countdown.stop();
        dialog.dispose();
        return pane.getValue() == confirm;
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    static String plural(int count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }

    /** A panel that always takes the scroll pane's width, so its contents wrap rather than scroll sideways. */
    static final class ScrollColumn extends JPanel implements Scrollable {
        ScrollColumn() {
            setBackground(HubTheme.BACKGROUND);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(16, visible.height - 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
