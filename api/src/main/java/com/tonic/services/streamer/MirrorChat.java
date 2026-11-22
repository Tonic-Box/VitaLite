package com.tonic.services.streamer;

import com.google.common.collect.MapMaker;
import com.tonic.Logger;
import com.tonic.Static;
import com.tonic.api.TChatLineBuffer;
import com.tonic.api.TClient;
import com.tonic.api.TDualNode;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.Client;
import net.runelite.api.IterableHashTable;
import net.runelite.api.MessageNode;
import net.runelite.api.ScriptID;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.EventBus;

import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The chatbox as the mirror shows it: messages added by content that is not stock are marked as they are added, and after each chatbox rebuild the client's own build script runs a second time, without those messages, with the streamer name in place of the player's, and into copies of the chatbox components, which the mirror then draws in place of the real lines. The copies are cut loose from the client's linked lists, so nothing that unlinks them can touch the real components. The real chatbox is never changed. Runs on the client thread, apart from marking queued messages and starting and stopping.
 */
public final class MirrorChat implements StreamerChat.Handler
{
    private static final String CHAT_MESSAGE_MANAGER = "net.runelite.client.chat.ChatMessageManager";
    private static final int CHATBOX = WidgetUtil.componentToInterface(InterfaceID.Chatbox.SCROLLAREA);
    private static final int LINES = WidgetUtil.componentToId(InterfaceID.Chatbox.SCROLLAREA);
    private static final int CHATBOX_REBUILD = 84;
    private static final int SCROLL = 7;
    private static final int SCROLL_HEIGHT = 8;
    private static final int BROADCAST_TIME = 55;
    private static final int TIMER_CYCLE = 65;
    private static final int REBUILT_CYCLE = 1112;
    private static final int NEWEST_PRIVATE_NAME = 356;
    private static final int PRUNE_AT = 256;
    private static final MirrorChat INSTANCE = new MirrorChat();

    private static final Set<QueuedMessage> queuedHidden = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

    private final Set<Integer> hidden = new HashSet<>();
    private boolean addingHidden;
    private boolean building;
    private boolean broken;
    private volatile boolean rebuildPending;
    private volatile Mirror mirror;
    private EventBus.Subscriber rebuilt;
    private EventBus.Subscriber rendering;
    private final Rectangle clip = new Rectangle();
    private int pruneAt = PRUNE_AT;
    private int shift;

    private MirrorChat()
    {
    }

    /**
     * Starts marking chat messages for the mirror.
     */
    static void install()
    {
        StreamerChat.setHandler(INSTANCE);
        StreamerName.addListener(() -> INSTANCE.rebuildPending = true);
    }

    /**
     * Starts keeping the mirror's chatbox, as streamer mode starts.
     */
    static void start()
    {
        EventBus eventBus = Static.getInjector().getInstance(EventBus.class);
        INSTANCE.broken = false;
        INSTANCE.rebuildPending = true;
        INSTANCE.rebuilt = eventBus.register(ScriptPostFired.class, INSTANCE::scriptRan, 0);
        INSTANCE.rendering = eventBus.register(BeforeRender.class, event -> INSTANCE.beforeRender(), 0);
    }

    /**
     * Stops keeping the mirror's chatbox and lets its copies go, as streamer mode stops.
     */
    static void stop()
    {
        EventBus eventBus = Static.getInjector().getInstance(EventBus.class);
        if (INSTANCE.rebuilt != null)
        {
            eventBus.unregister(INSTANCE.rebuilt);
            eventBus.unregister(INSTANCE.rendering);
            INSTANCE.rebuilt = null;
            INSTANCE.rendering = null;
        }
        INSTANCE.mirror = null;
    }

    /**
     * Marks a message RuneLite is queuing when content that is not stock queued it. Called by RuneLite's chat message manager, on any thread.
     *
     * @param message the message queued
     */
    public static void queued(QueuedMessage message)
    {
        if (message != null && !callerIsStock(false))
        {
            queuedHidden.add(message);
        }
    }

    /**
     * Notes the queued message RuneLite is about to add to the chat. Called by RuneLite's chat message manager on the client thread.
     *
     * @param message the message being added
     */
    public static void adding(QueuedMessage message)
    {
        INSTANCE.addingHidden = message != null && queuedHidden.remove(message);
    }

