package com.tonic.services.pathfinder.ui.components;

import com.tonic.services.pathfinder.requirements.*;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Panel for editing all types of requirements
 */
public class RequirementsEditorPanel extends JPanel {

    private static final Color BACKGROUND_COLOR = new Color(60, 63, 65);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(85, 85, 85);
    private static final Color FIELD_COLOR = new Color(69, 73, 74);

    private final TransportDetailPanel parent;
    private Requirements currentRequirements;

    // Requirement list panels
    private JPanel itemRequirementsPanel;
    private JPanel skillRequirementsPanel;
    private JPanel varRequirementsPanel;
    private JPanel questRequirementsPanel;
    private JPanel worldRequirementsPanel;

    // Requirement lists
    private List<ItemRequirement> itemRequirements = new ArrayList<>();
    private List<SkillRequirement> skillRequirements = new ArrayList<>();
    private List<VarRequirement> varRequirements = new ArrayList<>();
    private List<QuestRequirement> questRequirements = new ArrayList<>();
    private List<WorldRequirement> worldRequirements = new ArrayList<>();

    public RequirementsEditorPanel(TransportDetailPanel parent) {
        this.parent = parent;
        this.currentRequirements = new Requirements();
        initializeComponents();
        setupLayout();
    }

    private void initializeComponents() {
        setBackground(BACKGROUND_COLOR);
        setBorder(new TitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            "Requirements",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("SansSerif", Font.PLAIN, 11),
            TEXT_COLOR
        ));

