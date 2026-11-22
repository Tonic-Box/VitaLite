package com.tonic.api;

/**
 * A node of a doubly linked list, such as the client's queue of every chat message in the order they arrived.
 */
public interface TDualNode
{
    /**
     * @return the node after this one
     */
    TDualNode getNextDual();

    /**
     * @return the node before this one
     */
    TDualNode getPreviousDual();

    /**
     * @param node the node to put after this one
     */
    void setNextDual(TDualNode node);

    /**
     * @param node the node to put before this one
     */
    void setPreviousDual(TDualNode node);
}
