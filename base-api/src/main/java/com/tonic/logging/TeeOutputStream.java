package com.tonic.logging;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Output stream that writes to two destinations simultaneously.
 */
public class TeeOutputStream extends OutputStream {
    private final OutputStream primary;
    private final OutputStream secondary;

    public TeeOutputStream(OutputStream primary, OutputStream secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    @Override
    public void write(int b) throws IOException {
        primary.write(b);
        secondary.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        primary.write(b);
        secondary.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        primary.write(b, off, len);
        secondary.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        primary.flush();
        secondary.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            primary.close();
        } finally {
            secondary.close();
        }
    }
}
