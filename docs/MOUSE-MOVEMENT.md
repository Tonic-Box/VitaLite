# Mouse Movement System

## Overview

VitaLite's mouse movement system uses **trajectory-based generation** to create natural, human-like mouse movements that are indistinguishable from manual play. Unlike simple interpolation or bezier curves, this system learns from your actual mouse movements and replays variations of them.

## How It Works

### 1. Recording Phase
The system passively records your mouse movements during manual play by intercepting `OP_MOUSE_MOVEMENT` packets from the game client. Each packet contains:
- Multiple compressed mouse samples (position + timing data)
- Movement metadata (distance, direction, duration, sample count)
- Compression statistics (Small/Medium/Large/XLarge delta encoding)

Recorded trajectories are stored in a database and analyzed to build an **Adaptive Movement Profile** that learns your natural movement characteristics at different distances.

### 2. Generation Phase
When the bot needs to move the mouse, it:
1. **Queries** the database for similar recorded movements (by distance, direction, angle, and velocity)
2. **Warps** the retrieved trajectories using Dynamic Time Warping (DTW) to fit the new start/end positions
3. **Blends** multiple candidates together for variation
4. **Applies noise** (optional) for micro-variations
5. **Calculates target sample count** by analyzing low-complexity similar trajectories to determine natural point count variation
6. **Downsamples** using spatial distribution (arc-length based) to match target sample count while preserving curve shape
7. **Retimes** to match target duration while preserving natural acceleration patterns

The result is a movement that looks and feels like you moved the mouse manually.

### 3. Rapid Action Mode
Special logic detects when movements occur in quick succession (e.g., woodcutting: inventory→tree→inventory) and automatically:
- Reduces sample counts for faster-looking paths (1-3 samples vs 5+)
- Prefers faster recorded trajectories through velocity-aware matching
- Creates snappier movements that match real player behavior during repetitive actions

---

## Settings Reference

### Natural Movement Settings

These control the baseline characteristics of generated mouse movements.

#### **Adaptive Profiling**
- **Type**: Checkbox
- **Default**: `Enabled`
- **Description**: Learns movement characteristics (sample counts, durations) from your recorded trajectories. When enabled, the generator adapts to your personal mouse movement style at different distances. Disable to use fixed values instead.

#### **Max Samples Per Movement**
- **Range**: 1-10 samples
- **Default**: `5 samples`
- **Description**: Maximum number of mouse position samples in a generated path. Real players typically use 2-10 samples depending on distance. Lower = faster/snappier, higher = smoother/slower.
- **Notes**: Overridden by Adaptive Profiling when enabled. Rapid Action Mode may reduce this further.

#### **Movement Duration**
- **Range**: 50-500 ms
- **Default**: `250 ms`
- **Description**: Base duration for mouse movements. Real movements vary by distance - short movements are faster, long movements take more time.
- **Notes**: Overridden by Adaptive Profiling when enabled.

#### **Min Distance for Trajectory**
- **Range**: 0-200 pixels
- **Default**: `50 px`
- **Description**: Minimum distance required to use trajectory-based generation. Movements below this threshold become instant jumps (1-2 samples) to mimic natural fast clicks.

#### **Max Distance for Instant Jump**
- **Range**: 50-250 pixels
- **Default**: `120 px`
- **Description**: Maximum distance eligible for random instant jumps. Short movements have a chance to skip trajectory generation entirely.

#### **Instant Jump Chance**
- **Range**: 0-50%
- **Default**: `15%`
- **Description**: Probability that eligible short movements (< Max Distance for Instant Jump) will use instant jump (1-2 samples) instead of full trajectory. Mimics natural "flick" movements.

---

### Rapid Action Mode Settings

Detects and optimizes movements during rapid, repetitive actions like woodcutting or combat.

#### **Enable Rapid Mode**
- **Type**: Checkbox
- **Default**: `Enabled`
- **Description**: Activates intelligent sample reduction when movements occur in quick succession, making repetitive actions look more natural.

#### **Time Threshold**
- **Range**: 100-5000 ms
- **Default**: `1800 ms` (3 game ticks)
- **Description**: Maximum time between movements for Rapid Mode to trigger. Movements closer together than this threshold get reduced sample counts. Set lower for more aggressive triggering.

#### **Short Distance**
- **Range**: 50-200 pixels
- **Default**: `100 px`
- **Description**: Distance threshold for "short" rapid movements. These get the most aggressive sample reduction (typically 1 sample).

#### **Medium Distance**
- **Range**: 100-400 pixels
- **Default**: `200 px`
- **Description**: Distance threshold for "medium" rapid movements. These get moderate sample reduction (typically 2 samples).
- **Notes**: Movements above this threshold are "long" and get minimal reduction (typically 3 samples).

