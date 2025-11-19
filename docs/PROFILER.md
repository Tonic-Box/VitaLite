# VitaLite JVM Profiler

Comprehensive JVM profiling and performance monitoring tool with advanced visualization and analysis capabilities.

## Overview

The VitaLite JVM Profiler is a production-grade profiling tool that provides real-time monitoring, performance analysis, and optimization recommendations for your Java application. It features 8 specialized tabs covering every aspect of JVM performance, from garbage collection to CPU profiling to memory leak detection.

**Key Features:**
- Real-time resource monitoring with customizable time windows
- CPU sampling profiler with interactive flame graphs
- Memory leak detection with treemap visualization
- GC pause analysis with intelligent tuning recommendations
- Event timeline showing unified JVM events
- JIT compiler monitoring with escape analysis insights
- Thread profiling and stack trace analysis
- JVMTI interface for low-level JVM introspection

**Access:** Press the configured hotkey or call `ProfilerWindow.toggle()` to open/close the profiler.

---

## 1. Resource Monitor

Real-time monitoring of CPU, memory, threads, and garbage collection with historical charts and intelligent analysis.

### Features

#### 4 Live Charts
- **CPU & GC Activity:** Process CPU usage and garbage collection overhead
- **Heap Memory:** Used, committed, and max heap memory over time
- **Metaspace:** Class metadata memory usage and committed space
- **Thread Count:** Total and daemon thread counts

#### Summary Cards
Quick overview cards showing current:
- CPU usage percentage
- GC activity percentage
- Heap memory (used/max in MB)
- Metaspace (used/committed in MB)
- Thread counts (total with daemon count)

#### GC Pause Analysis
Real-time analysis of garbage collection pause events:

**Statistics:**
- Total number of GC pauses
- Total time spent in GC
- Average pause duration
- 95th and 99th percentile pause times
- Longest pause with details (type, action, cause, memory freed)

**Tuning Recommendations:**
Intelligent recommendations with severity levels (SUCCESS, INFO, WARNING, CRITICAL):

- **Excessive Pause Times:** Detects pauses >500ms (warning) or >1s (critical)
- **GC Frequency:** Warns if GC occurs >10 times per second
- **GC Overhead:** Alerts if >5% (warning) or >10% (critical) of runtime spent in GC
- **Heap Utilization:** Monitors heap pressure and over-provisioning
- **Algorithm Detection:** Identifies GC algorithm and provides algorithm-specific advice

Example recommendations:
```
[CRITICAL] Very Long GC Pauses
99th percentile pause time is 1250ms. This can cause severe application lag.
Suggestion: -XX:MaxGCPauseMillis=200 (reduce target pause time)

[WARNING] High Heap Utilization
Heap 85.3% full. Consider increasing heap size.
Suggestion: Increase heap size: -Xmx<larger_value>
```

### Controls

- **Pause/Resume:** Freeze monitoring to analyze current data
- **Time Window:** Select 1, 2, 5, or 10 minute history windows
- **Export CSV:** Export metrics to CSV file for external analysis
- **Force GC:** Trigger System.gc() for immediate garbage collection
- **Clear:** Reset all charts and history

### Best Practices

**Monitor During Load Testing:**
Run your application under realistic load while watching the Resource Monitor to identify:
- Peak memory usage
- GC pause patterns
- CPU bottlenecks
- Thread count growth

**Use GC Recommendations:**
The tuning advisor provides actionable JVM flags. Test recommendations in staging before production:
1. Note current baseline performance
2. Apply one recommendation at a time
3. Measure impact with same workload
4. Keep changes that improve performance

**Set Appropriate Time Windows:**
- **1-2 minutes:** Quick troubleshooting, identify immediate issues
- **5-10 minutes:** Long-term trends, capacity planning
- **Steady State:** Wait 5+ minutes after startup for JIT warmup

**Watch for Patterns:**
- Sawtooth heap pattern: Normal (allocation → GC → allocation)
- Steadily rising heap: Possible memory leak
- Frequent GC spikes: Heap too small or allocation rate too high
- High CPU with low heap usage: CPU-bound workload (not GC issue)

---

## 2. Sampling

CPU profiling through stack trace sampling with interactive flame graph visualization.

### Features

#### CPU Sampler
Periodically captures stack traces to identify hot methods and call patterns:

