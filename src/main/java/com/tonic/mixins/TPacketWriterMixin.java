package com.tonic.mixins;

import com.tonic.Static;
import com.tonic.api.*;
import com.tonic.events.PacketSent;
import com.tonic.injector.annotations.*;
import com.tonic.model.ui.VitaLiteOptionsPanel;
import com.tonic.packets.PacketMapReader;
import com.tonic.packets.types.MapEntry;
import lombok.Getter;
import net.runelite.api.widgets.WidgetInfo;

import java.util.HashMap;
import java.util.Map;

@Getter
@Mixin("PacketWriter")
public abstract class TPacketWriterMixin implements TPacketWriter
{
    @Shadow("isaacCipher")
    public TIsaacCipher isaacCipher;

    @Shadow("client")
    public static TClient client;

//    @Shadow("addNode2")
//    public abstract void addNode(TPacketWriter packetWriter, TPacketBufferNode node);
//
//    @MethodHook("addNode2")
//    @Inject
//    public static void onAddNode2(TPacketWriter packetWriter, TPacketBufferNode node)
//    {
//        addNodeHook(node);
//    }

    @Shadow("addNode")
    public abstract void addNode(TPacketBufferNode node);
    @MethodHook("addNode")
    @Inject
    public static void onAddNode1(TPacketBufferNode node)
    {
        addNodeHook(node);
    }

    @Inject
    public static void addNodeHook(TPacketBufferNode node)
    {
        if(node == null ||  node.getClientPacket() == null)
            return;

        TPacketBuffer buffer = node.getPacketBuffer();
        TClientPacket packet = node.getClientPacket();

        int offset = buffer.getOffset();
        int id = packet.getId();
        int len = packet.getLength();

        byte[] bytes = buffer.getArray();
        int payloadSize = (len > 0) ? len : (offset - 1);
        if(payloadSize > 1024)
            return;
        byte[] payload = new byte[payloadSize];
        System.arraycopy(bytes, 1, payload, 0, payloadSize);

        PacketSent packetSent = new PacketSent(id, len, payload);
        Static.post(packetSent);
        VitaLiteOptionsPanel.getInstance().onPacketSent(packetSent);
    }

    @Inject
    private void addNodeSwitch(TPacketBufferNode node)
    {
        //this.addNode(this, node);
        this.addNode(node);
    }

