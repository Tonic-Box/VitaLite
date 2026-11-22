package com.tonic.services.streamer;

import com.formdev.flatlaf.ui.FlatNativeWindowBorder;
import com.formdev.flatlaf.ui.FlatTitlePane;
import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.services.profiler.frames.FrameProfiler;
import com.tonic.util.ReflectBuilder;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.ColorScheme;

import javax.swing.Box;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The mirror: a window that never takes focus by itself, drawn as stock RuneLite's window would be, with the game frame at the real canvas's place, the stock sidebar and title bar buttons, and the real pointer drawn over it, title bar included. It is drawn pixel for pixel at the client's size, following the client live as it resizes, and cannot be resized or maximized by hand, so nothing is ever resampled. Its maximize button is still shown and lights up as stock RuneLite's does, but does nothing.
 */
final class StreamerMirrorWindow
{
    private static final int SYNC_MILLIS = 500;
    private static final int SIDEBAR_MILLIS = 66;
    private static final int PAINT = FrameProfiler.timer("mirror.edtPaint");

    private static Method titleActiveChanged;

    private final Client client = Static.getClient();
    private final RuneLiteConfig config = Static.getInjector().getInstance(RuneLiteConfig.class);
    private final JFrame realFrame;
    private final JPanel realContent;
    private final Component realGame;
    private final String baseTitle;
    private final boolean customChrome;
    private final JFrame frame = new JFrame();
    private final View view = new View();
    private final CursorLayer cursorLayer = new CursorLayer();
    private final WindowAdapter realActivity = new WindowAdapter()
    {
        @Override
        public void windowActivated(WindowEvent event)
        {
            SwingUtilities.invokeLater(StreamerMirrorWindow.this::matchTitleActive);
        }

        @Override
        public void windowDeactivated(WindowEvent event)
        {
            SwingUtilities.invokeLater(StreamerMirrorWindow.this::matchTitleActive);
        }
    };
    private final MirrorSidebar sidebar;
    private final Timer sync = new Timer(SYNC_MILLIS, event -> sync());
    private final Timer sidebarTicks = new Timer(SIDEBAR_MILLIS, event -> updateSidebar());
    private final ComponentListener sourceWatcher = onResize(this::sourceChanged);
    private final AtomicBoolean paintQueued = new AtomicBoolean();
    private final List<Runnable> titleButtonUnlinks = new ArrayList<>();
    private Dimension source;
    private Rectangle cursorArea;
    private Image cursorImage;

    StreamerMirrorWindow()
    {
        ClientUI clientUI = Static.getInjector().getInstance(ClientUI.class);
        realFrame = ReflectBuilder.of(clientUI).field("frame").get();
        realContent = ReflectBuilder.of(clientUI).field("content").get();
        realGame = ReflectBuilder.of(clientUI).field("clientPanel").get();
        baseTitle = ReflectBuilder.of(clientUI).field("title").get();
        customChrome = ReflectBuilder.of(clientUI).field("withTitleBar").get();
        sidebar = new MirrorSidebar(clientUI, customChrome);
    }

