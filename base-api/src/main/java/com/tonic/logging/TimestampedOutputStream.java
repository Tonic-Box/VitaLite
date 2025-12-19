package com.tonic.logging;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Output stream that prefixes each line with a timestamp.
 */
public class TimestampedOutputStream extends FilterOutputStream {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private boolean atLineStart = true;

    public TimestampedOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(int b) throws IOException {
        if (atLineStart) {
            writeTimestamp();
            atLineStart = false;
        }
        out.write(b);
        if (b == '\n') {
            atLineStart = true;
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            write(b[off + i]);
        }
    }

    private void writeTimestamp() throws IOException {
        String timestamp = "[" + LocalDateTime.now().format(FORMATTER) + "] ";
        out.write(timestamp.getBytes(StandardCharsets.UTF_8));
    }
}
