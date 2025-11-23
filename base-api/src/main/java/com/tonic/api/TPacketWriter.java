package com.tonic.api;

public interface TPacketWriter
{
    /**
     * Adds a packet buffer node to the writer.
     *
     * @param node the packet buffer node to add
     */
    void addNode(TPacketBufferNode node);
    
    /**
     * Gets the Isaac cipher used for encryption.
     *
     * @return the Isaac cipher
     */
    TIsaacCipher getIsaacCipher();

    /**
     * Gets the current server packet.
     *
     * @return the current server packet
     */
    TServerPacket getServerPacket();

    /**
     * Gets the length of the current server packet.
     *
     * @return the length of the current server packet
     */
    int getServerPacketLength();

    /**
     * Gets the server packet buffer.
     *
     * @return the server packet buffer
     */
    TPacketBuffer getServerPacketBuffer();
    
    /**
     * Sends a mouse click packet.
     *
     * @param mouseButton the mouse button pressed
     * @param x the x coordinate
     * @param y the y coordinate
     */
    void clickPacket(int mouseButton, int x, int y);

    /**
     * Sends a widget action packet.
     *
     * @param type the action type (0-9)
     * @param widgetId the widget id
     * @param childId the child id
     * @param itemId the item id
     */
    void widgetActionPacket(int type, int widgetId, int childId, int itemId);

    /**
     * Sends a widget action sub operation packet.
     *
     * @param type the action type
     * @param subOp the subtype
     * @param widgetId the widget id
     * @param childId the child id
     * @param itemId the item id
     */
    void widgetActionSubOpPacket(int type, int subOp, int widgetId, int childId, int itemId);

    /**
     * Sends a widget drag packet.
     *
     * @param widgetId the source widget id
     * @param itemId the source item id
     * @param slot the source slot
     * @param widgetId2 the destination widget id
     * @param itemId2 the destination item id
     * @param slot2 the destination slot
     */
    void widgetDragPacket(int widgetId, int itemId, int slot, int widgetId2, int itemId2, int slot2);

    /**
     * Sends a resume count dialogue packet.
     *
     * @param count the count value
     */
    void resumeCountDialoguePacket(int count);

    /**
     * Sends a resume string dialogue packet.
     *
     * @param text the text input
     */
    void resumeStringDialoguePacket(String text);

    /**
     * Sends a resume object dialogue packet.
     *
     * @param id the object id
     */
    void resumeObjectDialoguePacket(int id);

    /**
     * Continues a paused dialogue.
     *
     * @param widgetID the widget ID
     * @param optionIndex the option index (child ID)
     */
    void resumePauseWidgetPacket(int widgetID, int optionIndex);

    /**
     * Sends a walk packet.
     *
     * @param x the world X coordinate
     * @param y the world Y coordinate
     * @param ctrl whether ctrl is pressed
     */
    void walkPacket(int x, int y, boolean ctrl);

    /**
     * Sets the heading packet.
     *
     * @param heading the heading value
     */
    void setHeadingPacket(int heading);

    /**
     * Sends a widget on game object packet.
     *
     * @param selectedWidgetId the selected widget id
     * @param itemId the item id
     * @param slot the slot number
     * @param identifier the object identifier
     * @param worldX the world X coordinate
     * @param worldY the world Y coordinate
     * @param run whether to run
     */
    void widgetTargetOnGameObjectPacket(int selectedWidgetId, int itemId, int slot, int identifier, int worldX, int worldY, boolean run);

    /**
     * Sends a widget on NPC packet.
     *
     * @param identifier the NPC identifier
     * @param selectedWidgetId the widget id
     * @param itemId the item id
     * @param slot the slot number
     * @param run whether to run
     */
    void widgetTargetOnNpcPacket(int identifier, int selectedWidgetId, int itemId, int slot, boolean run);

    /**
     * Sends a widget on player packet.
     *
     * @param identifier the player identifier
     * @param selectedWidgetId the widget id
     * @param itemId the item id
     * @param slot the slot number
     * @param ctrl whether ctrl is pressed
     */
    void widgetTargetOnPlayerPacket(int identifier, int selectedWidgetId, int itemId, int slot, boolean ctrl);

