# VitaLite JVM Profiler

A built-in profiler for finding what makes the client slow: where CPU time goes, what blocks threads, what allocates, what fills the heap, and how the JVM itself is set up.

**Open it** from the VitaLite panel under **Debug → Profiler**, or call `ProfilerWindow.toggle()`. Clicking it again brings back a minimised window, or closes an open one.

Nothing heavy runs on its own. Charts and tables refresh only while their tab is showing. Recordings, heap snapshots and GCs happen only when you ask for them.

| Tab | What it answers |
|---|---|
| [Resource Monitor](#resource-monitor) | How busy are the CPU, heap, metaspace and threads, and how much do GC pauses hurt frames? |
| [Recording](#recording) | Which methods use the CPU, what threads wait on, and what allocates? |
| [Heap Snapshots](#heap-snapshots) | What is on the heap, and what grew between two moments? |
| [Threads](#threads) | Which threads are busy or stuck, and on what? |
| [JVM](#jvm) | How was this JVM launched, which flags are set, and how are objects laid out? |
| [JIT](#jit) | What has been compiled, at which tier, and how full is the code cache? |

---

## Resource Monitor

Live charts of CPU and GC load, heap, metaspace and thread counts, sampled once a second while the window is open.

- **Time window:** 1, 2, 5 or 10 minutes of history. Changing it keeps the history you have.
- **Pause / Resume:** stops sampling without closing the window.
- **Export CSV:** the samples in the current window.
- **Force GC:** runs a full collection off the UI thread.
- **Summary cards:** the latest CPU %, GC %, heap, metaspace and thread counts.

**GC Pause Statistics** cover the last 500 pauses: count, total, average, 95th and 99th percentile, and the longest pause with its collector, cause, and how much heap it freed.

**Frame Impact (last 60 s)** shows what GC does to frames. It counts pauses longer than 16 ms (a dropped frame at 60 FPS) and 33 ms (30 FPS), and gives the longest pause and the GC time per minute. VitaLite runs the Serial collector. Short, rare pauses are fine; a steady stream of 16 ms+ pauses means visible stutter.

---

## Recording

Samples the client with Java Flight Recorder (JFR). Press **Record**, use the client, then press **Stop**. Stopping and analysis run in the background.

**Options**
- **Sample every:** 1 to 50 ms. Shorter periods catch more, at more cost; 10 ms is a good default.
- **Record allocations:** also records allocation events. This costs noticeably more, so turn it on only when you want the Allocations view.
- **Show:** all methods, one package (such as `com.tonic`) or one class. Methods outside the filter are hidden, but the views never pretend that methods on either side of a hidden one called each other.

**Views**
- **Methods:** every sampled method. *Self* counts samples where it was running; *Total* counts samples where it was anywhere on the stack. Recursion counts once per sample, so Total never passes 100%. The ≈ms columns are samples × period: an estimate, not measured time. Rows are shaded by self share, and selecting one fills Method Details.
- **Method Details:** the selected method's callers and callees, counted from frames that were really next to each other.
- **Call Tree:** either the top-down call tree (outermost frames first), or *Callers*, which starts at the running methods and walks out to whoever called them. Nodes load as you expand them.
- **Flame Graph:** outermost frames at the top, width proportional to samples. Click a frame to focus it, scroll to zoom, drag with the middle button to pan.
- **Allocations:** bytes allocated by class and by allocating method. JFR samples allocations as threads fill their allocation buffers, so the bytes are estimates.
- **Blocked / Native:** threads inside native code, usually waiting on sockets, locks, the window system or the GPU. These samples stay out of the CPU views, because a thread waiting on a socket is not using the CPU.
- **Exact Timing:** timings from code you instrument yourself; see [Exact timing](#exact-timing-methodprofiler).

The summary warns when stacks were cut short. JFR keeps 64 frames by default, so very deep stacks lose their outermost callers.

---

## Heap Snapshots

A real class histogram of the live heap: instances and bytes per class. It's the same data as `jmap -histo`.

- **Capture Snapshot:** runs a full GC first, so only live objects count. Expect a short pause in the client. The last 20 snapshots are kept, even if you close the window.
- **Baseline / Compare:** pick two snapshots to see each class's count and bytes before, after and the change, largest growth first. With no baseline you see the whole heap of one snapshot.
- **Changed only**, a class filter and **Export CSV**.

**Finding a leak:** capture a baseline, do the thing you suspect leaks a few times, capture again, and look for classes that keep growing. Classes loaded by more than one class loader, such as a hot-reloaded plugin, are added together.

---

## Threads

Every live thread, sorted by CPU use.

- **CPU %:** the share of one core the thread used since the last refresh.
- **Blocked / Waited:** how often and for how long the thread blocked on a lock or waited. The profiler turns on the JVM's contention monitoring while this tab is open, so the times count from when the tab was opened.
- **Stack and locks:** the selected thread's stack, the monitors it holds, and what it is waiting on and who holds it.
- The selection stays put across refreshes. There's a name filter.

---

## JVM

- **Overview:**
  - JVM version and vendor, PID, uptime, garbage collector and compiler;
  - the exact launch arguments;
  - direct and mapped buffer use against the direct memory limit;
  - metaspace and compressed class space;
  - classes loaded, total and unloaded;
  - the finalizer queue, current and peak.
- **Flags:** every HotSpot flag with its type, value, kind and origin (default, command line, ergonomic and so on). There's a filter and a *changed from default only* toggle. Flags the JVM allows changing at runtime are shown in bold and can be edited by double-clicking the value.
- **Properties:** system properties, plus the startup properties the JDK hides from `System.getProperties()`.
- **Class Layout:** enter a loaded class name to see each field's real offset and size. You also get the exact bytes per instance, including object header, reference size, alignment and `@Contended` padding.

Launch arguments, hidden properties, the direct memory limit, the finalizer queue and class layouts read JVM internals. The profiler unlocks them the first time this tab is used, with no launch flags needed. If that fails, the Overview says privileged access is unavailable and those parts are skipped.

---

## JIT

- **Compiler:** compiler name, total compile time, whether compilation is on, each code heap's use against its size and peak, and counts of compiled methods, adapters and code blobs.
- **Compiled Methods:** press **Load** to list every compiled method with compile ID, tier (C1 variants or C2; tier 0 means a native-method wrapper), state, signature and machine-code size. You can filter by name or tier.
- **Compile Queue:** what the compiler threads are working on now and what is waiting.
- **Directives:** rules that change how methods compile: *Don't compile*, *Always inline* or *Never inline*. Methods are written like `net.runelite.client.Foo.bar`, with `*` as a wildcard. Rules apply to compilations from then on; code already compiled keeps running until it is recompiled. **Clear All** also removes directives added by other tools.

---

## Exact timing (MethodProfiler)

For precise timings of your own code, instrument it with `MethodProfiler` and watch the **Exact Timing** view or the API. Calls cost nothing while collection is off.

```java
MethodProfiler.setEnabled(true);

try (MethodProfiler.TimingScope ignored = MethodProfiler.time("MyPlugin.scan")) {
    scan();
}

MethodProfiler.time("MyPlugin.draw", () -> draw());

Result result = MethodProfiler.timeResult("MyPlugin.compute", () -> compute());

MethodProfiler.begin("MyPlugin.load");
load();
MethodProfiler.end("MyPlugin.load");
```

| Method | What it does |
|---|---|
| `setEnabled(boolean)`, `isEnabled()` | Turn collection on or off. |
| `begin(label)`, `end(label)` | Time the code between two calls on the same thread. |
| `time(label)` | Returns a `TimingScope` that ends the timing when closed. |
| `time(label, Runnable)`, `timeResult(label, Supplier)` | Time one call. |
| `getAllTimings()`, `getAllTimingsByAverage()`, `getTiming(label)` | Read the timings. |
| `hasData()`, `getMethodCount()` | Check what has been collected. |
| `generateReport()`, `generateCSVReport()` | Text or CSV reports. |
| `clear()` | Forget every timing. |

---

## HTTP API

Scripts and LLM agents can drive the profiler over HTTP. Start the server with **Start API** in the profiler's status bar. It listens on `http://127.0.0.1:8787` and keeps running after the window closes, until you stop it or the client exits. `GET /` lists every endpoint.

| Endpoint | What it does |
|---|---|
| `GET /profiler/status` | Server, exact timing and recording status. |
| `POST /profiler/jfr/start` | Start a recording. `?period=N` sets the period in ms (default 10); `?allocations=true` records allocations too; `?package=com.example` or `?class=com.example.Foo` shows only those methods and credits allocations to the first matching frame. Returns 409 when one is already running. |
| `POST /profiler/jfr/stop` | Stop and analyze it. |
| `GET /profiler/jfr` | Every method of the last recording. |
| `GET /profiler/hotspots` | The methods that ran the most. `?top=N` (default 10). |
| `GET /profiler/method?name=...` | A method's samples, callers and callees; use the fully qualified `Class.method`. |
| `GET /profiler/native` | Methods threads were blocked in. `?top=N`. |
| `GET /profiler/allocations` | Allocation bytes by class and by method, if the recording had allocations. `?top=N`. |
| `GET /profiler/exact`, `GET /profiler/exact/csv` | Exact timings as JSON or CSV. |
| `POST /profiler/exact/enable`, `/disable`, `/clear` | Control exact timing. |
| `POST /profiler/histogram` | Live heap histogram, after a full GC. `?top=N` (default 50). |
| `GET /profiler/resources` | CPU, GC, heap, metaspace, code cache and thread counts now. |
| `GET /profiler/threads` | Every thread with state and blocked/waited counts. `?stacks=true&depth=N` adds stacks. |
| `POST /profiler/gc` | Run a full GC and report what it freed. |

A typical session: `POST /profiler/jfr/start`, use the client for a few seconds, `POST /profiler/jfr/stop`, then `GET /profiler/hotspots` and `GET /profiler/method?name=...` on whatever stands out. Each method carries a confidence: HIGH at 100+ samples, MEDIUM at 30+, LOW at 10+, otherwise VERY_LOW.

---

## Troubleshooting

- **"Privileged JVM access unavailable":** the JVM refused the internal access the JVM tab uses. The other tabs keep working.
- **Recording says Flight Recorder is not available:** the Java runtime was built without JFR. Use a full JDK 11 build such as Temurin.
- **The client hitches when taking a heap snapshot:** expected. A snapshot needs a full GC to count only live objects.
- **Blocked times read 0:** they only count blocking that started after the Threads tab was opened.
