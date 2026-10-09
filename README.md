# Kamikaze Drones

Plane-type drones that randomly fly in, circle high above a player, pick the biggest nearby build and dive into it, blowing it up and setting everything on fire.

Minecraft **1.21.1**, Java 21. One mod, three loaders:

| Loader | Jar | Requires |
| --- | --- | --- |
| NeoForge | `dronemod-neoforge-1.1.0.jar` | NeoForge 21.1+ |
| Forge | `dronemod-forge-1.1.0.jar` | Forge 52+ (1.21.1) |
| Fabric | `dronemod-fabric-1.1.0.jar` | Fabric Loader 0.16+ and Fabric API |

Download the jar for your loader from the [Releases](../../releases) page and put it into the `mods` folder.

## Gameplay

- Every 5 minutes a drone appears west of each player in the Overworld.
- 90%: it just flies past towards the nearest village and leaves.
- 10%: it circles above the player for 30-90 seconds, then dives into the player's nearest build (even a far one, within loaded chunks) and explodes. The blast is stronger than TNT and leaves fires around.
- If the player is inside a village, the drone dives into a random house of that village instead.
- Builds are detected by counting man-made blocks (planks, bricks, furnaces, chests, ...) around the surface; logs count for less because of trees.
- If no build is found, the drone flies away after 3 minutes.
- The spawn egg (Spawn Eggs tab) always creates an attacking drone, handy for testing.

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
