package com.tonic.services.pathfinder.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tonic.data.locatables.BankLocations;
import com.tonic.services.pathfinder.Walker;
import com.tonic.services.pathfinder.model.TransportDto;
import com.tonic.services.pathfinder.ui.components.TransportDetailPanel;
import com.tonic.services.pathfinder.ui.components.TransportListPanel;
import com.tonic.services.pathfinder.ui.components.ToolbarPanel;
import com.tonic.services.pathfinder.ui.utils.JsonFileManager;
import com.tonic.util.ThreadPool;
import net.runelite.api.coords.WorldPoint;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Main application frame for editing pathfinder transport configurations.
 * Provides a clean, modern interface for managing transport JSON data.
 */
public class TransportEditorFrame extends JFrame {
    public static TransportEditorFrame INSTANCE;
    private static final String TITLE = "VitaLite Transport Editor";
    private static final Dimension PREFERRED_SIZE = new Dimension(1400, 900);
    private static final Color BACKGROUND_COLOR = new Color(45, 47, 49);
    private static final Color PANEL_COLOR = new Color(60, 63, 65);

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final JsonFileManager fileManager = new JsonFileManager();

    // Core components
    private ToolbarPanel toolbarPanel;
    private TransportListPanel listPanel;
    private TransportDetailPanel detailPanel;

    // Data
    private List<TransportDto> transports = new ArrayList<>();
    private boolean hasUnsavedChanges = false;
    private JMenu testsMenu;
    private JMenuItem cancel;
    private final List<JMenuItem> tests = new ArrayList<>();
    private Color origonalColor;

    public TransportEditorFrame() {
        initializeFrame();
        initializeComponents();
        setupLayout();
        setupEventHandlers();

        // Auto-load transports on startup
        SwingUtilities.invokeLater(this::loadTransportsFromFile);
        INSTANCE = this;
    }

    private void initializeFrame() {
        setTitle(TITLE);
        setSize(PREFERRED_SIZE);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set dark theme
        getContentPane().setBackground(BACKGROUND_COLOR);

        // Set application icon if available
        try {
            setIconImage(Toolkit.getDefaultToolkit().getImage(
                getClass().getResource("/icons/transport-editor.png")));
        } catch (Exception e) {
            // Icon not found, continue without
        }

        // Create menu bar
        createMenuBar();
    }

