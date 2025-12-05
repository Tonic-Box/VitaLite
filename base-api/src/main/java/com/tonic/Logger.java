package com.tonic;

import lombok.Setter;
import lombok.SneakyThrows;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple logger for VitaX
 */
@Singleton
public class Logger {
    private static Logger INSTANCE;
    @Setter
    private static JComponent loggerComponent;
    private static Container wrapper;            // the panel that holds the logger (BorderLayout)
    private static JFrame clientFrame;           // the top level window
    private static JScrollPane consoleScrollPane; // scroll pane for smart auto-scroll
    private static int loggerHeight = 150;         // height of the logger component
    private static int maxMessages = 50;           // maximum number of messages to keep in console

    static
    {
        setInstance();
    }

    public static JTextPane getConsole()
    {
        if(INSTANCE == null)
            return null;
        return INSTANCE.console;
    }

    /**
     * set normal logging
     * @param state state
     */
    public static void setNormal(boolean state)
    {
        if(INSTANCE == null)
            return;
        INSTANCE.normal = state;
    }

    /**
     * set info logging
     * @param state state
     */
    public static void setInfo(boolean state)
    {
        if(INSTANCE == null)
            return;
        INSTANCE.info = state;
    }

    /**
     * set warning logging
     * @param state state
     */
    public static void setWarning(boolean state)
    {
        if(INSTANCE == null)
            return;
        INSTANCE.warning = state;
    }

    /**
     * set error logging
     * @param state state
     */
    public static void setError(boolean state)
    {
        if(INSTANCE == null)
            return;
        INSTANCE.error = state;
    }

    /**
     * for console input
     * @param data data
     */
    public static void console(String data)
    {
        if(INSTANCE == null)
            return;
        INSTANCE._console(data);
    }

    /**
     * for console output
     * @param head header
     * @param body message
     */
    public static void consoleOutput(String head, String body)
    {
        if(INSTANCE == null)
            return;
        INSTANCE._consoleOutput("(" + head + ") ", body);
    }

    /**
     * for console error output
     * @param head header
     * @param body message
     */
    public static void consoleErrorOutput(String head, String body)
    {
        if(INSTANCE == null)
            return;
        INSTANCE._consoleErrorOutput("(" + head + ") ", body);
    }

    /**
     * for normal purposeful logging
     * @param data data
     */
    public static void norm(String data)
    {
        if(INSTANCE == null)
            return;
        INSTANCE._norm(data);
    }

    /**
     * for general diagnostic logging
     * @param data data
     */
    public static void info(String data)
    {
        if(INSTANCE == null)
            return;
        INSTANCE._info(data);
    }

    /**
     * For non fatal warnings
     * @param data data
     */
    public static void warn(String data)
    {
        if(INSTANCE == null)
            return;
        INSTANCE._warn(data);
    }

    /**
     * for fatal errors
     * @param data data
     */
    public static void error(String data)
    {
        if(INSTANCE == null)
        {
            System.err.println(data);
            return;
        }
        INSTANCE._error(data);
    }

    /**
     * for fatal errors
     * @param throwable exception
     */
    public static void error(Throwable throwable)
    {
        throwable.printStackTrace();
        if(INSTANCE == null)
        {
            return;
        }
        INSTANCE._error(throwable.getMessage());
    }

    /**
     * for fatal errors
     * @param throwable throwable
     */
    public static void error(Throwable throwable, String message)
    {
        if(INSTANCE == null)
            return;
        INSTANCE._error(message.replace("%e", throwable.getMessage()));
    }

    /**
     * Sets the maximum number of messages to keep in the console.
     * Older messages will be removed when this limit is exceeded.
     * @param max Maximum message count (must be > 0)
     */
    public static void setMaxMessages(int max)
    {
        if (max <= 0)
        {
            throw new IllegalArgumentException("Max messages must be greater than 0");
        }
        maxMessages = max;

        // Trim existing messages if new limit is lower
        if (INSTANCE != null)
        {
            INSTANCE.trimToMaxMessages();
        }
    }

    /**
     * Gets the current maximum message count limit.
     * @return Current max messages
     */
    public static int getMaxMessages()
    {
        return maxMessages;
    }

    /**
     * Initial binding for statically stored instance
     */
    public static void setInstance()
    {
        INSTANCE = new Logger();
    }

    private final JTextPane console;

    private boolean info = true;
    private boolean normal = true;
    private boolean warning = true;
    private boolean error = true;
    private int currentMessageCount = 0;
    private final SimpleAttributeSet CONSOLE;
    private final SimpleAttributeSet NORM;
    private final SimpleAttributeSet INFO;
    private final SimpleAttributeSet WARN;
    private final SimpleAttributeSet ERROR;
    private final float SPACING = 1.5f;
    private Logger()
    {
        console = new JTextPane();
        console.setBackground(Color.BLACK);
        console.setForeground(Color.GREEN);
        console.setEditable(false);
        console.setFont(new Font("Monoid", Font.PLAIN, 14));

        // Use NEVER_UPDATE so we control scrolling manually
        DefaultCaret caret = (DefaultCaret)console.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        fontFactory(console);
        addFullContextMenu(console);

        CONSOLE = new SimpleAttributeSet();
        StyleConstants.setForeground(CONSOLE, Color.LIGHT_GRAY);

        NORM = new SimpleAttributeSet();
        StyleConstants.setForeground(NORM, Color.GREEN);

        INFO = new SimpleAttributeSet();
        StyleConstants.setForeground(INFO, Color.decode("#ADD8E6"));

        WARN = new SimpleAttributeSet();
        StyleConstants.setForeground(WARN, Color.YELLOW);

        ERROR = new SimpleAttributeSet();
        StyleConstants.setForeground(ERROR, Color.RED);
    }

