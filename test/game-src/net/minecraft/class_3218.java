package net.minecraft;
import java.util.*;
/**
 * ServerWorld with a model of chunk tickets as they behave on the profiled server (vanilla + Lithium):
 *  - players hold their chunks at level 31 (entity-ticking)
 *  - World.getFluidState() on the main thread adds/refreshes a ChunkTicketType.UNKNOWN ticket at level 33
 *    (FULL = loaded but not ticking) with a 1-tick timeout; purgeStaleTickets drops it once it is >1 tick old
 *  - a chunk with no ticket unloads, taking its entities with it
 */
public class class_3218 extends class_1937 {
	public int tick;
	public final Set<Long> playerTickets = new HashSet<>();
	public final Map<Long, Integer> unknownTickets = new HashMap<>();
	public final List<class_1297> entities = new ArrayList<>();
	private final Map<Long, class_3611> fluids = new HashMap<>();
	public final Map<Long, Integer> fluidLookupsPerChunk = new HashMap<>();
	public int fluidLookups;

	public void setFluid(class_2338 pos, class_3611 f) { fluids.put(pos.key(), f); }
	public boolean isLoaded(long chunk) { return playerTickets.contains(chunk) || unknownTickets.containsKey(chunk); }
	public boolean isEntityTicking(long chunk) { return playerTickets.contains(chunk); }

	/** start of a server tick: DistanceManager.purgeStaleTickets() */
	public void beginTick() {
		tick++;
		unknownTickets.values().removeIf(created -> tick - created > 1);
	}

	/** iterateEntities(): every entity in a loaded chunk, ticking or not */
	public Iterable<class_1297> method_27909() {
		List<class_1297> out = new ArrayList<>();
		for (class_1297 e : entities) if (isLoaded(e.method_24515().chunkKey())) out.add(e);
		return out;
	}

	/** shouldTickEntity(BlockPos) */
	public boolean method_37118(class_2338 pos) { return isEntityTicking(pos.chunkKey()); }

	/** getFluidState(BlockPos) -> getChunk(x, z, FULL, true) -> adds/refreshes the level-33 UNKNOWN ticket */
	@Override public class_3610 method_8316(class_2338 pos) {
		fluidLookups++;
		fluidLookupsPerChunk.merge(pos.chunkKey(), 1, Integer::sum);
		unknownTickets.put(pos.chunkKey(), tick);
		return new class_3610(fluids.getOrDefault(pos.key(), class_3611.EMPTY));
	}
}