#### **Sample Reduction**
- **Range**: 30-90%
- **Default**: `60%`
- **Description**: Multiplier for sample count reduction in Rapid Mode. 60% means movements use 60% of normal sample count (e.g., 5 samples → 3 samples).
- **Notes**: Rapid mode does NOT affect instant jump chance - jumps are only based on distance and the base instant jump chance setting.

#### **Random Speedup Chance**
- **Range**: 0-100%
- **Default**: `0%` (disabled)
- **Description**: Probability to apply Rapid Mode speedup even to movements outside the time threshold. Adds unpredictability - sometimes you click fast even during slow sequences.
- **Example**: 10% = 1 in 10 "slow" movements will randomly be fast.

---

### Retrieval & Blending Settings

Control how recorded trajectories are selected and combined.

#### **Retrieval Count**
- **Range**: 1-10 paths
- **Default**: `3 paths`
- **Description**: Number of similar recorded trajectories to retrieve and blend together. Retrieved paths are warped and combined to create the final movement. Higher = more variation but slower.

#### **Variation Analysis Count**
- **Range**: 1-10 paths
- **Default**: `5 paths`
- **Description**: Number of similar trajectories to analyze when calculating natural sample count variation. The system examines this many paths, filters for low-complexity movements (straight paths with realistic sample density), and uses their observed point counts to determine appropriate variation. Should be equal to or higher than Retrieval Count.
- **Notes**: Only low-complexity paths (curvature < 0.3, sample density < 0.05 samples/px) are used to prevent curved training paths from inflating point counts.

#### **Min Similarity**
- **Range**: 0-100%
- **Default**: `30%`
- **Description**: Minimum similarity score required for a recorded trajectory to be used. Lower = more trajectories match (more variation), higher = stricter matching (more consistent).
- **Notes**: If no trajectories meet the threshold, falls back to linear interpolation.

#### **Blend Randomness**
- **Range**: 0-100%
- **Default**: `30%`
- **Description**: Amount of randomness when blending multiple trajectories. 0% = equal weights, 100% = completely random weights. Adds natural variation between generated movements.

---

### Noise Settings

Add micro-variations to generated paths for subtle randomness.

#### **Noise Type**
- **Options**: `NONE`, `WHITE`, `CORRELATED`
- **Default**: `NONE`
- **Description**: Type of noise to apply to generated paths:
  - **NONE**: No noise, paths follow trajectories exactly
  - **WHITE**: Random independent noise per sample (jittery)
  - **CORRELATED**: Smooth noise that affects nearby samples (natural tremor)

#### **White Noise Sigma**
- **Range**: 0.0-5.0
- **Default**: `1.5`
- **Description**: Standard deviation of white noise in pixels. Higher = more random jitter per sample.
- **Only active when**: Noise Type = WHITE

#### **Correlated Noise Sigma**
- **Range**: 0.0-5.0
- **Default**: `2.0`
- **Description**: Standard deviation of correlated noise in pixels. Higher = more smooth deviation from path.
- **Only active when**: Noise Type = CORRELATED

#### **Correlated Noise Correlation**
- **Range**: 0-100%
- **Default**: `70%`
- **Description**: Correlation between consecutive noise samples. Higher = smoother noise (looks like natural hand tremor), lower = more independent (closer to white noise).
- **Only active when**: Noise Type = CORRELATED

---

## Common Configurations

### Conservative (Safest)
For players who want movements that closely match their recorded play:
- Adaptive Profiling: **Enabled**
- Noise Type: **NONE**
- Retrieval Count: **5 paths**
- Variation Analysis Count: **7-10 paths**
- Min Similarity: **50%**
- Rapid Mode: **Enabled** (default settings)

### Aggressive (Fastest)
For high-intensity activities like combat or speed training:
- Max Samples: **3 samples**
- Movement Duration: **150 ms**
- Instant Jump Chance: **30%**
- Rapid Mode Time Threshold: **1200 ms**
- Rapid Mode Sample Reduction: **50%**

### Natural Variation (Most Human-Like)
For players who want maximum variation and unpredictability:
- Retrieval Count: **5 paths**
- Blend Randomness: **50%**
- Noise Type: **CORRELATED**
- Correlated Noise Sigma: **2.5**
- Random Speedup Chance: **10-15%**

### Recorded-Only (Purist)
For players who want to use ONLY their recorded movements:
- Min Similarity: **70%**
- Blend Randomness: **10%**
- Noise Type: **NONE**
- Instant Jump Chance: **0%**

---

## Tips & Best Practices

### Recording Quality Data
- **Play manually** for 30-60 minutes doing varied activities
- **Include different movement types**: short clicks, long drags, inventory management, combat
- **Avoid AFK periods** during recording
- **Clear bad data**: Delete the database if you accidentally recorded during bot usage

### Tuning for Your Activity
- **Skilling (woodcutting, fishing)**: Enable Rapid Mode, increase Random Speedup Chance slightly
- **Combat**: Reduce Movement Duration, increase Instant Jump Chance
- **Questing**: Use default settings, focus on good recorded data
- **PvP**: Lower sample counts, faster durations, more instant jumps