    private void addFullContextMenu(JTextPane textPane) {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem clearItem = new JMenuItem("Clear");
        clearItem.addActionListener(e -> {
            textPane.setText("");
            currentMessageCount = 0;  // Reset counter when clearing console
        });

        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.addActionListener(e -> textPane.copy());

        JMenuItem selectAllItem = new JMenuItem("Select All");
        selectAllItem.addActionListener(e -> textPane.selectAll());

        popupMenu.add(copyItem);
        popupMenu.add(selectAllItem);
        popupMenu.addSeparator();
        popupMenu.add(clearItem);

        textPane.setComponentPopupMenu(popupMenu);
    }

    private static void fontFactory(JTextPane console)
    {
        Font consoleFont = null;
        String[] fontNames = {
                "Consolas",
                "Menlo",
                "DejaVu Sans Mono",
                "Liberation Mono",
                "Courier New",
                Font.MONOSPACED
        };

        for (String fontName : fontNames) {
            Font f = new Font(fontName, Font.PLAIN, 12);
            if (!f.getFamily().equals(Font.DIALOG)) {
                consoleFont = f;
                break;
            }
        }

        if (consoleFont != null) {
            console.setFont(consoleFont);
        }
    }

    private void stream(String data, SimpleAttributeSet style)
    {
        String timestamp = "[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + "] ";
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
                StyleConstants.setLineSpacing(style,SPACING);
                console.getStyledDocument().insertString(console.getStyledDocument().getLength(), timestamp + data + "\n", style);
                currentMessageCount++;

                // Remove oldest messages if we exceed the limit
                trimToMaxMessages();

