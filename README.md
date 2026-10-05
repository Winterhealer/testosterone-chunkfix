# Testosterone Chunk Leak Fix

A small Fabric mod for Minecraft 1.20.1 that fixes a chunk leak in **Create: Testosterone 2.0.3**
(by mifort and JonMyDude).

> **Unofficial, temporary fix.** Remove it once Testosterone fixes this upstream. It only targets
> Testosterone 2.0.3. Related report: [ProminenceII-HasturianEra#610](https://github.com/nvb-uy/ProminenceII-HasturianEra/issues/610)
> (hostile mobs gradually stop spawning: frozen mobs in leaked chunks fill the mob cap).

## The bug
`net.mifort.testosterone.events.fluidEffectHandler` runs at the end of every world tick and does:

```java
for (Entity e : world.iterateEntities())            // every loaded entity, ticking or not
    if (e instanceof LivingEntity l)
        l.getWorld().getFluidState(l.getBlockPos()); // loads the chunk => adds a 1-tick chunk ticket
```

For entities sitting in chunks that are loaded but no longer ticking (players have left),
the `getFluidState` call re-adds a chunk ticket every tick. That keeps the chunk loaded, which keeps
the entity loaded, which makes the handler touch the chunk again next tick. Every chunk a player
passes that has a mob in it stays loaded until restart (seen as 100k+ loaded chunks, heap exhaustion
and 1.5–2 s full-GC freezes).

## The fix
A mixin injects at the head of `fluidEffectHandler.applyPotionEffect` and skips the entity unless
`ServerWorld.shouldTickEntity(pos)` is true. Entities in non-ticking chunks are frozen anyway, so
gameplay is unchanged: the effect still applies to anything actually standing in the fluid.

## Install
- Build it (see below), then drop `dist/testosterone-chunkfix-1.0.0.jar` into the **server's** `mods/` folder. Clients do not need it
  (it is harmless if present; it also fixes singleplayer worlds).
- It requires exactly Testosterone `2.0.3`. If Testosterone is updated, Fabric will refuse to start
  with a dependency message, so check whether the new version still has the bug before bumping
  the version in `res/fabric.mod.json`.

## Build
`./build.sh` (needs JDK 17+ only).

## Test
`test/run.sh /path/to/testosterone-2.0.3.jar` applies the fix with Fabric's real Mixin library
(`net.fabricmc:sponge-mixin`, downloaded from Maven Central) to the real `fluidEffectHandler` class,
the same way Fabric Loader's Knot does, then runs Testosterone's tick handler for 200 ticks against a
small model of the server's chunk tickets. It runs three cases:

- **without the fix:** chunks with a mob in them never unload after the player leaves (the bug).
- **with the fix:** those chunks unload one tick later, and mobs standing in the fluid inside running
  chunks still get the effect.
- **target method renamed** (like a future Testosterone version): the mixin fails loudly at class load
  instead of silently not applying.

Run it against a new Testosterone jar before raising the version pinned in `res/fabric.mod.json`.