- **Sample Rate:** Configurable (10-1000ms intervals)
- **Sample Count:** Configurable buffer size (100-10000 samples)
- **Control:** Start/Stop/Clear sampling
- **Export:** Save samples to file for offline analysis

#### Stack Trace Table
View raw stack samples with:
- Timestamp
- Thread name
- Thread state
- Full stack trace

#### Interactive Flame Graph
Visual representation of CPU time spent in each method:

**How to Read:**
- **Width:** Proportional to CPU time (wider = more samples)
- **Height:** Call stack depth (bottom = entry points, top = leaf methods)
- **Color:** Package-based coloring for visual grouping

**Interactions:**
- **Hover:** Shows method name, sample count, and percentage
- **Click:** Zoom into selected method and its callees
- **Click Header:** Reset zoom to full view

**Identifying Hotspots:**
1. Wide boxes at the top = leaf methods consuming CPU
2. Wide boxes at bottom = frequently called entry points
3. Look for unexpected wide boxes (optimization targets)

### Workflow

**Basic Profiling:**
```
1. Start Sampling → Run your workload → Stop Sampling
2. Switch to Flame Graph tab
3. Look for wide boxes at the top (hot methods)
4. Click to zoom into interesting call paths
5. Optimize the widest leaf methods
```

**Finding CPU Bottlenecks:**
```
1. Set sample rate to 10-50ms for fine granularity
2. Sample during high-CPU period
3. In flame graph, identify:
   - Wide leaves = expensive methods
   - Tall stacks = deep call chains (inlining candidates)
   - Unexpected packages = third-party overhead
```

### Best Practices

**Sampling Configuration:**
- **Development:** 10-50ms rate, 1000-5000 samples (detailed profiling)
- **Production:** 100-500ms rate, 500-1000 samples (low overhead)
- **Quick Check:** 50ms rate, 500 samples (30 second snapshot)

**Interpreting Flame Graphs:**
- **Flat Top:** CPU distributed across many methods (normal)
- **Narrow Spikes:** Deep recursion or specific bottleneck
- **Wide Platform:** Most time in framework/library code
- **Wide Application Code:** Optimization opportunity

**Common Patterns:**
- **Database Calls:** Wide boxes in JDBC/connection code → query optimization needed
- **JSON Parsing:** Wide boxes in Jackson/Gson → consider faster serialization
- **String Operations:** Wide boxes in String concat → use StringBuilder
- **Reflection:** Wide boxes in Method.invoke → cache or use MethodHandles

**Performance Tips:**
- Sampling overhead is minimal (<1% at 100ms intervals)
- Higher sample rates = more detail but higher overhead
- Sample during steady-state load (skip JIT warmup period)
- Compare before/after flame graphs to validate optimizations

---

## 3. Leak Detector

Heap analysis and memory leak detection with table and treemap visualizations.

### Features

#### Heap Histogram
Table view of all classes loaded in heap:
- Class name
- Instance count
- Total memory usage
- Average instance size
- Sortable by any column

#### Interactive Treemap
Hierarchical visualization of heap memory distribution:

**Layout:**
- **Root Level:** Top 50 packages by memory usage
- **Nested Level:** Top 20 classes per package
- **Rectangle Size:** Proportional to memory consumption
- **Colors:** Size-based gradient (red=large, orange=medium, yellow=small, green=tiny)

**Interactions:**
- **Hover:** Shows package/class name and exact byte count
- **Click:** Drill down into package to see its classes
- **Click Breadcrumb:** Navigate back up the hierarchy

**Identifying Leaks:**
1. Look for unexpectedly large packages/classes
2. Drill down to see which classes dominate
3. Cross-reference with application logic
4. Use heap snapshot comparison (take before/after)

#### View Toggle
Switch between table and treemap views with a single button click.

#### Snapshot & Export
- **Take Snapshot:** Capture current heap state
- **Compare:** Diff two snapshots to see growth
- **Export:** Save histogram to CSV

### Workflow

**Basic Leak Detection:**
```
1. Start application and let it warm up
2. Take Snapshot #1
3. Run workload that might leak
4. Take Snapshot #2
5. Compare snapshots to see which classes grew
6. Switch to Treemap view
7. Look for unexpectedly large packages
8. Drill down to identify leak candidates
```

