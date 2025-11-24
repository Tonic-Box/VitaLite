package com.tonic.api;

public interface TClient
{
    /**
     * Gets the packet writer for sending packets.
     *
     * @return the packet writer
     */
    TPacketWriter getPacketWriter();
    
    /**
     * Creates a new client packet with the specified id and length.
     *
     * @param id the packet id
     * @param length the packet length
     * @return a new client packet
     */
    TClientPacket newClientPacket(int id, int length);
    
    /**
     * Gets a packet buffer node for the given client packet and cipher.
     *
     * @param clientPacket the client packet
     * @param isaacCipher the Isaac cipher for encryption
     * @return a packet buffer node
     */
    TPacketBufferNode getPacketBufferNode(TClientPacket clientPacket, TIsaacCipher isaacCipher);
    
    /**
     * Gets the current tick count.
     *
     * @return the tick count
     */
    int getTickCount();
    
    /**
     * Sets a varbit value.
     *
     * @param varbit the varbit id
     * @param value the value to set
     */
    void setVarbit(int varbit, int value);
    
    /**
     * Gets a varbit value from the varps array.
     *
     * @param varps the varps array
     * @param varbitId the varbit id
     * @return the varbit value
     */
    int getVarbitValue(int[] varps, int varbitId);
    
    /**
     * Sets a varbit value in the varps array.
     *
     * @param varps the varps array
     * @param varbit the varbit id
     * @param value the value to set
     */
    void setVarbitValue(int[] varps, int varbit, int value);
    
    /**
     * Gets a varp value.
     *
     * @param varpId the varp id
     * @return the varp value
     */
    int getVarpValue(int varpId);
    
    /**
     * Checks if the current thread is the client thread.
     *
     * @return true if on client thread, false otherwise
     */
    boolean isClientThread();

    /**
     * Gets the mouse handler.
     *
     * @return the mouse handler
     */
    TMouseHandler getMouseHandler();

    /**
     * Gets the timestamp of the last mouse press in milliseconds.
     * @return the timestamp in milliseconds
     */
    long getClientMouseLastPressedMillis();

    /**
     * Sets the timestamp of the last mouse press in milliseconds.
     * @param millis the timestamp in milliseconds
     */
    void setClientMouseLastPressedMillis(long millis);

    /**
     * Invokes a menu action with the specified parameters.
     * @param option option
     * @param target target
     * @param identifier identifier
     * @param opcode opcode
     * @param param0 param0
     * @param param1 param1
     * @param itemId itemId
     * @param x x
     * @param y y
     */
    void invokeMenuAction(String option, String target, int identifier, int opcode, int param0, int param1, int itemId, int x, int y);

    /**
     * Invokes a menu action with the specified parameters.
     * @param option option
     * @param target target
     * @param identifier identifier
     * @param opcode opcode
     * @param param0 param0
     * @param param1 param1
     * @param itemId itemId
     * @param worldView worldView
     * @param x x
     * @param y y
     */
    void invokeMenuAction(String option, String target, int identifier, int opcode, int param0, int param1, int itemId, int worldView, int x, int y);

    String getDisplayName();

    void setDisplayName(String name);

    String getCharacterId();

    void setCharacterId(String id);

    String getSessionId();

    void setSessionId(String id);

    String getRefreshToken();

    void setRefreshToken(String token);

    String getAccessToken();

    void setAccessToken(String token);

    void setLoginIndex(int index);

    void setAccountTypeLegacy();

    void setAccountTypeJagex();

    int getShipHeading();
}