    @Inject
    @Override
    public void clickPacket(int mouseButton, int mouseX, int mouseY)
    {
        long ms = System.currentTimeMillis();
        int mousePressedTime = (int)((ms - client.getMouseHandler().getMouseLastPressedMillis()));
        if (mousePressedTime < 0)
        {
            mousePressedTime = 0;
        }
        if (mousePressedTime > 32767)
        {
            mousePressedTime = 32767;
        }
        client.getMouseHandler().setMouseLastPressedMillis(ms);
        client.setClientMouseLastPressedMillis(ms);
        int mouseInfo = (mousePressedTime << 1) + (mouseButton);
        MapEntry entry = PacketMapReader.get("OP_MOUSE_CLICK");
        Map<String,Object> args = new HashMap<>();
        args.put("mouseInfo", mouseInfo);
        args.put("x", mouseX);
        args.put("y", mouseY);
        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Override
    @Inject
    public void widgetActionPacket(int type, int widgetId, int childId, int itemId)
    {
        MapEntry entry = PacketMapReader.get("OP_WIDGET_ACTION");
        Map<String,Object> args = new HashMap<>();
        args.put("widgetId", widgetId);
        args.put("childId", childId);
        args.put("itemId", itemId);
        args.put("type", type);
        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void resumeCountDialoguePacket(int count)
    {
        MapEntry entry = PacketMapReader.get("OP_RESUME_COUNTDIALOG");
        Map<String,Object> args = new HashMap<>();
        args.put("count", count);
        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void resumePauseWidgetPacket(int widgetID, int optionIndex)
    {
        MapEntry entry = PacketMapReader.get("OP_RESUME_PAUSEBUTTON");
        Map<String,Object> args = new HashMap<>();
        args.put("widgetID", widgetID);
        args.put("optionIndex", optionIndex);
        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void resumeObjectDialoguePacket(int id) {
        MapEntry entry = PacketMapReader.get("OP_RESUME_OBJDIALOG");
        Map<String,Object> args = new HashMap<>();
        args.put("id", id);
        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void walkPacket(int worldX, int worldY, boolean ctrl)
    {
        MapEntry entry = PacketMapReader.get("OP_WALK");
        Map<String,Object> args = new HashMap<>();
        args.put("worldX", worldX);
        args.put("worldY", worldY);
        args.put("ctrl", ctrl ? 1 : 0);
        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void chatPacket(int type, String text) {
        if (text.length() >= 80)
            text = text.substring(0, 79);
        TPacketBufferNode packetBufferNode = sendChat(type, text, null, -1);
        if(packetBufferNode != null)
        {
            this.addNodeSwitch(packetBufferNode);
        }
    }

    @Shadow("constructChat")
    public abstract TPacketBufferNode sendChat(int type, String text, TLanguage language, int unknown);

    @Inject
    @Override
    public void widgetTargetOnGameObjectPacket(int selectedWidgetId, int itemId, int slot, int identifier, int worldX, int worldY, boolean ctrl)
    {
        MapEntry entry = PacketMapReader.get("OP_WIDGET_TARGET_ON_GAME_OBJECT");
        Map<String,Object> args = new HashMap<>();
        args.put("selectedWidgetId", selectedWidgetId);
        args.put("itemId", itemId);
        args.put("slot", slot);
        args.put("identifier", identifier);
        args.put("worldX", worldX);
        args.put("worldY", worldY);
        args.put("ctrl", ctrl ? 0 : 1);
        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void widgetTargetOnNpcPacket(int identifier, int selectedWidgetId, int itemId, int slot, boolean ctrl)
    {
        MapEntry entry = PacketMapReader.get("OP_WIDGET_TARGET_ON_NPC");
        Map<String,Object> args = new HashMap<>();
        args.put("selectedWidgetId", selectedWidgetId);
        args.put("itemId", itemId);
        args.put("slot", slot);
        args.put("identifier", identifier);
        args.put("ctrl", ctrl ? 0 : 1);
        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void widgetTargetOnPlayerPacket(int identifier, int selectedWidgetId, int itemId, int slot, boolean ctrl)
    {
        MapEntry entry = PacketMapReader.get("OP_WIDGET_TARGET_ON_PLAYER");
        Map<String,Object> args = new HashMap<>();
        args.put("selectedWidgetId", selectedWidgetId);
        args.put("itemId", itemId);
        args.put("slot", slot);
        args.put("identifier", identifier);
        args.put("ctrl", ctrl ? 0 : 1);
        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void resumeStringDialoguePacket(String text)
    {
        MapEntry entry = PacketMapReader.get("OP_RESUME_STRINGDIALOG");
        Map<String,Object> args = new HashMap<>();
        args.put("length", text.length());
        args.put("var7", text);
        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void resumeNameDialoguePacket(String text)
    {
        MapEntry entry = PacketMapReader.get("OP_RESUME_NAMEDIALOG");
        Map<String,Object> args = new HashMap<>();
        args.put("length", text.length());
        args.put("var7", text);
        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void objectActionPacket(int type, int identifier, int worldX, int worldY, boolean ctrl)
    {
        MapEntry entry = PacketMapReader.get("OP_GAME_OBJECT_ACTION_" + type);
        if(entry == null)
        {
            System.err.println("Packets::objectActionPacket invalid type");
            return;
        }

        Map<String,Object> args = new HashMap<>();
        args.put("identifier", identifier);
        args.put("ctrl", ctrl ? 1 : 0);
        args.put("worldX", worldX);
        args.put("worldY", worldY);

        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void groundItemActionPacket(int type, int identifier, int worldX, int worldY, boolean ctrl)
    {
        MapEntry entry = PacketMapReader.get("OP_GROUND_ITEM_ACTION_" + type);
        if(entry == null)
        {
            System.err.println("Packets::groundItemActionPacket invalid type");
            return;
        }

        Map<String,Object> args = new HashMap<>();
        args.put("identifier", identifier);
        args.put("ctrl", ctrl ? 1 : 0);
        args.put("worldX", worldX);
        args.put("worldY", worldY);

        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void playerActionPacket(int type, int playerIndex, boolean ctrl)
    {
        MapEntry entry = PacketMapReader.get("OP_PLAYER_ACTION_" + type);
        if(entry == null)
        {
            System.err.println("Packets::playerActionPacket invalid type");
            return;
        }

        Map<String,Object> args = new HashMap<>();
        args.put("identifier", playerIndex);
        args.put("ctrl", ctrl ? 1 : 0);

        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void npcActionPacket(int type, int npcIndex, boolean ctrl)
    {
        MapEntry entry = PacketMapReader.get("OP_NPC_ACTION_" + type);
        if(entry == null)
        {
            System.err.println("Packets::npcActionPacket invalid type");
            return;
        }

        Map<String,Object> args = new HashMap<>();
        args.put("identifier", npcIndex);
        args.put("ctrl", ctrl ? 1 : 0);

        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void widgetOnWidgetPacket(int selectedWidgetId, int itemId, int slot, int targetWidgetId, int itemId2, int slot2)
    {
        MapEntry entry = PacketMapReader.get("OP_WIDGET_TARGET_ON_WIDGET");
        Map<String,Object> args = new HashMap<>();
        args.put("selectedWidgetId", selectedWidgetId);
        args.put("itemId", itemId);
        args.put("slot", slot);
        args.put("targetWidgetID", targetWidgetId);
        args.put("identifier2", itemId2);
        args.put("param0", slot2);
        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void widgetOnGroundItemPacket(int selectedWidgetId, int itemId, int slot, int groundItemID, int worldX, int worldY, boolean ctrl)
    {
        MapEntry entry = PacketMapReader.get("OP_WIDGET_TARGET_ON_GROUND_ITEM");
        Map<String,Object> args = new HashMap<>();
        args.put("selectedWidgetId", selectedWidgetId);
        args.put("itemId", itemId);
        args.put("slot", slot);
        args.put("identifier", groundItemID);
        args.put("worldX", worldX);
        args.put("worldY", worldY);
        args.put("ctrl", ctrl ? 0 : 1);
        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void interfaceClosePacket()
    {
        MapEntry entry = PacketMapReader.get("OP_INTERFACE_CLOSE");
        Map<String,Object> args = new HashMap<>();
        this.addNodeSwitch(PacketMapReader.createBuffer(entry, args).toPacketBufferNode(client));
    }

    @Inject
    @Override
    public void itemOnNpcPacket(int itemId, int slot, int npcIndex, boolean run)
    {
        widgetTargetOnNpcPacket(npcIndex, WidgetInfo.INVENTORY.getId(), itemId, slot, run);
    }

    @Inject
    @Override
    public void itemOnPlayerPacket(int itemId, int slot, int playerIndex, boolean run)
    {
        widgetTargetOnPlayerPacket(playerIndex, WidgetInfo.INVENTORY.getId(), itemId, slot, run);
    }

    @Inject
    @Override
    public void itemOnGameObjectPacket(int itemID, int slot, int objectID, int worldX, int worldY, boolean run)
    {
        widgetTargetOnGameObjectPacket(WidgetInfo.INVENTORY.getId(), itemID, slot, objectID, worldX, worldY, run);
    }

    @Inject
    @Override
    public void itemOnItemPacket(int itemId, int slot, int itemId2, int slot2)
    {
        widgetOnWidgetPacket(WidgetInfo.INVENTORY.getId(), itemId, slot, WidgetInfo.INVENTORY.getId(), itemId2, slot2);
    }

    @Inject
    @Override
    public void itemActionPacket(int slot, int id, int action) {
        widgetActionPacket(action, WidgetInfo.INVENTORY.getId(), slot, id);
    }
}
