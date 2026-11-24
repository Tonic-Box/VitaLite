package com.tonic.services.pathfinder.ui;

import com.tonic.Static;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.transports.Transport;
import com.tonic.util.ThreadPool;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.ColorScheme;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * The form for one transport, edited as a draft that Apply validates and hands on and Revert throws away; the walk buttons send the walker to either tile.
 */
final class TransportDetailPanel extends JPanel
{
    private static final Color ERROR = new Color(0xE0, 0x60, 0x4C);

    private final transient RequirementDescriptions descriptions;
    private final transient Consumer<Transport> apply;
    private final JTextField name = new JTextField(18);
    private final JRadioButton object = new JRadioButton("Object", true);
    private final JRadioButton npc = new JRadioButton("NPC");
    private final JSpinner id = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    private final JLabel targetName = new JLabel(" ");
    private final JSpinner option = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
    private final JSpinner sourceX = coordinate();
    private final JSpinner sourceY = coordinate();
    private final JSpinner sourcePlane = plane();
    private final JSpinner destinationX = coordinate();
    private final JSpinner destinationY = coordinate();
    private final JSpinner destinationPlane = plane();
    private final JSpinner duration = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
    private final JTextArea dialogue = new JTextArea(3, 24);
    private final RequirementsPanel requirements;
    private final JLabel errors = new JLabel(" ");
    private final JButton applyButton = new JButton("Apply");
    private final JButton revertButton = new JButton("Revert");
    private final JButton walkSource = new JButton("Walk to source");
    private final JButton walkDestination = new JButton("Walk to destination");
    private transient TransportRow row;
    private boolean loading;

