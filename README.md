
# SMP Director — v0.1.3-rc1
**Dynamic event director for SMP servers (Paper 1.21.4+, Java 17+).**  
SMP Director continuously measures each player's tension (combat, hunger, darkness, nearby mobs) and occasionally triggers small events to keep survival gameplay fresh and balanced — things like a light ambush, a relief-drop care package, or a short weather cell.

---

## ✨ Features
- **Per‑player Tension System** with live bossbar.
- **Smart Event Director**: weighted by tension *band* (calm/normal/high), per‑event cooldowns, and a global per‑player gap with jitter so events don’t feel periodic.
- **Three built-in events**:
  - **Ambush** — spawns a few hostile mobs at a safe minimum distance.
  - **Relief Drop** — places a barrel with useful loot (never empty; delayed fill for Paper 1.21.4 stability).
  - **Weather Cell** — short, localized storm with occasional lightning effects.
- **Config auto‑merge**: new keys are filled in from defaults on boot.
- **Admin tools**: start/stop/pause, list events, reload config, manual trigger.
- **Lightweight**: pure server‑side, no dependencies (PlaceholderAPI optional as soft‑depend).

---

## 🚀 Quick Start
1. Place the built JAR into `plugins/` and start the server once.
2. A default `config.yml` is generated (missing keys are auto‑filled on upgrades).
3. Use `/director start` to enable the director tick if you paused it, or just let it run (enabled by default on boot).
4. Watch the **Tension** bossbar and try `/director trigger relief_drop` for a quick test.

> **Java**: 17+ required.  
> **Server**: Paper **1.21.4** recommended (works on 1.20.x+).

---

## 🧠 How it Works (Tension & Triggering)
- Every second, SMP Director **decays** tension and **samples** inputs per player:
  - Taking damage, death (events also add tension via listeners).
  - Low hunger, darkness, number of nearby hostile mobs.
- Every `global.evaluateEverySeconds` the director **evaluates** each player:
  1. If the player is within `global.joinGraceSeconds` after login → **skip**.
  2. If the player hasn’t waited at least `global.minSecondsBetweenEvents` (+ random `minSecondsBetweenEventsJitter`) after the last event → **skip**.
  3. Roll a **base chance** based on tension band (`calm/normal/high`). On success → pick a weighted event (per‑band weights) that isn’t on cooldown.

Tension bands default to:
- **calm < 20**, **normal 20–79**, **high ≥ 80** (configurable).

---

## 📦 Built-in Events
### 1) `ambush`
Spawns a small group of mobs around the player, at least `minDistance` blocks away so the player isn’t hit immediately.
- **Key config**: `count`, `radius`, `minDistance`, `mobSet`, `cooldownSeconds`, and `weight` per band.
- **Notes**: Each mob is tagged and targets the player if the type supports it.

### 2) `relief_drop`
Places a **barrel** near the player and fills it with items from `lootPool`.  
Filling happens **a few ticks later** to avoid empty inventories on Paper 1.21.4. If nothing passes the RNG, fallback items are inserted so the barrel is **never empty**.
- **Key config**: `radius`, `removeAfterSeconds`, `minItems`, `maxItems`, `lootPool`, `cooldownSeconds`, and `weight` per band.
- **Loot grammar**: `MATERIAL[:MIN-MAX][@P]`
  - Examples: `BREAD:6-12@85%`, `IRON_INGOT:4-12@0.7`, `DIAMOND:1-3@15%`, `ENCHANTED_GOLDEN_APPLE:1@3%`
  - `@` accepts either `0–1` or `%` form (e.g., `0.25` or `25%`).
- **Removal**: If `removeAfterSeconds > 0`, the barrel gets removed **only if empty** at that time.
- **Troubleshooting**: If you *still* see empty barrels, increase the delay in code (currently 3 ticks) or check region protection plugins that might block container updates.

### 3) `weather_cell`
A brief local storm with a few lightning effects near the player. Clears automatically after `durationSeconds`.

---

