package com.tonic.services.ipc;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Formatted message structure for IPC communication.
 * Each message contains client identification, routing info, and payload data.
 */
public class Message implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final String messageId;
	private final String senderId;
	private final String senderName;
	private final long timestamp;
	private final String type;
	private final Map<String, Object> payload;

	private Message(Builder builder)
	{
		this.messageId = builder.messageId != null ? builder.messageId : UUID.randomUUID().toString();
		this.senderId = builder.senderId;
		this.senderName = builder.senderName;
		this.timestamp = builder.timestamp;
		this.type = builder.type;
		this.payload = new HashMap<>(builder.payload);
	}

	public String getMessageId()
	{
		return messageId;
	}

	public String getSenderId()
	{
		return senderId;
	}

	public String getSenderName()
	{
		return senderName;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public String getType()
	{
		return type;
	}

	public Map<String, Object> getPayload()
	{
		return new HashMap<>(payload);
	}

	public Object get(String key)
	{
		return payload.get(key);
	}

	public <T> T get(String key, Class<T> type)
	{
		Object value = payload.get(key);
		return type.isInstance(value) ? type.cast(value) : null;
	}

	public boolean isFromSender(String senderId)
	{
		return this.senderId.equals(senderId);
	}

	public boolean isType(String type)
	{
		return this.type.equals(type);
	}

	@Override
	public String toString()
	{
		return String.format("Message{id=%s, from=%s(%s), type=%s, time=%s}",
			messageId, senderName, senderId, type, Instant.ofEpochMilli(timestamp));
	}

	public static class Builder
	{
		private String messageId;
		private final String senderId;
		private final String senderName;
		private long timestamp = System.currentTimeMillis();
		private String type = "default";
		private final Map<String, Object> payload = new HashMap<>();

		public Builder(String senderId, String senderName)
		{
			if (senderId == null || senderId.trim().isEmpty())
			{
				throw new IllegalArgumentException("senderId cannot be null or empty");
			}
			if (senderName == null || senderName.trim().isEmpty())
			{
				throw new IllegalArgumentException("senderName cannot be null or empty");
			}
			this.senderId = senderId;
			this.senderName = senderName;
		}

		public Builder messageId(String messageId)
		{
			this.messageId = messageId;
			return this;
		}

		public Builder timestamp(long timestamp)
		{
			this.timestamp = timestamp;
			return this;
		}

		public Builder type(String type)
		{
			if (type != null && !type.trim().isEmpty())
			{
				this.type = type;
			}
			return this;
		}

		public Builder put(String key, Object value)
		{
			if (key != null && !key.trim().isEmpty())
			{
				this.payload.put(key, value);
			}
			return this;
		}

		public Builder putAll(Map<String, Object> data)
		{
			if (data != null)
			{
				this.payload.putAll(data);
			}
			return this;
		}

		public Message build()
		{
			return new Message(this);
		}
	}
}
