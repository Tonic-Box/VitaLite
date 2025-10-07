package com.tonic.services.ipc;

import java.net.NetworkInterface;
import java.util.UUID;

public class ChannelBuilder {
    protected final String clientId;
    protected final String clientName;
    protected int port = 5000;
    protected String multicastGroup = "230.0.0.1";
    protected int ttl = 1;
    protected NetworkInterface networkInterface;

    /**
     * Create a channel builder with auto-generated client ID.
     *
     * @param clientName Display name for this client
     */
    public ChannelBuilder(String clientName)
    {
        this(UUID.randomUUID().toString(), clientName);
    }

    /**
     * Create a channel builder with specific client ID.
     *
     * @param clientId   Unique identifier for this client
     * @param clientName Display name for this client
     */
    public ChannelBuilder(String clientId, String clientName)
    {
        if (clientId == null || clientId.trim().isEmpty())
        {
            throw new IllegalArgumentException("clientId cannot be null or empty");
        }
        if (clientName == null || clientName.trim().isEmpty())
        {
            throw new IllegalArgumentException("clientName cannot be null or empty");
        }
        this.clientId = clientId;
        this.clientName = clientName;
    }

    /**
     * Set the multicast port (default: 5000).
     */
    public ChannelBuilder port(int port)
    {
        if (port < 1024 || port > 65535)
        {
            throw new IllegalArgumentException("Port must be between 1024 and 65535");
        }
        this.port = port;
        return this;
    }

    /**
     * Set the multicast group address (default: 230.0.0.1).
     * Must be in range 224.0.0.0 to 239.255.255.255.
     */
    public ChannelBuilder group(String multicastGroup)
    {
        if (multicastGroup == null || multicastGroup.trim().isEmpty())
        {
            throw new IllegalArgumentException("Multicast group cannot be null or empty");
        }
        this.multicastGroup = multicastGroup;
        return this;
    }

    /**
     * Set the TTL (time-to-live) for multicast packets (default: 1 = local network).
     * 0 = same host, 1 = same subnet, 32 = same site, 255 = unrestricted
     */
    public ChannelBuilder ttl(int ttl)
    {
        if (ttl < 0 || ttl > 255)
        {
            throw new IllegalArgumentException("TTL must be between 0 and 255");
        }
        this.ttl = ttl;
        return this;
    }

    /**
     * Set the network interface to use for multicast (optional).
     */
    public ChannelBuilder networkInterface(NetworkInterface networkInterface)
    {
        this.networkInterface = networkInterface;
        return this;
    }

    /**
     * Build the channel instance.
     */
    public Channel build()
    {
        return new Channel(this);
    }
}
