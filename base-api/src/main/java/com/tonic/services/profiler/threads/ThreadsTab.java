package com.tonic.services.profiler.threads;

import com.tonic.services.profiler.ProfilerTab;

import javax.swing.*;
import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import static com.tonic.services.profiler.ProfilerTheme.*;

/**
 * The live threads and the stack of the selected one
 */
public final class ThreadsTab implements ProfilerTab {
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final DefaultListModel<String> threadModel = new DefaultListModel<>();
    private final JList<String> threadList = new JList<>(threadModel);
    private final JTextArea stack = textArea(MONO);
    private final JPanel panel;

    /**
     * Creates the tab
     */
    public ThreadsTab() {
        panel = page(new BorderLayout(10, 10));

        JPanel listPanel = section("Active Threads");
        listPanel.setLayout(new BorderLayout());
        threadList.setBackground(PANEL);
        threadList.setForeground(TEXT);
        threadList.setFont(new Font("Consolas", Font.PLAIN, 12));
        threadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        threadList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showStack();
            }
        });
        JScrollPane listScroll = scroll(threadList);
        listScroll.setPreferredSize(new Dimension(250, 0));
        listPanel.add(listScroll, BorderLayout.CENTER);
        JButton refresh = button("Refresh", ACCENT);
        refresh.addActionListener(e -> refresh());
        listPanel.add(refresh, BorderLayout.SOUTH);

        JPanel stackPanel = section("Thread Stack Trace");
        stackPanel.setLayout(new BorderLayout());
        stackPanel.add(scroll(stack), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, stackPanel);
        split.setDividerLocation(300);
        split.setBackground(BACKGROUND);
        panel.add(split, BorderLayout.CENTER);
    }

    @Override
    public String title() {
        return "Threads";
    }

    @Override
    public JComponent component() {
        return panel;
    }

    @Override
    public void shown() {
        refresh();
    }

    @Override
    public void refresh() {
        threadModel.clear();
        for (long id : threadBean.getAllThreadIds()) {
            ThreadInfo info = threadBean.getThreadInfo(id);
            if (info != null) {
                threadModel.addElement(String.format("[%d] %s - %s", info.getThreadId(), info.getThreadName(), info.getThreadState()));
            }
        }
    }

    private void showStack() {
        String selected = threadList.getSelectedValue();
        if (selected == null || selected.indexOf(']') < 0) {
            return;
        }
        long id = Long.parseLong(selected.substring(1, selected.indexOf(']')));
        ThreadInfo info = threadBean.getThreadInfo(id, Integer.MAX_VALUE);
        if (info == null) {
            stack.setText("Thread not found or terminated");
            return;
        }
        StringBuilder text = new StringBuilder();
        text.append("Thread: ").append(info.getThreadName()).append('\n');
        text.append("ID: ").append(info.getThreadId()).append('\n');
        text.append("State: ").append(info.getThreadState()).append('\n');
        text.append("Blocked Count: ").append(info.getBlockedCount()).append("\n\n");
        text.append("Stack Trace:\n");
        for (StackTraceElement element : info.getStackTrace()) {
            text.append("  at ").append(element).append('\n');
        }
        setText(stack, text.toString());
    }
}
