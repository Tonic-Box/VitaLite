package com.tonic.mixins;

import com.tonic.api.TBuffer;
import com.tonic.api.TObjectComposition;
import com.tonic.injector.annotations.Disable;

import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Mixin;
import lombok.Getter;

@Getter
@Mixin("ObjectComposition")
public class TObjectCompositionMixin implements TObjectComposition
{
    @Inject
    private int blockAccessFlags;

    @Disable("decodeNext")
    public boolean decodeNext(TBuffer buffer, int opcode)
    {
        if(opcode == 69)
        {
            byte[] array = buffer.getArray();
            int offset = buffer.getOffset();
            blockAccessFlags = array[offset] & 0xFF;
        }

        return true;
    }
}
