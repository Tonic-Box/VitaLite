package com.tonic.model.ui.components;

import lombok.Getter;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;

public class VitaFrame extends JFrame {
    private static final Color TITLE_BAR_COLOR = new Color(40, 42, 46);
    private static final Color TITLE_BAR_HOVER = new Color(50, 52, 56);
    private static final Color CLOSE_HOVER = new Color(232, 17, 35);
    private static final Color BUTTON_HOVER = new Color(60, 62, 66);
    private static final Color CONTENT_COLOR = new Color(30, 31, 34);
    private static final Color TEXT_COLOR = new Color(200, 200, 200);
    private static final Color BORDER_COLOR = new Color(20, 21, 24);

    private JPanel titleBar;
    private JLabel titleLabel;
    private JLabel iconLabel;  // Added for icon support
    private JButton minimizeBtn, maximizeBtn, closeBtn;
    @Getter
    private JPanel contentPanel;

    // Menu bar support
    private JMenuBar menuBar;
    private JPanel mainContainer;

    // For window dragging
    private Point mouseDownPoint;
    private Point windowLocation;

    // For resizing
    private static final int RESIZE_BORDER = 5;
    private int resizeDir = 0;
    private Point resizeStart;
    private Rectangle boundsBeforeResize;

    // For maximize/restore
    private Rectangle boundsBeforeMaximize;
    private boolean isMaximized = false;

    // Resize direction constants
    private static final int NORTH = 1, SOUTH = 2, WEST = 4, EAST = 8;
    private static final int NORTH_WEST = NORTH | WEST;
    private static final int NORTH_EAST = NORTH | EAST;
    private static final int SOUTH_WEST = SOUTH | WEST;
    private static final int SOUTH_EAST = SOUTH | EAST;

    public VitaFrame() {
        this("VitaFrame");
    }

    public VitaFrame(String title) {
        super(title);
        setupFrame(title);
    }

    /**
     * Call this method in your subclass constructor after super()
     * if you need to ensure proper initialization
     */
    protected void setupFrame(String title) {
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(400, 300));

