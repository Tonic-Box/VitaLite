package com.tonic.ui.sdn;

import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.SwingUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * One plugin in the Vita Hub tab: icon, name, author and repository, a description that expands on click, its version,
 * and the controls that apply to it.
 */
final class PluginCard extends JPanel {
    /** Everything a card shows and can do. */
    static final class Model {
        String key;
        ImageIcon icon;
        String name;
        String meta;
        String description;
        String version;
        Color versionColor;
        List<String> requires = List.of();
        String source;
        Runnable openConfig;
        Boolean active;
        Consumer<Boolean> setActive;
        String primary;
        Color primaryColor;
        Runnable primaryAction;
        Runnable trash;
        Runnable reload;
    }

    private final Model model;
    private final WrappedText description;
    private final JPanel details;
    private boolean expanded;

    /**
     * @param model    what to show
     * @param expanded whether its description starts expanded
     * @param onToggle told the card's new expanded state when the user clicks it
     */
    PluginCard(Model model, boolean expanded, Consumer<Boolean> onToggle) {
        super(new GridBagLayout());
        this.model = model;
        this.expanded = expanded;
        setBackground(HubTheme.CARD);
        setBorder(new EmptyBorder(HubTheme.PADDING, HubTheme.PADDING, HubTheme.PADDING, HubTheme.PADDING));

        JLabel icon = HubTheme.clear(new JLabel(model.icon));
        icon.setVerticalAlignment(SwingConstants.TOP);
        GridBagConstraints iconCell = new GridBagConstraints();
        iconCell.gridx = 0;
        iconCell.gridy = 0;
        iconCell.gridheight = 6;
        iconCell.anchor = GridBagConstraints.NORTHWEST;
        iconCell.insets = new Insets(0, 0, 0, HubTheme.PADDING);
        add(icon, iconCell);

        add(header(), cell(0, 0));

        if (model.meta != null && !model.meta.isEmpty()) {
            add(new WrappedText(model.meta, HubTheme.textFont(), HubTheme.MUTED, 0), cell(1, 2));
        }

        description = new WrappedText(model.description, HubTheme.textFont(), HubTheme.TEXT, expanded ? 0 : 2);
        if (model.description != null && !model.description.isEmpty()) {
            add(description, cell(2, 4));
        }

        details = details();
        details.setVisible(expanded);
        add(details, cell(3, 4));

        if (model.version != null && !model.version.isEmpty()) {
            add(new WrappedText(model.version, HubTheme.textFont(), model.versionColor == null ? HubTheme.MUTED : model.versionColor, 0), cell(4, 4));
        }

        JPanel actions = actions();
        if (actions.getComponentCount() > 0) {
            add(actions, cell(5, 6));
        }

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(HubTheme.CARD_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!contains(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), PluginCard.this))) {
                    setBackground(HubTheme.CARD);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!e.isConsumed() && SwingUtilities.isLeftMouseButton(e) && canExpand()) {
                    setExpanded(!PluginCard.this.expanded);
                    onToggle.accept(PluginCard.this.expanded);
                }
            }
        };
        listen(this, mouse);
    }

    private JPanel header() {
        JPanel header = HubTheme.row(new BorderLayout(HubTheme.GAP, 0));
        header.add(new WrappedText(model.name, HubTheme.titleFont(), HubTheme.TITLE, 0), BorderLayout.CENTER);

        JPanel controls = HubTheme.row(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        if (model.openConfig != null) {
            controls.add(HubTheme.iconButton(HubTheme.cog(), "Edit plugin configuration", model.openConfig));
        }
        if (model.active != null) {
            JToggleButton toggle = new JToggleButton(HubTheme.switchOff());
            toggle.setSelectedIcon(HubTheme.switchOn());
            SwingUtil.removeButtonDecorations(toggle);
            HubTheme.bare(toggle);
            toggle.setFocusable(false);
            toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            toggle.setSelected(model.active);
            toggle.setToolTipText(model.active ? "Turn off" : "Turn on");
            toggle.addActionListener(e -> {
                toggle.setToolTipText(toggle.isSelected() ? "Turn off" : "Turn on");
                model.setActive.accept(toggle.isSelected());
            });
            controls.add(toggle);
        }
        if (controls.getComponentCount() > 0) {
            header.add(controls, BorderLayout.EAST);
        }
        return header;
    }

    private JPanel details() {
        JPanel panel = HubTheme.row(new DynamicGridLayout(0, 1, 0, 2));
        if (!model.requires.isEmpty()) {
            panel.add(new WrappedText("Requires " + String.join(", ", model.requires), HubTheme.textFont(), HubTheme.MUTED, 0));
        }
        if (hasSource()) {
            WrappedText link = new WrappedText(model.source.replaceFirst("^https://", ""), HubTheme.textFont(), HubTheme.accent(), 0);
            link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            link.setToolTipText("Open in your browser");
            link.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    LinkBrowser.browse(model.source);
                    e.consume();
                }
            });
            panel.add(link);
        }
        return panel;
    }

    private JPanel actions() {
        JPanel actions = HubTheme.row(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        if (hasSource()) {
            actions.add(HubTheme.iconButton(HubTheme.github(), "Open the source on GitHub", () -> LinkBrowser.browse(model.source)));
        }
        if (model.reload != null) {
            actions.add(HubTheme.iconButton(HubTheme.reload(), "Reload", model.reload));
        }
        if (model.trash != null) {
            actions.add(HubTheme.iconButton(HubTheme.trash(), "Uninstall", model.trash));
        }
        if (model.primary != null) {
            actions.add(HubTheme.textButton(model.primary, model.primaryColor, model.primaryAction));
        }
        return actions;
    }

    private boolean hasSource() {
        return model.source != null && model.source.startsWith("https://");
    }

    private boolean canExpand() {
        return expanded || description.isTruncated() || details.getComponentCount() > 0;
    }

    private void setExpanded(boolean expanded) {
        this.expanded = expanded;
        description.setMaxLines(expanded ? 0 : 2);
        details.setVisible(expanded);
        revalidate();
        repaint();
    }

    private static GridBagConstraints cell(int row, int topGap) {
        GridBagConstraints cell = new GridBagConstraints();
        cell.gridx = 1;
        cell.gridy = row;
        cell.weightx = 1;
        cell.fill = GridBagConstraints.HORIZONTAL;
        cell.anchor = GridBagConstraints.NORTHWEST;
        cell.insets = new Insets(row == 0 ? 0 : topGap, 0, 0, 0);
        return cell;
    }

    private static void listen(Component component, MouseAdapter mouse) {
        if (component instanceof AbstractButton) {
            component.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    mouse.mouseEntered(e);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    mouse.mouseExited(e);
                }
            });
            return;
        }
        component.addMouseListener(mouse);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                listen(child, mouse);
            }
        }
    }
}
