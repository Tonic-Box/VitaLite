package com.tonic.services.pathfinder.ui.utils;

import com.tonic.services.pathfinder.model.TransportDto;
import com.tonic.services.pathfinder.requirements.*;
import net.runelite.api.coords.WorldPoint;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for validating transport data and showing validation dialogs
 */
public class ValidationUtils {

    /**
     * Validate a single transport and return any validation issues
     */
    public static ValidationResult validateTransport(TransportDto transport) {
        ValidationResult result = new ValidationResult();

        if (transport == null) {
            result.addError("Transport is null");
            return result;
        }

        // Validate source coordinates
        validateWorldPoint(transport.getSource(), "source", result);

        // Validate destination coordinates
        validateWorldPoint(transport.getDestination(), "destination", result);

        // Validate action
        validateAction(transport.getAction(), result);

        // Validate object ID
        validateObjectId(transport.getObjectId(), result);

        // Validate requirements
        validateRequirements(transport.getRequirements(), result);

        return result;
    }

    /**
     * Validate coordinate input fields in real-time
     */
    public static boolean isValidCoordinate(String value) {
        try {
            int coord = Integer.parseInt(value.trim());
            return coord >= -32768 && coord <= 32767;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate plane input fields in real-time
     */
    public static boolean isValidPlane(String value) {
        try {
            int plane = Integer.parseInt(value.trim());
            return plane >= 0 && plane <= 3;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate object ID input fields
     */
    public static boolean isValidObjectId(String value) {
        try {
            int id = Integer.parseInt(value.trim());
            return id >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Show validation errors in a dialog
     */
    public static void showValidationDialog(Component parent, ValidationResult result, String title) {
        if (result.hasErrors() || result.hasWarnings()) {
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), title, true);
            dialog.setLayout(new BorderLayout());

            // Create content panel
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Create text area for messages
            JTextArea textArea = new JTextArea(result.getErrorSummary());
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            textArea.setBackground(new Color(69, 73, 74));
            textArea.setForeground(Color.WHITE);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(500, 300));
            contentPanel.add(scrollPane, BorderLayout.CENTER);

            // Create button panel
            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton okButton = new JButton("OK");
            okButton.addActionListener(e -> dialog.dispose());
            buttonPanel.add(okButton);

            contentPanel.add(buttonPanel, BorderLayout.SOUTH);

            dialog.add(contentPanel);
            dialog.pack();
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
        }
    }

    /**
     * Create a visual indicator for validation status
     */
    public static JLabel createValidationIndicator() {
        JLabel indicator = new JLabel();
        indicator.setPreferredSize(new Dimension(16, 16));
        indicator.setHorizontalAlignment(SwingConstants.CENTER);
        return indicator;
    }

    /**
     * Update validation indicator appearance
     */
    public static void updateValidationIndicator(JLabel indicator, ValidationResult result) {
        if (result.hasErrors()) {
            indicator.setText("[X]]");
            indicator.setToolTipText("Validation errors: " + result.getErrors().size());
            indicator.setForeground(Color.RED);
        } else if (result.hasWarnings()) {
            indicator.setText("[!]");
            indicator.setToolTipText("Validation warnings: " + result.getWarnings().size());
            indicator.setForeground(Color.ORANGE);
        } else {
            indicator.setText("[+]");
            indicator.setToolTipText("Validation passed");
            indicator.setForeground(Color.GREEN);
        }
    }

    // Private validation methods

    private static void validateWorldPoint(WorldPoint point, String name, ValidationResult result) {
        if (point == null) {
            result.addError(name + " coordinates are null");
            return;
        }

        int x = point.getX();
        int y = point.getY();
        int plane = point.getPlane();

        if (x < -32768 || x > 32767) {
            result.addError(name + " X coordinate out of range: " + x);
        }

        if (y < -32768 || y > 32767) {
            result.addError(name + " Y coordinate out of range: " + y);
        }

        if (plane < 0 || plane > 3) {
            result.addError(name + " plane out of range: " + plane);
        }

        // Check for obviously invalid coordinates
        if (x == 0 && y == 0) {
            result.addWarning(name + " coordinates are at origin (0,0)");
        }
    }

    private static void validateAction(String action, ValidationResult result) {
        if (action == null || action.trim().isEmpty()) {
            result.addError("Action is empty");
            return;
        }

        String trimmed = action.trim();
        if (!trimmed.equals(action)) {
            result.addWarning("Action has leading/trailing whitespace");
        }

        // Check for common action patterns
        String[] commonActions = {
            "Click", "Open", "Close", "Enter", "Exit", "Climb", "Climb-up", "Climb-down",
            "Use", "Push", "Pull", "Squeeze-through", "Go-through", "Cross", "Jump-over",
            "Climb-over", "Chop-down", "Leave"
        };

        boolean isCommonAction = false;
        for (String common : commonActions) {
            if (trimmed.equalsIgnoreCase(common)) {
                isCommonAction = true;
                break;
            }
        }

        if (!isCommonAction) {
            result.addWarning("Unusual action: '" + trimmed + "' (not in common action list)");
        }
    }

    private static void validateObjectId(Integer objectId, ValidationResult result) {
        if (objectId == null) {
            result.addError("Object ID is null");
            return;
        }

        if (objectId < 0) {
            result.addError("Object ID cannot be negative: " + objectId);
        }

        if (objectId == 0) {
            result.addWarning("Object ID is 0 (may not be a valid game object)");
        }

        if (objectId > 100000) {
            result.addWarning("Object ID is very high: " + objectId + " (verify this is correct)");
        }
    }

    private static void validateRequirements(Requirements requirements, ValidationResult result) {
        if (requirements == null) {
            result.addWarning("Requirements are null");
            return;
        }

        // Validate item requirements
        if (requirements.getItemRequirements() != null) {
            for (ItemRequirement item : requirements.getItemRequirements()) {
                validateItemRequirement(item, result);
            }
        }

        // Validate skill requirements
        if (requirements.getSkillRequirements() != null) {
            for (SkillRequirement skill : requirements.getSkillRequirements()) {
                validateSkillRequirement(skill, result);
            }
        }

        // Validate variable requirements
        if (requirements.getVarRequirements() != null) {
            for (VarRequirement var : requirements.getVarRequirements()) {
                validateVarRequirement(var, result);
            }
        }

        // Quest and world requirements are typically valid by construction
    }

    private static void validateItemRequirement(ItemRequirement item, ValidationResult result) {
        if (item == null) {
            result.addError("Item requirement is null");
            return;
        }

        if (item.getIds() == null || item.getIds().isEmpty()) {
            result.addError("Item requirement has no item IDs");
            return;
        }

        for (Integer id : item.getIds()) {
            if (id == null || id <= 0) {
                result.addError("Invalid item ID: " + id);
            }
        }

        if (item.getAmount() <= 0) {
            result.addError("Item requirement amount must be positive: " + item.getAmount());
        }

        if (item.getAmount() > 1000) {
            result.addWarning("Very high item amount: " + item.getAmount());
        }
    }

    private static void validateSkillRequirement(SkillRequirement skill, ValidationResult result) {
        if (skill == null) {
            result.addError("Skill requirement is null");
            return;
        }

        if (skill.getSkill() == null) {
            result.addError("Skill requirement has null skill");
            return;
        }

        int level = skill.getLevel();
        if (level < 1 || level > 99) {
            result.addError("Skill level out of range (1-99): " + level);
        }
    }

    private static void validateVarRequirement(VarRequirement var, ValidationResult result) {
        if (var == null) {
            result.addError("Variable requirement is null");
            return;
        }

        if (var.getType() == null) {
            result.addError("Variable requirement has null type");
        }

        if (var.getComparison() == null) {
            result.addError("Variable requirement has null comparison");
        }

        if (var.getVar() < 0) {
            result.addWarning("Negative variable index: " + var.getVar());
        }
    }

    /**
     * Result of validation operation
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }

        public String getErrorSummary() {
            if (errors.isEmpty() && warnings.isEmpty()) {
                return "Validation passed with no issues.";
            }

            StringBuilder summary = new StringBuilder();
            if (!errors.isEmpty()) {
                summary.append("ERRORS (").append(errors.size()).append("):\n");
                for (String error : errors) {
                    summary.append("[X] ").append(error).append("\n");
                }
            }

            if (!warnings.isEmpty()) {
                if (summary.length() > 0) summary.append("\n");
                summary.append("WARNINGS (").append(warnings.size()).append("):\n");
                for (String warning : warnings) {
                    summary.append("[!]  ").append(warning).append("\n");
                }
            }

            return summary.toString();
        }
    }
}