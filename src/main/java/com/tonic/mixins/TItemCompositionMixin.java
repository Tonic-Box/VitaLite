package com.tonic.mixins;

import com.tonic.api.TItemComposition;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;
import com.tonic.util.TextUtil;
import lombok.Getter;

@Mixin("ItemComposition")
public abstract class TItemCompositionMixin implements TItemComposition
{
    @Shadow("groundActions")
    public String[] groundActions;

    public String[] getGroundActions()
    {
        String[] cleaned = new String[groundActions.length];
        for(int i = 0; i < groundActions.length; i++)
        {
            if(groundActions[i] != null)
            {
                cleaned[i] = TextUtil.sanitize(groundActions[i]);
            }
        }
        return cleaned;
    }
}
