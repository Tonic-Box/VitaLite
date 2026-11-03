package com.tonic.mixins;

import com.tonic.api.TItemComposition;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;
import com.tonic.util.TextUtil;
import lombok.Getter;
import net.runelite.api.EntityOps;

@Mixin("ItemComposition")
public abstract class TItemCompositionMixin implements TItemComposition
{
    @Shadow("groundActions")
    public EntityOps groundActions;

    public String[] getGroundActions()
    {
        String[] cleaned = new String[EntityOps.MAX_OPS];
        if(groundActions == null)
        {
            return cleaned;
        }
        int count = Math.min(cleaned.length, groundActions.getNumOps() + 1);
        for(int i = 0; i < count; i++)
        {
            String action = groundActions.getOp(i);
            if(action != null)
            {
                cleaned[i] = TextUtil.sanitize(action);
            }
        }
        return cleaned;
    }
}
