package com.tonic.services;

import lombok.Getter;

/**
 * How much of the viewport's width and height the CPU renderer draws the 3D scene at before stretching it to fit.
 */
@Getter
public enum RenderScale
{
    FULL(100),
    THREE_QUARTERS(75),
    TWO_THIRDS(66),
    HALF(50),
    THIRD(33),
    QUARTER(25),
    FIFTH(20),
    TENTH(10),
    TWENTIETH(5);

    private final int percent;

    RenderScale(int percent)
    {
        this.percent = percent;
    }

    @Override
    public String toString()
    {
        return percent + "%";
    }
}