**Analyzing Specific Classes:**
```
1. In table view, sort by Total Memory (descending)
2. Check top 10 classes for:
   - Business objects that should be GC'd
   - Collections that might not be cleared
   - Caches without eviction policies
3. Click instance count to see growth trend
```

**Using Treemap for Visual Analysis:**
```
1. Switch to Treemap view
2. Red/large rectangles = memory hogs
3. Click large packages to drill down
4. Compare package proportions to expected distribution
5. Investigate unexpected large classes
```

### Best Practices

**Detecting Leaks:**
- **Growth Pattern:** Run workload 3+ times, compare snapshots
- **Expected vs Actual:** Know your domain objects' expected counts
- **After GC:** Force GC before snapshot to eliminate transient objects
- **Isolation:** Test suspected feature in isolation

**Common Leak Patterns:**
- **Static Collections:** HashMap/List in static fields never cleared
- **Event Listeners:** Registered but never unregistered
- **Thread Locals:** Not cleaned up after use
- **Cache Misuse:** Unbounded caches without eviction
- **Finalizers:** Objects with finalizers accumulating in queue

**Memory Optimization:**
- Large collections dominating heap → Consider pagination or streaming
- Many small objects → Object pooling or primitive collections
- String duplicates → String interning or deduplication
- Large byte arrays → Compression or off-heap storage

**Reading Treemap:**
- Concentrate on red/orange rectangles (largest consumers)
- Package-level view shows architectural memory distribution
- Class-level view shows specific optimization targets
- Breadcrumb shows current focus (Root > package > class)

---

## 4. Event Timeline

Unified timeline showing all JVM events across garbage collection, compilation, threads, and system activities.

### Features

#### Event Types
- **GC Events:** Young, Old, Mixed collections with pause times
- **Compilation:** JIT compilation of hot methods
- **Thread Events:** Thread start/stop events
- **Safepoints:** JVM safepoint operations
- **Class Loading:** Dynamic class loading events

#### Event Table
Chronological list (newest first) with:
- Timestamp (HH:mm:ss.SSS)
- Event type
- Description
- Duration (if applicable)
- Severity (LOW, MEDIUM, HIGH, CRITICAL)

#### Event Details Panel
Select any event to view:
- Full event type description
- Exact timestamp
- Duration
- Severity level
- Detailed event-specific information

#### Timeline Statistics
Real-time summary showing:
- Total events captured
- Breakdown by type (GC, Compilation, Thread)
- Time span covered
- Longest event duration

### Use Cases

**Correlating Events:**
```
Problem: Application occasionally freezes
Timeline Analysis:
1. Look for clusters of events around freeze time
2. Check if long GC pauses coincide with freeze
3. Look for thread events (deadlock, excessive creation)
4. Check for safepoint operations
```

**GC Pattern Analysis:**
```
1. Filter/observe GC events
2. Note frequency of Young vs Old collections
3. Check for Mixed collections (indicates G1GC)
4. Long pauses = upgrade to better GC algorithm
5. Frequent pauses = increase heap size
```

**Compilation Analysis:**
```
1. Watch JIT compilation events
2. Early compilations = hot code identified quickly
3. Late compilations = optimization opportunities
4. Many recompilations = unstable call sites
```

### Best Practices

**Monitoring Production:**
- Event timeline has minimal overhead (safe for production)
- Useful for post-mortem analysis of incidents
- Correlate with application logs for full picture

**Identifying Jitter:**
- Look for regular event patterns (normal)
- Irregular bursts of events = jitter
- Long gaps between GC = good (low allocation rate)
- Frequent small GCs = high allocation rate

**Thread Analysis:**
- Thread creation events should be rare after startup
- Frequent thread creation = thread pool misconfiguration
- Thread stops during runtime = possible resource leaks

---

## 5. JIT Compiler

Just-In-Time compiler monitoring with compilation statistics and optimization insights.

### Features

#### Compiler Status
- Compiler name (C1, C2, Graal, etc.)
- Total compilation time
- Time monitoring support
- Available processors

#### Compilation Levels
Understanding tiered compilation:
- **Level 0:** Interpreter (no compilation)
- **Level 1:** C1 Simple (fast compilation, basic optimizations)
- **Level 2:** C1 Limited Profile (profiling data collection)
- **Level 3:** C1 Full Profile (extensive profiling)
- **Level 4:** C2/JVMCI (full optimizations, escape analysis, inlining)

