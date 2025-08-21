# SMP Director (PaperMC 1.20–1.21, Java 17+)

Ein dynamischer Ereignis-Regisseur für Survival-Server. Misst live den *Spannungslevel* (Tension) jedes Spielers und triggert passende Mini-Events: **Ambush**, **Relief Drop**, **Weather Cell** – alle vollständig konfigurierbar.

> **Status:** Frühes MVP. Läuft stabil und genügsam, API-first entworfen – ready für Erweiterungen.

## Features
- Live-Tension (0–100) aus Kampf-, Hunger-, Dunkelheits- und Mobnähe-Daten
- Bossbar-Feedback mit Farbe je nach Tension-Band (calm/normal/high)
- Gewichtete, cooldown-gesteuerte Events (per Spieler)
- Papierfreundlich: PDC-Tagging, async Checks wo sinnvoll, keine globalen Weltwechsel
- Saubere Config inkl. Schwellen, Gewichten, Pools
- Admin-Befehle: `start`, `stop`, `pause`, `status`, `debug`, `trigger`

## Schnellstart

### Voraussetzungen
- **Java 17+**
- **Paper 1.20.6** oder **1.21.x**
- Build mit **Maven** (empfohlen): `mvn -version`

### Build
```bash
mvn -q -e -DskipTests package
```
Das JAR liegt danach in `target/smp-director-0.1.1.jar`.

### Installation
- JAR nach `plugins/` kopieren
- Server starten → `plugins/SMPDirector/config.yml` anpassen (optional)
- `/director status` prüfen, dann spielen 😄

## Commands & Permissions
- `/director start|stop|pause|status` – `director.admin`
- `/director debug [player]` – `director.debug`
- `/director trigger <event>` – `director.trigger` (Events: `ambush`, `relief_drop`, `weather_cell`)

## Konfig (Auszug)
Siehe vollständige `config.yml` im Projekt. Standardwerte:
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
    weight: {"calm":1, "normal":5, "high":12}
    cooldownSeconds: 240
    count: 4
    radius: 8
    mobSet: ["ZOMBIE","SKELETON"]
  relief_drop:
    weight: {"calm":8, "normal":3, "high":1}
    cooldownSeconds: 600
    radius: 6
    removeAfterSeconds: 180
    lootPool:
      - "BREAD:6-12"
      - "IRON_INGOT:3-6"
      - "TORCH:16-32"
  weather_cell:
    weight: {"calm":0, "normal":2, "high":6}
    cooldownSeconds: 360
    durationSeconds: 45
global:
  taggedMobKey: "director"
  bossbar:
    enabled: true
    title: "&bTension: &f%val%"
```

## Event-Idee kurz erklärt
- **Ambush**: Spawnt 3–6 Mobs aus `mobSet` im Radius um den Spieler, markiert per PDC
- **Relief Drop**: Platziert ein Fass („Lieferkiste“) nahe dem Spieler und füllt es aus `lootPool`
- **Weather Cell**: Nur **lokales** Wetter per `setPlayerWeather` (keine ganze Welt), endet automatisch

## Entwicklung
- Projektstruktur: Maven, Java 17, Paper API
- Paket: `dev.yourserver.smpdirector`

```text
src/
 └─ main/
    ├─ java/dev/yourserver/smpdirector/...
    └─ resources/
       ├─ plugin.yml
       └─ config.yml
```

### Bauen & Testen mit Paper
1. Paper-Server bereitstellen (1.20.6/1.21.x)
2. JAR unter `plugins/` ablegen
3. Starten, Logs beobachten (`[SMPDirector] ...`)
4. Ingame `/director debug` und Bossbar checken

## Roadmap
- Mehr Event-Typen (Händler, Treasure Ping, Calm Period)
- PlaceholderAPI Expansion (`%director_tension%`)
- Per-Welt-Configs, Datenspeicherung

---

**Lizenz:** MIT – mach damit, was du willst. Feedback & PRs willkommen!

---

## Changelog
### 0.1.1
- **Relief Drop:** Loot-Pool unterstützt jetzt optionale Drop-Chancen via `@` (z. B. `DIAMOND:1-3@0.15` oder `@15%`).
- Standard-Config mit häufigen Basics, seltenen High-Tier-Drops (Diamant, Netherite, Verzauberter Apfel).
