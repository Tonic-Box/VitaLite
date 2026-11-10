package com.tonic.services.profiler.recording;

import com.tonic.services.profiler.ProfilerTab;
import com.tonic.services.profiler.ProfilerTheme;
import com.tonic.util.ThreadPool;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.tonic.services.profiler.ProfilerTheme.*;

/**
 * Java Flight Recorder sampling: records what the client runs, what blocks it and optionally what it allocates, then
 * shows the recording as tables, trees and a flame graph
 */
public final class RecordingTab implements ProfilerTab {
    private static final int[] PERIODS = {1, 5, 10, 20, 50};

    private final JFRMethodRecorder recorder = new JFRMethodRecorder();
    private final JButton record = button("Record", SUCCESS);
    private final JButton stop = button("Stop", ProfilerTheme.ERROR);
    private final JButton export = button("Export CSV", ACCENT);
    private final JComboBox<String> period = new JComboBox<>(new String[]{"1 ms", "5 ms", "10 ms", "20 ms", "50 ms"});
    private final JCheckBox allocations = new JCheckBox("Record allocations");
    private final JRadioButton allMethods = new JRadioButton("All", true);
    private final JRadioButton packageScope = new JRadioButton("Package:");
    private final JRadioButton classScope = new JRadioButton("Class:");
    private final JTextField packageField = field(new JTextField("com.tonic", 18));
    private final JTextField classField = field(new JTextField(22));
    private final JLabel status = label("Ready");
    private final JTabbedPane viewTabs = new JTabbedPane();
    private final MethodDetailsView details = new MethodDetailsView();
    private final ExactTimingView exactTiming = new ExactTimingView();
    private final List<ResultsView> views;
    private final Timer elapsed = new Timer(500, e -> status.setText(String.format("Recording %.1f s", recorder.getRecordingDurationMs() / 1000.0)));
    private final JPanel panel;
    private MethodTimingResults results;

    /**
     * Creates the tab
     */
    public RecordingTab() {
        views = Arrays.asList(new MethodsView(details::select), details, new CallTreeView(), new FlameView(), new AllocationsView(), new NativeView(), exactTiming);
        viewTabs.setBackground(PANEL);
        viewTabs.setForeground(TEXT);
        for (ResultsView view : views) {
            viewTabs.addTab(view.title(), view.component());
        }
        viewTabs.addChangeListener(e -> {
            if (viewTabs.getSelectedComponent() == exactTiming.component()) {
                exactTiming.refresh();
            }
        });

        panel = page(new BorderLayout(0, 8));
        panel.add(controls(), BorderLayout.NORTH);
        panel.add(viewTabs, BorderLayout.CENTER);
    }

    @Override
    public String title() {
        return "Recording";
    }

    @Override
    public JComponent component() {
        return panel;
    }

    @Override
    public void refresh() {
        if (viewTabs.getSelectedComponent() == exactTiming.component()) {
            exactTiming.refresh();
        }
    }

    @Override
    public void dispose() {
        elapsed.stop();
        ThreadPool.submit(recorder::discard);
    }

    private JPanel controls() {
        JPanel controls = new JPanel(new GridLayout(2, 1, 0, 6));
        controls.setOpaque(false);

        JPanel recording = row();
        stop.setEnabled(false);
        record.addActionListener(e -> start());
        stop.addActionListener(e -> stopAndAnalyze());
        recording.add(record);
        recording.add(stop);
        recording.add(label("Sample every:"));
        period.setSelectedIndex(2);
        period.setBackground(PANEL);
        period.setForeground(TEXT);
        recording.add(period);
        allocations.setOpaque(false);
        allocations.setForeground(TEXT);
        allocations.setToolTipText("Adds allocation events with their stacks; costs noticeably more than sampling alone");
        recording.add(allocations);
        export.setEnabled(false);
        export.addActionListener(e -> export());
        recording.add(export);
        status.setFont(UI_BOLD);
        recording.add(status);
        controls.add(recording);

        JPanel scope = row();
        scope.add(label("Show:"));
        ButtonGroup group = new ButtonGroup();
        for (JRadioButton button : new JRadioButton[]{allMethods, packageScope, classScope}) {
            button.setOpaque(false);
            button.setForeground(TEXT);
            button.addActionListener(e -> updateScopeFields());
            group.add(button);
        }
        scope.add(allMethods);
        scope.add(packageScope);
        scope.add(packageField);
        scope.add(classScope);
        scope.add(classField);
        updateScopeFields();
        controls.add(scope);
        return controls;
    }

