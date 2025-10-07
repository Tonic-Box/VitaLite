package com.tonic.services.codeeval;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

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
        // Initialize/refresh the evaluator with current classloader context
        refreshContext();
        setVisible(true);
        toFront();
        repaint();
    }

    public void close() {
        setVisible(false);
        // Clear any sensitive output
        clearOutput();
    }

    private void refreshContext() {
        // Get the RLClassLoader from a class that we know is loaded by it
        // GameManager is loaded by RLClassLoader and has access to RuneLite classes
        ClassLoader rlClassLoader = com.tonic.services.GameManager.class.getClassLoader();
        outputArea.append(">>> Using RLClassLoader: " + rlClassLoader.getClass().getName() + "\n");
        outputArea.append(">>> RLClassLoader toString: " + rlClassLoader.toString() + "\n");
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

        // Initialize evaluator with RLClassLoader - will be refreshed on each open() call
        this.evaluator = new SimpleCodeEvaluator(GameManager.class.getClassLoader());

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLayout(new BorderLayout());

        final BufferedImage iconImage = ImageUtil.loadImageResource(CodeEvalFrame.class, "jshell.png");
        setIconImage(iconImage);

        // Add window listener to handle cleanup when window is closed
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                close();
            }
        });

        // Create split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Code input area
        JPanel codePanel = new JPanel(new BorderLayout());
        codePanel.setBorder(BorderFactory.createTitledBorder("Code (Ctrl+Enter to run)"));

        codeArea = new RSyntaxTextArea(20, 120);
        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        codeArea.setCodeFoldingEnabled(true);
        codeArea.setAntiAliasingEnabled(true);
        codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        codeArea.setTabSize(4);

        // Apply dark theme
        try {
            Theme theme = Theme.load(getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
            theme.apply(codeArea);
        } catch (Exception e) {
            // Fallback to manual dark theme
            codeArea.setBackground(new Color(40, 40, 40));
            codeArea.setForeground(Color.WHITE);
            codeArea.setCurrentLineHighlightColor(new Color(50, 50, 50));
        }

        // Setup basic autocompletion
        setupAutoCompletion();

        // Load default code example from resources
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

        // Ctrl+Enter to run
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

        // Output area
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("Output"));

        outputArea = new JTextArea(20, 120);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        outputArea.setEditable(false);
        outputArea.setBackground(Color.BLACK);
        outputArea.setForeground(Color.GREEN);

        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        runButton = new JButton("Run Code (Ctrl+Enter)");
        runButton.addActionListener(e -> runCode());

        JButton clearButton = new JButton("Clear Output");
        clearButton.addActionListener(e -> outputArea.setText(""));

        JCheckBox alwaysOnTopCheckbox = new JCheckBox("Always on Top", true);
        alwaysOnTopCheckbox.addActionListener(e -> setAlwaysOnTop(alwaysOnTopCheckbox.isSelected()));

        buttonPanel.add(runButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(alwaysOnTopCheckbox);

        codePanel.add(buttonPanel, BorderLayout.SOUTH);

        // Assemble UI
        splitPane.setTopComponent(codePanel);
        splitPane.setBottomComponent(outputPanel);
        splitPane.setDividerLocation(500);

        add(splitPane, BorderLayout.CENTER);

        // Set always on top by default
        setAlwaysOnTop(true);

        pack();
        setLocationRelativeTo(null);
    }

    private void setupAutoCompletion() {
        // Create a simple completion provider with basic completions
        DefaultCompletionProvider provider = new DefaultCompletionProvider();

        // Add basic Java completions
        provider.addCompletion(new BasicCompletion(provider, "System.out.println"));
        provider.addCompletion(new BasicCompletion(provider, "out.println"));

        // Add VitaLite-specific completions
        provider.addCompletion(new BasicCompletion(provider, "client()", "Get RuneLite Client instance"));
        provider.addCompletion(new BasicCompletion(provider, "inject(\"className\")", "Inject class via Guice"));
        provider.addCompletion(new BasicCompletion(provider, "loadClass(\"className\")", "Load class dynamically"));

        // Add GameManager static methods
        provider.addCompletion(new BasicCompletion(provider, "GameManager.playerList()", "Get list of players"));
        provider.addCompletion(new BasicCompletion(provider, "GameManager.npcList()", "Get list of NPCs"));
        provider.addCompletion(new BasicCompletion(provider, "GameManager.objectList()", "Get list of objects"));
        provider.addCompletion(new BasicCompletion(provider, "GameManager.tileItemList()", "Get list of ground items"));

        // Add Static utilities
        provider.addCompletion(new BasicCompletion(provider, "Static.getClient()", "Get RuneLite client"));
        provider.addCompletion(new BasicCompletion(provider, "Static.getInjector()", "Get Guice injector"));

        // Create and install the auto completion
        AutoCompletion ac = new AutoCompletion(provider);
        ac.setAutoCompleteEnabled(true);
        ac.setAutoActivationEnabled(true);
        ac.setAutoActivationDelay(300);
        ac.setShowDescWindow(true);
        ac.install(codeArea);

        // Set trigger key
        try {
            ac.setTriggerKey(KeyStroke.getKeyStroke("ctrl SPACE"));
        } catch (Exception e) {
            // Fallback trigger
        }
    }

    private void runCode() {
        runButton.setEnabled(false);
        ThreadPool.submit(() -> {
            outputArea.append(">>> Running code...\n");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;

            try {
                System.setOut(new PrintStream(baos));
                System.setErr(new PrintStream(baos));

                Object result = evaluator.evaluate(codeArea.getText());

                if (result != null) {
                    System.out.println("Result: " + result);
                }

            } finally {
                System.setOut(originalOut);
                System.setErr(originalErr);

                String output = baos.toString();
                if (!output.trim().isEmpty()) {
                    outputArea.append(output);
                }
                outputArea.append(">>> Done.\n\n");
                outputArea.setCaretPosition(outputArea.getDocument().getLength());
                runButton.setEnabled(true);
            }
        });
    }

    public static void install()
    {
        final BufferedImage iconImage = ImageUtil.loadImageResource(CodeEvalFrame.class, "jshell.png");
        final NavigationButton titleBarButton = NavigationButton.builder()
                .tooltip("JavaShell")
                .icon(iconImage)
                .onClick(() -> CodeEvalFrame.get().toggle())
                .build();

        ClientToolbar clientToolbar = Static.getInjector().getInstance(ClientToolbar.class);
        clientToolbar.addNavigation(titleBarButton);
    }
}