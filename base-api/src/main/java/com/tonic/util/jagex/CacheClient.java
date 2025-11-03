package com.tonic.util.jagex;

import com.tonic.Static;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.Getter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheClient {
    private static final Logger logger = Logger.getLogger(CacheClient.class.getName());

    private static final int INIT_JS5_REMOTE_CONNECTION = 15;
    private static final int CONNECTION_STATUS_SUCCESS = 0;
    private static final Path MASTER_INDEX_CACHE_PATH = Static.VITA_DIR
            .resolve("cache_master_index.dat");

    @Getter
    private static volatile byte[] cachedMasterIndex = null;
    private static volatile byte[] lastFetchedIndex = null;

    static {
        loadCachedMasterIndex();
    }

    /**
     * Checks if a cache update is available WITHOUT caching
     * @return true if update available, false otherwise
     */
    public static boolean checkForUpdate(int revision) {
        return checkForUpdate("oldschool1.runescape.com", 43594, revision, new int[]{0, 0, 0, 0}, 10);
    }

    /**
     * Checks if a cache update is available WITHOUT caching
     */
    public static boolean checkForUpdate(String host, int port, int revision, int[] keys, int timeoutSeconds) {
        byte[] serverIndex = fetchMasterIndex(host, port, revision, keys, timeoutSeconds);

        if (serverIndex == null) {
            logger.warning("Failed to fetch master index from server");
            return false;
        }

        lastFetchedIndex = serverIndex;

        if (cachedMasterIndex == null) {
            logger.info("No cached master index found - update available");
            return false;
        }

        boolean updateAvailable = !Arrays.equals(cachedMasterIndex, serverIndex);
        logger.info(updateAvailable ? "Cache update available" : "Cache is up to date");
        return updateAvailable;
    }

    /**
     * Updates the cached master index with the last fetched version
     * @return true if successfully cached, false otherwise
     */
    public static boolean updateCache() {
        if (lastFetchedIndex == null) {
            logger.warning("No fetched index available to cache");
            return false;
        }
        return cacheMasterIndex(lastFetchedIndex);
    }

    /**
     * Fetches and immediately caches the master index
     * @return true if successfully fetched and cached
     */
    public static boolean fetchAndCache(int revision) {
        return fetchAndCache("oldschool1.runescape.com", 43594, revision, new int[]{0, 0, 0, 0}, 10);
    }

    /**
     * Fetches and immediately caches the master index
     */
    public static boolean fetchAndCache(String host, int port, int revision, int[] keys, int timeoutSeconds) {
        byte[] serverIndex = fetchMasterIndex(host, port, revision, keys, timeoutSeconds);

        if (serverIndex == null) {
            logger.warning("Failed to fetch master index");
            return false;
        }

        lastFetchedIndex = serverIndex;
        return cacheMasterIndex(serverIndex);
    }

    /**
     * Fetches the master index from the server
     */
    private static byte[] fetchMasterIndex(String host, int port, int revision, int[] keys, int timeoutSeconds) {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            UpdateCheckHandler handler = new UpdateCheckHandler(revision, keys);

            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new JS5Decoder());
                            ch.pipeline().addLast(handler);
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            Channel channel = future.channel();

            byte[] masterIndex = handler.awaitMasterIndex(timeoutSeconds);
            channel.close().sync();

            if (masterIndex == null) {
                return null;
            }

            return Arrays.copyOfRange(masterIndex, 8, masterIndex.length);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error fetching master index", e);
            return null;
        } finally {
            group.shutdownGracefully();
        }
    }

    private static void loadCachedMasterIndex() {
        try {
            if (Files.exists(MASTER_INDEX_CACHE_PATH)) {
                cachedMasterIndex = Files.readAllBytes(MASTER_INDEX_CACHE_PATH);
                logger.info("Loaded cached master index (" + cachedMasterIndex.length + " bytes)");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load cached master index", e);
        }
    }

    private static boolean cacheMasterIndex(byte[] masterIndex) {
        try {
            Files.createDirectories(MASTER_INDEX_CACHE_PATH.getParent());
            Files.write(MASTER_INDEX_CACHE_PATH, masterIndex);
            cachedMasterIndex = masterIndex;
            logger.info("Cached master index (" + masterIndex.length + " bytes)");
            return true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to cache master index", e);
            return false;
        }
    }

    public static void clearCache() {
        cachedMasterIndex = null;
        lastFetchedIndex = null;
        try {
            Files.deleteIfExists(MASTER_INDEX_CACHE_PATH);
            logger.info("Cache cleared");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to delete cache file", e);
        }
    }

    private static class UpdateCheckHandler extends SimpleChannelInboundHandler<ByteBuf> {
        private final int revision;
        private final int[] keys;
        private final CountDownLatch connectionLatch = new CountDownLatch(1);
        private final AtomicReference<byte[]> masterIndexRef = new AtomicReference<>();
        private boolean connectionInitialized = false;
        private boolean waitingForMasterIndex = false;

        UpdateCheckHandler(int revision, int[] keys) {
            this.revision = revision;
            this.keys = keys;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            ByteBuf buf = Unpooled.buffer(21);
            buf.writeByte(INIT_JS5_REMOTE_CONNECTION);
            buf.writeInt(revision);
            for (int key : keys) {
                buf.writeInt(key);
            }
            ctx.writeAndFlush(buf);
        }

        @Override
        protected void messageReceived(ChannelHandlerContext ctx, ByteBuf msg) {
            if (!connectionInitialized) {
                int connectionStatus = msg.readByte() & 0xFF;
                connectionInitialized = true;

                if (connectionStatus == CONNECTION_STATUS_SUCCESS) {
                    ByteBuf request = Unpooled.buffer(4);
                    request.writeByte(1);
                    request.writeByte(255);
                    request.writeShort(255);
                    ctx.writeAndFlush(request);
                    waitingForMasterIndex = true;
                } else {
                    connectionLatch.countDown();
                }
            } else if (waitingForMasterIndex) {
                byte[] data = new byte[msg.readableBytes()];
                msg.readBytes(data);
                masterIndexRef.set(data);
                connectionLatch.countDown();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            connectionLatch.countDown();
            ctx.close();
        }

        byte[] awaitMasterIndex(int timeoutSeconds) {
            try {
                if (connectionLatch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                    return masterIndexRef.get();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private static class JS5Decoder extends ByteToMessageDecoder {
        private boolean expectingConnectionStatus = true;
        private ByteBuf accumulator = null;
        private int expectedSize = -1;

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            if (!in.isReadable()) return;

            if (expectingConnectionStatus && in.readableBytes() >= 1) {
                ByteBuf slice = in.readSlice(1);
                slice.retain();
                out.add(slice);
                expectingConnectionStatus = false;
                return;
            }

            if (!expectingConnectionStatus) {
                if (accumulator == null && in.readableBytes() >= 8) {
                    int readerIndex = in.readerIndex();
                    in.skipBytes(3); // archive + group
                    int compression = in.readByte() & 0xFF;
                    expectedSize = in.readInt();

                    if (compression != 0) {
                        expectedSize += 4;
                    }

                    in.readerIndex(readerIndex);
                    accumulator = Unpooled.buffer(8 + expectedSize);
                }

                if (accumulator != null) {
                    int needed = (8 + expectedSize) - accumulator.writerIndex();
                    int toRead = Math.min(needed, in.readableBytes());

                    if (toRead > 0) {
                        accumulator.writeBytes(in, toRead);
                    }

                    if (accumulator.writerIndex() >= 8 + expectedSize) {
                        accumulator.retain();
                        out.add(accumulator);
                        accumulator = null;
                        expectedSize = -1;
                    }
                }
            }
        }
    }
}