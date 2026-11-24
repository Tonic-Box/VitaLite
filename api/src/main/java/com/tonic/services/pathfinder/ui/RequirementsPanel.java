package com.tonic.services.pathfinder.ui;

import com.tonic.services.pathfinder.requirements.ItemRequirement;
import com.tonic.services.pathfinder.requirements.OtherRequirement;
import com.tonic.services.pathfinder.requirements.QuestRequirement;
import com.tonic.services.pathfinder.requirements.Requirement;
import com.tonic.services.pathfinder.requirements.Requirements;
import com.tonic.services.pathfinder.requirements.SkillRequirement;
import com.tonic.services.pathfinder.requirements.VarRequirement;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import net.runelite.client.ui.ColorScheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The requirements of the transport being edited: a titled section per kind with an Add button and a row per requirement with edit and remove buttons; code-only requirements are shown but cannot be edited.
 */
final class RequirementsPanel extends JPanel
{
    private final transient RequirementDescriptions descriptions;
    private final transient Runnable changed;
    private final JPanel sections = new JPanel();
    private transient Requirements requirements = new Requirements();

    RequirementsPanel(RequirementDescriptions descriptions, Runnable changed)
    {
        super(new BorderLayout());
        this.descriptions = descriptions;
        this.changed = changed;
        sections.setLayout(new BoxLayout(sections, BoxLayout.Y_AXIS));
        sections.setOpaque(false);
        setOpaque(false);
        add(sections, BorderLayout.NORTH);
        rebuild();
    }

    void requirements(Requirements requirements)
    {
        this.requirements = requirements == null ? new Requirements() : requirements;
        rebuild();
    }

    Requirements requirements()
    {
        return requirements;
    }

    private void rebuild()
    {
        sections.removeAll();
        sections.add(section("Items", requirements.getItemRequirements(), () -> RequirementDialogs.item(window(), null, descriptions), existing -> RequirementDialogs.item(window(), (ItemRequirement) existing, descriptions)));
        sections.add(section("Skills", requirements.getSkillRequirements(), () -> RequirementDialogs.skill(window(), null), existing -> RequirementDialogs.skill(window(), (SkillRequirement) existing)));
        sections.add(section("Variables", requirements.getVarRequirements(), () -> RequirementDialogs.var(window(), null), existing -> RequirementDialogs.var(window(), (VarRequirement) existing)));
        sections.add(section("Quests", requirements.getQuestRequirements(), () -> RequirementDialogs.quest(window(), null), existing -> RequirementDialogs.quest(window(), (QuestRequirement) existing)));
        sections.add(section("World", requirements.getWorldRequirements(), () -> RequirementDialogs.world(window(), null), existing -> RequirementDialogs.world(window(), (WorldRequirement) existing)));
        if (!requirements.getOtherRequirements().isEmpty())
        {
            sections.add(section("Code-only", requirements.getOtherRequirements(), null, null));
        }
        sections.revalidate();
        sections.repaint();
    }

    private <T extends Requirement> JPanel section(String title, List<T> list, Supplier<T> add, Function<Requirement, T> edit)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), title));
        if (add != null)
        {
            JButton addButton = new JButton("Add");
            addButton.addActionListener(e ->
            {
                T made = add.get();
                if (made != null)
                {
                    list.add(made);
                    rebuild();
                    changed.run();
                }
            });
            panel.add(left(addButton));
        }

        for (T requirement : new ArrayList<>(list))
        {
            JPanel row = new JPanel(new BorderLayout(6, 0));
            row.setOpaque(false);
            JLabel label = new JLabel(descriptions.describe(requirement));
            label.setToolTipText(label.getText());
            row.add(label, BorderLayout.CENTER);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            buttons.setOpaque(false);
            if (edit != null && !(requirement instanceof OtherRequirement))
            {
                JButton editButton = new JButton("Edit");
                editButton.addActionListener(e ->
                {
                    T made = edit.apply(requirement);
                    if (made != null)
                    {
                        int index = list.indexOf(requirement);
                        if (index >= 0)
                        {
                            list.set(index, made);
                        }
                        rebuild();
                        changed.run();
                    }
                });
                buttons.add(editButton);
            }
            JButton remove = new JButton("Remove");
            remove.addActionListener(e ->
            {
                list.remove(requirement);
                rebuild();
                changed.run();
            });
            buttons.add(remove);
            row.add(buttons, BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + 4));
            panel.add(row);
        }
        panel.add(Box.createVerticalStrut(2));
        return panel;
    }

    private static Component left(JButton button)
    {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        row.setOpaque(false);
        row.add(button);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private Window window()
    {
        return SwingUtilities.getWindowAncestor(this);
    }
}
