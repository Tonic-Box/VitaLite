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
}
