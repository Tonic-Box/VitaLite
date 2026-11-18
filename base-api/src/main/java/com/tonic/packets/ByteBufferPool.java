package com.tonic.packets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

public class ByteBufferPool
{
    private static final ByteBufAllocator ALLOCATOR = new UnpooledByteBufAllocator(false);
    private static final int BUFFER_SIZE = 200;

    public static ByteBuf allocate() {
        return ALLOCATOR.heapBuffer(BUFFER_SIZE);
    }

    public static ByteBuf allocate(int size) {
        return ALLOCATOR.heapBuffer(size);
    }

    public static void release(ByteBuf buffer) {
        if (buffer != null) {
            buffer.release();
        }
    }
}