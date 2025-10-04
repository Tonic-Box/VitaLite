package com.tonic.plugins.profiles.jagex.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public class JagLoginToken {
    private String sessionId;
    private JagCharacter[] characters;

    @Override
    public String toString() {
        return "JagLoginToken: sessionId: "
                + sessionId + " characters: "
                + Arrays.toString(characters);
    }
}
