# VitaLite SDK Documentation

## Overview

### Key Classes
```
📦 VitaLite SDK
├──Static       # Static access to client object, and others. Also Static access
│               # to helper methods such as inboking on/getting returns from client 
│               # thread.
│
├──Logger       # Static logger for logging to the embedded console window.
```

### API
Everything in the built-in API is designed with thread safety in mind
```
📦 VitaLite SDK
├── 🎮 Game APIs
│   ├── 🏃‍♂️ entities/
│   │   ├── ActorAPI          # Actor api
│   │   ├── NpcAPI            # NPC api
│   │   ├── PlayerAPI         # Player api
│   │   ├── TileItemAPI       # Tile item api
│   │   └── TileObjectAPI     # Tile object api
│   │
│   ├── 🎯 game/
│   │   ├── ClientScriptAPI   # CS2 script execution
│   │   ├── CombatAPI         # Combat calculations & state
│   │   ├── GameAPI           # Core game utilities
│   │   ├── HouseAPI          # Player-owned house api
│   │   ├── MovementAPI       # Walking api
│   │   ├── QuestAPI          # Quest progress & completion
│   │   ├── SceneAPI          # Scene/region management
│   │   ├── SkillAPI          # Skill levels & experience
│   │   └── VarAPI            # Game variable access
│   │
│   └── 🔄 threaded/
│       ├── Cannon            # Dwarf cannon automation
│       ├── Delays            # Smart timing utilities
│       ├── Dialogues         # Dialogue interaction system
│       ├── GrandExchange     # GE trading automation
│       ├── Minigames         # Minigame-teleport API
│       └── WorldsAPI         # World hopping & selection
│
├── 🎨 Widget APIs
│   ├── BankAPI               # Banking operations
│   ├── DialogueAPI           # Chat & dialogue handling
│   ├── EmoteAPI              # Emote api
│   ├── EquipmentAPI          # Equipment api
│   ├── GrandExchangeAPI      # Grand Exchange automation
│   ├── InventoryAPI          # Inventory api
│   ├── MagicAPI              # Spellcasting & magic
│   ├── PrayerAPI             # Prayer api
│   ├── ShopAPI               # Shop interface handling
│   ├── SlayerRewardsAPI      # Slayer rewards interface api
│   ├── TabsAPI               # Inventory tabs management
│   ├── WidgetAPI             # General widget api
│   ├── MiniMapAPI            # Minimap api
│   └── WorldMapAPI           # World map api
│
├── 🗺️ Advanced Services
│   ├─── 🧭 ipc/
│   │   ├── ChannelBuilder      # Builder class for Channel instance
│   │   └── Channel             # Stateless inter-client comunications service
│   └─── 🧭 pathfinder/
│       ├── Pathfinder          # OSRS Pathfinding
│       └── Walker              # World walker
│
├── 🔍 Query System
│   ├── InventoryQuery           # Inventory filtering & search
│   ├── NpcQuery                 # NPC filtering & selection
│   ├── PlayerQuery              # Player filtering & search
│   ├── TileItemQuery            # Ground item queries
│   ├── TileObjectQuery          # Game object queries
│   ├── WidgetQuery              # UI widget queries
│   └── WorldQuery               # World queries
│
├── 🎨 UI Components
│   ├── VPluginPanel             # Base plugin panel
│   ├── FancyButton              # Styled button component
│   ├── FancyDropdown            # Enhanced dropdown menus
│   ├── ToggleSlider             # Modern toggle switches
│   └── UI utilities             # Layout & styling helpers
│
└── 🔧 Utilities
    ├── ActorPathing             # NPC/player movement utilities
    ├── ClickManagerUtil         # Click interaction helpers
    ├── ClientConfig             # Client configuration management
    ├── Coroutine                # Async task management
    ├── Location                 # Location & coordinate utilities
    ├── MessageUtil              # Game message handling
    ├── Profiler                 # Performance profiling
    ├── ReflectBuilder           # Fluent reflection API
    ├── ReflectUtil              # Reflection helpers
    ├── ResourceUtil             # Resource loading utilities
    ├── RuneliteConfigUtil       # RuneLite config integration
    ├── TextUtil                 # Text processing utilities
    ├── ThreadPool               # Thread management
    └── WorldPointUtil           # World coordinate utilities
```

**Key Features:**
- 🚀 **High-Performance**: Optimized for speed with advanced caching
- 🧠 **Smart Automation**: Built-in pathfinding, dialogue, and interaction systems
- 🔌 **Plugin Ecosystem**: Hot-swappable plugin architecture