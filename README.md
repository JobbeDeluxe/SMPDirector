# SMP Director — v0.1.3-rc1 (Paper 1.21.4, Java 17+)
- Join grace handled internally (no `noteJoin` needed).
- ReliefDrop fills barrel 3 ticks later (stable on 1.21.4) + fallback contents.
- Ambush respects `minDistance` and spawns further away.
- Event pacing via `evaluateEverySeconds`, `minSecondsBetweenEvents` + jitter.

Build:
```bash
mvn -q -DskipTests package
# -> target/smp-director-0.1.3-rc1.jar
```
