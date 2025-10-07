package com.tonic.services.ipc;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test class for IPC Channel - run client1() and client2() on different JVMs/machines
 * to test bidirectional peer-to-peer communication.
 */
public class ChannelTest
{
	private static final int PORT = 5000;
	private static final String GROUP = "230.0.0.1";
	private static final int MESSAGE_DELAY_MS = 2000;

	public static void c1()
	{
		try
		{
			Channel channel = new ChannelBuilder("Client-1")
					.port(PORT)
					.group(GROUP)
					.build();

			channel.addHandler(message -> {
				if (message.isFromSender(channel.getClientId())) {
					return;
				}
				System.out.println("[RECV] " + message.get("text"));
			});

			channel.start();

			channel.broadcast("greeting", Map.of(
					"text", "Hello"
			));

			Thread.sleep(1000);

			channel.broadcast("greeting", Map.of(
					"text", "Goodbye!"
			));

			Thread.sleep(1000);

			channel.stop();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private static volatile boolean shutdown = false;

	public static void c2()
	{
		shutdown = false;

		Channel channel = new ChannelBuilder("Client-2")
				.port(PORT)
				.group(GROUP)
				.build();

		channel.addHandler(message -> {
			if (message.isFromSender(channel.getClientId())) {
				return;
			}
			System.out.println("[RECV] " + message.get("text"));

			if(message.get("text").equals("Goodbye!"))
			{
				shutdown = true;
			}

			channel.broadcast("greeting", Map.of(
					"text", "(pong) " + message.get("text"),
					"sequence", 1
			));
		});

		channel.start();

		while(!shutdown)
		{
			try
			{
				Thread.sleep(100);
			}
			catch (Exception ignored) {}
		}
		channel.stop();
		shutdown = false;
	}
}
