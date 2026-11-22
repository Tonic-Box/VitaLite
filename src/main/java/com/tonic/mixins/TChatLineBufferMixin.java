package com.tonic.mixins;

import com.tonic.api.TChatLineBuffer;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;

/**
 * A chat type's line buffer, whose line count the streamer mirror sets for a moment while it builds its own chatbox.
 */
@Mixin("ChatLineBuffer")
public abstract class TChatLineBufferMixin implements TChatLineBuffer
{
    @Shadow("lineCount")
    private int lineCount;

    @Inject
    @Override
    public void setLineCount(int count)
    {
        lineCount = count;
    }
}