        initComponents(title);
        addWindowListeners();
        addResizeListeners();
    }

    private void initComponents(String title) {
        // Clear any existing layout first
        getContentPane().removeAll();
        setLayout(new BorderLayout());
        getContentPane().setBackground(CONTENT_COLOR);

        // Create title bar
        titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(TITLE_BAR_COLOR);
        titleBar.setPreferredSize(new Dimension(getWidth(), 32));
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        // Title panel with icon support
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        titlePanel.setOpaque(false);

        // Icon label (initially empty)
        iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(20, 20)); // Reserve space
        iconLabel.setVisible(false); // Initially hidden
        titlePanel.add(iconLabel);

        // Title label
        titleLabel = new JLabel(title);
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        titlePanel.add(titleLabel);

        // Window control buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);

        // Use simple ASCII characters
        minimizeBtn = createControlButton("_");  // Underscore for minimize
        maximizeBtn = createControlButton("[]"); // Square brackets for maximize
        closeBtn = createControlButton("X");     // X for close

        // Special styling for close button
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setBackground(CLOSE_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setBackground(TITLE_BAR_COLOR);
            }
        });

        buttonPanel.add(minimizeBtn);
        buttonPanel.add(maximizeBtn);
        buttonPanel.add(closeBtn);

        titleBar.add(titlePanel, BorderLayout.WEST);
        titleBar.add(buttonPanel, BorderLayout.EAST);

        // Add title bar dragging functionality
        MouseAdapter dragListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    // Double-click to maximize/restore
                    toggleMaximize();
                } else {
                    mouseDownPoint = e.getPoint();
                    windowLocation = getLocation();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (mouseDownPoint != null) {
                    // If dragging while maximized, restore to normal size first
                    if (isMaximized) {
                        isMaximized = false;
                        maximizeBtn.setText("[]");

                        // Restore to saved bounds if available
                        if (boundsBeforeMaximize != null) {
                            setBounds(boundsBeforeMaximize);

                            // Position window so title bar is centered under cursor
                            Point currentLocation = e.getLocationOnScreen();
                            int titleBarCenterOffset = boundsBeforeMaximize.width / 2;
                            int newX = currentLocation.x - titleBarCenterOffset;
                            int newY = currentLocation.y - mouseDownPoint.y;
                            setLocation(newX, newY);

                            // Update mouse down point to center of title bar for smooth dragging
                            mouseDownPoint = new Point(titleBarCenterOffset, mouseDownPoint.y);
                            windowLocation = getLocation();
                        }
                    } else {
                        // Normal dragging
                        Point currentLocation = e.getLocationOnScreen();
                        int x = currentLocation.x - mouseDownPoint.x;
                        int y = currentLocation.y - mouseDownPoint.y;
                        setLocation(x, y);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mouseDownPoint = null;
            }
        };

        titleBar.addMouseListener(dragListener);
        titleBar.addMouseMotionListener(dragListener);

        // Create main container for menu bar and content
        mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(CONTENT_COLOR);

        // Content panel with padding
        contentPanel = new JPanel();
        contentPanel.setBackground(CONTENT_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.setLayout(new BorderLayout());

        // Add content panel to main container
        mainContainer.add(contentPanel, BorderLayout.CENTER);

        // Add border to the frame
        getRootPane().setBorder(new LineBorder(BORDER_COLOR, 1));

        // Add components to frame - IMPORTANT: use getContentPane()
        getContentPane().add(titleBar, BorderLayout.NORTH);
        getContentPane().add(mainContainer, BorderLayout.CENTER);

        // Force a repaint to ensure visibility
        revalidate();
        repaint();
    }

    /**
     * Override setJMenuBar to place menu bar correctly under title bar
     */
    @Override
    public void setJMenuBar(JMenuBar menubar) {
        if (this.menuBar != null) {
            mainContainer.remove(this.menuBar);
        }
        this.menuBar = menubar;
        if (menubar != null) {
            // Style the menu bar to match dark theme
            menubar.setBackground(new Color(35, 37, 41));
            menubar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

            // Style menu items if needed
            for (int i = 0; i < menubar.getMenuCount(); i++) {
                JMenu menu = menubar.getMenu(i);
                if (menu != null) {
                    menu.setForeground(TEXT_COLOR);
                }
            }

            // Add menu bar to main container, not to frame
            mainContainer.add(menubar, BorderLayout.NORTH);
        }
        revalidate();
        repaint();
    }

    /**
     * Override getJMenuBar to return our custom menu bar
     */
    @Override
    public JMenuBar getJMenuBar() {
        return menuBar;
    }

    private JButton createControlButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                super.paintComponent(g2);
                g2.dispose();
            }
        };

        button.setForeground(TEXT_COLOR);
        button.setBackground(TITLE_BAR_COLOR);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(46, 32));
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button != closeBtn) {
                    button.setBackground(BUTTON_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(TITLE_BAR_COLOR);
            }
        });

        return button;
    }

    private void addWindowListeners() {
        minimizeBtn.addActionListener(e -> setExtendedState(JFrame.ICONIFIED));
        maximizeBtn.addActionListener(e -> toggleMaximize());
        closeBtn.addActionListener(e -> {
            setVisible(false);
            dispose();
        });
    }

    private void toggleMaximize() {
        if (isMaximized) {
            // Restoring to normal
            isMaximized = false;
            setExtendedState(JFrame.NORMAL);
            maximizeBtn.setText("[]");  // Normal state shows maximize icon

            // Restore to saved bounds if available
            if (boundsBeforeMaximize != null) {
                setBounds(boundsBeforeMaximize);
            }
        } else {
            // Maximizing - save current bounds first
            boundsBeforeMaximize = getBounds();
            isMaximized = true;
            setExtendedState(JFrame.NORMAL); // Keep as NORMAL to prevent OS maximize

            // Get maximum usable screen bounds (excludes taskbar/dock/menu bar)
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle maxBounds = env.getMaximumWindowBounds();
            setBounds(maxBounds);

            maximizeBtn.setText("[=]"); // Maximized state shows restore icon
        }
    }

    private void addResizeListeners() {
        MouseAdapter resizeListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                resizeDir = getResizeDirection(e.getPoint());
                resizeStart = e.getLocationOnScreen();
                boundsBeforeResize = getBounds();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                resizeDir = 0;
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int dir = getResizeDirection(e.getPoint());
                setCursor(Cursor.getPredefinedCursor(getCursorForDirection(dir)));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (resizeDir == 0) return;

                Point currentLocation = e.getLocationOnScreen();
                int dx = currentLocation.x - resizeStart.x;
                int dy = currentLocation.y - resizeStart.y;

                Rectangle newBounds = new Rectangle(boundsBeforeResize);

                if ((resizeDir & NORTH) != 0) {
                    newBounds.y += dy;
                    newBounds.height -= dy;
                }
                if ((resizeDir & SOUTH) != 0) {
                    newBounds.height += dy;
                }
                if ((resizeDir & WEST) != 0) {
                    newBounds.x += dx;
                    newBounds.width -= dx;
                }
                if ((resizeDir & EAST) != 0) {
                    newBounds.width += dx;
                }

                // Respect minimum size
                Dimension minSize = getMinimumSize();
                if (newBounds.width >= minSize.width && newBounds.height >= minSize.height) {
                    setBounds(newBounds);
                    validate();
                    repaint();
                }
            }
        };

        addMouseListener(resizeListener);
        addMouseMotionListener(resizeListener);
    }

    private int getResizeDirection(Point p) {
        // Don't allow resizing when maximized
        if (isMaximized) return 0;

        int dir = 0;

        if (p.x < RESIZE_BORDER) dir |= WEST;
        if (p.x > getWidth() - RESIZE_BORDER) dir |= EAST;
        if (p.y < RESIZE_BORDER) dir |= NORTH;
        if (p.y > getHeight() - RESIZE_BORDER) dir |= SOUTH;

        return dir;
    }

    private int getCursorForDirection(int dir) {
        switch (dir) {
            case NORTH: return Cursor.N_RESIZE_CURSOR;
            case SOUTH: return Cursor.S_RESIZE_CURSOR;
            case WEST: return Cursor.W_RESIZE_CURSOR;
            case EAST: return Cursor.E_RESIZE_CURSOR;
            case NORTH_WEST: return Cursor.NW_RESIZE_CURSOR;
            case NORTH_EAST: return Cursor.NE_RESIZE_CURSOR;
            case SOUTH_WEST: return Cursor.SW_RESIZE_CURSOR;
            case SOUTH_EAST: return Cursor.SE_RESIZE_CURSOR;
            default: return Cursor.DEFAULT_CURSOR;
        }
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        if (titleLabel != null) {
            titleLabel.setText(title);
        }
    }

    /**
     * Override setIconImage to display icon in title bar
     */
    @Override
    public void setIconImage(Image image) {
        super.setIconImage(image);
        updateTitleBarIcon(image);
    }

    /**
     * Override setIconImages for multiple icon support
     */
    @Override
    public void setIconImages(java.util.List<? extends Image> icons) {
        super.setIconImages(icons);
        // Update title bar icon with the first image
        if (icons != null && !icons.isEmpty()) {
            updateTitleBarIcon(icons.get(0));
        } else {
            updateTitleBarIcon(null);
        }
    }

    /**
     * Helper method to update the title bar icon without causing recursion
     */
    private void updateTitleBarIcon(Image image) {
        if (iconLabel != null && image != null) {
            // Scale the image to fit the title bar height (20x20 pixels)
            Image scaledImage = image.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            iconLabel.setIcon(new ImageIcon(scaledImage));
            iconLabel.setVisible(true);
        } else if (iconLabel != null) {
            iconLabel.setIcon(null);
            iconLabel.setVisible(false);
        }
    }

    /**
     * Override this method in subclasses to add your own components
     * Call super.initializeContent() first if you want to keep the default setup
     */
    protected void initializeContent() {
        // Subclasses can override this to add their own content
    }

    /**
     * Utility method for subclasses to easily add components to the content area
     */
    protected void addToContent(Component component) {
        if (contentPanel != null) {
            contentPanel.add(component);
        }
    }

    /**
     * Utility method for subclasses to easily add components with constraints
     */
    protected void addToContent(Component component, Object constraints) {
        if (contentPanel != null) {
            contentPanel.add(component, constraints);
        }
    }
}