    /**
     * Notes that RuneLite has finished adding a queued message. Called by RuneLite's chat message manager on the client thread.
     */
    public static void addDone()
    {
        INSTANCE.addingHidden = false;
    }

    @Override
    public void added(Object node)
    {
        if (node instanceof MessageNode && !callerIsStock(true))
        {
            hidden.add(((MessageNode) node).getId());
            if (hidden.size() > pruneAt)
            {
                IterableHashTable<MessageNode> messages = Static.<Client>getClient().getMessages();
                hidden.removeIf(id -> messages.get(id) == null);
                pruneAt = Math.max(PRUNE_AT, hidden.size() * 2);
            }
        }
    }

    @Override
    public Object[] mirrorOf(Object[] widgets)
    {
        Mirror current = mirror;
        if (current == null || current.realLines.getChildren() != current.realChildren)
        {
            return null;
        }
        Object[] shown;
        if (widgets == current.realGroup)
        {
            shown = current.mirrorGroup;
        }
        else if (widgets == current.realChildren)
        {
            shown = current.mirrorChildren;
        }
        else
        {
            return null;
        }
        shift = current.shift();
        return shown;
    }

    @Override
    public void real(int clipX1, int clipY1, int clipX2, int clipY2)
    {
        clip.setBounds(clipX1, clipY1, Math.max(0, clipX2 - clipX1), Math.max(0, clipY2 - clipY1));
        MirrorHiding.hideFrame(clip);
    }

    @Override
    public int mirror()
    {
        MirrorHiding.mirrorFrame(clip);
        return shift;
    }

    @Override
    public void done()
    {
        MirrorHiding.end();
    }

    private void scriptRan(ScriptPostFired event)
    {
        if (!building && event.getScriptId() == CHATBOX_REBUILD)
        {
            rebuildPending = true;
        }
    }

    private void beforeRender()
    {
        if (!rebuildPending || !Streamer.active)
        {
            return;
        }
        rebuildPending = false;
        Client client = Static.getClient();
        List<MessageNode> nodes = liveHidden(client);
        if (broken || nodes.isEmpty() && !Stores.holdsName(client))
        {
            mirror = null;
            return;
        }
        try
        {
            mirror = build(client, nodes);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            broken = true;
            mirror = null;
            Logger.error("Streamer mirror could not build its chatbox: " + e);
        }
    }

    private List<MessageNode> liveHidden(Client client)
    {
        List<MessageNode> nodes = new ArrayList<>();
        if (hidden.isEmpty())
        {
            return nodes;
        }
        IterableHashTable<MessageNode> messages = client.getMessages();
        hidden.removeIf(id ->
        {
            MessageNode node = messages.get(id);
            if (node == null)
            {
                return true;
            }
            nodes.add(node);
            return false;
        });
        return nodes;
    }

    private Mirror build(Client client, List<MessageNode> nodes) throws ReflectiveOperationException
    {
        Widget[] group = (Widget[]) ((TClient) client).getComponentGroup(CHATBOX);
        if (group == null || LINES >= group.length || group[LINES] == null || group[LINES].getChildren() == null)
        {
            return null;
        }
        Widget realLines = group[LINES];
        Widget[] real = group.clone();
        Widget[] copies = group.clone();
        Map<Widget, Widget> copied = new IdentityHashMap<>();
        for (int i = 0; i < copies.length; i++)
        {
            if (copies[i] != null)
            {
                copies[i] = copyTree(copies[i], copied);
            }
        }

        Stores stores = new Stores(client, nodes);
        int[] varcs = {SCROLL, SCROLL_HEIGHT, BROADCAST_TIME, TIMER_CYCLE, REBUILT_CYCLE};
        int[] savedVarcs = new int[varcs.length];
        for (int i = 0; i < varcs.length; i++)
        {
            savedVarcs[i] = client.getVarcIntValue(varcs[i]);
        }
        String savedName = client.getVarcStrValue(NEWEST_PRIVATE_NAME);

        System.arraycopy(copies, 0, group, 0, group.length);
        building = true;
        try
        {
            stores.hide();
            stores.name();
            client.setVarcIntValue(REBUILT_CYCLE, -1);
            client.runScript(ScriptID.BUILD_CHATBOX);
        }
        finally
        {
            building = false;
            for (int i = 0; i < varcs.length; i++)
            {
                client.setVarcIntValue(varcs[i], savedVarcs[i]);
            }
            client.setVarcStrValue(NEWEST_PRIVATE_NAME, savedName);
            stores.restore();
            System.arraycopy(real, 0, group, 0, group.length);
        }

        Widget mirrorLines = copies[LINES];
        return new Mirror(group, realLines, realLines.getChildren(), copies, mirrorLines, mirrorLines.getChildren());
    }

