package com.tonic.services.mouserecorder.markov;

import com.tonic.util.config.ConfigGroup;
import com.tonic.util.config.ConfigKey;
import com.tonic.util.config.VitaConfig;

/**
 * Configuration interface for Markov mouse generator tuning parameters.
 * Provides persistent storage of generation settings.
 */
@ConfigGroup("MarkovGenerator")
public interface MarkovGeneratorConfig extends VitaConfig
{
    // Bias Configuration
    @ConfigKey(value = "biasMinimum", defaultValue = "0.15")
    double getBiasMinimum();
    @ConfigKey(value = "biasMinimum")
    void setBiasMinimum(double bias);

    @ConfigKey(value = "biasMaximum", defaultValue = "0.75")
    double getBiasMaximum();
    @ConfigKey(value = "biasMaximum")
    void setBiasMaximum(double bias);

    @ConfigKey(value = "biasDistance", defaultValue = "400.0")
    double getBiasDistance();
    @ConfigKey(value = "biasDistance")
    void setBiasDistance(double distance);

    // Generation Parameters
    @ConfigKey(value = "maxSteps", defaultValue = "200")
    int getMaxSteps();
    @ConfigKey(value = "maxSteps")
    void setMaxSteps(int steps);

    @ConfigKey(value = "targetTolerance", defaultValue = "10.0")
    double getTargetTolerance();
    @ConfigKey(value = "targetTolerance")
    void setTargetTolerance(double tolerance);

    // Temporal Jitter
    @ConfigKey(value = "temporalJitterEnabled", defaultValue = "false")
    boolean isTemporalJitterEnabled();
    @ConfigKey(value = "temporalJitterEnabled")
    void setTemporalJitterEnabled(boolean enabled);

    @ConfigKey(value = "temporalJitterAmount", defaultValue = "0.3")
    double getTemporalJitterAmount();
    @ConfigKey(value = "temporalJitterAmount")
    void setTemporalJitterAmount(double amount);

    // Bezier Smoothing
    @ConfigKey(value = "bezierSmoothingEnabled", defaultValue = "false")
    boolean isBezierSmoothingEnabled();
    @ConfigKey(value = "bezierSmoothingEnabled")
    void setBezierSmoothingEnabled(boolean enabled);

    @ConfigKey(value = "bezierTension", defaultValue = "0.5")
    double getBezierTension();
    @ConfigKey(value = "bezierTension")
    void setBezierTension(double tension);

    // Advanced Options
    @ConfigKey(value = "velocityAwareBiasing", defaultValue = "false")
    boolean isVelocityAwareBiasingEnabled();
    @ConfigKey(value = "velocityAwareBiasing")
    void setVelocityAwareBiasingEnabled(boolean enabled);

    @ConfigKey(value = "secondOrderMarkov", defaultValue = "false")
    boolean isSecondOrderMarkovEnabled();
    @ConfigKey(value = "secondOrderMarkov")
    void setSecondOrderMarkovEnabled(boolean enabled);
}
