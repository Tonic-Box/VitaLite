# Click Manager
The click manager enables users to control the behavior of how clicks are crafted. This includes clicking at static points, clicking at random points, clicking in defined controlled areas (must be supported by plugin developer), or not sending clicks at all. When you set the strategy to `STATIC` you will see a panel show where you can define the x/y for the static click location.

## Configuration
In the VitaLite options sidebar, there is a dropdown for configuring this option.

## Developers
If you want to support controlled clicks, you will need to integrate your automation with the ClickManager API.

At any point during your automation you can call:
```
ClickManager.queueClickBox(Shape shape)
```

This sets the area the click manager picks a random point from when the strategy is `CONTROLLED`. It stays set until it's cleared with `ClickManager.clearClickBox()` or replaced by a new shape.

There is also a Util class for setting this up easier:
## ClickManagerUtil
```
//Queue click on a TileObject
ClickManagerUtil.queueClickBox(TileObjectEx object)

//Queue click on an actor
ClickManagerUtil.queueClickBox(ActorEx<?> actor)

//Queue click on an item
ClickManagerUtil.queueClickBox(ItemEx item)

//Queue click on a ground item
ClickManagerUtil.queueClickBox(TileItemEx item)

//Queue click on a widget, or an interface by id
ClickManagerUtil.queueClickBox(Widget widget)
ClickManagerUtil.queueClickBoxInterface(int interfaceId)
```

## Notes
If the click box is not set, but the user has the strategy set to `CONTROLLED`, it will fallback to `STATIC` strategy.