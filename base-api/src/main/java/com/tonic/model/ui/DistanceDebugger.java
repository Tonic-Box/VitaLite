package com.tonic.model.ui;

import com.tonic.model.ui.components.VitaFrame;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class DistanceDebugger extends VitaFrame
{
    public static boolean isOn()
    {
        if(instance == null)
        {
            return false;
        }
        return instance.enableToggle.isSelected();
    }

    public static DistanceMode getDistanceMode()
    {
        if(instance == null)
        {
            return null;
        }
        return (DistanceMode) instance.distanceModeDropdown.getSelectedItem();
    }

    public static boolean getPlayersEnabled()
    {
        if(instance == null)
        {
            return false;
        }
        return instance.playersCheckbox.isSelected();
    }

    public static boolean getNpcsEnabled()
    {
        if(instance == null)
        {
            return false;
        }
        return instance.npcsCheckbox.isSelected();
    }

    public static boolean getTileObjectsEnabled()
    {
        if(instance == null)
        {
            return false;
        }
        return instance.tileObjectsCheckbox.isSelected();
    }

    public static boolean getTileItemsEnabled()
    {
        if(instance == null)
        {
            return false;
        }
        return instance.tileItemsCheckbox.isSelected();
    }

    private static DistanceDebugger instance = null;
    private final JToggleButton enableToggle;
    private final JComboBox<DistanceMode> distanceModeDropdown;
    private final JCheckBox tileObjectsCheckbox;
    private final JCheckBox playersCheckbox;
    private final JCheckBox npcsCheckbox;
    private final JCheckBox tileItemsCheckbox;

    public static DistanceDebugger getInstance()
    {
        if (instance == null)
        {
            instance = new DistanceDebugger();
        }
        return instance;
    }

    private DistanceDebugger()
    {
        super("Distance Debugger");

        getContentPanel().setLayout(new BorderLayout(10, 10));
        getContentPanel().setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(new Color(30, 31, 34));

        enableToggle = createToggleButton();
        distanceModeDropdown = createDistanceModeDropdown();

        tileObjectsCheckbox = new JCheckBox("GameObjects");
        playersCheckbox = new JCheckBox("Players");
        npcsCheckbox = new JCheckBox("NPCs");
        tileItemsCheckbox = new JCheckBox("TileItems");

        mainPanel.add(createControlSection());
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(createEntityFiltersSection());

        getContentPanel().add(mainPanel, BorderLayout.CENTER);

        setSize(450, 280);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private JPanel createControlSection()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(40, 41, 44));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 61, 64)),
                "Distance Calculation",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(200, 200, 200)
            ),
            new EmptyBorder(10, 10, 10, 10)
        ));

        JPanel toggleRow = new JPanel(new BorderLayout(10, 0));
        toggleRow.setBackground(new Color(40, 41, 44));
        toggleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel toggleLabel = new JLabel("Enable Debugger:");
        toggleLabel.setForeground(Color.WHITE);
        toggleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        toggleRow.add(toggleLabel, BorderLayout.WEST);
        toggleRow.add(enableToggle, BorderLayout.EAST);

        JPanel dropdownRow = new JPanel(new BorderLayout(10, 0));
        dropdownRow.setBackground(new Color(40, 41, 44));
        dropdownRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel modeLabel = new JLabel("Distance Mode:");
        modeLabel.setForeground(Color.WHITE);
        modeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        dropdownRow.add(modeLabel, BorderLayout.WEST);
        dropdownRow.add(distanceModeDropdown, BorderLayout.CENTER);

        panel.add(toggleRow);
        panel.add(Box.createVerticalStrut(10));
        panel.add(dropdownRow);

        return panel;
    }

    private JPanel createEntityFiltersSection()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(40, 41, 44));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 61, 64)),
                "Entity Filters",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(200, 200, 200)
            ),
            new EmptyBorder(10, 10, 10, 10)
        ));

        JPanel checkboxRow = new JPanel(new GridLayout(1, 4, 10, 0));
        checkboxRow.setBackground(new Color(40, 41, 44));
        checkboxRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        styleCheckbox(tileObjectsCheckbox);
        styleCheckbox(playersCheckbox);
        styleCheckbox(npcsCheckbox);
        styleCheckbox(tileItemsCheckbox);

        checkboxRow.add(tileObjectsCheckbox);
        checkboxRow.add(playersCheckbox);
        checkboxRow.add(npcsCheckbox);
        checkboxRow.add(tileItemsCheckbox);

        panel.add(checkboxRow);

        return panel;
    }

    private JToggleButton createToggleButton()
    {
        JToggleButton toggle = new JToggleButton("OFF");
        toggle.setPreferredSize(new Dimension(80, 30));
        toggle.setFocusPainted(false);
        toggle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        toggle.setBackground(new Color(180, 60, 60));
        toggle.setForeground(Color.WHITE);
        toggle.setBorder(BorderFactory.createLineBorder(new Color(140, 40, 40), 2));

        toggle.addActionListener(e -> {
            if (toggle.isSelected())
            {
                toggle.setText("ON");
                toggle.setBackground(new Color(60, 180, 60));
                toggle.setBorder(BorderFactory.createLineBorder(new Color(40, 140, 40), 2));
                distanceModeDropdown.setEnabled(true);
            }
            else
            {
                toggle.setText("OFF");
                toggle.setBackground(new Color(180, 60, 60));
                toggle.setBorder(BorderFactory.createLineBorder(new Color(140, 40, 40), 2));
                distanceModeDropdown.setEnabled(false);
            }
        });

        return toggle;
    }

    private JComboBox<DistanceMode> createDistanceModeDropdown()
    {
        JComboBox<DistanceMode> dropdown = new JComboBox<>(DistanceMode.values());
        dropdown.setBackground(new Color(50, 51, 54));
        dropdown.setForeground(Color.WHITE);
        dropdown.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        dropdown.setEnabled(false);
        dropdown.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        dropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
            {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DistanceMode)
                {
                    DistanceMode mode = (DistanceMode) value;
                    setText(formatModeName(mode));
                }
                if (isSelected)
                {
                    setBackground(new Color(70, 120, 180));
                }
                else
                {
                    setBackground(new Color(50, 51, 54));
                }
                setForeground(Color.WHITE);
                return c;
            }
        });

        return dropdown;
    }

    private void styleCheckbox(JCheckBox checkbox)
    {
        checkbox.setBackground(new Color(40, 41, 44));
        checkbox.setForeground(Color.WHITE);
        checkbox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        checkbox.setFocusPainted(false);
    }

    private String formatModeName(DistanceMode mode)
    {
        String name = mode.name().replace('_', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    public enum DistanceMode {
        PATH_TO,
        EUCLIDEAN,
        EUCLIDEAN_SQUARED,
        MANHATTAN,
        CHEBYSHEV,
        DIAGONAL,

    }
}