#### Escape Analysis Insights
Educational content explaining JIT optimizations:

**Escape Analysis:**
- Stack allocation of objects that don't escape
- Scalar replacement (replace object fields with locals)
- Lock elision (remove unnecessary synchronization)

**Common Optimizations:**
- Method inlining (inline small, frequently called methods)
- Loop unrolling (expand loops to reduce branch overhead)
- Dead code elimination (remove unused code paths)
- Constant folding (evaluate constant expressions at compile time)

**Performance Recommendations:**
Based on compilation time overhead:
- **Low (<1s):** Healthy, efficient JIT activity
- **Moderate (1-5s):** Monitor for excessive recompilation
- **High (>5s):** Possible deoptimization cycles, polymorphism issues

#### Compilation Controls
- **Force Compile:** Queue method for compilation at specific level
- **Deoptimize All:** Force recompilation of all methods (for testing)
- **Refresh Stats:** Update compilation statistics

### Best Practices

**Understanding Warmup:**
- JVM starts in interpreter mode (slow)
- Hot methods compile to Level 1 (C1 - fast compile)
- With profiling, promote to Level 4 (C2 - full optimization)
- Warmup takes 30 seconds to 5 minutes depending on workload

**Optimizing for JIT:**
- **Monomorphic Calls:** Single implementation = inlining
- **Bimorphic Calls:** Two implementations = inline both
- **Megamorphic Calls:** 3+ implementations = no inlining (slow)
- **Final Classes/Methods:** Enable better inlining decisions
- **Small Methods:** More likely to be inlined

**Avoiding Deoptimization:**
- Avoid changing class shapes after warmup
- Don't mix types in generic collections
- Stable control flow (avoid exception-based control)
- Consistent call sites (same implementation)

**Escape Analysis Benefits:**
Methods that benefit most from escape analysis:
- Methods creating temporary objects (StringBuilder, arrays)
- Methods using objects only locally (no field assignment)
- Methods with short-lived objects (within scope)

Example of optimization-friendly code:
```java
// Good: Object doesn't escape, can be stack-allocated
public int calculateSum(int[] values) {
    IntBuffer buffer = new IntBuffer(); // Local only
    for (int v : values) {
        buffer.add(v);
    }
    return buffer.sum();
}

// Bad: Object escapes to field
public int calculateSum(int[] values) {
    this.buffer = new IntBuffer(); // Escapes
    // Cannot stack-allocate
}
```

---

## 6. Threads

Thread profiling and analysis with stack trace inspection.

### Features

#### Active Thread List
Shows all threads with:
- Thread ID
- Thread name
- Thread state (RUNNABLE, WAITING, BLOCKED, etc.)

#### Stack Trace Viewer
Select any thread to view:
- Thread ID and name
- Current state
- Blocked time (time waiting for locks)
- Blocked count (number of times blocked)
- Full stack trace

#### Refresh
Manual refresh to capture current thread state.

### Use Cases

**Deadlock Detection:**
```
1. If application hangs, check Threads tab
2. Look for multiple BLOCKED threads
3. Check stack traces for lock acquisition
4. Identify circular lock dependencies
```

**High CPU Thread:**
```
1. Sort by thread state (RUNNABLE = consuming CPU)
2. Check stack trace to see what it's executing
3. Look for unexpected loops or busy-waits
4. Compare with Sampling flame graph for confirmation
```

**Thread Leak Detection:**
```
1. Note thread count in summary card
2. Run workload
3. Check for growing thread count
4. Investigate newly created threads
5. Look for pool misconfiguration
```

### Best Practices

**Thread States:**
- **RUNNABLE:** Executing code or waiting for OS (normal)
- **WAITING:** Waiting indefinitely (park, wait)
- **TIMED_WAITING:** Waiting with timeout (sleep, timed wait)
- **BLOCKED:** Waiting for monitor lock (contention)

**Performance:**
- High BLOCKED count = lock contention (use concurrent data structures)
- Many WAITING threads = normal for thread pools
- Unexpected RUNNABLE threads = possible busy-wait or infinite loop

**Thread Naming:**
Use descriptive thread names for easier debugging:
```java
Thread t = new Thread(task, "MyApp-Worker-" + id);
```

**Common Issues:**
- Thread pools not shutting down (executor.shutdown())
- Fire-and-forget threads never terminating
- Daemon threads preventing JVM shutdown
- Thread locals not cleaned up

