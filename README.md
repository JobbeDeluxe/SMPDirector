# SMP Director (PaperMC 1.20–1.21, Java 17+) — v0.1.2

Dynamic event “director” for SMP servers. Each player has a live **tension** score (combat, hunger, darkness, nearby mobs). The plugin periodically evaluates and triggers balanced micro-events: **Ambush**, **Relief Drop**, **Weather Cell**.

## What's new in 0.1.2
- **Safer Ambush spawns**: `minDistance` (default 7) so mobs don’t spawn in melee range.
- **Relief Drop quality**: non-empty guarantee, coords chat message, min/max items knobs.
- **Event cadence control**: `evaluateEverySeconds`, `minSecondsBetweenEvents`, and band-based `baseTriggerChance`.
- **Commands**: `/director list` to see event IDs, `/director reload` to re-read config.
- **Config auto-fill**: missing keys are merged from defaults at startup.

## Build
```bash
mvn -q -DskipTests package
# => target/smp-director-0.1.2.jar
```

## Commands & Permissions
- `/director start|stop|pause|status` — `director.admin`
- `/director debug [player]` — `director.debug`
- `/director list` — `director.debug`
- `/director reload` — `director.admin`
- `/director trigger <ambush|relief_drop|weather_cell>` — `director.trigger`

## Config (highlights)
- `events.ambush.minDistance` — minimum spawn distance around player (blocks).
- `events.relief_drop.minItems / maxItems` — soft bounds; fallback fills if empty.
- `global.evaluateEverySeconds` — how often to consider triggering (default 5s).
- `global.minSecondsBetweenEvents` — per-player cooldown between any events.
- `global.baseTriggerChance` — chance per evaluation by tension band.

## How triggering works
Every server second, the director ticks. It **only evaluates** a player every `evaluateEverySeconds`. If the player’s `minSecondsBetweenEvents` gap has passed, it rolls a chance based on their tension band. On success, an event is **weighted** and checked against its own cooldown. If none fits, nothing happens and the next evaluation is in a few seconds.

---

MIT License

### Anti-spawn-camp
- `global.joinGraceSeconds`: suppresses events right after a player joins.
- `global.minSecondsBetweenEventsJitter`: adds randomness to the global gap so events don't feel periodic.
