package com.tonic.services.codeeval;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class CodeEvalFrame extends JFrame {
    private static CodeEvalFrame INSTANCE;
    private SimpleCodeEvaluator evaluator;
    private final JTextArea codeArea;
    private final JTextArea outputArea;

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
        this.evaluator = new SimpleCodeEvaluator(com.tonic.services.GameManager.class.getClassLoader());

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLayout(new BorderLayout());

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

        codeArea = new JTextArea(15, 60);
        codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        codeArea.setTabSize(4);

        // Load default code example from resources
        try {
            InputStream exampleStream = getClass().getResourceAsStream("/com/tonic/services/codeeval/default_example.java");
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

        outputArea = new JTextArea(10, 60);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        outputArea.setEditable(false);
        outputArea.setBackground(Color.BLACK);
        outputArea.setForeground(Color.GREEN);

        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton runButton = new JButton("Run Code (Ctrl+Enter)");
        runButton.addActionListener(e -> runCode());

        JButton clearButton = new JButton("Clear Output");
        clearButton.addActionListener(e -> outputArea.setText(""));

        buttonPanel.add(runButton);
        buttonPanel.add(clearButton);

        codePanel.add(buttonPanel, BorderLayout.SOUTH);

        // Assemble UI
        splitPane.setTopComponent(codePanel);
        splitPane.setBottomComponent(outputPanel);
        splitPane.setDividerLocation(400);

        add(splitPane, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    private void runCode() {
        outputArea.append(">>> Running code...\n");

        // Capture System.out
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
        }
    }
}