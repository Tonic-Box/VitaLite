package com.tonic.plugins.profiles.panel;

import com.tonic.plugins.profiles.jagex.JagexLogin;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * The modal wait while the user signs in to Jagex in their browser: shows the sign-in link in case the
 * browser did not open, offers Cancel, and once the sign-in returns runs the step that uses its id token.
 */
public class JagexSignInDialog extends JDialog {
    private static final Duration TIMEOUT = Duration.ofMinutes(2);

    /**
     * What to do with the id token a sign-in returned; runs off the event thread.
     */
    @FunctionalInterface
    public interface SignInStep<T> {
        T apply(String idToken) throws Exception;
    }

    private final JagexLogin login = new JagexLogin();
    private final JLabel message = new JLabel("<html>Sign in to your Jagex account in the browser window that opened.</html>");
    private final JButton cancel = new JButton("Cancel");

    private JagexSignInDialog(Component parent, String title) {
        super(SwingUtilities.getWindowAncestor(parent), title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (cancel.isEnabled()) {
                    cancel.doClick();
                }
            }
        });

        JTextField link = new JTextField(login.getAuthorizeUrl());
        link.setEditable(false);
        link.setColumns(28);
        JButton copy = new JButton("Copy link");
        copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(login.getAuthorizeUrl()), null));
        cancel.addActionListener(e -> {
            cancel.setEnabled(false);
            message.setText("<html>Cancelling...</html>");
            login.cancel();
        });

        JPanel linkRow = new JPanel(new BorderLayout(4, 0));
        linkRow.add(link, BorderLayout.CENTER);
        linkRow.add(copy, BorderLayout.EAST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.add(cancel);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(message, BorderLayout.NORTH);
        content.add(linkRow, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Runs a browser sign-in behind the dialog, then the step with its id token. Both outcomes are reported
     * on the event thread after the dialog has closed.
     *
     * @param parent the component the dialog is centred on
     * @param title the dialog's title
     * @param step what to do with the id token
     * @param onSuccess receives the step's result
     * @param onFailure receives why the sign-in or the step failed, including a cancel
     * @param <T> the step's result
     */
    public static <T> void run(Component parent, String title, SignInStep<T> step, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        JagexSignInDialog dialog = new JagexSignInDialog(parent, title);
        SwingWorker<T, String> worker = new SwingWorker<T, String>() {
            @Override
            protected T doInBackground() throws Exception {
                String idToken = dialog.login.await(this::openBrowser, TIMEOUT);
                publish("Signed in, loading the account's characters...");
                return step.apply(idToken);
            }

            private void openBrowser(String url) {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Exception | LinkageError e) {
                    publish("Couldn't open the browser, open the link below in it.");
                }
            }

            @Override
            protected void process(List<String> messages) {
                String latest = messages.get(messages.size() - 1);
                dialog.message.setText("<html>" + latest + "</html>");
                if (latest.startsWith("Signed in")) {
                    dialog.cancel.setEnabled(false);
                }
            }

            @Override
            protected void done() {
                dialog.dispose();
                try {
                    onSuccess.accept(get());
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    onFailure.accept(cause instanceof Exception ? (Exception) cause : new Exception(cause));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    onFailure.accept(e);
                }
            }
        };
        worker.execute();
        dialog.setVisible(true);
    }
}