## 🔧 Commands
| Command | Description | Permission |
|---|---|---|
| `/director start` | Start the director tick | `director.admin` |
| `/director stop` or `/director pause` | Stop/pause the director tick | `director.admin` |
| `/director status` | Show whether the director is enabled | `director.admin` |
| `/director debug [player]` | Show your (or everyone’s) tension values | `director.debug` |
| `/director list` | List available event IDs | `director.debug` |
| `/director reload` | Reload `config.yml` and timing | `director.admin` |
| `/director trigger <id>` | Manually trigger `ambush`, `relief_drop`, or `weather_cell` for yourself | `director.trigger` |

**Default permissions**: all `op` by default (see `plugin.yml`).

---

## ⚙️ Configuration (highlights)
```yml
tension:
  decayPerSecond: 0.8
  hit: 6
  death: 35
  lowFood: 3
  darkness: 2
  nearMobFactor: 0.5
  nearMobRadius: 12

thresholds:
  calm: 20
  normal: 50
  high: 80

events:
  ambush:
    weight: { calm: 1, normal: 5, high: 12 }
    cooldownSeconds: 240
    count: 4
    radius: 12
    minDistance: 7
    mobSet: ["ZOMBIE", "SKELETON", "SPIDER"]

  relief_drop:
    weight: { calm: 8, normal: 3, high: 1 }
    cooldownSeconds: 600
    radius: 8
    removeAfterSeconds: 180
    minItems: 2
    maxItems: 6
    lootPool:
      - "BREAD:6-12@85%"
      - "TORCH:16-32@80%"
      - "COOKED_BEEF:6-12@65%"
      - "ARROW:16-32@60%"
      - "IRON_INGOT:4-12@70%"
      - "GOLD_INGOT:2-8@50%"
      - "EMERALD:1-3@30%"
      - "ENDER_PEARL:1-3@25%"
      - "LAPIS_LAZULI:8-16@35%"
      - "DIAMOND:1-3@15%"
      - "GOLDEN_CARROT:6-12@20%"
      - "OBSIDIAN:4-8@20%"
      - "ENCHANTED_GOLDEN_APPLE:1@3%"
      - "NETHERITE_INGOT:1@2%"

  weather_cell:
    weight: { calm: 0, normal: 2, high: 6 }
    cooldownSeconds: 360
    durationSeconds: 45

global:
  taggedMobKey: "director"
  evaluateEverySeconds: 7
  minSecondsBetweenEvents: 90
  minSecondsBetweenEventsJitter: 60
  joinGraceSeconds: 20
  baseTriggerChance: { calm: 0.12, normal: 0.28, high: 0.35 }
  bossbar:
    enabled: true
    title: "&bTension: &f%val%"
```

### Tunables that most affect pacing
- `global.evaluateEverySeconds` — how often to consider triggering.
- `global.minSecondsBetweenEvents` — base required gap per player.
- `global.minSecondsBetweenEventsJitter` — adds randomness (0–J seconds) on top of the gap.
- `global.baseTriggerChance.*` — per‑band chance per evaluation.
- Per‑event `weight` and `cooldownSeconds` — selection weight and event cooldown.

---

## 🧪 Testing & Debugging
- `/director debug` to see your current tension.
- `/director list` to list event IDs.
- `/director trigger relief_drop` for an immediate barrel test (should never be empty).
- Watch the console for warnings from other plugins that may block block‑state updates or container edits.

---

## 🛠️ Build
- **Maven**: `mvn -q -DskipTests package` → `target/smp-director-0.1.3-rc1.jar`  
- **Docker sample**:
  ```bash
  sudo docker run --rm -u $(id -u):$(id -g)     -v "$PWD":/src -w /src maven:3.9-eclipse-temurin-21     mvn -U -DskipTests clean package
  ```

---

## 📦 Compatibility & Notes
- Paper **1.21.4** recommended (tested); should work on **1.20.x**+.
- **PlaceholderAPI** is a soft‑depend (not required).
- Relief barrel fill is delayed by a few ticks for stability on recent Paper builds.

---

## 📜 License
MIT

---

## 🗒️ Changelog (excerpt)
- **0.1.3-rc1**: Internal join‑grace; variable gap with jitter; relief barrel stability; safer ambush spawn.
- **0.1.2**: First public drop with ambush/relief/weather and bossbar.