### Troubleshooting

**Movements look robotic:**
- Increase Blend Randomness (40-50%)
- Enable Correlated Noise (sigma 2.0-3.0)
- Increase Random Speedup Chance (5-10%)
- Record more varied training data

**Movements are too slow:**
- Enable Rapid Mode
- Reduce Movement Duration (150-200 ms)
- Increase Instant Jump Chance (20-30%)

**Bot gets stuck/misses clicks:**
- Increase Min Distance for Trajectory (75-100 px)
- Reduce noise levels
- Check that you have enough recorded trajectories (50+ recommended)

**Too much variation (inconsistent):**
- Increase Min Similarity (40-50%)
- Reduce Blend Randomness (10-20%)
- Disable noise
- Reduce Retrieval Count (1-2 paths)

---

## Technical Details

### Trajectory Database
Trajectories are stored with metadata:
- Distance, angle, direction
- Sample count, duration, velocity
- Compression statistics
- Timestamp for adaptive profiling

### Similarity Matching (Velocity-Aware)
Trajectories are matched using a weighted scoring system:
- **Distance similarity** (40%): How close the distances are
- **Angle similarity** (35%): How similar the directions are
- **Velocity similarity** (15%): How similar the movement speeds are
- **Curvature similarity** (10%): How similar the curve shapes are

During rapid mode, the system queries with reduced expected duration, which increases the target velocity and naturally prefers faster recorded trajectories for more realistic rapid movements.

### DTW (Dynamic Time Warping)
Allows trajectories to be stretched/compressed in time to fit new distances while preserving natural acceleration patterns.

### Sample Count Calculation (Complexity-Aware)
To determine how many points a generated path should have:
1. Retrieves N similar trajectories (configured via Variation Analysis Count)
2. Filters for low-complexity paths only (curvature < 0.3, sample density < 0.05 samples/px)
3. Calculates average sample count from filtered trajectories
4. Applies small variation (±1 sample) around the observed average
5. Caps at configured Max Samples Per Movement

This prevents curved/complex training paths from inflating sample counts. For example, if training data includes a curved 200px path with 15 points and a straight 200px path with 3 points, only the straight path influences the calculation since they have the same Euclidean distance but different complexity.

### Spatial Downsampling (Arc-Length Based)
When reducing sample count, the system:
1. Calculates cumulative arc length (distance traveled) along the curve
2. Selects points at evenly spaced arc lengths (e.g., 0%, 33%, 66%, 100%)
3. Preserves curve shape by maintaining spatial distribution

This ensures rapid mode paths remain curved (not straight lines) even with fewer samples.

### Acceleration-Preserving Retiming
When adjusting movement duration:
1. Extracts relative temporal positions from the selected points (where they were in original timing)
2. Scales those positions to the target duration (e.g., 250ms → 150ms for rapid mode)
3. Preserves natural acceleration curves (slow start → fast middle → slow end)

This ensures movements look realistic with proper velocity profiles, not constant-speed robotic motion.

### Adaptive Profiling
Analyzes your recorded data to learn:
- Average sample count by distance
- Average duration by distance
- Movement speed patterns
- Acceleration characteristics

Updated automatically when new trajectories are recorded.

### Packet Encoding
Generated movements are encoded back into OSRS packet format using the same compression schemes the real client uses:
- **Small Delta** (2 bytes): ±32px, 0-7 ticks
- **Medium Delta** (3 bytes): ±128px, 0-127 ticks
- **Large Delta** (5 bytes): Absolute position, 0-31 ticks
- **XLarge Delta** (6 bytes): Absolute position, 0-8191 ticks

This ensures generated movements are indistinguishable from real movements at the packet level.

---

## FAQ

**Q: How many recorded trajectories do I need?**
A: Minimum 20-30 for basic functionality, 50-100 for good variation, 200+ for excellent coverage of all movement types.

**Q: Can I share my trajectory database?**
A: Technically yes, but not recommended. Movement patterns are personal and using someone else's data may not match your account's typical behavior patterns.

**Q: Will this work without any recorded data?**
A: Yes, it falls back to natural interpolation with adaptive defaults. But recorded data significantly improves quality.

**Q: Does Rapid Mode make it more detectable?**
A: No - Rapid Mode mimics how real players move during repetitive actions. Manual play analysis shows the same fast/slow patterns.

**Q: Should I use noise?**
A: Not necessary for most users. Trajectory-based generation already has natural variation. Only use noise if you understand what it does.

**Q: How often does Adaptive Profiling update?**
A: Automatically whenever new trajectories are recorded. No manual refresh needed.

---

## Related Documentation
- [Click Manager](CLICKMANAGER.md) - Click timing and interaction system
- [Features](FEATURES.md) - Overview of all bot features
- [SDK Documentation](SDK-DOCS.md) - Developer API reference