        // Initialize requirement panels
        itemRequirementsPanel = createRequirementListPanel("Item Requirements");
        skillRequirementsPanel = createRequirementListPanel("Skill Requirements");
        varRequirementsPanel = createRequirementListPanel("Variable Requirements");
        questRequirementsPanel = createRequirementListPanel("Quest Requirements");
        worldRequirementsPanel = createRequirementListPanel("World Requirements");
    }

    private void setupLayout() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Add all requirement panels
        add(itemRequirementsPanel);
        add(Box.createVerticalStrut(5));
        add(skillRequirementsPanel);
        add(Box.createVerticalStrut(5));
        add(varRequirementsPanel);
        add(Box.createVerticalStrut(5));
        add(questRequirementsPanel);
        add(Box.createVerticalStrut(5));
        add(worldRequirementsPanel);

        // Add some space at the bottom
        add(Box.createVerticalGlue());
    }

    private JPanel createRequirementListPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(new TitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            title,
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("SansSerif", Font.PLAIN, 10),
            TEXT_COLOR
        ));

        // Add button to add new requirement
        JButton addButton = createAddButton(title);
        panel.add(addButton);

        return panel;
    }

    private JButton createAddButton(String requirementType) {
        JButton button = new JButton("Add " + requirementType.replace(" Requirements", ""));
        button.setBackground(new Color(75, 110, 175));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(3, 8, 3, 8));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);

        button.addActionListener(e -> showAddRequirementDialog(requirementType));

        return button;
    }

    // Public API

    public void setRequirements(Requirements requirements) {
        this.currentRequirements = requirements;
        updateRequirementDisplays();
    }

    public Requirements getRequirements() {
        return new Requirements(
            new ArrayList<>(itemRequirements),
            new ArrayList<>(skillRequirements),
            new ArrayList<>(varRequirements),
            new ArrayList<>(questRequirements),
            new ArrayList<>(worldRequirements),
            new ArrayList<>() // No other requirements in UI
        );
    }

    public void clearRequirements() {
        itemRequirements.clear();
        skillRequirements.clear();
        varRequirements.clear();
        questRequirements.clear();
        worldRequirements.clear();
        updateRequirementDisplays();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        // Enable/disable all child components
        setComponentsEnabled(this, enabled);
    }

    // Private methods

    private void updateRequirementDisplays() {
        if (currentRequirements != null) {
            itemRequirements = new ArrayList<>(currentRequirements.getItemRequirements());
            skillRequirements = new ArrayList<>(currentRequirements.getSkillRequirements());
            varRequirements = new ArrayList<>(currentRequirements.getVarRequirements());
            questRequirements = new ArrayList<>(currentRequirements.getQuestRequirements());
            worldRequirements = new ArrayList<>(currentRequirements.getWorldRequirements());
        }

        refreshRequirementPanel(itemRequirementsPanel, itemRequirements, "Item");
        refreshRequirementPanel(skillRequirementsPanel, skillRequirements, "Skill");
        refreshRequirementPanel(varRequirementsPanel, varRequirements, "Variable");
        refreshRequirementPanel(questRequirementsPanel, questRequirements, "Quest");
        refreshRequirementPanel(worldRequirementsPanel, worldRequirements, "World");
    }

    private void refreshRequirementPanel(JPanel panel, List<? extends Requirement> requirements, String type) {
        // Remove all except the add button (first component)
        Component addButton = panel.getComponent(0);
        panel.removeAll();
        panel.add(addButton);

        // Add requirement display panels
        for (int i = 0; i < requirements.size(); i++) {
            Requirement req = requirements.get(i);
            JPanel reqPanel = createRequirementDisplayPanel(req, i, type);
            panel.add(reqPanel);
        }

        panel.revalidate();
        panel.repaint();
    }

    private JPanel createRequirementDisplayPanel(Requirement requirement, int index, String type) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(55, 57, 59));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            new EmptyBorder(5, 8, 5, 8)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        // Create description label
        JLabel descLabel = new JLabel(getRequirementDescription(requirement));
        descLabel.setForeground(TEXT_COLOR);
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));

        // Create delete button
        JButton deleteButton = new JButton("×");
        deleteButton.setBackground(new Color(150, 50, 50));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFocusPainted(false);
        deleteButton.setBorder(new EmptyBorder(2, 6, 2, 6));
        deleteButton.setPreferredSize(new Dimension(20, 20));

        deleteButton.addActionListener(e -> removeRequirement(requirement, type));

        panel.add(descLabel, BorderLayout.CENTER);
        panel.add(deleteButton, BorderLayout.EAST);

        return panel;
    }

    private String getRequirementDescription(Requirement requirement) {
        if (requirement instanceof ItemRequirement) {
            ItemRequirement item = (ItemRequirement) requirement;
            return String.format("%s %s: %d of %s",
                item.getReduction(),
                item.isEquipped() ? "Equipped" : "Inventory",
                item.getAmount(),
                item.getIds().toString());
        } else if (requirement instanceof SkillRequirement) {
            SkillRequirement skill = (SkillRequirement) requirement;
            return String.format("%s level %d", skill.getSkill(), skill.getLevel());
        } else if (requirement instanceof VarRequirement) {
            VarRequirement var = (VarRequirement) requirement;
            return String.format("%s %d %s %d", var.getType(), var.getVar(), var.getComparison(), var.getValue());
        } else if (requirement instanceof QuestRequirement) {
            QuestRequirement quest = (QuestRequirement) requirement;
            return String.format("Quest %s: %s", quest.getQuest(), quest.getStates());
        } else if (requirement instanceof WorldRequirement) {
            WorldRequirement world = (WorldRequirement) requirement;
            return world.isMemberWorld() ? "Members World Required" : "F2P World Required";
        }

        return requirement.toString();
    }

    private void showAddRequirementDialog(String requirementType) {
        switch (requirementType) {
            case "Item Requirements":
                showAddItemRequirementDialog();
                break;
            case "Skill Requirements":
                showAddSkillRequirementDialog();
                break;
            case "Variable Requirements":
                showAddVarRequirementDialog();
                break;
            case "Quest Requirements":
                showAddQuestRequirementDialog();
                break;
            case "World Requirements":
                showAddWorldRequirementDialog();
                break;
        }
    }

    private void showAddItemRequirementDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Item Requirement", true);
        dialog.setLayout(new GridBagLayout());
        dialog.getContentPane().setBackground(BACKGROUND_COLOR);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Reduction type
        JLabel reductionLabel = new JLabel("Logic:");
        reductionLabel.setForeground(TEXT_COLOR);
        JComboBox<Reduction> reductionCombo = new JComboBox<>(Reduction.values());
        styleComboBox(reductionCombo);

        // Equipped checkbox
        JCheckBox equippedCheck = new JCheckBox("Equipped (vs Inventory)");
        equippedCheck.setBackground(BACKGROUND_COLOR);
        equippedCheck.setForeground(TEXT_COLOR);

        // Amount
        JLabel amountLabel = new JLabel("Amount:");
        amountLabel.setForeground(TEXT_COLOR);
        JSpinner amountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        styleSpinner(amountSpinner);

        // Item IDs
        JLabel idsLabel = new JLabel("Item IDs (comma-separated):");
        idsLabel.setForeground(TEXT_COLOR);
        JTextField idsField = createStyledTextField();

        // Buttons
        JButton okButton = new JButton("Add");
        JButton cancelButton = new JButton("Cancel");
        styleDialogButton(okButton);
        styleDialogButton(cancelButton);

        // Layout
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        dialog.add(reductionLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        dialog.add(reductionCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        dialog.add(equippedCheck, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
        dialog.add(amountLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        dialog.add(amountSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.WEST;
        dialog.add(idsLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        dialog.add(idsField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        dialog.add(okButton, gbc);
        gbc.gridx = 1;
        dialog.add(cancelButton, gbc);

        // Event handlers
        okButton.addActionListener(e -> {
            try {
                String[] idStrings = idsField.getText().split(",");
                int[] ids = new int[idStrings.length];
                for (int i = 0; i < idStrings.length; i++) {
                    ids[i] = Integer.parseInt(idStrings[i].trim());
                }

                ItemRequirement requirement = new ItemRequirement(
                    equippedCheck.isSelected(),
                    (Integer) amountSpinner.getValue(),
                    ids
                );

                // Set reduction type using reflection
                try {
                    java.lang.reflect.Field reductionField = ItemRequirement.class.getDeclaredField("reduction");
                    reductionField.setAccessible(true);
                    reductionField.set(requirement, reductionCombo.getSelectedItem());
                } catch (Exception ex) {
                    // Fallback - create new requirement manually
                }

                itemRequirements.add(requirement);
                updateRequirementDisplays();
                notifyParentOfChange();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid item IDs", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showAddSkillRequirementDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Skill Requirement", true);
        dialog.setLayout(new GridBagLayout());
        dialog.getContentPane().setBackground(BACKGROUND_COLOR);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Skill
        JLabel skillLabel = new JLabel("Skill:");
        skillLabel.setForeground(TEXT_COLOR);
        JComboBox<Skill> skillCombo = new JComboBox<>(Skill.values());
        styleComboBox(skillCombo);

        // Level
        JLabel levelLabel = new JLabel("Level:");
        levelLabel.setForeground(TEXT_COLOR);
        JSpinner levelSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        styleSpinner(levelSpinner);

        // Buttons
        JButton okButton = new JButton("Add");
        JButton cancelButton = new JButton("Cancel");
        styleDialogButton(okButton);
        styleDialogButton(cancelButton);

        // Layout
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        dialog.add(skillLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        dialog.add(skillCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        dialog.add(levelLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        dialog.add(levelSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        dialog.add(okButton, gbc);
        gbc.gridx = 1;
        dialog.add(cancelButton, gbc);

        // Event handlers
        okButton.addActionListener(e -> {
            SkillRequirement requirement = new SkillRequirement(
                (Skill) skillCombo.getSelectedItem(),
                (Integer) levelSpinner.getValue()
            );

            skillRequirements.add(requirement);
            updateRequirementDisplays();
            notifyParentOfChange();
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showAddVarRequirementDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Variable Requirement", true);
        dialog.setLayout(new GridBagLayout());
        dialog.getContentPane().setBackground(BACKGROUND_COLOR);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Type
        JLabel typeLabel = new JLabel("Type:");
        typeLabel.setForeground(TEXT_COLOR);
        JComboBox<VarType> typeCombo = new JComboBox<>(VarType.values());
        styleComboBox(typeCombo);

        // Variable index
        JLabel varLabel = new JLabel("Variable:");
        varLabel.setForeground(TEXT_COLOR);
        JSpinner varSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        styleSpinner(varSpinner);

        // Comparison
        JLabel compLabel = new JLabel("Comparison:");
        compLabel.setForeground(TEXT_COLOR);
        JComboBox<Comparison> compCombo = new JComboBox<>(Comparison.values());
        styleComboBox(compCombo);

        // Value
        JLabel valueLabel = new JLabel("Value:");
        valueLabel.setForeground(TEXT_COLOR);
        JSpinner valueSpinner = new JSpinner(new SpinnerNumberModel(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
        styleSpinner(valueSpinner);

        // Buttons
        JButton okButton = new JButton("Add");
        JButton cancelButton = new JButton("Cancel");
        styleDialogButton(okButton);
        styleDialogButton(cancelButton);

        // Layout
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        dialog.add(typeLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        dialog.add(typeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        dialog.add(varLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        dialog.add(varSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
        dialog.add(compLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        dialog.add(compCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.WEST;
        dialog.add(valueLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        dialog.add(valueSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE;
        dialog.add(okButton, gbc);
        gbc.gridx = 1;
        dialog.add(cancelButton, gbc);

        // Event handlers
        okButton.addActionListener(e -> {
            VarRequirement requirement = new VarRequirement(
                (Comparison) compCombo.getSelectedItem(),
                (VarType) typeCombo.getSelectedItem(),
                (Integer) varSpinner.getValue(),
                (Integer) valueSpinner.getValue()
            );

            varRequirements.add(requirement);
            updateRequirementDisplays();
            notifyParentOfChange();
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showAddQuestRequirementDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add Quest Requirement", true);
        dialog.setLayout(new GridBagLayout());
        dialog.getContentPane().setBackground(BACKGROUND_COLOR);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Quest dropdown
        JLabel questLabel = new JLabel("Quest:");
        questLabel.setForeground(TEXT_COLOR);
        JComboBox<Quest> questCombo = new JComboBox<>(Quest.values());
        questCombo.setBackground(FIELD_COLOR);
        questCombo.setForeground(TEXT_COLOR);
        questCombo.setMaximumRowCount(15); // Limit dropdown height for better UX

        // Quest state checkboxes (multiple states can be selected)
        JLabel stateLabel = new JLabel("Required States:");
        stateLabel.setForeground(TEXT_COLOR);

        JPanel statePanel = new JPanel();
        statePanel.setLayout(new BoxLayout(statePanel, BoxLayout.Y_AXIS));
        statePanel.setBackground(BACKGROUND_COLOR);

        JCheckBox notStartedCheck = new JCheckBox("NOT_STARTED");
        JCheckBox inProgressCheck = new JCheckBox("IN_PROGRESS");
        JCheckBox finishedCheck = new JCheckBox("FINISHED");

        // Style checkboxes
        JCheckBox[] checkboxes = {notStartedCheck, inProgressCheck, finishedCheck};
        for (JCheckBox checkbox : checkboxes) {
            checkbox.setBackground(BACKGROUND_COLOR);
            checkbox.setForeground(TEXT_COLOR);
            checkbox.setFocusPainted(false);
            statePanel.add(checkbox);
        }

        // Buttons
        JButton okButton = new JButton("Add");
        JButton cancelButton = new JButton("Cancel");
        styleDialogButton(okButton);
        styleDialogButton(cancelButton);

        // Layout
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        dialog.add(questLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        dialog.add(questCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.NORTHWEST; gbc.weightx = 0.0;
        dialog.add(stateLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        dialog.add(statePanel, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.gridwidth = 1;
        dialog.add(okButton, gbc);
        gbc.gridx = 1;
        dialog.add(cancelButton, gbc);

        // Event handlers
        okButton.addActionListener(e -> {
            Quest selectedQuest = (Quest) questCombo.getSelectedItem();
            if (selectedQuest == null) {
                JOptionPane.showMessageDialog(dialog, "Please select a quest.", "No Quest Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Collect selected states
            Set<QuestState> selectedStates = new HashSet<>();
            if (notStartedCheck.isSelected()) selectedStates.add(QuestState.NOT_STARTED);
            if (inProgressCheck.isSelected()) selectedStates.add(QuestState.IN_PROGRESS);
            if (finishedCheck.isSelected()) selectedStates.add(QuestState.FINISHED);

            if (selectedStates.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please select at least one quest state.", "No State Selected", JOptionPane.WARNING_MESSAGE);
                return;
            }

            QuestRequirement requirement = new QuestRequirement(selectedQuest, selectedStates);

            questRequirements.add(requirement);
            updateRequirementDisplays();
            notifyParentOfChange();
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showAddWorldRequirementDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add World Requirement", true);
        dialog.setLayout(new GridBagLayout());
        dialog.getContentPane().setBackground(BACKGROUND_COLOR);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Member world checkbox
        JCheckBox memberCheck = new JCheckBox("Requires Members World");
        memberCheck.setBackground(BACKGROUND_COLOR);
        memberCheck.setForeground(TEXT_COLOR);

        // Buttons
        JButton okButton = new JButton("Add");
        JButton cancelButton = new JButton("Cancel");
        styleDialogButton(okButton);
        styleDialogButton(cancelButton);

        // Layout
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        dialog.add(memberCheck, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        dialog.add(okButton, gbc);
        gbc.gridx = 1;
        dialog.add(cancelButton, gbc);

        // Event handlers
        okButton.addActionListener(e -> {
            WorldRequirement requirement = new WorldRequirement(memberCheck.isSelected());

            worldRequirements.add(requirement);
            updateRequirementDisplays();
            notifyParentOfChange();
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void removeRequirement(Requirement requirement, String type) {
        switch (type) {
            case "Item":
                itemRequirements.remove(requirement);
                break;
            case "Skill":
                skillRequirements.remove(requirement);
                break;
            case "Variable":
                varRequirements.remove(requirement);
                break;
            case "Quest":
                questRequirements.remove(requirement);
                break;
            case "World":
                worldRequirements.remove(requirement);
                break;
        }

        updateRequirementDisplays();
        notifyParentOfChange();
    }

    private void notifyParentOfChange() {
        parent.onRequirementsChanged();
    }

    // Styling helper methods

    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(FIELD_COLOR);
        comboBox.setForeground(TEXT_COLOR);
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBackground(FIELD_COLOR);
        spinner.setForeground(TEXT_COLOR);

        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setBackground(FIELD_COLOR);
            textField.setForeground(TEXT_COLOR);
            textField.setCaretColor(TEXT_COLOR);
            textField.setBorder(new EmptyBorder(3, 5, 3, 5));
        }
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setBackground(FIELD_COLOR);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(TEXT_COLOR);
        field.setBorder(new EmptyBorder(5, 8, 5, 8));
        return field;
    }

    private void styleDialogButton(JButton button) {
        button.setBackground(new Color(75, 110, 175));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(5, 15, 5, 15));
    }

    private void setComponentsEnabled(Container container, boolean enabled) {
        for (Component component : container.getComponents()) {
            component.setEnabled(enabled);
            if (component instanceof Container) {
                setComponentsEnabled((Container) component, enabled);
            }
        }
    }
}