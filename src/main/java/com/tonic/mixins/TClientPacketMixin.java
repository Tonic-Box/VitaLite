package com.tonic.mixins;

import com.tonic.api.TClientPacket;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;
import lombok.Getter;

@Mixin("ClientPacket")
@Getter
public class TClientPacketMixin implements TClientPacket {
    @Shadow("id")
    public int id;

    @Shadow("length")
    public int length;
}
