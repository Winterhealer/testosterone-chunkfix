package testdriver;

import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.*;
import net.mifort.testosterone.fluids.testosteroneFluids;

public class Scenario {
	static final int TICKS = 200, LEAVE = 4, RETURN_B = 150;

	public static String run() throws Exception {
		Class<?> h = Class.forName("net.mifort.testosterone.events.fluidEffectHandler", true, Scenario.class.getClassLoader());
		Method onWorldTick = h.getDeclaredMethod("onWorldTick", class_3218.class); // Testosterone's END_WORLD_TICK listener
		onWorldTick.setAccessible(true);

		class_3218 w = new class_3218();
		class_2338 pA = new class_2338(5, 64, 5), pB1 = new class_2338(1605, 64, 5), pB2 = new class_2338(1610, 64, 9),
				pC = new class_2338(3205, 64, 5), pD = new class_2338(4805, 64, 5);
		w.setFluid(pA, testosteroneFluids.TESTOSTERONE_FLUID.getSource());
		w.setFluid(pB1, (class_3611) testosteroneFluids.TRENBOLONE_FLUID.get());
		w.setFluid(pD, (class_3611) testosteroneFluids.TESTOSTERONE_FLUID.get());
		class_1309 zombie = new class_1309("zombie (A, in testosterone source)", w, pA);
		class_1309 cow = new class_1309("cow (B, in flowing trenbolone)", w, pB1);
		class_1309 sheep = new class_1309("sheep (B, dry land)", w, pB2);
		class_1297 item = new class_1297("item (C)", w, pC);
		class_1309 villager = new class_1309("villager (D, in flowing testosterone)", w, pD);
		w.entities.addAll(List.of(zombie, cow, sheep, item, villager));

		Map<String, Long> chunks = new LinkedHashMap<>();
		chunks.put("A home (player stays)", pA.chunkKey());
		chunks.put("B cow+sheep (player leaves t" + LEAVE + ", returns t" + RETURN_B + ")", pB1.chunkKey());
		chunks.put("C item only (player leaves t" + LEAVE + ")", pC.chunkKey());
		chunks.put("D villager (player leaves t" + LEAVE + ")", pD.chunkKey());
		w.playerTickets.addAll(chunks.values());

		Map<String, Integer> loadedWithoutPlayer = new LinkedHashMap<>(), firstUnload = new LinkedHashMap<>();
		int lookupsWithoutPlayer = 0;
		for (int t = 1; t <= TICKS; t++) {
			w.beginTick();
			if (t == LEAVE) { w.playerTickets.remove(pB1.chunkKey()); w.playerTickets.remove(pC.chunkKey()); w.playerTickets.remove(pD.chunkKey()); }
			if (t == RETURN_B) w.playerTickets.add(pB1.chunkKey());
			for (var e : chunks.entrySet()) {
				long k = e.getValue();
				if (!w.playerTickets.contains(k) && w.isLoaded(k)) loadedWithoutPlayer.merge(e.getKey(), 1, Integer::sum);
				if (!w.isLoaded(k)) firstUnload.putIfAbsent(e.getKey(), t);
			}
			int before = w.fluidLookups;
			Set<Long> unheld = new HashSet<>(); for (long k : chunks.values()) if (!w.playerTickets.contains(k)) unheld.add(k);
			Map<Long, Integer> snap = new HashMap<>(w.fluidLookupsPerChunk);
			onWorldTick.invoke(null, w);
			for (long k : unheld) lookupsWithoutPlayer += w.fluidLookupsPerChunk.getOrDefault(k, 0) - snap.getOrDefault(k, 0);
		}

		StringBuilder sb = new StringBuilder();
		sb.append("  chunks after the player leaves (").append(TICKS).append(" ticks simulated):\n");
		for (String c : chunks.keySet()) {
			sb.append(String.format("    %-48s loaded-with-no-player: %3d ticks, first unloaded at tick: %s%n",
					c, loadedWithoutPlayer.getOrDefault(c, 0), firstUnload.containsKey(c) ? firstUnload.get(c) : "never"));
		}
		sb.append("  status effects applied (count over ").append(TICKS).append(" ticks):\n");
		for (class_1297 e : w.entities) {
			if (e instanceof class_1309 l) sb.append(String.format("    %-40s %s  [%s]%n", l.name, l.effectsApplied.isEmpty() ? "{}" : l.effectsApplied, l.lastEffectParams));
		}
		sb.append(String.format("  fluid lookups: %d total, %d of them in chunks no player was holding (each one re-tickets that chunk)%n", w.fluidLookups, lookupsWithoutPlayer));
		return sb.toString();
	}
}
