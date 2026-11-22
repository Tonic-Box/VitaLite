package com.tonic.services.streamer;

import lombok.Getter;

/**
 * How many frames a second at most the client hands to the streamer mirror; the frames it skips cost nothing extra.
 */
@Getter
public enum MirrorFrameRate
{
    SIXTY(60),
    THIRTY(30);

    private final int framesPerSecond;

    MirrorFrameRate(int framesPerSecond)
    {
        this.framesPerSecond = framesPerSecond;
    }

    @Override
    public String toString()
    {
        return framesPerSecond + " fps";
    }
}