                // Auto-scroll if at bottom and no selection
                scrollToBottomIfNeeded();
            }
        });
    }

    @SneakyThrows
    private void _console(String data)
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
                StyleConstants.setLineSpacing(NORM,SPACING);
                StyleConstants.setLineSpacing(CONSOLE,SPACING);
                console.getStyledDocument().insertString(console.getStyledDocument().getLength(), "$ ", NORM);
                console.getStyledDocument().insertString(console.getStyledDocument().getLength(), data + "\n", CONSOLE);
                currentMessageCount++;

                // Remove oldest messages if we exceed the limit
                trimToMaxMessages();

                // Auto-scroll if at bottom and no selection
                scrollToBottomIfNeeded();
            }
        });
    }

    @SneakyThrows
    private void _consoleOutput(String head, String body)
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
                StyleConstants.setLineSpacing(NORM,SPACING);
                StyleConstants.setLineSpacing(CONSOLE,SPACING);
                console.getStyledDocument().insertString(console.getStyledDocument().getLength(), head, INFO);
                console.getStyledDocument().insertString(console.getStyledDocument().getLength(), body + "\n", CONSOLE);
                currentMessageCount++;

                // Remove oldest messages if we exceed the limit
                trimToMaxMessages();

                // Auto-scroll if at bottom and no selection
                scrollToBottomIfNeeded();
            }
        });
    }

    @SneakyThrows
    private void _consoleErrorOutput(String head, String body)
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
                StyleConstants.setLineSpacing(NORM,SPACING);
                StyleConstants.setLineSpacing(CONSOLE,SPACING);
                console.getStyledDocument().insertString(console.getStyledDocument().getLength(), head, INFO);
                console.getStyledDocument().insertString(console.getStyledDocument().getLength(), body + "\n", ERROR);
                currentMessageCount++;

                // Remove oldest messages if we exceed the limit
                trimToMaxMessages();

                // Auto-scroll if at bottom and no selection
                scrollToBottomIfNeeded();
            }
        });
    }

    /**
     * Called once after the UI has been built.  It stores the wrapper panel,
     * the top level frame and the height of the logger component so that
     * {@link #setLoggerVisible(boolean)} can resize the window.
     *
     * @param component   the scroll pane (or any component) that represents the logger
     * @param wrapperPanel the container that has the logger added in BorderLayout.SOUTH
     * @param frame        the client {@link JFrame}
     */
    public static void initLoggerUI(JComponent component, Container wrapperPanel, JFrame frame) {
        loggerComponent = component;
        wrapper = wrapperPanel;
        clientFrame = frame;

        // Capture scroll pane reference for smart auto-scrolling
        if (component instanceof JScrollPane) {
            consoleScrollPane = (JScrollPane) component;
        }

        // Remember the preferred height (including borders etc.)
        if (component != null) {
            loggerHeight = component.getPreferredSize().height;
        }
    }

    /**
     * Sets the scroll pane used for the console (for smart auto-scroll behavior).
     * @param scrollPane The JScrollPane containing the console
     */
    public static void setConsoleScrollPane(JScrollPane scrollPane) {
        consoleScrollPane = scrollPane;
    }

    /**
     * Checks if the user has text selected in the console.
     */
    private static boolean hasSelection() {
        if (INSTANCE == null) return false;
        return INSTANCE.console.getSelectionStart() != INSTANCE.console.getSelectionEnd();
    }

    /**
     * Checks if the console is scrolled to the bottom.
     * @return true if at bottom (or within threshold), false if scrolled up
     */
    private static boolean shouldAutoScroll() {
        // Don't auto-scroll if user has text selected
        if (hasSelection()) {
            return false;
        }

        if (consoleScrollPane == null) {
            return true; // Default to auto-scroll if no scroll pane set
        }
        JScrollBar vertical = consoleScrollPane.getVerticalScrollBar();
        int max = vertical.getMaximum();
        int extent = vertical.getVisibleAmount();
        int value = vertical.getValue();

        // If no scrollbar yet (empty/small content), always auto-scroll
        if (max <= extent) {
            return true;
        }

        // Consider "at bottom" if within a small threshold
        return (max - extent - value) < 30;
    }

    /**
     * Scrolls the console to the bottom if appropriate.
     * Uses scrollbar manipulation instead of caret to avoid disrupting selection.
     */
    private static void scrollToBottomIfNeeded() {
        if (shouldAutoScroll() && consoleScrollPane != null) {
            // Force layout to update scrollbar bounds immediately
            consoleScrollPane.validate();
            JScrollBar vertical = consoleScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        }
    }

    /**
     * Shows or hides the logger component and resizes the client window
     * by the exact height of the logger.
     *
     * @param visible visible
     */
    public static void setLoggerVisible(boolean visible) {
        if (loggerComponent == null || wrapper == null || clientFrame == null) {
            return;
        }

        if (visible) {
            wrapper.add(loggerComponent, BorderLayout.SOUTH);
        } else {
            wrapper.remove(loggerComponent);
        }

        wrapper.revalidate();
        wrapper.repaint();

        Dimension size = clientFrame.getSize();

        if (visible) {
            size.height += loggerHeight;
        } else {
            size.height -= loggerHeight;
        }

        clientFrame.setSize(size);
        clientFrame.validate();
    }

    /**
     * Current visibility state of the logger component.
     *
     * @return true if the logger component is visible, false otherwise
     */
    public static boolean isLoggerVisible() {
        return loggerComponent != null && loggerComponent.isVisible();
    }

    private void _norm(String data)
    {
        if(!normal)
            return;

        stream(data, NORM);
    }

    private void _info(String data)
    {
        if(!info)
            return;

        stream(data, INFO);
    }

    private void _warn(String data)
    {
        if(!warning)
            return;

        stream(data, WARN);
    }

    private void _error(String data)
    {
        if(!error)
            return;

        stream(data, ERROR);
    }

    /**
     * Trims the console to keep only the most recent maxMessages.
     * Removes oldest messages from the beginning of the document.
     * Preserves scroll position and selection by adjusting for removed content.
     */
    @SneakyThrows
    private void trimToMaxMessages()
    {
        if (currentMessageCount <= maxMessages) {
            return;
        }

        // Check if we should preserve scroll position (not at bottom)
        boolean preserveScroll = !shouldAutoScroll();
        int scrollValue = 0;
        if (preserveScroll && consoleScrollPane != null) {
            scrollValue = consoleScrollPane.getVerticalScrollBar().getValue();
        }

        // Save selection before removing
        int selStart = console.getSelectionStart();
        int selEnd = console.getSelectionEnd();
        boolean hadSelection = selStart != selEnd;

        int totalCharsRemoved = 0;

        // Trim messages
        while (currentMessageCount > maxMessages)
        {
            // Find the first newline (end of first message)
            String text = console.getStyledDocument().getText(0, console.getStyledDocument().getLength());
            int firstNewline = text.indexOf('\n');

            if (firstNewline >= 0)
            {
                int charsToRemove = firstNewline + 1;
                console.getStyledDocument().remove(0, charsToRemove);
                currentMessageCount--;
                totalCharsRemoved += charsToRemove;
            }
            else
            {
                // No newline found, shouldn't happen but break to prevent infinite loop
                break;
            }
        }

        // Restore selection if there was one, adjusted for removed content
        if (hadSelection && totalCharsRemoved > 0) {
            int newSelStart = Math.max(0, selStart - totalCharsRemoved);
            int newSelEnd = Math.max(0, selEnd - totalCharsRemoved);
            if (newSelEnd > newSelStart) {
                console.setSelectionStart(newSelStart);
                console.setSelectionEnd(newSelEnd);
            }
        }

        // Restore scroll position if we weren't at bottom
        // The scroll position doesn't need adjustment since we're removing from top
        // and the viewport should naturally shift up with the content
    }
}