package com.tonic.mixins;

import com.tonic.api.TServerPacket;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;
import lombok.Getter;

@Mixin("ServerPacket")
@Getter
public class TServerPacketMixin implements TServerPacket
{
    @Shadow("id")
    public int id;

    @Shadow("length")
    public int length;
}
