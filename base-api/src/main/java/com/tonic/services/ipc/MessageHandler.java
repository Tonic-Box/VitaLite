package com.tonic.services.ipc;

/**
 * Callback interface for handling incoming IPC messages.
 * Implement this to receive and process messages from other clients.
 */
@FunctionalInterface
public interface MessageHandler
{
	/**
	 * Called when a message is received on the channel.
	 *
	 * @param message The received message
	 */
	void onMessage(Message message) throws Exception;

	/**
	 * Called when an error occurs during message processing.
	 * Default implementation does nothing - override for custom error handling.
	 *
	 * @param error The error that occurred
	 */
	default void onError(Throwable error)
	{
		// Default: silent fail
	}
}
