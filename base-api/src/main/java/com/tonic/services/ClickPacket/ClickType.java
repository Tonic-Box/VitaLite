package com.tonic.services.ClickPacket;

import lombok.Getter;

import java.awt.*;

@Getter
public enum ClickType {
    MOVEMENT(new Color(0, 255, 0)),      // Green for walking
    ACTOR(new Color(255, 255, 0)),         // Yellow for NPCs
    OBJECT(new Color(0, 255, 255)),      // Cyan for objects
    ITEM(new Color(255, 165, 0)),        // Orange for items
    GROUND_ITEM(new Color(255, 0, 255)), // Magenta for ground items
    WIDGET(new Color(138, 43, 226)),     // Purple for widgets
    GENERIC(new Color(255, 255, 255));   // White for generic

    private final Color color;

    ClickType(Color color) {
        this.color = color;
    }

}