---

## 7. VM Configuration

JVM and system configuration inspection.

### Features

#### VM Flags
Display of JVM properties:
- JVM name, version, vendor
- Runtime version
- Specification versions

#### Flag Modification
Experimental interface for runtime flag modification (limited support).

#### CPU Features & System Info
- Available processors (cores)
- Operating system name and version
- Architecture (x86_64, aarch64, etc.)

### Use Cases

**Validating Configuration:**
Check that your production JVM matches expected settings:
- Verify JVM version
- Confirm OS and architecture
- Check available cores for thread pool sizing

**Troubleshooting:**
When debugging issues, capture VM info for:
- Bug reports (JVM version specifics)
- Performance comparisons (hardware info)
- Compatibility checks (OS version)

### Best Practices

**Document Your Flags:**
Keep a record of JVM flags used in production:
```bash
java -XX:+PrintFlagsFinal -version > jvm_flags.txt
```

**Common Flags:**
- `-Xmx<size>`: Max heap size
- `-Xms<size>`: Initial heap size
- `-XX:+UseG1GC`: Use G1 garbage collector
- `-XX:MaxGCPauseMillis=<ms>`: GC pause time goal
- `-XX:+UseStringDeduplication`: Deduplicate strings (G1GC)

---

## 8. JVMTI / VM

Low-level JVM Tool Interface monitoring and diagnostics.

### Features

#### Status Cards
Visual indicators for:
- JVMTI availability
- VM interface availability
- System cores count

#### Live Charts
- **Memory Utilization:** Progress bar and pie chart
- **Thread Analysis:** Daemon vs user thread pie chart
- **Class Loading:** Bar chart of regular, interface, array classes

#### JVM Runtime Information
- JVM platform (name, version, vendor)
- Operating system details
- Runtime state (thread count, classes loaded, heap usage)
- VM initialization level
- Module system status
- Direct memory configuration
- Finalization queue depth

#### JVMTI Interface Components
Status of individual JVMTI components:
- **VMObjectAccess:** Thread enumeration, object sizing
- **AdvancedVMAccess:** Class metadata, field offsets
- **VM:** VM state, initialization levels

#### Comprehensive Diagnostics
Run full system diagnostics to generate report with:
- Memory analysis (utilization, GC efficiency)
- Thread analysis (total, alive, daemon counts)
- Class loading analysis (types, counts, percentages)
- Object size verification (test objects)
- JIT compiler status
- JVM runtime arguments

### Use Cases

**Deep Troubleshooting:**
When standard monitoring isn't enough:
1. Run full diagnostics
2. Check JVMTI component availability
3. Analyze memory utilization patterns
4. Review class loading statistics
5. Inspect finalization queue depth

**Production Health Check:**
Regular diagnostics as baseline:
- Establish normal memory utilization (60-80%)
- Track class loading trends
- Monitor finalization queue (should be near 0)
- Compare thread counts over time

### Best Practices

**JVMTI Availability:**
- Most features available on HotSpot JVM
- Some features require special JVM flags
- GraalVM native-image has limited support

**Finalization Queue:**
- High pending count (>100) = finalizer bottleneck
- Finalizers are deprecated, avoid in new code
- Use try-with-resources instead

**Direct Memory:**
- Shows max direct memory allocation
- Important for NIO, ByteBuffer operations
- Set with `-XX:MaxDirectMemorySize=<size>`

---

## General Best Practices

### Performance Profiling Workflow

**1. Establish Baseline:**
```
- Run application under normal load
- Monitor for 5-10 minutes (JIT warmup)
- Record resource monitor metrics
- Take heap snapshot
- Capture flame graph sample
```

**2. Load Testing:**
```
- Gradually increase load
- Watch for resource exhaustion
- Monitor GC pause patterns
- Check thread count growth
- Look for memory leaks
```

**3. Identify Bottlenecks:**
```
- CPU-bound: Flame graph shows hot methods
- Memory-bound: Heap fills quickly, frequent GC
- I/O-bound: Threads WAITING on I/O
- Lock contention: Threads BLOCKED
```

**4. Optimize:**
```
- Apply one change at a time
- Measure impact with profiler
- Compare flame graphs before/after
- Validate with load testing
```

### Avoiding Common Pitfalls

