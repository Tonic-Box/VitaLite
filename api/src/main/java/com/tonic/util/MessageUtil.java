package com.tonic.util;

import com.tonic.Static;
import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;

import java.awt.*;

public class MessageUtil {

    public static void sendChatMessage(Color color, String message) {
        ChatMessageManager chatMessageManager = Static.getInjector().getInstance(ChatMessageManager.class);
        ChatMessageBuilder chatMessageBuilder = new ChatMessageBuilder();
        String chatMessage = color == null ? chatMessageBuilder.append(message).build() : chatMessageBuilder.append(color, message).build();
        QueuedMessage queuedMessage = QueuedMessage.builder().type(ChatMessageType.ENGINE).value(chatMessage).build();

        chatMessageManager.queue(queuedMessage);
    }

    public static void sendChatMessage(String message) {
        sendChatMessage(null, message);
    }

}