    /**
     * Sends an object action packet.
     *
     * @param type the action type
     * @param identifier the object identifier
     * @param worldX the world X coordinate
     * @param worldY the world Y coordinate
     * @param ctrl whether ctrl is pressed
     */
    void objectActionPacket(int type, int identifier, int worldX, int worldY, boolean ctrl);

    /**
     * Sends a ground item action packet.
     *
     * @param type the action type
     * @param identifier the ground item identifier
     * @param worldX the world X coordinate
     * @param worldY the world Y coordinate
     * @param ctrl whether ctrl is pressed
     */
    void groundItemActionPacket(int type, int identifier, int worldX, int worldY, boolean ctrl);

    /**
     * Sends a player action packet.
     *
     * @param type the action type
     * @param playerIndex the NPC index
     * @param ctrl whether ctrl is pressed
     */
    void playerActionPacket(int type, int playerIndex, boolean ctrl);

    /**
     * Sends an NPC action packet.
     *
     * @param type the action type
     * @param npcIndex the NPC index
     * @param ctrl whether ctrl is pressed
     */
    void npcActionPacket(int type, int npcIndex, boolean ctrl);

    /**
     * Sends a widget on widget packet.
     *
     * @param selectedWidgetId the selected widget id
     * @param itemId the first item id
     * @param slot the first slot number
     * @param targetWidgetId the target widget id
     * @param itemId2 the second item id
     * @param slot2 the second slot number
     */
    void widgetOnWidgetPacket(int selectedWidgetId, int itemId, int slot, int targetWidgetId, int itemId2, int slot2);

//    /**
//     * Sends a resume name dialogue packet.
//     *
//     * @param text the name text
//     */
//    void resumeNameDialoguePacket(String text);

    /**
     * Sends a widget on ground item packet.
     *
     * @param selectedWidgetId the selected widget id
     * @param itemId the item id
     * @param slot the slot number
     * @param groundItemID the ground item ID
     * @param worldX the world X coordinate
     * @param worldY the world Y coordinate
     * @param ctrl whether ctrl is pressed
     */
    void widgetOnGroundItemPacket(int selectedWidgetId, int itemId, int slot, int groundItemID, int worldX, int worldY, boolean ctrl);

//    /**
//     * Sends an interface close packet.
//     */
//    void interfaceClosePacket();

    /**
     * Sends a chat packet.
     *
     * @param type the chat type
     * @param text the chat message text
     */
    void chatPacket(int type, String text);

    /**
     * Sends an item action packet.
     *
     * @param slot the slot of the item in the inventory
     * @param id the item id
     * @param action the action to perform on the item
     */
    void itemActionPacket(int slot, int id, int action);

    /**
     * Sends an item on item packet.
     *
     * @param itemId the first item id
     * @param slot the first slot number
     * @param itemId2 the second item id
     * @param slot2 the second slot number
     */
    void itemOnItemPacket(int itemId, int slot, int itemId2, int slot2);

    /**
     * Sends an item on game object packet.
     *
     * @param itemID the item ID
     * @param slot the slot number
     * @param objectID the object ID
     * @param worldX the world X coordinate
     * @param worldY the world Y coordinate
     * @param run whether to run
     */
    void itemOnGameObjectPacket(int itemID, int slot, int objectID, int worldX, int worldY, boolean run);

    /**
     * Sends an item on player packet.
     *
     * @param itemId the item id
     * @param slot the slot number
     * @param playerIndex the player index
     * @param run whether to run
     */
    void itemOnPlayerPacket(int itemId, int slot, int playerIndex, boolean run);

    /**
     * Sends an item on NPC packet.
     *
     * @param itemId the item id
     * @param slot the slot number
     * @param npcIndex the NPC index
     * @param run whether to run
     */
    void itemOnNpcPacket(int itemId, int slot, int npcIndex, boolean run);
}
