package com.tonic.mixins;

import com.tonic.api.TBuffer;
import com.tonic.injector.annotations.Inject;
import com.tonic.injector.annotations.Mixin;
import com.tonic.injector.annotations.Shadow;
import com.tonic.util.TextUtil;
import lombok.Getter;

@Mixin("Buffer")
@Getter
public abstract class TBufferMixin implements TBuffer
{
    @Shadow("array")
    public byte[] array;

    @Shadow("offset")
    public int offset;

    @Inject
    @Override
    public void writeByte(int val)
    {
        array[++offset - 1] = (byte)val;
    }

    @Inject
    @Override
    public void writeByteAdd(int var)
    {
        writeByte(var + 128);
    }

    @Inject
    @Override
    public void writeByteNeg(int var)
    {
        writeByte(-var);
    }

    @Inject
    @Override
    public void writeByteSub(int var)
    {
        writeByte(128 - var);
    }

    //shorts

    @Inject
    @Override
    public void writeShort(int var)
    {
        writeByte(var >> 8);
        writeByte(var);
    }

    @Inject
    @Override
    public void writeShortLE(int var)
    {
        writeByte(var);
        writeByte(var >> 8);
    }

    @Inject
    @Override
    public void writeShortAdd(int var)
    {
        writeByte(var >> 8);
        writeByte(var + 128);
    }

    @Inject
    @Override
    public void writeShortAddLE(int var)
    {
        writeByte(var + 128);
        writeByte(var >> 8);
    }

    //ints

    @Inject
    @Override
    public void writeIntME(int var)
    {
        writeByte(var >> 16);
        writeByte(var >> 24);
        writeByte(var);
        writeByte(var >> 8);
    }

    @Inject
    @Override
    public void writeIntLE(int var)
    {
        writeByte(var);
        writeByte(var >> 8);
        writeByte(var >> 16);
        writeByte(var >> 24);
    }

    @Inject
    @Override
    public void writeInt(int var)
    {
        writeByte(var >> 24);
        writeByte(var >> 16);
        writeByte(var >> 8);
        writeByte(var);
    }

    @Inject
    @Override
    public void writeIntIME(int var)
    {
        writeByte(var >> 8);
        writeByte(var);
        writeByte(var >> 24);
        writeByte(var >> 16);
    }

    @Inject
    @Override
    public void writeLengthByte(int var)
    {
        if(var >= 0 && var <= 255) {
            array[offset - var - 1] = (byte)var;
        }
    }

    @Inject
    @Override
    public void writeStringCp1252NullTerminated(String var)
    {
        int var2 = var.indexOf(0);
        if(var2 >= 0) {
            return;
        } else {
            offset += TextUtil.encodeStringCp1252(var, 0, var.length(), array, offset);
            array[offset++] = 0;
        }
    }

    @Inject
    @Override
    public void writeStringCp1252NullCircumfixed(String var)
    {
        int var2 = var.indexOf(0);
        if(var2 >= 0) {
            return;
        } else {
            array[offset++] = 0;
            offset += TextUtil.encodeStringCp1252(var, 0, var.length(), array, offset);
            array[offset++] = 0;
        }
    }
}
