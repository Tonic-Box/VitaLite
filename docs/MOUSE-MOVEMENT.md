# Mouse Movement System

VitaLite includes a mouse movement subsystem that can:

- Record mouse movement trajectories from outbound mouse packets.
- Persist those trajectories to disk.
- Generate synthetic mouse movement samples and feed them into the click pipeline.

Source: `base-api/src/main/java/com/tonic/model/ui/VitaLiteOptionsPanel.java:onPacketSent`
Source: `base-api/src/main/java/com/tonic/services/mouserecorder/trajectory/TrajectoryService.java`
Source: `base-api/src/main/java/com/tonic/services/ClickManager.java`

## Recording and Storage

### How recording is enabled

Trajectory recording is toggled from the VitaLite options panel. When enabled, the code starts packet capture.
Source: `base-api/src/main/java/com/tonic/model/ui/VitaLiteOptionsPanel.java` (calls `TrajectoryService.startRecording()` and `stopRecording()`)

### Storage file

Trajectories are serialized to:

- `${user.home}/.runelite/vitalite/data/trajectories.dat`

Source: `base-api/src/main/java/com/tonic/services/mouserecorder/trajectory/TrajectoryService.java` (`SAVE_PATH`)

### Auto save interval

The trajectory service auto saves on a fixed interval.
Source: `base-api/src/main/java/com/tonic/services/mouserecorder/trajectory/TrajectoryService.java` (`AUTO_SAVE_INTERVAL_MS`)

## Generation

Trajectory generation is implemented by `TrajectoryGenerator`. It retrieves similar recorded trajectories, warps them, blends them, optionally applies noise, and can downsample and retime the result.

Source: `base-api/src/main/java/com/tonic/services/mouserecorder/trajectory/TrajectoryGenerator.java:generate`
Source: `base-api/src/main/java/com/tonic/services/mouserecorder/trajectory/DynamicTimeWarping.java`

## How movement generation is used during clicks

If `ClientConfig.mouseMovements` is enabled, the click manager generates movement samples before sending a click.
Source: `base-api/src/main/java/com/tonic/util/ClientConfig.java:shouldSpoofMouseMovemnt`
Source: `base-api/src/main/java/com/tonic/services/ClickManager.java:click`

### Enablement threshold

The UI disables the mouse movement toggle until at least 50 trajectories are present.
Source: `base-api/src/main/java/com/tonic/model/ui/VitaLiteOptionsPanel.java:updateTrajectoryQualityState`

The click manager also uses a trajectory count check when deciding whether to use realistic movement vs fallback behavior.
Source: `base-api/src/main/java/com/tonic/services/ClickManager.java:shouldUseRealisticMovement`

## Settings

Movement generation is configurable via `TrajectoryGeneratorConfig` (config group `TrajectoryGenerator`).
Source: `base-api/src/main/java/com/tonic/services/mouserecorder/trajectory/TrajectoryGeneratorConfig.java`
Source: `base-api/src/main/java/com/tonic/services/mouserecorder/trajectory/ui/TrajectorySettingsPanel.java`

Key defaults include:

- Retrieval count: `3`
- Variation analysis count: `5`
- Min similarity: `0.3`
- Noise type: `NONE`
- Max samples per movement: `5`
- Movement duration: `250` ms
- Min distance for trajectory: `50` px
- Max distance for instant jump: `120` px
- Instant jump chance: `0.15`
- Adaptive profiling: enabled
- Rapid mode: enabled

Source: `base-api/src/main/java/com/tonic/services/mouserecorder/trajectory/TrajectoryGeneratorConfig.java`