    /**
     * Shows the mirror beside the client.
     */
    void open()
    {
        frame.setTitle(title());
        frame.setIconImages(Arrays.asList(ClientUI.ICON_128, ClientUI.ICON_16));
        frame.setAutoRequestFocus(false);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent event)
            {
                StreamerMode.set(false);
            }
        });
        if (customChrome)
        {
            JMenuBar menuBar = new JMenuBar();
            menuBar.add(Box.createGlue());
            menuBar.add(sidebar.toolbar());
            frame.setJMenuBar(menuBar);
            JRootPane rootPane = frame.getRootPane();
            if (FlatNativeWindowBorder.isSupported())
            {
                rootPane.putClientProperty("JRootPane.useWindowDecorations", true);
            }
            else
            {
                frame.setUndecorated(true);
                rootPane.setWindowDecorationStyle(JRootPane.FRAME);
            }
        }
        view.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        source = mirrorSize();
        view.setPreferredSize(source);
        frame.setContentPane(view);
        frame.getLayeredPane().add(cursorLayer, JLayeredPane.DRAG_LAYER);
        frame.setResizable(false);
        frame.pack();
        placeOnScreen();
        frame.setVisible(true);
        MirrorConfiguration.startTracking();
        realContent.addComponentListener(sourceWatcher);
        realGame.addComponentListener(sourceWatcher);
        frame.getLayeredPane().addComponentListener(onResize(this::coverWindow));
        coverWindow();
        realFrame.addWindowListener(realActivity);
        frame.addWindowListener(realActivity);
        frame.addWindowStateListener(event -> stayUnmaximized(event.getNewState()));
        matchTitleActive();
        keepMaximizeButton();
        followTitleButtons();
        updateSidebar();
        sync.start();
        sidebarTicks.start();
    }

    /**
     * Closes the mirror.
     */
    void close()
    {
        MirrorConfiguration.stopTracking();
        sync.stop();
        sidebarTicks.stop();
        realFrame.removeWindowListener(realActivity);
        titleButtonUnlinks.forEach(Runnable::run);
        titleButtonUnlinks.clear();
        realContent.removeComponentListener(sourceWatcher);
        realGame.removeComponentListener(sourceWatcher);
        frame.dispose();
    }

    /**
     * Asks for the mirror's game area to be painted with the newest frame, unless a paint is already waiting. Called on the client thread.
     */
    void frameReady()
    {
        if (paintQueued.compareAndSet(false, true))
        {
            SwingUtilities.invokeLater(this::paintFrame);
        }
    }

    private void paintFrame()
    {
        paintQueued.set(false);
        if (minimized())
        {
            return;
        }
        Component realLayers = realFrame.getLayeredPane();
        moveCursor(realPointer(realLayers), realContentIn(realLayers), realLayers);
        view.paintImmediately(inView(realGame.getBounds()));
    }

    private boolean minimized()
    {
        return !frame.isShowing() || (frame.getExtendedState() & Frame.ICONIFIED) != 0;
    }

    private Rectangle inView(Rectangle contentArea)
    {
        Dimension size = mirrorSize();
        contentArea.translate(offset(view.getWidth(), size.width), offset(view.getHeight(), size.height));
        return contentArea;
    }

    private void moveCursor(Point real, Rectangle content, Component realLayers)
    {
        Point mirror = real == null ? null : mirrorPoint(real, content, realLayers);
        Image image = StreamerCursor.current();
        Rectangle area = mirror == null || image == null ? null : new Rectangle(mirror.x, mirror.y, image.getWidth(null), image.getHeight(null));
        if (image == cursorImage && Objects.equals(area, cursorArea))
        {
            return;
        }
        if (cursorArea != null)
        {
            cursorLayer.repaint(cursorArea);
        }
        cursorArea = area;
        cursorImage = image;
        if (area != null)
        {
            cursorLayer.repaint(area);
        }
    }

    private void coverWindow()
    {
        cursorLayer.setBounds(0, 0, frame.getLayeredPane().getWidth(), frame.getLayeredPane().getHeight());
    }

    private void updateSidebar()
    {
        if (minimized())
        {
            return;
        }
        try
        {
            Point contentPointer = null;
            Point titlePointer = null;
            Component realLayers = realFrame.getLayeredPane();
            Point real = realPointer(realLayers);
            Rectangle content = realContentIn(realLayers);
            if (real != null)
            {
                if (real.y < content.y)
                {
                    titlePointer = titlePoint(real, realLayers);
                }
                else
                {
                    contentPointer = new Point(real.x - content.x, real.y - content.y);
                }
            }
            if (sidebar.update(contentPointer, titlePointer, frame.getLayeredPane()))
            {
                if (sidebar.image() == null)
                {
                    view.repaint();
                }
                else
                {
                    view.repaint(inView(sidebar.bounds()));
                }
            }
            moveCursor(real, content, realLayers);
        }
        catch (RuntimeException e)
        {
            sidebarTicks.stop();
            Logger.error("Streamer mirror stopped updating its sidebar: " + e);
        }
    }

    private Point realPointer(Component realLayers)
    {
        PointerInfo pointer = MouseInfo.getPointerInfo();
        if (pointer == null || !realFrame.isShowing())
        {
            return null;
        }
        Point real = pointer.getLocation();
        SwingUtilities.convertPointFromScreen(real, realLayers);
        return realLayers.contains(real) ? real : null;
    }

    private Rectangle realContentIn(Component realLayers)
    {
        return SwingUtilities.convertRectangle(realContent.getParent(), realContent.getBounds(), realLayers);
    }

    private Point titlePoint(Point real, Component realLayers)
    {
        Component mirrorLayers = frame.getLayeredPane();
        int x = real.x < realLayers.getWidth() / 2 ? real.x : mirrorLayers.getWidth() - (realLayers.getWidth() - real.x);
        return new Point(x, real.y);
    }

    private void matchTitleActive()
    {
        FlatTitlePane titlePane = titlePane(frame.getLayeredPane());
        if (titlePane == null)
        {
            return;
        }
        try
        {
            if (titleActiveChanged == null)
            {
                titleActiveChanged = Members.method(FlatTitlePane.class, "activeChanged", boolean.class);
            }
            titleActiveChanged.invoke(titlePane, realFrame.isActive());
        }
        catch (ReflectiveOperationException ignored)
        {
        }
    }

    private void stayUnmaximized(int state)
    {
        if ((state & JFrame.MAXIMIZED_BOTH) != 0)
        {
            frame.setExtendedState(state & ~JFrame.MAXIMIZED_BOTH);
        }
    }

    private void keepMaximizeButton()
    {
        FlatTitlePane titlePane = titlePane(frame.getLayeredPane());
        if (titlePane == null)
        {
            return;
        }
        try
        {
            JButton maximize = (JButton) Members.field(FlatTitlePane.class, "maximizeButton").get(titlePane);
            for (ActionListener action : maximize.getActionListeners())
            {
                maximize.removeActionListener(action);
            }
            maximize.setVisible(true);
            maximize.addComponentListener(new ComponentAdapter()
            {
                @Override
                public void componentHidden(ComponentEvent event)
                {
                    SwingUtilities.invokeLater(() -> maximize.setVisible(true));
                }
            });
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            Logger.error("Streamer mirror could not show its maximize button: " + e);
        }
    }

    private void followTitleButtons()
    {
        FlatTitlePane realPane = titlePane(realFrame.getLayeredPane());
        FlatTitlePane mirrorPane = titlePane(frame.getLayeredPane());
        if (realPane == null || mirrorPane == null)
        {
            return;
        }
        try
        {
            followRollover(realPane, "iconifyButton", mirrorPane, "iconifyButton");
            followRollover(realPane, "maximizeButton", mirrorPane, "maximizeButton");
            followRollover(realPane, "restoreButton", mirrorPane, "maximizeButton");
            followRollover(realPane, "closeButton", mirrorPane, "closeButton");
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            Logger.error("Streamer mirror could not follow the title bar buttons' hover: " + e);
        }
    }

    private void followRollover(FlatTitlePane realPane, String realButton, FlatTitlePane mirrorPane, String mirrorButton) throws ReflectiveOperationException
    {
        ButtonModel real = ((JButton) Members.field(FlatTitlePane.class, realButton).get(realPane)).getModel();
        ButtonModel mirror = ((JButton) Members.field(FlatTitlePane.class, mirrorButton).get(mirrorPane)).getModel();
        ChangeListener follow = event -> mirror.setRollover(real.isRollover());
        real.addChangeListener(follow);
        titleButtonUnlinks.add(() -> real.removeChangeListener(follow));
    }

    private static FlatTitlePane titlePane(Container container)
    {
        for (Component child : container.getComponents())
        {
            if (child instanceof FlatTitlePane)
            {
                return (FlatTitlePane) child;
            }
            if (child instanceof Container)
            {
                FlatTitlePane found = titlePane((Container) child);
                if (found != null)
                {
                    return found;
                }
            }
        }
        return null;
    }

    private void sync()
    {
        matchTitleActive();
        String title = title();
        if (!Objects.equals(title, frame.getTitle()))
        {
            frame.setTitle(title);
        }
    }

    private void sourceChanged()
    {
        Dimension next = mirrorSize();
        if (next.width <= 0 || next.height <= 0 || next.equals(source))
        {
            return;
        }
        source = next;
        view.setPreferredSize(next);
        frame.pack();
        view.repaint();
    }

    private static ComponentListener onResize(Runnable action)
    {
        return new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent event)
            {
                action.run();
            }
        };
    }

    private void placeOnScreen()
    {
        GraphicsConfiguration screen = realFrame.getGraphicsConfiguration();
        Rectangle usable = screen.getBounds();
        Insets taskbar = Toolkit.getDefaultToolkit().getScreenInsets(screen);
        usable.x += taskbar.left;
        usable.y += taskbar.top;
        usable.width -= taskbar.left + taskbar.right;
        usable.height -= taskbar.top + taskbar.bottom;

        Dimension window = frame.getSize();
        frame.setLocation(usable.x + Math.max(0, (usable.width - window.width) / 2), usable.y + Math.max(0, (usable.height - window.height) / 2));
    }

    private Dimension mirrorSize()
    {
        Rectangle game = realGame.getBounds();
        int bottom = game.y + game.height;
        if (realContent.getComponentCount() > 1)
        {
            Component sidebar = realContent.getComponent(1);
            if (sidebar.isVisible())
            {
                bottom = Math.max(bottom, sidebar.getY() + sidebar.getHeight());
            }
        }
        return new Dimension(realContent.getWidth(), bottom + realContent.getInsets().bottom);
    }

    private String title()
    {
        Player player = client.getGameState() == GameState.LOGGED_IN ? client.getLocalPlayer() : null;
        boolean named = player != null && player.getName() != null || client.getLauncherDisplayName() != null;
        return config.usernameInTitle() && named ? baseTitle + " - " + StreamerName.fake() : baseTitle;
    }

    private Point mirrorPoint(Point real, Rectangle content, Component realLayers)
    {
        if (real.y < content.y)
        {
            return SwingUtilities.convertPoint(frame.getLayeredPane(), titlePoint(real, realLayers), cursorLayer);
        }
        Dimension source = mirrorSize();
        int contentX = real.x - content.x;
        int contentY = real.y - content.y;
        if (contentX < 0 || contentX >= source.width || contentY >= source.height)
        {
            return null;
        }
        Point origin = SwingUtilities.convertPoint(view, 0, 0, cursorLayer);
        return new Point(origin.x + offset(view.getWidth(), source.width) + contentX, origin.y + offset(view.getHeight(), source.height) + contentY);
    }

    private static int offset(int space, int size)
    {
        return Math.max(0, (space - size) / 2);
    }

    private final class View extends JComponent
    {
        @Override
        protected void paintComponent(Graphics graphics)
        {
            long began = FrameProfiler.begin();
            paintView(graphics);
            FrameProfiler.end(PAINT, began);
        }

        private void paintView(Graphics graphics)
        {
            Dimension source = mirrorSize();
            if (source.width <= 0 || source.height <= 0)
            {
                return;
            }

            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, getWidth(), getHeight());
            Graphics2D g = (Graphics2D) graphics.create();
            try
            {
                g.translate(offset(getWidth(), source.width), offset(getHeight(), source.height));
                g.setColor(getBackground());
                g.fillRect(0, 0, source.width, source.height);
                paintGame(g);
                paintSidebar(g);
            }
            finally
            {
                g.dispose();
            }
        }

        private void paintGame(Graphics2D g)
        {
            Rectangle game = realGame.getBounds();
            g.setColor(Color.BLACK);
            g.fillRect(game.x, game.y, game.width, game.height);
            MirrorFrames frames = MirrorFrames.get();
            MirrorFrames.Frame frame = frames == null ? null : frames.take();
            if (frame == null)
            {
                return;
            }

            Shape clip = g.getClip();
            g.clipRect(game.x, game.y, game.width, game.height);
            boolean nearest = frame.stretchedSize == null || frame.fast;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, nearest ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int width = frame.stretchedSize != null ? frame.stretchedSize.width : frame.image.getWidth();
            int height = frame.stretchedSize != null ? frame.stretchedSize.height : frame.image.getHeight();
            if (frame.hasScene)
            {
                paintScene(g, frame, game.x, game.y, width, height);
            }
            g.drawImage(frame.image, game.x, game.y, width, height, null);
            g.setClip(clip);
        }

        private void paintScene(Graphics2D g, MirrorFrames.Frame frame, int x, int y, int width, int height)
        {
            int left = x + Math.round(frame.sceneLeft * width);
            int top = y + Math.round(frame.sceneTop * height);
            int right = x + Math.round((frame.sceneLeft + frame.sceneWidth) * width);
            int bottom = y + Math.round((frame.sceneTop + frame.sceneHeight) * height);
            if (frame.scene.getWidth() == right - left && frame.scene.getHeight() == bottom - top)
            {
                g.drawImage(frame.scene, left, top, right - left, bottom - top, null);
                return;
            }
            Object interpolation = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(frame.scene, left, top, right - left, bottom - top, null);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
        }

        private void paintSidebar(Graphics2D g)
        {
            BufferedImage image = sidebar.image();
            if (image != null)
            {
                Rectangle bounds = sidebar.bounds();
                g.drawImage(image, bounds.x, bounds.y, null);
            }
        }
    }

    private final class CursorLayer extends JComponent
    {
        @Override
        public boolean contains(int x, int y)
        {
            return false;
        }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            if (cursorArea != null)
            {
                graphics.drawImage(cursorImage, cursorArea.x, cursorArea.y, null);
            }
        }
    }
}
