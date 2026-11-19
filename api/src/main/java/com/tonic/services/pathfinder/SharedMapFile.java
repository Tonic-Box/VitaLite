package com.tonic.services.pathfinder;

import com.tonic.Static;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.jar.JarEntry;
import java.util.zip.CRC32;

/**
 * Read-only pathfinder data built once into a file under the VitaLite folder and memory-mapped, so every local client maps the same file and the operating system keeps one copy of its pages for all of them.
 */
public final class SharedMapFile
{
    private static final int MAGIC = 0x56504D46;
    private static final int HEADER = 8;
    private static final long STALE_TEMP_MILLIS = 3_600_000L;
    private static final Path DIRECTORY = Static.VITA_DIR.resolve("pathfinder");

    private SharedMapFile()
    {
    }

    /**
     * Writes a map's data.
     */
    @FunctionalInterface
    public interface Writer
    {
        /**
         * @param out where the data goes
         * @throws IOException when the data cannot be built or written
         */
        void write(Output out) throws IOException;
    }

    /**
     * Maps the file built from a walker resource, building it first when no client has yet.
     *
     * @param name the file's name
     * @param resource the walker resource it is built from
     * @param format the layout version, changed whenever the writer's layout changes
     * @param writer builds the data
     * @return the data, little-endian, starting at offset 0
     * @throws IOException when the file cannot be built or mapped
     */
    public static ByteBuffer open(String name, String resource, int format, Writer writer) throws IOException
    {
        String prefix = name + "-";
        String key = Long.toHexString(resourceKey(resource) * 31 + format);
        Path file = DIRECTORY.resolve(prefix + key + ".bin");
        ByteBuffer data;
        try
        {
            data = buildAndMap(file, prefix + key, format, writer);
        }
        catch (NoSuchFileException e)
        {
            data = buildAndMap(file, prefix + key, format, writer);
        }
        removeStale(prefix, file);
        return data;
    }

    private static ByteBuffer buildAndMap(Path file, String stem, int format, Writer writer) throws IOException
    {
        if (!isComplete(file))
        {
            Files.createDirectories(DIRECTORY);
            Path temp = DIRECTORY.resolve(stem + "." + ProcessHandle.current().pid() + ".tmp");
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))
            {
                Output out = new Output(channel);
                out.writeInt(MAGIC);
                out.writeInt(format);
                writer.write(out);
                out.flush();
                channel.force(true);
            }
            try
            {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (IOException e)
            {
                Files.deleteIfExists(temp);
                if (!isComplete(file))
                {
                    throw e;
                }
            }
        }

        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ))
        {
            ByteBuffer mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            return slice(mapped, HEADER, (int) channel.size() - HEADER);
        }
    }

    /**
     * Builds a map's data in memory, for data that is not shared.
     *
     * @param writer builds the data
     * @return the data, little-endian, starting at offset 0
     * @throws IOException when the data cannot be built
     */
    public static ByteBuffer build(Writer writer) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Output out = new Output(Channels.newChannel(bytes));
        writer.write(out);
        out.flush();
        return ByteBuffer.wrap(bytes.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * @param data the data
     * @param offset where the part starts
     * @param length the part's length in bytes
     * @return that part of the data, little-endian
     */
    public static ByteBuffer slice(ByteBuffer data, int offset, int length)
    {
        ByteBuffer part = data.duplicate();
        part.limit(offset + length).position(offset);
        return part.slice().order(ByteOrder.LITTLE_ENDIAN);
    }

    private static boolean isComplete(Path file)
    {
        if (!Files.isRegularFile(file))
        {
            return false;
        }
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ))
        {
            ByteBuffer header = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            return channel.size() >= HEADER && channel.read(header, 0) == 4 && header.getInt(0) == MAGIC;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    private static void removeStale(String prefix, Path current)
    {
        long now = System.currentTimeMillis();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(DIRECTORY, prefix + "*"))
        {
            for (Path file : files)
            {
                String name = file.getFileName().toString();
                boolean staleBin = name.endsWith(".bin") && !file.equals(current);
                boolean staleTemp = name.endsWith(".tmp") && now - Files.getLastModifiedTime(file).toMillis() > STALE_TEMP_MILLIS;
                if (staleBin || staleTemp)
                {
                    try
                    {
                        Files.deleteIfExists(file);
                    }
                    catch (IOException ignored)
                    {
                    }
                }
            }
        }
        catch (IOException ignored)
        {
        }
    }

    private static long resourceKey(String resource) throws IOException
    {
        URL url = Walker.class.getResource(resource);
        if (url != null)
        {
            URLConnection connection = url.openConnection();
            if (connection instanceof JarURLConnection)
            {
                JarEntry entry = ((JarURLConnection) connection).getJarEntry();
                if (entry != null && entry.getCrc() != -1)
                {
                    return entry.getCrc() << 32 ^ entry.getSize();
                }
            }
        }

        try (InputStream in = Walker.class.getResourceAsStream(resource))
        {
            if (in == null)
            {
                throw new IOException("Missing walker resource " + resource);
            }
            CRC32 crc = new CRC32();
            byte[] buffer = new byte[1 << 16];
            long size = 0;
            int read;
            while ((read = in.read(buffer)) != -1)
            {
                crc.update(buffer, 0, read);
                size += read;
            }
            return crc.getValue() << 32 ^ size;
        }
    }

    /**
     * Little-endian output for a map's data.
     */
    public static final class Output
    {
        private final WritableByteChannel channel;
        private final ByteBuffer buffer = ByteBuffer.allocate(1 << 20).order(ByteOrder.LITTLE_ENDIAN);

        private Output(WritableByteChannel channel)
        {
            this.channel = channel;
        }

        /**
         * @param value the int to write
         * @throws IOException when it cannot be written
         */
        public void writeInt(int value) throws IOException
        {
            if (buffer.remaining() < Integer.BYTES)
            {
                flush();
            }
            buffer.putInt(value);
        }

        /**
         * @param value the long to write
         * @throws IOException when it cannot be written
         */
        public void writeLong(long value) throws IOException
        {
            if (buffer.remaining() < Long.BYTES)
            {
                flush();
            }
            buffer.putLong(value);
        }

        private void flush() throws IOException
        {
            buffer.flip();
            while (buffer.hasRemaining())
            {
                channel.write(buffer);
            }
            buffer.clear();
        }
    }
}
