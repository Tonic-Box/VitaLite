# Click Manager

The click manager controls how click packets are produced and optionally how mouse movement samples are generated before a click.
Source: `base-api/src/main/java/com/tonic/services/ClickManager.java`

## Click Strategies

The configured click strategy is stored in `ClientConfig.clickStrategy`.
Source: `base-api/src/main/java/com/tonic/util/ClientConfig.java:getClickStrategy`

Available strategies:

- `STATIC` - click at a configured static point (`clickPointX`, `clickPointY`).
- `RANDOM` - click at a random point inside the viewport bounds.
- `CONTROLLED` - click at a random point inside a plugin provided click box shape.
- `NONE` - no click packet is generated.

Source: `base-api/src/main/java/com/tonic/services/ClickStrategy.java`
Source: `base-api/src/main/java/com/tonic/services/ClickManager.java:click` (switch behavior)

## Plugin Integration: Controlled Click Boxes

If you want your plugin to support controlled clicking, set a click box for the current target before interacting, and then clear it when you are done.

### Direct API

```java
import com.tonic.services.ClickManager;
import java.awt.Rectangle;

ClickManager.queueClickBox(new Rectangle(100, 100, 50, 20));
// ... interact using other APIs ...
ClickManager.clearClickBox();
```

Source: `base-api/src/main/java/com/tonic/services/ClickManager.java:queueClickBox`

### Convenience helpers (`ClickManagerUtil`)

`ClickManagerUtil` provides helpers for common shapes, such as NPCs, objects, items, and widgets.
These helpers run on the client thread via `Static.invoke(...)`.

Source: `api/src/main/java/com/tonic/util/ClickManagerUtil.java`

Example:

```java
import com.tonic.api.entities.NpcAPI;
import com.tonic.data.wrappers.NpcEx;
import com.tonic.util.ClickManagerUtil;
import com.tonic.services.ClickManager;

NpcEx banker = NpcAPI.search().withName("Banker").nearest();
ClickManagerUtil.queueClickBox(banker);
NpcAPI.interact(banker, "Bank");
ClickManager.clearClickBox();
```

## Failure Modes and Fallbacks

### Controlled strategy without a click box

If the strategy is `CONTROLLED` but no shape was queued, the click manager logs a warning and falls back to the static click behavior.
Source: `base-api/src/main/java/com/tonic/services/ClickManager.java:click`

### Infinite loops with `queueClickBox`

`queueClickBox` only sets a shape. It does not clear it automatically. If your plugin sets a click box and never clears it, future clicks can keep using that shape.
Source: `base-api/src/main/java/com/tonic/services/ClickManager.java:clearClickBox`

### Strategy `NONE`

When the strategy is `NONE`, `ClickManager.click(...)` does not match any switch case and does not emit a click packet.
Source: `base-api/src/main/java/com/tonic/services/ClickManager.java:click`
