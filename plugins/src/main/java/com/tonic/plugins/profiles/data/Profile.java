package com.tonic.plugins.profiles.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Profile {
    private String identifier = "";

    private boolean isJagexAccount = false;
    private String username = "";
    private String password = "";
    private String characterName = "";
    private String sessionId = "";
    private String characterId = "";
    private String bankPin = "";
}
