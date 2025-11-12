package com.tonic.services.mouserecorder.trajectory;

import com.tonic.util.config.ConfigGroup;
import com.tonic.util.config.ConfigKey;
import com.tonic.util.config.VitaConfig;

/**
 * Configuration interface for trajectory generator tuning parameters.
 * Controls retrieval, blending, and noise generation behavior.
 */
@ConfigGroup("TrajectoryGenerator")
public interface TrajectoryGeneratorConfig extends VitaConfig
{
    @ConfigKey(value = "retrievalCount", defaultValue = "3")
    int getRetrievalCount();
    @ConfigKey(value = "retrievalCount")
    void setRetrievalCount(int count);

    @ConfigKey(value = "minSimilarity", defaultValue = "0.3")
    double getMinSimilarity();
    @ConfigKey(value = "minSimilarity")
    void setMinSimilarity(double similarity);

    @ConfigKey(value = "noiseType", defaultValue = "NONE")
    String getNoiseType();
    @ConfigKey(value = "noiseType")
    void setNoiseType(String type);

    @ConfigKey(value = "whiteNoiseSigma", defaultValue = "1.5")
    double getWhiteNoiseSigma();
    @ConfigKey(value = "whiteNoiseSigma")
    void setWhiteNoiseSigma(double sigma);

    @ConfigKey(value = "correlatedNoiseSigma", defaultValue = "2.0")
    double getCorrelatedNoiseSigma();
    @ConfigKey(value = "correlatedNoiseSigma")
    void setCorrelatedNoiseSigma(double sigma);

    @ConfigKey(value = "correlatedNoiseCorrelation", defaultValue = "0.7")
    double getCorrelatedNoiseCorrelation();
    @ConfigKey(value = "correlatedNoiseCorrelation")
    void setCorrelatedNoiseCorrelation(double correlation);

    @ConfigKey(value = "blendRandomness", defaultValue = "0.3")
    double getBlendRandomness();
    @ConfigKey(value = "blendRandomness")
    void setBlendRandomness(double randomness);

    @ConfigKey(value = "movementTimeoutMs", defaultValue = "100")
    int getMovementTimeoutMs();
    @ConfigKey(value = "movementTimeoutMs")
    void setMovementTimeoutMs(int timeoutMs);

    @ConfigKey(value = "visualizationHistoryCount", defaultValue = "20")
    int getVisualizationHistoryCount();
    @ConfigKey(value = "visualizationHistoryCount")
    void setVisualizationHistoryCount(int count);

    // Natural movement configuration to match real player behavior
    @ConfigKey(value = "maxSamplesPerMovement", defaultValue = "5")
    int getMaxSamplesPerMovement();
    @ConfigKey(value = "maxSamplesPerMovement")
    void setMaxSamplesPerMovement(int samples);

    @ConfigKey(value = "movementDurationMs", defaultValue = "250")
    int getMovementDurationMs();
    @ConfigKey(value = "movementDurationMs")
    void setMovementDurationMs(int durationMs);

    @ConfigKey(value = "minDistanceForTrajectory", defaultValue = "50")
    int getMinDistanceForTrajectory();
    @ConfigKey(value = "minDistanceForTrajectory")
    void setMinDistanceForTrajectory(int distance);

    @ConfigKey(value = "maxDistanceForInstantJump", defaultValue = "120")
    int getMaxDistanceForInstantJump();
    @ConfigKey(value = "maxDistanceForInstantJump")
    void setMaxDistanceForInstantJump(int distance);

    @ConfigKey(value = "instantJumpChance", defaultValue = "0.15")
    double getInstantJumpChance();
    @ConfigKey(value = "instantJumpChance")
    void setInstantJumpChance(double chance);

    @ConfigKey(value = "useAdaptiveProfiling", defaultValue = "true")
    boolean shouldUseAdaptiveProfiling();
    @ConfigKey(value = "useAdaptiveProfiling")
    void setUseAdaptiveProfiling(boolean enabled);
}
