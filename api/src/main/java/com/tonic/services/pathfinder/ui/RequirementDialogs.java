package com.tonic.services.pathfinder.ui;

import com.tonic.Static;
import com.tonic.api.game.QuestAPI;
import com.tonic.services.pathfinder.requirements.Comparison;
import com.tonic.services.pathfinder.requirements.ItemForm;
import com.tonic.services.pathfinder.requirements.ItemRequirement;
import com.tonic.services.pathfinder.requirements.QuestRequirement;
import com.tonic.services.pathfinder.requirements.Reduction;
import com.tonic.services.pathfinder.requirements.SkillRequirement;
import com.tonic.services.pathfinder.requirements.VarRequirement;
import com.tonic.services.pathfinder.requirements.VarType;
import com.tonic.services.pathfinder.requirements.WorldRequirement;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The modal dialogs that add or edit one requirement of each kind, each returning the requirement made or null when cancelled; item names resolve as ids are typed and var and quest values are read live when logged in.
 */
final class RequirementDialogs
{
    private static final String[] WHERE = {"Inventory or worn", "Inventory", "Worn"};
    private static final String[] FORMS = {"Unnoted", "Noted", "Either"};

    private RequirementDialogs()
    {
    }

    static ItemRequirement item(Window owner, ItemRequirement existing, RequirementDescriptions names)
    {
        JComboBox<Reduction> logic = new JComboBox<>(Reduction.values());
        JComboBox<String> where = new JComboBox<>(WHERE);
        JComboBox<String> form = new JComboBox<>(FORMS);
        JSpinner amount = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        JTextField ids = new JTextField(24);
        JLabel preview = new JLabel(" ");
        if (existing != null)
        {
            logic.setSelectedItem(existing.getReduction() == null ? Reduction.OR : existing.getReduction());
            where.setSelectedIndex(existing.isEquipped() == null ? 0 : existing.isEquipped() ? 2 : 1);
            ItemForm itemForm = existing.getForm() == null ? ItemForm.UNNOTED : existing.getForm();
            form.setSelectedIndex(itemForm == ItemForm.NOTED ? 1 : itemForm == ItemForm.CANONICAL ? 2 : 0);
            amount.setValue(Math.max(1, existing.getAmount()));
            List<String> text = new ArrayList<>();
            for (int id : existing.getIds())
            {
                text.add(String.valueOf(id));
            }
            ids.setText(String.join(", ", text));
        }

        Runnable refresh = () ->
        {
            List<String> shown = new ArrayList<>();
            for (int id : parseIds(ids.getText()))
            {
                shown.add(names.item(id));
            }
            preview.setText(shown.isEmpty() ? " " : String.join(", ", shown));
        };
        ids.getDocument().addDocumentListener(onChange(refresh));
        refresh.run();

        Form layout = new Form();
        layout.row("Logic", logic);
        layout.row("Where", where);
        layout.row("Form", form);
        layout.row("Amount", amount);
        layout.row("Item ids", ids);
        layout.row("", preview);
        Supplier<ItemRequirement> build = () ->
        {
            int[] parsed = parseIds(ids.getText());
            if (parsed.length == 0)
            {
                JOptionPane.showMessageDialog(owner, "Enter at least one item id, comma separated.", "Item requirement", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            Boolean equipped = where.getSelectedIndex() == 0 ? null : where.getSelectedIndex() == 2;
            ItemForm itemForm = form.getSelectedIndex() == 1 ? ItemForm.NOTED : form.getSelectedIndex() == 2 ? ItemForm.CANONICAL : ItemForm.UNNOTED;
            return new ItemRequirement((Reduction) logic.getSelectedItem(), equipped, itemForm, (Integer) amount.getValue(), parsed);
        };
        return show(owner, existing == null ? "Add item requirement" : "Edit item requirement", layout.panel, build, ids);
    }

    static SkillRequirement skill(Window owner, SkillRequirement existing)
    {
        JComboBox<Skill> skill = new JComboBox<>(Skill.values());
        JSpinner level = new JSpinner(new SpinnerNumberModel(1, 1, 126, 1));
        JCheckBox boostable = new JCheckBox("Boosts count");
        if (existing != null)
        {
            skill.setSelectedItem(existing.getSkill());
            level.setValue(Math.max(1, Math.min(126, existing.getLevel())));
            boostable.setSelected(existing.isBoostable());
        }

        Form layout = new Form();
        layout.row("Skill", skill);
        layout.row("Level", level);
        layout.row("", boostable);
        return show(owner, existing == null ? "Add skill requirement" : "Edit skill requirement", layout.panel, () -> new SkillRequirement((Skill) skill.getSelectedItem(), (Integer) level.getValue(), boostable.isSelected()), skill);
    }

    static VarRequirement var(Window owner, VarRequirement existing)
    {
        JComboBox<VarType> type = new JComboBox<>(VarType.values());
        JSpinner id = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        JComboBox<Comparison> comparison = new JComboBox<>(Comparison.values());
        JSpinner value = new JSpinner(new SpinnerNumberModel(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
        JLabel current = new JLabel(" ");
        if (existing != null)
        {
            type.setSelectedItem(existing.getType());
            id.setValue(Math.max(0, existing.getVar()));
            comparison.setSelectedItem(existing.getComparison());
            value.setValue(existing.getValue());
        }

        Runnable refresh = () ->
        {
            if (!loggedIn())
            {
                current.setText("Current value: not logged in");
                return;
            }
            try
            {
                current.setText("Current value: " + ((VarType) type.getSelectedItem()).apply((Integer) id.getValue()));
            }
            catch (RuntimeException e)
            {
                current.setText("Current value: unreadable");
            }
        };
        type.addActionListener(e -> refresh.run());
        id.addChangeListener(e -> refresh.run());
        refresh.run();

        Form layout = new Form();
        layout.row("Type", type);
        layout.row("Id", id);
        layout.row("Comparison", comparison);
        layout.row("Value", value);
        layout.row("", current);
        return show(owner, existing == null ? "Add var requirement" : "Edit var requirement", layout.panel, () -> new VarRequirement((Comparison) comparison.getSelectedItem(), (VarType) type.getSelectedItem(), (Integer) id.getValue(), (Integer) value.getValue()), id);
    }

    static QuestRequirement quest(Window owner, QuestRequirement existing)
    {
        QuestPicker picker = new QuestPicker(existing == null ? null : existing.getQuest());
        JCheckBox notStarted = new JCheckBox("Not started");
        JCheckBox inProgress = new JCheckBox("In progress");
        JCheckBox finished = new JCheckBox("Finished");
        JLabel current = new JLabel(" ");
        if (existing != null && existing.getStates() != null)
        {
            notStarted.setSelected(existing.getStates().contains(QuestState.NOT_STARTED));
            inProgress.setSelected(existing.getStates().contains(QuestState.IN_PROGRESS));
            finished.setSelected(existing.getStates().contains(QuestState.FINISHED));
        }

        JPanel states = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        states.add(notStarted);
        states.add(inProgress);
        states.add(finished);
        JButton check = new JButton("Current state");
        check.addActionListener(e ->
        {
            Quest quest = picker.selected();
            current.setText(!loggedIn() ? "Not logged in" : quest == null ? "Pick a quest" : quest.getName() + ": " + QuestAPI.getState(quest));
        });

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(picker, BorderLayout.CENTER);
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.add(states);
        JPanel checkRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        checkRow.add(check);
        checkRow.add(current);
        south.add(checkRow);
        panel.add(south, BorderLayout.SOUTH);

        Supplier<QuestRequirement> build = () ->
        {
            Quest quest = picker.selected();
            Set<QuestState> chosen = EnumSet.noneOf(QuestState.class);
            if (notStarted.isSelected())
            {
                chosen.add(QuestState.NOT_STARTED);
            }
            if (inProgress.isSelected())
            {
                chosen.add(QuestState.IN_PROGRESS);
            }
            if (finished.isSelected())
            {
                chosen.add(QuestState.FINISHED);
            }
            if (quest == null || chosen.isEmpty())
            {
                JOptionPane.showMessageDialog(owner, "Pick a quest and at least one state.", "Quest requirement", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return new QuestRequirement(quest, chosen);
        };
        return show(owner, existing == null ? "Add quest requirement" : "Edit quest requirement", panel, build, picker.filterField());
    }

    static WorldRequirement world(Window owner, WorldRequirement existing)
    {
        JCheckBox members = new JCheckBox("Members' world required", existing != null && existing.isMemberWorld());
        Form layout = new Form();
        layout.row("", members);
        return show(owner, existing == null ? "Add world requirement" : "Edit world requirement", layout.panel, () -> new WorldRequirement(members.isSelected()), members);
    }

    static int[] parseIds(String text)
    {
        List<Integer> ids = new ArrayList<>();
        if (text != null)
        {
            for (String token : text.split("[,\\s]+"))
            {
                if (token.isEmpty())
                {
                    continue;
                }
                try
                {
                    ids.add(Integer.parseInt(token.trim()));
                }
                catch (NumberFormatException ignored)
                {
                }
            }
        }

        int[] out = new int[ids.size()];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = ids.get(i);
        }
        return out;
    }

    private static <T> T show(Window owner, String title, JComponent content, Supplier<T> build, JComponent focus)
    {
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        List<T> result = new ArrayList<>();
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e ->
        {
            T built = build.get();
            if (built != null)
            {
                result.add(built);
                dialog.dispose();
            }
        });
        cancel.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(cancel);
        buttons.add(ok);
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(content, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        if (focus != null)
        {
            focus.requestFocusInWindow();
        }
        dialog.setVisible(true);
        return result.isEmpty() ? null : result.get(0);
    }

    private static boolean loggedIn()
    {
        Client client = Static.getClient();
        return client != null && client.getGameState() == GameState.LOGGED_IN;
    }

    private static DocumentListener onChange(Runnable action)
    {
        return new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                action.run();
            }
        };
    }

    private static final class Form
    {
        private final JPanel panel = new JPanel(new GridBagLayout());
        private int row;

        void row(String label, JComponent field)
        {
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridy = row++;
            constraints.insets = new Insets(3, 3, 3, 3);
            constraints.anchor = GridBagConstraints.WEST;
            constraints.gridx = 0;
            panel.add(new JLabel(label), constraints);
            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.weightx = 1;
            panel.add(field, constraints);
        }
    }
}
