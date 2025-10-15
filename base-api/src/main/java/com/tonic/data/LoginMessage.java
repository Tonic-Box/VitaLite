package com.tonic.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LoginMessage {
    private final String line1;
    private final String line2;
    private final String line3;

    @Override
    public String toString() {
        return (line1.trim() + " " + line2.trim() + " " + line3.trim()).trim();
    }
}
