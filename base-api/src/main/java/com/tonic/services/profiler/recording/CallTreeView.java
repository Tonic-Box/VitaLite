package com.tonic.services.profiler.recording;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

import static com.tonic.services.profiler.ProfilerTheme.*;
import static com.tonic.services.profiler.recording.RecordingViews.*;

/**
 * The recording as a tree, either top-down from the outermost frames or bottom-up from the running methods to their
 * callers; nodes are built as they are expanded
 */
final class CallTreeView implements ResultsView {
    private static final String TOP_DOWN = "Call tree (outermost first)";
    private static final String BOTTOM_UP = "Callers (running methods first)";

    private final JComboBox<String> mode = new JComboBox<>(new String[]{TOP_DOWN, BOTTOM_UP});
    private final DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode("No recording"));
    private final JTree tree = new JTree(model);
    private final JLabel hint = label("Each node: share of samples on that path, and the share running there.");
    private final JPanel panel = viewPanel();
    private MethodTimingResults results;

    /**
     * Creates the view
     */
    CallTreeView() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        mode.setBackground(PANEL);
        mode.setForeground(TEXT);
        mode.addActionListener(e -> rebuild());
        top.add(mode);
        top.add(hint);
        panel.add(top, BorderLayout.NORTH);

        tree.setBackground(PANEL);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setBackgroundNonSelectionColor(PANEL);
        renderer.setTextNonSelectionColor(TEXT);
        renderer.setBackgroundSelectionColor(ACCENT.darker());
        renderer.setTextSelectionColor(Color.WHITE);
        renderer.setFont(MONO);
        tree.setCellRenderer(renderer);
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) {
                Object last = event.getPath().getLastPathComponent();
                if (last instanceof Lazy) {
                    ((Lazy) last).load(model);
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {
            }
        });
        panel.add(scroll(tree), BorderLayout.CENTER);
    }

    @Override
    public String title() {
        return "Call Tree";
    }

    @Override
    public JComponent component() {
        return panel;
    }

    @Override
    public void show(MethodTimingResults results) {
        this.results = results;
        rebuild();
    }

    private void rebuild() {
        if (results == null || results.callTreeRoot.getTotalSamples() == 0) {
            model.setRoot(new DefaultMutableTreeNode("No recording"));
            return;
        }
        CallTreeNode root = TOP_DOWN.equals(mode.getSelectedItem()) ? results.callTreeRoot : results.callersRoot;
        Lazy top = new Lazy(root, root.getTotalSamples());
        top.load(model);
        model.setRoot(top);
        for (int row = 0; row < Math.min(tree.getRowCount(), 3); row++) {
            tree.expandRow(row);
        }
    }

    private static final class Lazy extends DefaultMutableTreeNode {
        private final CallTreeNode node;
        private final int recordingSamples;
        private boolean loaded;

        Lazy(CallTreeNode node, int recordingSamples) {
            super(node);
            this.node = node;
            this.recordingSamples = recordingSamples;
            if (node.hasChildren()) {
                add(new DefaultMutableTreeNode("..."));
            }
        }

        void load(DefaultTreeModel model) {
            if (loaded) {
                return;
            }
            loaded = true;
            removeAllChildren();
            for (CallTreeNode child : node.getChildrenSorted()) {
                add(new Lazy(child, recordingSamples));
            }
            model.nodeStructureChanged(this);
        }

        @Override
        public String toString() {
            return String.format("%s  %.1f%%  (running %.1f%%)", node.getFullName(), percent(node.getTotalSamples(), recordingSamples), percent(node.getSelfSamples(), recordingSamples));
        }
    }
}
