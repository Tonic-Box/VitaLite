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
import com.tonic.model.ui.components.VitaFrame;
import com.tonic.plugins.codeeval.completion.*;
import com.tonic.services.GameManager;
import com.tonic.util.ThreadPool;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.autocomplete.*;

public class CodeEvalFrame extends VitaFrame {
    public enum EvalContext { API, CLIENT }

    private static CodeEvalFrame INSTANCE;
    private final RSyntaxTextArea codeArea;
    private final JTextArea outputArea;
    private final JButton runButton;
    private Future<?> future;
    private AutoCompletion autoCompletion;

    // Dual-context fields
    private EvalContext currentContext = EvalContext.API;
    private CompletionContext apiContext;
    private CompletionContext clientContext;
    private ImportScanner sharedImportScanner;
    private JToggleButton contextToggle;

    public static CodeEvalFrame get() {
        if (INSTANCE == null) {
            if (SwingUtilities.isEventDispatchThread()) {
                INSTANCE = new CodeEvalFrame();
            } else {
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        if (INSTANCE == null) {
                            INSTANCE = new CodeEvalFrame();
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create CodeEvalFrame on EDT", e);
                }
            }
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
        codeArea.requestFocusInWindow();
        repaint();
    }

    public void close() {
        setVisible(false);
        clearOutput();
    }

    private void refreshContext() {
        CompletionContext ctx = getCurrentCompletionContext();
        if (ctx != null && ctx.isInitialized()) {
            outputArea.append(">>> Using " + currentContext + " context (" +
                    ctx.getClassLoader().getClass().getName() + ")\n");
        } else {
            outputArea.append(">>> " + currentContext + " context initializing...\n");
        }
    }

    private CompletionContext getCurrentCompletionContext() {
        return currentContext == EvalContext.API ? apiContext : clientContext;
    }

    private void switchContext(EvalContext newContext) {
        if (currentContext == newContext) return;

        currentContext = newContext;
        CompletionContext ctx = getCurrentCompletionContext();

        if (ctx != null && ctx.isInitialized()) {
            installCompletion(ctx);
            outputArea.append(">>> Switched to " + newContext + " context\n");
        } else {
            outputArea.append(">>> " + newContext + " context still initializing...\n");
        }
    }

    private void installCompletion(CompletionContext context) {
        SwingUtilities.invokeLater(() -> {
            // Uninstall previous if exists
            if (autoCompletion != null) {
                autoCompletion.uninstall();
            }

            autoCompletion = new AutoCompletion(context.getCompletionProvider());
            autoCompletion.setAutoActivationEnabled(true);
            autoCompletion.setAutoActivationDelay(150);
            autoCompletion.setShowDescWindow(true);
            autoCompletion.setAutoCompleteSingleChoices(false);
            autoCompletion.setParameterAssistanceEnabled(true);
            autoCompletion.install(codeArea);

            System.out.println("Installed " + context.getName() + " context with " +
                    context.getClassCache().getClassCount() + " classes");
        });
    }

    private void clearOutput() {
        outputArea.setText("");
    }

    public CodeEvalFrame() {
        super("VitaLite Code Evaluator");
        JPanel contentPanel = getContentPanel();

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        contentPanel.setLayout(new BorderLayout());

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

        // Use IsolatedRSyntaxTextArea to prevent conflicts with other RSyntaxTextArea instances
        // loaded by different classloaders (fixes read-only bug when both CodeEval and
        // external plugins using RSyntaxTextArea are opened)
        // See: https://github.com/bobbylight/RSyntaxTextArea/issues/269
        codeArea = new IsolatedRSyntaxTextArea(20, 120);
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

        // Context toggle button
        contextToggle = new JToggleButton("API");
        contextToggle.setToolTipText("Toggle between API (VitaLite) and Client (obfuscated) classloader context");
        contextToggle.addActionListener(e -> {
            if (contextToggle.isSelected()) {
                contextToggle.setText("Client");
                switchContext(EvalContext.CLIENT);
            } else {
                contextToggle.setText("API");
                switchContext(EvalContext.API);
            }
        });

        buttonPanel.add(runButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(contextToggle);
        buttonPanel.add(alwaysOnTopCheckbox);

        codePanel.add(buttonPanel, BorderLayout.SOUTH);

        splitPane.setTopComponent(codePanel);
        splitPane.setBottomComponent(outputPanel);
        splitPane.setDividerLocation(500);

        contentPanel.add(splitPane, BorderLayout.CENTER);

        setAlwaysOnTop(true);

        // Initialize autocompletion in background
        setupAutoCompletion();

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Sets up IntelliJ-style autocompletion with dual-context support
     */
    private void setupAutoCompletion() {
        ThreadPool.submit(() -> {
            try {
                // Parse template imports ONCE - shared between contexts
                sharedImportScanner = new ImportScanner();
                InputStream templateStream = getClass().getResourceAsStream("code_template.java");
                if (templateStream != null) {
                    sharedImportScanner.parseTemplate(templateStream);
                    templateStream.close();
                }

                java.util.Set<String> packagesToScan = sharedImportScanner.getPackagesToScan();

                // Initialize API context (VitaLite API classes)
                ClassLoader apiClassLoader = GameManager.class.getClassLoader();
                apiContext = new CompletionContext("API", apiClassLoader, sharedImportScanner);
                apiContext.initializeAsync(packagesToScan, () -> {
                    if (currentContext == EvalContext.API) {
                        installCompletion(apiContext);
                    }
                });

                // Initialize Client context (obfuscated game client classes + template imports)
                ClassLoader clientClassLoader = Static.getClientClassLoader();
                if (clientClassLoader != null) {
                    clientContext = new CompletionContext("Client", clientClassLoader, sharedImportScanner);
                    clientContext.initializeAsync(packagesToScan, () -> {
                        // Also index root package classes (obfuscated classes like a.class, b.class)
                        clientContext.indexRootPackageClasses();
                        if (currentContext == EvalContext.CLIENT) {
                            installCompletion(clientContext);
                        }
                    });
                } else {
                    System.err.println("Client classloader not available yet");
                }

            } catch (Exception e) {
                System.err.println("Failed to initialize autocompletion: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void runCode() {
        CompletionContext ctx = getCurrentCompletionContext();
        if (ctx == null || !ctx.isInitialized()) {
            outputArea.append(">>> Context not initialized yet, please wait...\n");
            runButton.setText("Run Code (Ctrl+Enter)");
            return;
        }

        // Check for infinite loop warning
        String warning = ctx.getEvaluator().checkForInfiniteLoop(codeArea.getText());
        if (warning != null) {
            int result = JOptionPane.showOptionDialog(
                this,
                warning,
                "Infinite Loop Warning",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new String[]{"Run Anyway", "Cancel"},
                "Cancel"
            );

            if (result != JOptionPane.YES_OPTION) {
                outputArea.append(">>> Execution cancelled - infinite loop warning.\n");
                runButton.setText("Run Code (Ctrl+Enter)");
                return;
            }
        }

        future = ThreadPool.submit(() -> {
            outputArea.append(">>> Running code in " + currentContext + " context...\n");

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

                Object result = ctx.getEvaluator().evaluate(codeArea.getText());

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