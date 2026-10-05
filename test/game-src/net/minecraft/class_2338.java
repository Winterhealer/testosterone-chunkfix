package net.minecraft;
/** BlockPos */
public class class_2338 {
	public final int x, y, z;
	public class_2338(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
	public long chunkKey() { return ((long) (x >> 4) & 0xFFFFFFFFL) | (((long) (z >> 4) & 0xFFFFFFFFL) << 32); }
	public long key() { return ((long) x << 38) ^ ((long) y << 26) ^ z; }
	@Override public String toString() { return "chunk(" + (x >> 4) + "," + (z >> 4) + ")"; }
}