    private static Widget copyTree(Widget widget, Map<Widget, Widget> copied) throws ReflectiveOperationException
    {
        Widget done = copied.get(widget);
        if (done != null)
        {
            return done;
        }
        Widget copy = FieldCopy.copy(widget);
        for (Field field : FieldCopy.fields(copy.getClass()))
        {
            if (field.getDeclaringClass() != copy.getClass() && field.getType() == field.getDeclaringClass())
            {
                field.set(copy, null);
            }
        }
        copied.put(widget, copy);
        Widget[] children = copy.getChildren();
        if (children != null)
        {
            for (int i = 0; i < children.length; i++)
            {
                if (children[i] != null)
                {
                    children[i] = copyTree(children[i], copied);
                }
            }
        }
        return copy;
    }

    private static boolean callerIsStock(boolean added)
    {
        Class<?> caller = StockContent.caller(type -> skipped(type, added));
        if (caller == null)
        {
            return true;
        }
        if (added && caller.getName().startsWith(CHAT_MESSAGE_MANAGER))
        {
            return !INSTANCE.addingHidden;
        }
        return StockContent.isStock(caller);
    }

    private static boolean skipped(Class<?> type, boolean added)
    {
        String name = type.getName();
        return type.getPackageName().isEmpty()
                || name.startsWith(StockContent.NOTIFIER)
                || !added && name.startsWith(CHAT_MESSAGE_MANAGER)
                || name.startsWith("java.")
                || name.startsWith("jdk.")
                || name.startsWith("sun.");
    }

    /**
     * The client's chat stores with the hidden messages taken out and the player's name replaced for the mirror's build, and what is needed to put them back exactly.
     */
    private static final class Stores
    {
        private final Map<ChatLineBuffer, MessageNode[]> savedLines = new IdentityHashMap<>();
        private final Map<ChatLineBuffer, Integer> savedCounts = new IdentityHashMap<>();
        private final List<TDualNode[]> unlinked = new ArrayList<>();
        private final List<MessageNode> renamed = new ArrayList<>();
        private final List<String[]> originals = new ArrayList<>();
        private final Client client;
        private final List<MessageNode> nodes;

        private Stores(Client client, List<MessageNode> nodes)
        {
            this.client = client;
            this.nodes = nodes;
        }

        private void hide()
        {
            Set<Integer> ids = new HashSet<>();
            for (MessageNode node : nodes)
            {
                ids.add(node.getId());
            }
            for (ChatLineBuffer buffer : client.getChatLineMap().values())
            {
                MessageNode[] lines = buffer.getLines();
                int count = buffer.getLength();
                if (!holdsAny(lines, count, ids))
                {
                    continue;
                }
                savedLines.put(buffer, lines.clone());
                savedCounts.put(buffer, count);
                int kept = 0;
                for (int i = 0; i < count; i++)
                {
                    if (lines[i] != null && !ids.contains(lines[i].getId()))
                    {
                        lines[kept++] = lines[i];
                    }
                }
                for (int i = kept; i < count; i++)
                {
                    lines[i] = null;
                }
                ((TChatLineBuffer) buffer).setLineCount(kept);
            }
            for (MessageNode node : nodes)
            {
                TDualNode link = (TDualNode) node;
                TDualNode previous = link.getPreviousDual();
                TDualNode next = link.getNextDual();
                if (previous == null || next == null)
                {
                    continue;
                }
                previous.setNextDual(next);
                next.setPreviousDual(previous);
                unlinked.add(new TDualNode[]{link, previous, next});
            }
        }

