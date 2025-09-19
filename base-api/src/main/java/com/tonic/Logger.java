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
    private static int loggerHeight = 150;         // height of the logger component

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
        console.setAutoscrolls(true);
        console.setEditable(false);
        console.setFont(new Font("Monoid", Font.PLAIN, 14));


        DefaultCaret caret = (DefaultCaret)console.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
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
        clearItem.addActionListener(e -> textPane.setText(""));

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

        // Remember the preferred height (including borders etc.)
        if (component != null) {
            loggerHeight = component.getPreferredSize().height;
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
}