package com.tonic.services.pathfinder.collision;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Reads the vcol collision map format: GZIP around a magic, a version and the blocked-edge bit set's non-empty 32768-bit pages, each an int page index and 512 longs.
 */
public final class VcolCodec
{
    private static final int MAGIC = 0x56434F4C;
    private static final int VERSION = 1;
    private static final int PAGE_BITS = 15;
    private static final int WORDS_PER_PAGE = 1 << (PAGE_BITS - 6);

    private VcolCodec()
    {
    }

    /**
     * Reads a complete vcol stream and closes the stream.
     *
     * @param stream the stream
     * @return the blocked edges
     * @throws IOException when it cannot be read or is not a vcol map of this version
     */
    public static SparseBitSet readStream(InputStream stream) throws IOException
    {
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(stream)))
        {
            int magic = in.readInt();
            if (magic != MAGIC)
            {
                throw new IOException("Not a collision map (bad magic: 0x" + Integer.toHexString(magic) + ")");
            }
            int version = in.readUnsignedByte();
            if (version != VERSION)
            {
                throw new IOException("Unsupported collision map version: " + version);
            }

            SparseBitSet bitSet = new SparseBitSet();
            int pageCount = in.readInt();
            for (int p = 0; p < pageCount; p++)
            {
                int pageBase = in.readInt() << PAGE_BITS;
                for (int w = 0; w < WORDS_PER_PAGE; w++)
                {
                    long word = in.readLong();
                    while (word != 0)
                    {
                        int bit = Long.numberOfTrailingZeros(word);
                        bitSet.set(pageBase + (w << 6) + bit);
                        word &= word - 1;
                    }
                }
            }
            return bitSet;
        }
    }
}
