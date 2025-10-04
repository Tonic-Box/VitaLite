package com.tonic.plugins.profiles.jagex.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JagCharacter {
    private final String displayName;
    private final String accountId;

    @Override
    public String toString() {
        return "{ \"displayName\": \"" + displayName + "\", \"accountId\": \"" + accountId + "\"}";
    }
}