    TransportDetailPanel(RequirementDescriptions descriptions, Consumer<Transport> apply, Runnable revert)
    {
        super(new BorderLayout(0, 6));
        this.descriptions = descriptions;
        this.apply = apply;
        this.requirements = new RequirementsPanel(descriptions, this::touched);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        ButtonGroup kind = new ButtonGroup();
        kind.add(object);
        kind.add(npc);
        JPanel kindRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        kindRow.setOpaque(false);
        kindRow.add(object);
        kindRow.add(npc);
        id.addChangeListener(e -> refreshTargetName());
        object.addActionListener(e -> refreshTargetName());
        npc.addActionListener(e -> refreshTargetName());
        targetName.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int rowIndex = 0;
        rowIndex = formRow(form, rowIndex, "Name", name);
        rowIndex = formRow(form, rowIndex, "Target", kindRow);
        rowIndex = formRow(form, rowIndex, "Target id", withLabel(id, targetName));
        rowIndex = formRow(form, rowIndex, "Option", option);
        rowIndex = formRow(form, rowIndex, "Source", tileRow(sourceX, sourceY, sourcePlane));
        rowIndex = formRow(form, rowIndex, "Destination", tileRow(destinationX, destinationY, destinationPlane));
        rowIndex = formRow(form, rowIndex, "Duration", duration);
        dialogue.setLineWrap(false);
        JScrollPane dialogueScroll = new JScrollPane(dialogue);
        dialogueScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        rowIndex = formRow(form, rowIndex, "Dialogue", dialogueScroll);
        formRow(form, rowIndex, "", errors);
        errors.setForeground(ERROR);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.setOpaque(false);
        applyButton.addActionListener(e -> applyDraft());
        revertButton.addActionListener(e -> revert.run());
        walkSource.addActionListener(e -> walk(new WorldPoint(value(sourceX), value(sourceY), value(sourcePlane))));
        walkDestination.addActionListener(e -> walk(new WorldPoint(value(destinationX), value(destinationY), value(destinationPlane))));
        buttons.add(applyButton);
        buttons.add(revertButton);
        buttons.add(walkSource);
        buttons.add(walkDestination);

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(form, BorderLayout.NORTH);
        top.add(buttons, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        JScrollPane requirementScroll = new JScrollPane(requirements);
        requirementScroll.setBorder(BorderFactory.createEmptyBorder());
        requirementScroll.getVerticalScrollBar().setUnitIncrement(12);
        add(requirementScroll, BorderLayout.CENTER);
        show(null);
    }

    void show(TransportRow row)
    {
        this.row = row;
        loading = true;
        try
        {
            TransportDraft draft = row == null || row.current() == null ? new TransportDraft() : new TransportDraft(row.current());
            name.setText(draft.name);
            object.setSelected(draft.object);
            npc.setSelected(!draft.object);
            id.setValue(Math.max(0, draft.id));
            option.setValue(Math.max(1, Math.min(5, draft.option)));
            sourceX.setValue(Math.max(0, draft.sourceX));
            sourceY.setValue(Math.max(0, draft.sourceY));
            sourcePlane.setValue(Math.max(0, Math.min(3, draft.sourcePlane)));
            destinationX.setValue(Math.max(0, draft.destinationX));
            destinationY.setValue(Math.max(0, draft.destinationY));
            destinationPlane.setValue(Math.max(0, Math.min(3, draft.destinationPlane)));
            duration.setValue(Math.max(1, draft.duration));
            dialogue.setText(String.join("\n", draft.dialogueOptions));
            requirements.requirements(draft.requirements);
            errors.setText(" ");
            boolean enabled = row != null && !row.isDeleted();
            for (JComponent component : new JComponent[]{name, object, npc, id, option, sourceX, sourceY, sourcePlane, destinationX, destinationY, destinationPlane, duration, dialogue, applyButton, revertButton, walkSource, walkDestination})
            {
                component.setEnabled(enabled);
            }
            refreshTargetName();
        }
        finally
        {
            loading = false;
        }
    }

    TransportRow row()
    {
        return row;
    }

    private TransportDraft draft()
    {
        TransportDraft draft = new TransportDraft();
        draft.name = name.getText();
        draft.object = object.isSelected();
        draft.id = value(id);
        draft.option = value(option);
        draft.sourceX = value(sourceX);
        draft.sourceY = value(sourceY);
        draft.sourcePlane = value(sourcePlane);
        draft.destinationX = value(destinationX);
        draft.destinationY = value(destinationY);
        draft.destinationPlane = value(destinationPlane);
        draft.duration = value(duration);
        draft.dialogueOptions = Arrays.asList(dialogue.getText().split("\\r?\\n"));
        draft.requirements = requirements.requirements();
        return draft;
    }

    private void applyDraft()
    {
        if (row == null)
        {
            return;
        }

        TransportDraft draft = draft();
        List<String> problems = draft.validate();
        if (!problems.isEmpty())
        {
            errors.setText("<html>" + String.join("<br>", problems) + "</html>");
            return;
        }
        errors.setText(" ");
        apply.accept(draft.build());
    }

    private void touched()
    {
        if (!loading)
        {
            errors.setText("Requirements changed; press Apply to keep them.");
        }
    }

    private void refreshTargetName()
    {
        if (loading && row == null)
        {
            targetName.setText(" ");
            return;
        }

        String found = descriptions.targetName(object.isSelected(), value(id));
        targetName.setText(found.isEmpty() ? " " : found);
    }

    private void walk(WorldPoint target)
    {
        Client client = Static.getClient();
        if (client == null || client.getGameState() != GameState.LOGGED_IN)
        {
            errors.setText("Not logged in; nothing to walk with.");
            return;
        }

        ThreadPool.submit(() -> Walker.walkTo(target));
        errors.setText("Walking to " + target.getX() + "," + target.getY() + "," + target.getPlane());
    }

    private static int value(JSpinner spinner)
    {
        return (Integer) spinner.getValue();
    }

    private static JSpinner coordinate()
    {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 16383, 1));
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(5);
        return spinner;
    }

    private static JSpinner plane()
    {
        return new JSpinner(new SpinnerNumberModel(0, 0, 3, 1));
    }

    private static JPanel tileRow(JSpinner x, JSpinner y, JSpinner plane)
    {
        JPanel tile = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        tile.setOpaque(false);
        tile.add(x);
        tile.add(y);
        tile.add(plane);
        return tile;
    }

    private static JPanel withLabel(JComponent field, JLabel label)
    {
        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        line.setOpaque(false);
        line.add(field);
        line.add(label);
        return line;
    }

    private static int formRow(JPanel form, int rowIndex, String label, JComponent field)
    {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = rowIndex;
        constraints.insets = new Insets(2, 2, 2, 2);
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.gridx = 0;
        form.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        form.add(field, constraints);
        return rowIndex + 1;
    }
}