    private void initializeComponents() {
        toolbarPanel = new ToolbarPanel(this);
        listPanel = new TransportListPanel(this);
        detailPanel = new TransportDetailPanel(this);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Create main content panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Add toolbar at top
        mainPanel.add(toolbarPanel, BorderLayout.NORTH);

        // Create split pane for list and detail panels
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, detailPanel);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.3);
        splitPane.setBackground(BACKGROUND_COLOR);
        splitPane.setBorder(null);

        mainPanel.add(splitPane, BorderLayout.CENTER);
        add(mainPanel);
    }

    private void setupEventHandlers() {
        // Handle window closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Tests menu
        testsMenu = new JMenu("Tests");
        origonalColor = testsMenu.getForeground();

        // Add menu listener to execute code when menu is opened
        testsMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                onTestsMenuOpened();
            }

            @Override
            public void menuDeselected(MenuEvent e) {
                // Called when menu is closed/deselected
            }

            @Override
            public void menuCanceled(MenuEvent e) {
                // Called when menu is canceled (e.g., ESC key)
            }
        });

        cancel = new JMenuItem("Cancel Test");
        cancel.addActionListener(e -> Walker.cancelWalk());
        testsMenu.add(cancel);
        JMenuItem bankMenuItem = new JMenuItem("Bank");
        bankMenuItem.addActionListener(e -> ThreadPool.submit(BankLocations::walkToNearest));
        tests.add(bankMenuItem);
        testsMenu.add(bankMenuItem);

        menuBar.add(testsMenu);
        setJMenuBar(menuBar);
    }

    private void onTestsMenuOpened() {
        if(Walker.isWalking())
        {
            testsMenu.setForeground(Color.GREEN);
            cancel.setEnabled(true);
            cancel.setVisible(true);
            for(JMenuItem item : tests)
            {
                item.setEnabled(false);
            }
        }
        else
        {
            testsMenu.setForeground(origonalColor);
            cancel.setEnabled(false);
            cancel.setVisible(false);
            for(JMenuItem item : tests)
            {
                item.setEnabled(true);
            }
        }
    }

    // Public API for components

    public void loadTransportsFromFile() {
        try {
            TransportDto[] transportArray = fileManager.loadTransports();
            if (transportArray != null) {
                transports = new ArrayList<>(Arrays.asList(transportArray));
                listPanel.refreshTransportList(transports);
                detailPanel.clearSelection();
                setHasUnsavedChanges(false);
                showStatusMessage("Loaded " + transports.size() + " transports");
            }
        } catch (Exception e) {
            showErrorDialog("Failed to load transports", e);
        }
    }

    public void saveTransportsToFile() {
        try {
            TransportDto[] transportArray = transports.toArray(new TransportDto[0]);
            fileManager.saveTransports(transportArray);
            setHasUnsavedChanges(false);
            showStatusMessage("Saved " + transports.size() + " transports");
        } catch (Exception e) {
            showErrorDialog("Failed to save transports", e);
        }
    }

    public void saveTransportsAsNewFile() {
        try {
            TransportDto[] transportArray = transports.toArray(new TransportDto[0]);
            fileManager.saveTransportsAs(transportArray);
            setHasUnsavedChanges(false);
            showStatusMessage("Saved " + transports.size() + " transports to new file");
        } catch (Exception e) {
            showErrorDialog("Failed to save transports", e);
        }
    }

    public void addNewTransport() {
        TransportDto newTransport = createDefaultTransport();
        transports.add(newTransport);
        listPanel.refreshTransportList(transports);
        listPanel.selectTransport(newTransport);
        setHasUnsavedChanges(true);
    }

    public void duplicateTransport(TransportDto transport) {
        if (transport != null) {
            // Create a copy using JSON serialization
            String json = gson.toJson(transport);
            TransportDto copy = gson.fromJson(json, TransportDto.class);

            transports.add(copy);
            listPanel.refreshTransportList(transports);
            listPanel.selectTransport(copy);
            setHasUnsavedChanges(true);
        }
    }

    public void deleteTransport(TransportDto transport) {
        System.out.println("deleteTransport called with transport: " + (transport != null ? transport.getAction() : "null"));

        if (transport != null && confirmDeleteTransport()) {
            System.out.println("User confirmed deletion, removing transport...");

            boolean removed = transports.remove(transport);
            System.out.println("Transport removed from list: " + removed);

            System.out.println("Clearing detail panel selection first...");
            detailPanel.clearSelection();
            System.out.println("Detail panel cleared");

            System.out.println("Refreshing transport list...");
            listPanel.refreshTransportList(transports);
            System.out.println("Transport list refreshed");

            System.out.println("Setting unsaved changes flag...");
            setHasUnsavedChanges(true);
            System.out.println("deleteTransport completed successfully");
        } else {
            System.out.println("Deletion cancelled or transport was null");
        }
    }

    public void onTransportSelected(TransportDto transport) {
        System.out.println("onTransportSelected called with: " + (transport != null ? transport.getAction() : "null"));
        detailPanel.displayTransport(transport);
        System.out.println("detailPanel.displayTransport completed");
    }

    public void onTransportModified() {
        setHasUnsavedChanges(true);
        listPanel.refreshCurrentSelection();
    }

    public void onTransportUpdated(TransportDto oldTransport, TransportDto newTransport) {
        // Replace the transport in the list (since TransportDto is immutable)
        int index = transports.indexOf(oldTransport);
        if (index >= 0) {
            transports.set(index, newTransport);
            listPanel.refreshTransportList(transports);
            listPanel.selectTransport(newTransport);
            setHasUnsavedChanges(true);
        }
    }

    public List<TransportDto> getTransports() {
        return transports;
    }

    public boolean selectTransportByObjectAndSource(int objectId, WorldPoint worldPoint) {
        return selectTransportByObjectAndSource(objectId, worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane());
    }

    /**
     * Find and select a transport by object ID and source coordinates
     * @param objectId The object ID to match
     * @param x Source X coordinate
     * @param y Source Y coordinate
     * @param plane Source plane
     * @return true if transport was found and selected, false otherwise
     */
    public boolean selectTransportByObjectAndSource(int objectId, int x, int y, int plane) {
        for (TransportDto transport : transports) {
            if (transport.getObjectId() == objectId &&
                transport.getSource().getX() == x &&
                transport.getSource().getY() == y &&
                transport.getSource().getPlane() == plane) {

                // Found matching transport - select it
                listPanel.selectTransport(transport);
                detailPanel.displayTransport(transport);

                // Bring window to front if it exists
                SwingUtilities.invokeLater(() -> {
                    if (!isVisible()) {
                        setVisible(true);
                    }
                    toFront();
                    requestFocus();
                });

                return true;
            }
        }

        return false;
    }

    // Helper methods

    private TransportDto createDefaultTransport() {
        return new TransportDto(
            new net.runelite.api.coords.WorldPoint(0, 0, 0),
            new net.runelite.api.coords.WorldPoint(0, 0, 0),
            "Click",
            0,
            new com.tonic.services.pathfinder.requirements.Requirements()
        );
    }

    private void setHasUnsavedChanges(boolean hasChanges) {
        this.hasUnsavedChanges = hasChanges;
        updateTitle();
        toolbarPanel.updateSaveButtonState(hasChanges);
    }

    private void updateTitle() {
        String title = TITLE;
        if (hasUnsavedChanges) {
            title += " *";
        }
        setTitle(title);
    }

    private boolean confirmDiscardChanges() {
        if (!hasUnsavedChanges) {
            return true;
        }

        int result = JOptionPane.showConfirmDialog(
            this,
            "You have unsaved changes. Are you sure you want to exit?",
            "Unsaved Changes",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        return result == JOptionPane.YES_OPTION;
    }

    private boolean confirmDeleteTransport() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this transport?",
            "Delete Transport",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        return result == JOptionPane.YES_OPTION;
    }

    private void showStatusMessage(String message) {
        // Could be implemented with a status bar
        System.out.println("Status: " + message);
    }

    private void showErrorDialog(String message, Exception e) {
        String fullMessage = message;
        if (e != null) {
            fullMessage += "\n\nError: " + e.getMessage();
        }

        JOptionPane.showMessageDialog(
            this,
            fullMessage,
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}