        private static boolean holdsAny(MessageNode[] lines, int count, Set<Integer> ids)
        {
            for (int i = 0; i < count; i++)
            {
                if (lines[i] != null && ids.contains(lines[i].getId()))
                {
                    return true;
                }
            }
            return false;
        }

        private static boolean holdsName(Client client)
        {
            if (!StreamerName.hasNames())
            {
                return false;
            }
            for (ChatLineBuffer buffer : client.getChatLineMap().values())
            {
                MessageNode[] lines = buffer.getLines();
                for (int i = 0; i < buffer.getLength(); i++)
                {
                    if (lines[i] != null && named(lines[i]))
                    {
                        return true;
                    }
                }
            }
            return false;
        }

        private static boolean named(MessageNode node)
        {
            return StreamerName.replace(node.getName()) != null || StreamerName.replace(node.getSender()) != null || StreamerName.replace(node.getValue()) != null || StreamerName.replace(node.getRuneLiteFormatMessage()) != null;
        }

        private void name()
        {
            if (!StreamerName.hasNames())
            {
                return;
            }
            Set<MessageNode> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            for (ChatLineBuffer buffer : client.getChatLineMap().values())
            {
                MessageNode[] lines = buffer.getLines();
                for (int i = 0; i < buffer.getLength(); i++)
                {
                    MessageNode node = lines[i];
                    if (node == null || !seen.add(node))
                    {
                        continue;
                    }
                    String[] fields = {node.getName(), node.getSender(), node.getValue(), node.getRuneLiteFormatMessage()};
                    String[] shown = {orSame(fields[0]), orSame(fields[1]), orSame(fields[2]), orSame(fields[3])};
                    if (Arrays.equals(fields, shown))
                    {
                        continue;
                    }
                    renamed.add(node);
                    originals.add(fields);
                    node.setName(shown[0]);
                    node.setSender(shown[1]);
                    node.setValue(shown[2]);
                    node.setRuneLiteFormatMessage(shown[3]);
                }
            }
        }

        private static String orSame(String text)
        {
            String replaced = StreamerName.replace(text);
            return replaced != null ? replaced : text;
        }

        private void restore()
        {
            for (int i = renamed.size() - 1; i >= 0; i--)
            {
                MessageNode node = renamed.get(i);
                String[] fields = originals.get(i);
                node.setName(fields[0]);
                node.setSender(fields[1]);
                node.setValue(fields[2]);
                node.setRuneLiteFormatMessage(fields[3]);
            }
            for (int i = unlinked.size() - 1; i >= 0; i--)
            {
                TDualNode[] link = unlinked.get(i);
                link[1].setNextDual(link[0]);
                link[2].setPreviousDual(link[0]);
            }
            for (Map.Entry<ChatLineBuffer, MessageNode[]> saved : savedLines.entrySet())
            {
                MessageNode[] lines = saved.getKey().getLines();
                System.arraycopy(saved.getValue(), 0, lines, 0, lines.length);
                ((TChatLineBuffer) saved.getKey()).setLineCount(savedCounts.get(saved.getKey()));
            }
        }
    }

    /**
     * The mirror's copies of the chatbox components, with the real ones they stand in for.
     */
    private static final class Mirror
    {
        private final Widget[] realGroup;
        private final Widget realLines;
        private final Widget[] realChildren;
        private final Widget[] mirrorGroup;
        private final Widget mirrorLines;
        private final Widget[] mirrorChildren;

        private Mirror(Widget[] realGroup, Widget realLines, Widget[] realChildren, Widget[] mirrorGroup, Widget mirrorLines, Widget[] mirrorChildren)
        {
            this.realGroup = realGroup;
            this.realLines = realLines;
            this.realChildren = realChildren;
            this.mirrorGroup = mirrorGroup;
            this.mirrorLines = mirrorLines;
            this.mirrorChildren = mirrorChildren;
        }

        private int shift()
        {
            int realScroll = realLines.getScrollY();
            int view = realLines.getHeight();
            int fromBottom = realLines.getScrollHeight() - realScroll;
            int mirrorScroll = Math.max(0, Math.min(mirrorLines.getScrollHeight() - fromBottom, mirrorLines.getScrollHeight() - view));
            return realScroll - mirrorScroll;
        }
    }
}
