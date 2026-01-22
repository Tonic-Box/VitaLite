# JVM Profiler

VitaLite includes a JVM profiler window implemented in `ProfilerWindow`.
Source: `base-api/src/main/java/com/tonic/services/profiler/ProfilerWindow.java`

This is separate from the lightweight timing utility `com.tonic.util.Profiler`.
Source: `base-api/src/main/java/com/tonic/util/Profiler.java`

## Opening the profiler window

You can open the profiler window from the VitaLite options panel.
Source: `base-api/src/main/java/com/tonic/model/ui/VitaLiteOptionsPanel.java` (Profiler button)

You can also toggle it programmatically:

```java
import com.tonic.services.profiler.ProfilerWindow;
ProfilerWindow.toggle();
```

Source: `base-api/src/main/java/com/tonic/services/profiler/ProfilerWindow.java:toggle`

## Tabs

The profiler window contains the following tabs:

- Resource Monitor
- Sampling
- Recording
- Leak Detector
- Event Timeline
- JIT Compiler
- Threads
- VM Configuration
- JVMTI / VM

Source: `base-api/src/main/java/com/tonic/services/profiler/ProfilerWindow.java` (tabbed pane setup)

### Resource Monitor

The resource monitor collects metric snapshots and keeps a rolling history window.
It also supports exporting metrics to CSV.

Source: `base-api/src/main/java/com/tonic/services/profiler/ProfilerWindow.java:updateResourceMonitor`

### Sampling

CPU and memory samplers are used by the profiler and also exposed by the local profiler server.
Source: `base-api/src/main/java/com/tonic/services/profiler/server/ProfilerServer.java` (`CPUSampler`, `MemorySampler`)

### VM Configuration

VM flag modification is not available at runtime and the UI shows a message instead.
Source: `base-api/src/main/java/com/tonic/services/profiler/ProfilerWindow.java:setVMFlag`

## Local profiler HTTP API

VitaLite includes a local HTTP server that exposes profiler data and controls over `127.0.0.1`.

- Default port: `8787`
- Base URL: `http://127.0.0.1:8787`

Source: `base-api/src/main/java/com/tonic/services/profiler/server/ProfilerServer.java` (`port`, `start`)

The server registers a set of endpoints under `/profiler/...` including status, exact timing, sampling, resource metrics, thread inspection, and explicit GC requests.
Source: `base-api/src/main/java/com/tonic/services/profiler/server/ProfilerServer.java` (createContext calls and class header comment)