**Observer Effect:**
- Profiling adds overhead (especially sampling)
- Sample at lower rates in production (100-500ms)
- Disable sampling when not actively profiling

**Warmup Period:**
- Don't measure during first 1-2 minutes
- JIT needs time to optimize code
- First runs are not representative

**Heap Snapshots:**
- Taking snapshots triggers full GC
- Avoid during critical operations
- Snapshots are point-in-time (not continuous)

**GC Tuning:**
- Test one flag change at a time
- Document baseline before changes
- Some flags conflict (research before combining)

### Production vs Development

**Development:**
- Aggressive sampling (10-50ms)
- Frequent heap snapshots
- Enable all diagnostics
- Experiment with JIT controls

**Production:**
- Conservative sampling (100-500ms) or off
- Rare heap snapshots (on-demand only)
- Monitor Resource Monitor continuously
- Event Timeline for correlation
- GC analysis for tuning decisions

### Integration with Application Monitoring

**Combine Profiler Data With:**
- Application performance monitoring (APM)
- Log aggregation (correlate events with logs)
- Metrics systems (Prometheus, Grafana)
- Database query analyzers
- Network monitoring

**Correlation Example:**
```
1. APM shows slow endpoint response
2. Profiler flame graph shows hot method in that endpoint
3. Logs show database query during that time
4. Event timeline shows long GC pause during query
5. Conclusion: GC pause interrupted slow query, causing timeout
```

---

## Keyboard Shortcuts

- **Open/Close Profiler:** Configured hotkey (default varies)
- **Tab Navigation:** Ctrl+Tab / Ctrl+Shift+Tab
- **Refresh Active Tab:** F5
- **Export Data:** Ctrl+E (where applicable)

---

## Troubleshooting

### Profiler Won't Open
- Check if JVMTI is available (run diagnostics)
- Verify JVM supports required features
- Check console for error messages

### Charts Not Updating
- Ensure auto-refresh is enabled
- Check if monitoring is paused
- Verify timers are running (restart profiler)

### High Memory Usage from Profiler
- Reduce time window (2 minutes instead of 10)
- Lower sample buffer size
- Stop sampling when not needed
- Clear history buffers

### Missing JVMTI Features
- Some JVMs have limited JVMTI support
- Try newer JVM version
- Check if restricted by security manager
- Consult JVM documentation

---

## Advanced Tips

### Custom Export Format
Modify CSV export to include:
- Custom calculated fields
- Additional metrics
- Formatted for Excel/BI tools

### Automated Profiling
Schedule profiling sessions:
- Take heap snapshots every N hours
- Export metrics to time-series database
- Trigger sampling during specific events
- Automated comparison of snapshots

### Remote Profiling
Attach profiler to remote JVM:
- Enable JMX remote in target JVM
- Connect profiler via JMX URL
- Monitor production from development machine
- (Note: Ensure secure connection)

### Scripting Integration
Integrate with build/test pipelines:
- Profile performance tests automatically
- Compare flame graphs between versions
- Detect performance regressions
- Generate reports for CI/CD

---

## References

**JVM Documentation:**
- [HotSpot VM Options](https://chriswhocodes.com/)
- [Java Platform Standard Edition Documentation](https://docs.oracle.com/en/java/)
- [Garbage Collection Tuning Guide](https://docs.oracle.com/en/java/javase/17/gctuning/)

**Profiling Resources:**
- [JVM Performance Optimization](https://www.oreilly.com/library/view/java-performance-the/9781492056102/)
- [Flame Graph Visualization](https://www.brendangregg.com/flamegraphs.html)
- [JIT Compiler Insights](https://shipilev.net/)

**GC Algorithms:**
- G1GC: Low latency, pause time goal-oriented
- ZGC: Ultra-low latency (<10ms pauses)
- Shenandoah: Concurrent, low pause times
- Parallel GC: High throughput, longer pauses

---

## Version History

**Current Version:** Includes all 6 advanced profiling features
- GC Pause Analyzer & Tuning Advisor
- Event Timeline
- Auto-Tuning Advisor (embedded in GC recommendations)
- Interactive Flame Graph Generator
- Memory Layout Visualizer (Treemap)
- Escape Analysis Insights

**Future Enhancements:**
- JFR (Java Flight Recorder) integration
- Heap dump analysis
- Allocation profiling
- Off-heap memory tracking
- Network I/O profiling
- Database query profiling integration
