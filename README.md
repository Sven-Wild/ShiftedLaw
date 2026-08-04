# RollLaw

A Fabric mod for Minecraft 1.21.1 (Java 21). Run `/rolllaw` to be randomly
assigned one of 5 unique Laws. Once every online player has a Law, a 10-second
countdown starts for everyone. Dying returns your Law to the pool and resets
the countdown.

## Features

- `/rolllaw` — rolls a Law for the player who ran it. Broadcasts a short
  "❓ ❓ ❓" suspense animation to every online player, then reveals the result
  with green **PROS** and red **CONS** in chat.
- 5 unique Laws, no duplicates — once claimed, a Law leaves the pool until its
  holder dies or disconnects.
- Once every online player holds a Law, a shared 10-second countdown starts:
  `5, 4, 3, 2, 1`, then `⚡ GO! ⚡`.
- Dying returns your Law to the pool, cancels any running countdown, and
  automatically re-rolls you a new Law on respawn.

## The 5 Laws

| Law | Pros | Cons |
|---|---|---|
| The Gravity Anchor | Fall damage converts into healing | Jumping costs hunger |
| The Inferno | Half damage from fire and lava | Fire spreads twice as fast around you |
| The Cultivator | Nearby crops grow 50% faster | Can't sprint |
| The Phantom | 20% faster sneaking, 20% slower falling | — |
| The Beacon | Allies gain +20% resistance | You take +10% more damage |

## Project layout

```
build.gradle                 Gradle/Loom build script
gradle.properties            Minecraft/Yarn/Loader/Fabric API versions
settings.gradle               Gradle plugin repositories
src/main/java/com/shiftedlaw/rolllaw/
  RollLawMod.java             Mod entrypoint — registers the command and events
  LawManager.java             Law pool, roll animation, and countdown state machine
  LawEffects.java             Per-tick passive effects and damage handling
  Law.java                    The 5 Laws, their display names, pros and cons
src/main/resources/fabric.mod.json
```

## Building

Requires a JDK 21 and internet access (Gradle needs to download Minecraft,
Yarn mappings, Fabric Loader and Fabric API on first run).

```bash
./gradlew build
```

The finished mod jar will be at `build/libs/rolllaw-1.0.0.jar` — drop that
single file into your Fabric server or client's `mods` folder (alongside the
matching [Fabric API](https://modrinth.com/mod/fabric-api) jar for 1.21.1,
which is a required dependency and is not bundled into this jar).

If dependency resolution fails, the pinned versions in `gradle.properties`
may have been superseded — check https://fabricmc.net/develop for the
current Loader/Yarn/Fabric API versions for Minecraft 1.21.1 and update
`gradle.properties` accordingly (the `fabric-loom` plugin version at the top
of `build.gradle` may also need bumping to match).

## Notes on the approximated mechanics

A couple of effects are implemented as close, compile-safe approximations of
vanilla behavior rather than deep hooks into vanilla's internal randomness,
since precisely reproducing vanilla's exact spread/growth-tick probabilities
would require mixins into internal game classes:

- **The Cultivator**'s crop growth boost periodically nudges nearby immature
  crops forward using the same `Fertilizable` growth hook bone meal uses.
- **The Inferno**'s faster fire spread periodically ignites a nearby
  flammable-adjacent air block while its holder is on fire.
- **The Gravity Anchor**'s hunger-on-jump is detected by watching for an
  upward liftoff from the ground each tick.
