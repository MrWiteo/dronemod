# Kamikaze Drones

Plane-type drones that randomly fly in, circle high above a player, pick the biggest nearby build and dive into it, blowing it up and setting everything on fire.

Minecraft **1.21.1**, Java 21. One mod, three loaders:

| Loader | Jar | Requires |
| --- | --- | --- |
| NeoForge | `dronemod-neoforge-1.0.0.jar` | NeoForge 21.1+ |
| Forge | `dronemod-forge-1.0.0.jar` | Forge 52+ (1.21.1) |
| Fabric | `dronemod-fabric-1.0.0.jar` | Fabric Loader 0.16+ and Fabric API |

Download the jar for your loader from the [Releases](../../releases) page and put it into the `mods` folder.

## Gameplay

- A raid starts every 5-20 minutes (on average) for a random player in the Overworld.
- The drone flies to about 45 blocks above the player, orbits and looks for a dense cluster of non-natural blocks (a base).
- When it finds one it dives onto it; the explosion is stronger than TNT and leaves fires around.
- If no build is found, it flies away after 20 minutes.
- Spawn egg is in the creative inventory (Spawn Eggs tab).

## Project layout

```
common/     loader-independent code and assets (entity, model, renderer, sounds)
neoforge/   NeoForge entry point and build
forge/      Forge entry point and build
fabric/     Fabric entry point and build
```

## Building

Needs JDK 21 and Gradle 8.10+. Each loader is a separate Gradle build:

```
cd neoforge && gradle build     # jar in neoforge/build/libs
cd forge    && gradle build
cd fabric   && gradle build
```

GitHub Actions builds all three on every push and publishes the jars as a release.