    private void start() {
        try {
            recorder.setSamplePeriodMs(PERIODS[period.getSelectedIndex()]);
            recorder.setFilterMode(packageScope.isSelected() ? JFRMethodRecorder.FilterMode.PACKAGE : classScope.isSelected() ? JFRMethodRecorder.FilterMode.CLASS : JFRMethodRecorder.FilterMode.ALL);
            recorder.setPackageFilter(packageField.getText());
            recorder.setClassFilter(classField.getText());
            recorder.setRecordAllocations(allocations.isSelected());
            recorder.startRecording();
        } catch (Exception e) {
            status.setText("Could not start: " + e.getMessage());
            return;
        }
        setRecording(true);
        elapsed.start();
    }

    private void stopAndAnalyze() {
        elapsed.stop();
        stop.setEnabled(false);
        status.setText("Analyzing...");
        ThreadPool.submit(() -> {
            MethodTimingResults analyzed = null;
            String failure = null;
            try {
                recorder.stopRecording();
                analyzed = recorder.analyze();
            } catch (Exception e) {
                failure = e.getMessage();
            }
            MethodTimingResults done = analyzed;
            String error = failure;
            SwingUtilities.invokeLater(() -> {
                setRecording(false);
                if (error != null) {
                    status.setText("Analysis failed: " + error);
                    return;
                }
                results = done;
                for (ResultsView view : views) {
                    view.show(done);
                }
                export.setEnabled(done.hasData());
                status.setText(String.format("%,d Java samples, %,d native samples in %s", done.totalSamples, done.nativeSamples, done.getFormattedDuration()));
            });
        });
    }

    private void setRecording(boolean recording) {
        record.setEnabled(!recording);
        stop.setEnabled(recording);
        period.setEnabled(!recording);
        allocations.setEnabled(!recording);
        allMethods.setEnabled(!recording);
        packageScope.setEnabled(!recording);
        classScope.setEnabled(!recording);
        if (recording) {
            packageField.setEnabled(false);
            classField.setEnabled(false);
        } else {
            updateScopeFields();
        }
    }

    private void updateScopeFields() {
        packageField.setEnabled(packageScope.isSelected());
        classField.setEnabled(classScope.isSelected());
    }

    private void export() {
        if (results == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("recording_" + System.currentTimeMillis() + ".csv"));
        if (chooser.showSaveDialog(panel) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try (PrintWriter writer = new PrintWriter(chooser.getSelectedFile())) {
            writer.println("Method,SelfSamples,SelfPercent,TotalSamples,TotalPercent,SamplePeriodMs");
            for (MethodStats method : results.getAllMethodsBySelfTime()) {
                writer.printf(Locale.ROOT, "\"%s\",%d,%.2f,%d,%.2f,%d%n", method.methodKey, method.getSelfSamples(), RecordingViews.percent(method.getSelfSamples(), results.totalSamples), method.getTotalSamples(), RecordingViews.percent(method.getTotalSamples(), results.totalSamples), results.samplePeriodMs);
            }
            status.setText("Exported " + chooser.getSelectedFile().getName());
        } catch (Exception e) {
            status.setText("Export failed: " + e.getMessage());
        }
    }

    private static JPanel row() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        return row;
    }
}
