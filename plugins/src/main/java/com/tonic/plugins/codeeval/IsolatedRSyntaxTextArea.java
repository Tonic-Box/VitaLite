package com.tonic.plugins.codeeval;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * RSyntaxTextArea subclass that uses isolated keymaps to prevent conflicts
 * with other RSyntaxTextArea instances loaded by different classloaders.
 *
 * This fixes a known bug where having multiple RSyntaxTextArea instances
 * from different plugins causes all text areas to become read-only.
 *
 * @see <a href="https://github.com/bobbylight/RSyntaxTextArea/issues/269">GitHub Issue #269</a>
 */
public class IsolatedRSyntaxTextArea extends RSyntaxTextArea {

    private static int instanceCounter = 0;
    private final String instanceId;

    public IsolatedRSyntaxTextArea(int rows, int cols) {
        super(rows, cols);
        this.instanceId = "CodeEval_RSTA_" + (++instanceCounter);
        installIsolatedKeymap();
    }

    /**
     * Installs an isolated keymap for this instance that won't conflict
     * with other RSyntaxTextArea instances.
     */
    private void installIsolatedKeymap() {
        // Get the current keymap as parent (contains all RSyntaxTextArea bindings)
        Keymap parent = getKeymap();

        // Create a unique keymap for this instance with parent's bindings as fallback
        Keymap isolatedKeymap = JTextComponent.addKeymap(instanceId + "_keymap", parent);

        // Copy all key bindings from parent to our isolated keymap
        KeyStroke[] boundKeys = parent.getBoundKeyStrokes();
        if (boundKeys != null) {
            for (KeyStroke ks : boundKeys) {
                Action action = parent.getAction(ks);
                if (action != null) {
                    isolatedKeymap.addActionForKeyStroke(ks, action);
                }
            }
        }

        // Set default typing action that handles ALL printable characters
        isolatedKeymap.setDefaultAction(new DefaultKeyTypedAction());

        setKeymap(isolatedKeymap);
    }

    /**
     * Default action for key typed events - inserts the typed character.
     */
    private class DefaultKeyTypedAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            JTextComponent target = IsolatedRSyntaxTextArea.this;
            if (target == null || !target.isEditable() || !target.isEnabled()) {
                return;
            }

            String content = e.getActionCommand();
            if (content == null || content.isEmpty()) {
                return;
            }

            char c = content.charAt(0);

            // Filter out control characters but allow all printable ones
            // Control chars are 0x00-0x1F and 0x7F (DEL)
            // We want to allow everything >= 0x20 except 0x7F
            if (c >= 0x20 && c != 0x7F) {
                target.replaceSelection(content);
            }
        }
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // Re-install isolated keymap after UI update to prevent it from being overwritten
        if (instanceId != null) {
            SwingUtilities.invokeLater(this::installIsolatedKeymap);
        }
    }
}
