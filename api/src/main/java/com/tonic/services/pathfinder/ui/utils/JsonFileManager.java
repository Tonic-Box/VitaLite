package com.tonic.services.pathfinder.ui.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tonic.services.pathfinder.model.TransportDto;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles JSON file loading and saving operations for transport data
 */
public class JsonFileManager {

    private static final String DEFAULT_TRANSPORTS_PATH = "api/src/main/resources/com/tonic/services/pathfinder/transports.json";
    private final Gson gson;
    private File lastLoadedFile;

    public JsonFileManager() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    }

    /**
     * Load transports from the default location or show file chooser
     */
    public TransportDto[] loadTransports() throws IOException {
        // Try to load from default location first
        Path defaultPath = Paths.get(DEFAULT_TRANSPORTS_PATH);
        if (Files.exists(defaultPath)) {
            lastLoadedFile = defaultPath.toFile();
            return loadTransportsFromFile(lastLoadedFile);
        }

        // If default doesn't exist, show file chooser
        return loadTransportsWithFileChooser();
    }

    /**
     * Load transports using file chooser dialog
     */
    public TransportDto[] loadTransportsWithFileChooser() throws IOException {
        JFileChooser fileChooser = createFileChooser();
        fileChooser.setDialogTitle("Load Transports JSON");

        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            lastLoadedFile = fileChooser.getSelectedFile();
            return loadTransportsFromFile(lastLoadedFile);
        }

        return null; // User cancelled
    }

    /**
     * Save transports to the last loaded file
     */
    public void saveTransports(TransportDto[] transports) throws IOException {
        if (lastLoadedFile == null) {
            saveTransportsAs(transports);
            return;
        }

        saveTransportsToFile(transports, lastLoadedFile);
    }

    /**
     * Save transports to a new file using file chooser
     */
    public void saveTransportsAs(TransportDto[] transports) throws IOException {
        JFileChooser fileChooser = createFileChooser();
        fileChooser.setDialogTitle("Save Transports As");
        fileChooser.setSelectedFile(new File("transports.json"));

        int result = fileChooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            // Ensure .json extension
            if (!selectedFile.getName().toLowerCase().endsWith(".json")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".json");
            }

            // Check if file exists and confirm overwrite
            if (selectedFile.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(
                    null,
                    "File already exists. Do you want to overwrite it?",
                    "File Exists",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );

                if (overwrite != JOptionPane.YES_OPTION) {
                    return; // User chose not to overwrite
                }
            }

            saveTransportsToFile(transports, selectedFile);
            lastLoadedFile = selectedFile; // Update last loaded file
        }
    }

    /**
     * Get information about the currently loaded file
     */
    public String getCurrentFileInfo() {
        if (lastLoadedFile == null) {
            return "No file loaded";
        }

        return String.format("%s (%,.0f KB)",
            lastLoadedFile.getName(),
            lastLoadedFile.length() / 1024.0);
    }

    /**
     * Get the path of the currently loaded file
     */
    public String getCurrentFilePath() {
        return lastLoadedFile != null ? lastLoadedFile.getAbsolutePath() : null;
    }

    // Private helper methods

    private TransportDto[] loadTransportsFromFile(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("Transport file not found: " + file.getAbsolutePath());
        }

        if (!file.canRead()) {
            throw new IOException("Cannot read transport file: " + file.getAbsolutePath());
        }

        try (FileReader reader = new FileReader(file)) {
            TransportDto[] transports = gson.fromJson(reader, TransportDto[].class);

            if (transports == null) {
                throw new IOException("Invalid JSON format in transport file");
            }

            return transports;

        } catch (Exception e) {
            throw new IOException("Failed to parse transport file: " + e.getMessage(), e);
        }
    }

    private void saveTransportsToFile(TransportDto[] transports, File file) throws IOException {
        if (transports == null) {
            throw new IllegalArgumentException("Transports array cannot be null");
        }

        // Create parent directories if they don't exist
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create parent directories: " + parentDir.getAbsolutePath());
            }
        }

        // Create backup of existing file
        createBackupIfExists(file);

        try (FileWriter writer = new FileWriter(file, false)) {
            gson.toJson(transports, writer);
            writer.flush();

        } catch (Exception e) {
            throw new IOException("Failed to save transport file: " + e.getMessage(), e);
        }
    }

    private void createBackupIfExists(File file) {
        if (file.exists()) {
            try {
                String backupName = file.getName().replaceFirst("(\\.[^.]*)?$", ".backup$1");
                File backupFile = new File(file.getParent(), backupName);

                Files.copy(file.toPath(), backupFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            } catch (IOException e) {
                // Backup failed, but continue with save operation
                System.err.println("Warning: Failed to create backup: " + e.getMessage());
            }
        }
    }

    private JFileChooser createFileChooser() {
        JFileChooser fileChooser = new JFileChooser();

        // Set default directory to project resources if it exists
        Path resourcesDir = Paths.get("api/src/main/resources/com/tonic/services/pathfinder");
        if (Files.exists(resourcesDir)) {
            fileChooser.setCurrentDirectory(resourcesDir.toFile());
        } else {
            // Fall back to current working directory
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        }

        // Set file filter for JSON files
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "JSON Files (*.json)", "json");
        fileChooser.setFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(true);

        return fileChooser;
    }

    /**
     * Validate transport data before saving
     */
    public ValidationResult validateTransports(TransportDto[] transports) {
        ValidationResult result = new ValidationResult();

        if (transports == null) {
            result.addError("Transport array is null");
            return result;
        }

        for (int i = 0; i < transports.length; i++) {
            TransportDto transport = transports[i];
            String prefix = "Transport " + (i + 1) + ": ";

            if (transport == null) {
                result.addError(prefix + "Transport is null");
                continue;
            }

            // Validate coordinates
            if (transport.getSource() == null) {
                result.addError(prefix + "Source coordinates are null");
            }

            if (transport.getDestination() == null) {
                result.addError(prefix + "Destination coordinates are null");
            }

            // Validate action
            if (transport.getAction() == null || transport.getAction().trim().isEmpty()) {
                result.addError(prefix + "Action is empty");
            }

            // Validate object ID
            if (transport.getObjectId() == null || transport.getObjectId() < 0) {
                result.addWarning(prefix + "Object ID is invalid");
            }

            // Validate requirements
            if (transport.getRequirements() == null) {
                result.addWarning(prefix + "Requirements are null");
            }
        }

        return result;
    }

    /**
     * Result of validation operation
     */
    public static class ValidationResult {
        private final java.util.List<String> errors = new java.util.ArrayList<>();
        private final java.util.List<String> warnings = new java.util.ArrayList<>();

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

        public java.util.List<String> getErrors() {
            return new java.util.ArrayList<>(errors);
        }

        public java.util.List<String> getWarnings() {
            return new java.util.ArrayList<>(warnings);
        }

        public String getErrorSummary() {
            if (errors.isEmpty() && warnings.isEmpty()) {
                return "Validation passed with no issues.";
            }

            StringBuilder summary = new StringBuilder();
            if (!errors.isEmpty()) {
                summary.append("Errors (").append(errors.size()).append("):\n");
                for (String error : errors) {
                    summary.append("• ").append(error).append("\n");
                }
            }

            if (!warnings.isEmpty()) {
                if (summary.length() > 0) summary.append("\n");
                summary.append("Warnings (").append(warnings.size()).append("):\n");
                for (String warning : warnings) {
                    summary.append("• ").append(warning).append("\n");
                }
            }

            return summary.toString();
        }
    }
}