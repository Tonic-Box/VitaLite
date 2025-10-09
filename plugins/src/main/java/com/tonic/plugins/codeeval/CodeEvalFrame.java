package com.tonic.plugins.codeeval;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Scanner;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import com.tonic.Static;
import com.tonic.services.GameManager;
import com.tonic.util.ThreadPool;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.autocomplete.*;

public class CodeEvalFrame extends JFrame {
    private static CodeEvalFrame INSTANCE;
    private SimpleCodeEvaluator evaluator;
    private final RSyntaxTextArea codeArea;
    private final JTextArea outputArea;
    private final JButton runButton;
    private Future<?> future;

    public static CodeEvalFrame get() {
        if (INSTANCE == null) {
            INSTANCE = new CodeEvalFrame();
        }
        return INSTANCE;
    }

    public void toggle() {
        SwingUtilities.invokeLater(() -> {
            if (isVisible()) {
                close();
            } else {
                open();
            }
        });
    }

    public void open() {
        refreshContext();
        setVisible(true);
        toFront();
        repaint();
    }

    public void close() {
        setVisible(false);
        clearOutput();
    }

    private void refreshContext() {
        ClassLoader rlClassLoader = com.tonic.services.GameManager.class.getClassLoader();
        outputArea.append(">>> Using RLClassLoader: " + rlClassLoader.getClass().getName() + "\n");
        outputArea.append(">>> RLClassLoader toString: " + rlClassLoader + "\n");
        if (evaluator == null || !evaluator.getParentClassLoader().equals(rlClassLoader)) {
            outputArea.append(">>> Refreshing classloader context...\n");
            evaluator = new SimpleCodeEvaluator(rlClassLoader);
            outputArea.append(">>> Ready for code evaluation.\n");
        }
    }

    private void clearOutput() {
        outputArea.setText("");
    }

    public CodeEvalFrame() {
        super("VitaLite Code Evaluator");
        this.evaluator = new SimpleCodeEvaluator(GameManager.class.getClassLoader());

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLayout(new BorderLayout());

        final BufferedImage iconImage = ImageUtil.loadImageResource(CodeEvalFrame.class, "jshell.png");
        setIconImage(iconImage);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                close();
            }
        });
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        JPanel codePanel = new JPanel(new BorderLayout());
        codePanel.setBorder(BorderFactory.createTitledBorder("Code (Ctrl+Enter to run)"));

        codeArea = new RSyntaxTextArea(20, 120);
        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        codeArea.setCodeFoldingEnabled(true);
        codeArea.setAntiAliasingEnabled(true);
        codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        codeArea.setTabSize(4);

        try {
            Theme theme = Theme.load(getClass().getResourceAsStream("dark.xml"));
            theme.apply(codeArea);
        } catch (Exception e) {
            codeArea.setBackground(new Color(40, 40, 40));
            codeArea.setForeground(Color.WHITE);
            codeArea.setCurrentLineHighlightColor(new Color(50, 50, 50));
        }

        codeArea.setWhitespaceVisible(false);
        codeArea.setEOLMarkersVisible(false);

        try {
            InputStream exampleStream = getClass().getResourceAsStream("default_example.java");
            if (exampleStream != null) {
                try (Scanner scanner = new Scanner(exampleStream, "UTF-8")) {
                    scanner.useDelimiter("\\A");
                    String defaultCode = scanner.hasNext() ? scanner.next() : "";
                    codeArea.setText(defaultCode);
                }
            } else {
                codeArea.setText("// Enter your Java code here\nout.println(\"Hello World!\");");
            }
        } catch (Exception e) {
            codeArea.setText("// Enter your Java code here\nout.println(\"Hello World!\");");
        }

        codeArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isControlDown()) {
                    runCode();
                    e.consume();
                }
            }
        });

        JScrollPane codeScrollPane = new JScrollPane(codeArea);
        codePanel.add(codeScrollPane, BorderLayout.CENTER);

        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("Output"));

        outputArea = new JTextArea(20, 120);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        outputArea.setEditable(false);
        outputArea.setBackground(Color.BLACK);
        outputArea.setForeground(Color.GREEN);

        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        runButton = new JButton("Run Code (Ctrl+Enter)");
        runButton.addActionListener(e -> {
            if(runButton.getText().equals("Run Code (Ctrl+Enter)"))
            {
                runButton.setText("Stop");
                repaint();
                runCode();
            }
            else
            {
                forceStopFuture(future);
                runButton.setText("Run Code (Ctrl+Enter)");
                outputArea.append(">>> Canceled.\n");
                outputArea.setCaretPosition(outputArea.getDocument().getLength());
            }
        });

        JButton clearButton = new JButton("Clear Output");
        clearButton.addActionListener(e -> outputArea.setText(""));

        JCheckBox alwaysOnTopCheckbox = new JCheckBox("Always on Top", true);
        alwaysOnTopCheckbox.addActionListener(e -> setAlwaysOnTop(alwaysOnTopCheckbox.isSelected()));

        buttonPanel.add(runButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(alwaysOnTopCheckbox);

        codePanel.add(buttonPanel, BorderLayout.SOUTH);

        splitPane.setTopComponent(codePanel);
        splitPane.setBottomComponent(outputPanel);
        splitPane.setDividerLocation(500);

        add(splitPane, BorderLayout.CENTER);

        setAlwaysOnTop(true);

        pack();
        setLocationRelativeTo(null);
    }

    private void runCode() {
        future = ThreadPool.submit(() -> {
            outputArea.append(">>> Running code...\n");

            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            PrintStream customOut = new PrintStream(new OutputStream() {
                private StringBuilder buffer = new StringBuilder();

                @Override
                public void write(int b) {
                    if (b == '\n') {
                        String line = buffer.toString();
                        SwingUtilities.invokeLater(() -> {
                            outputArea.append(line + "\n");
                            outputArea.setCaretPosition(outputArea.getDocument().getLength());
                        });
                        buffer = new StringBuilder();
                    } else {
                        buffer.append((char) b);
                    }
                }

                @Override
                public void flush() {
                    if (buffer.length() > 0) {
                        String text = buffer.toString();
                        SwingUtilities.invokeLater(() -> {
                            outputArea.append(text);
                            outputArea.setCaretPosition(outputArea.getDocument().getLength());
                        });
                        buffer = new StringBuilder();
                    }
                }
            });

            try {
                System.setOut(customOut);
                System.setErr(customOut);

                Object result = evaluator.evaluate(codeArea.getText());

                if (result != null) {
                    System.out.println("Result: " + result);
                }

            } finally {
                customOut.flush();
                System.setOut(originalOut);
                System.setErr(originalErr);

                SwingUtilities.invokeLater(() -> {
                    outputArea.append(">>> Done.\n\n");
                    outputArea.setCaretPosition(outputArea.getDocument().getLength());
                    runButton.setText("Run Code (Ctrl+Enter)");
                });
            }
        });
    }

    public static void forceStopFuture(Future<?> future) {
        try {
            if (future instanceof FutureTask) {
                Field runnerField = FutureTask.class.getDeclaredField("runner");
                runnerField.setAccessible(true);
                Thread runner = (Thread) runnerField.get(future);

                if (runner != null) {
                    runner.setUncaughtExceptionHandler((t, e) -> {
                        if (!(e instanceof ThreadDeath)) {
                            e.printStackTrace();
                        }
                    });
                    runner.stop();
                }
            }
        } catch (ThreadDeath | Exception ignored) {
        }
    }
}