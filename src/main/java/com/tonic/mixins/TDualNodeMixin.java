package com.tonic.mixins;

import com.tonic.api.TDualNode;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;

/**
 * A node of the client's doubly linked lists, whose links the streamer mirror reads and sets to take chat messages out of the message queue for a moment.
 */
@Mixin("DualNode")
public abstract class TDualNodeMixin implements TDualNode
{
    @Shadow("nextDual")
    private TDualNode nextDual;

    @Shadow("previousDual")
    private TDualNode previousDual;

    @Inject
    @Override
    public TDualNode getNextDual()
    {
        return nextDual;
    }

    @Inject
    @Override
    public TDualNode getPreviousDual()
    {
        return previousDual;
    }

    @Inject
    @Override
    public void setNextDual(TDualNode node)
    {
        nextDual = node;
    }

    @Inject
    @Override
    public void setPreviousDual(TDualNode node)
    {
        previousDual = node;
    }
}
