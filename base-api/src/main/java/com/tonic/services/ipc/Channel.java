package com.tonic.services.ipc;

import com.tonic.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Multicast-based IPC channel for peer-to-peer bidirectional communication.
 * No master-slave relationship - all nodes are equal participants.
 *
 * <p>Usage example:
 * <pre>{@code
 * Channel channel = new Channel.Builder("MyClient")
 *     .port(5000)
 *     .group("230.0.0.1")
 *     .build();
 *
 * channel.addHandler(msg -> {
 *     System.out.println("Received: " + msg.get("data"));
 * });
 *
 * channel.start();
 * channel.broadcast("hello", Map.of("data", "Hello peers!"));
 * channel.stop();
 * }</pre>
 */
public class Channel
{
	private static final int BUFFER_SIZE = 65536;

	private final String clientId;
	private final String clientName;
	private final int port;
	private final String multicastGroup;
	private final int ttl;
	private final NetworkInterface networkInterface;

	private MulticastSocket socket;
	private InetAddress group;
	private final AtomicBoolean running = new AtomicBoolean(false);
	private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "IPC-Channel-Receiver");
		t.setDaemon(true);
		return t;
	});

	private final CopyOnWriteArrayList<MessageHandler> handlers = new CopyOnWriteArrayList<>();
	private final Map<String, Long> recentMessages = new ConcurrentHashMap<>();
	private static final long DUPLICATE_WINDOW_MS = 5000;

	Channel(ChannelBuilder builder)
	{
		this.clientId = builder.clientId;
		this.clientName = builder.clientName;
		this.port = builder.port;
		this.multicastGroup = builder.multicastGroup;
		this.ttl = builder.ttl;
		this.networkInterface = builder.networkInterface;
	}

	/**
	 * Start the channel - begins listening for messages.
	 */
	public synchronized void start()
	{
		try
		{
			if (running.get())
			{
				return;
			}

			socket = new MulticastSocket(port);
			group = InetAddress.getByName(multicastGroup);

			if (networkInterface != null)
			{
				socket.setNetworkInterface(networkInterface);
			}

			socket.setTimeToLive(ttl);
			socket.joinGroup(group);

			running.set(true);
			executor.submit(this::receiveLoop);
		}
		catch (Exception e)
		{
			Logger.error(e);
			e.printStackTrace();
		}
	}

	/**
	 * Stop the channel - stops listening and releases resources.
	 */
	public synchronized void stop()
	{
		if (!running.get())
		{
			return;
		}

		running.set(false);

		try
		{
			if (socket != null && group != null)
			{
				socket.leaveGroup(group);
			}
		}
		catch (Exception e)
		{
			// Ignore
		}

		if (socket != null && !socket.isClosed())
		{
			socket.close();
		}

		executor.shutdown();
		recentMessages.clear();
	}

	/**
	 * Broadcast a message to all peers on the channel.
	 */
	public void broadcast(String type, Map<String, Object> payload) throws Exception
	{
		Message message = new Message.Builder(clientId, clientName)
			.type(type)
			.putAll(payload)
			.build();
		broadcast(message);
	}

	/**
	 * Broadcast a pre-built message to all peers.
	 */
	public void broadcast(Message message) throws Exception
	{
		if (!running.get())
		{
			throw new IllegalStateException("Channel not started");
		}

		byte[] data = serialize(message);
		DatagramPacket packet = new DatagramPacket(data, data.length, group, port);
		socket.send(packet);

		// Track our own message to avoid duplicate processing
		recentMessages.put(message.getMessageId(), System.currentTimeMillis());
	}

	/**
	 * Add a message handler to receive incoming messages.
	 */
	public void addHandler(MessageHandler handler)
	{
		if (handler != null)
		{
			handlers.add(handler);
		}
	}

	/**
	 * Remove a message handler.
	 */
	public void removeHandler(MessageHandler handler)
	{
		handlers.remove(handler);
	}

	/**
	 * Get this client's unique ID.
	 */
	public String getClientId()
	{
		return clientId;
	}

	/**
	 * Get this client's display name.
	 */
	public String getClientName()
	{
		return clientName;
	}

	/**
	 * Check if the channel is currently running.
	 */
	public boolean isRunning()
	{
		return running.get();
	}

	private void receiveLoop()
	{
		byte[] buffer = new byte[BUFFER_SIZE];

		while (running.get())
		{
			try
			{
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);

				byte[] data = new byte[packet.getLength()];
				System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());

				Message message = deserialize(data);

				if (message != null && !isDuplicate(message))
				{
					dispatchMessage(message);
				}
			}
			catch (Exception e)
			{
				if (running.get())
				{
					notifyError(e);
				}
			}
		}
	}

	private boolean isDuplicate(Message message)
	{
		long now = System.currentTimeMillis();

		// Clean old entries
		recentMessages.entrySet().removeIf(e -> now - e.getValue() > DUPLICATE_WINDOW_MS);

		// Check if we've seen this message recently
		Long timestamp = recentMessages.putIfAbsent(message.getMessageId(), now);
		return timestamp != null;
	}

	private void dispatchMessage(Message message)
	{
		for (MessageHandler handler : handlers)
		{
			try
			{
				handler.onMessage(message);
			}
			catch (Exception e)
			{
				try
				{
					handler.onError(e);
				}
				catch (Exception ignored)
				{
					// Handler error handling failed - nothing we can do
				}
			}
		}
	}

	private void notifyError(Throwable error)
	{
		for (MessageHandler handler : handlers)
		{
			try
			{
				handler.onError(error);
			}
			catch (Exception ignored)
			{
				// Handler error handling failed - nothing we can do
			}
		}
	}

	private byte[] serialize(Message message) throws Exception
	{
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
		     ObjectOutputStream oos = new ObjectOutputStream(bos))
		{
			oos.writeObject(message);
			oos.flush();
			return bos.toByteArray();
		}
	}

	private Message deserialize(byte[] data) throws Exception
	{
		try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
		     ObjectInputStream ois = new ObjectInputStream(bis))
		{
			return (Message) ois.readObject();
		}
	}
}
