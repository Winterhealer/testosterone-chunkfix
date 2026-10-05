package net.minecraft;
import java.util.*;
/** LivingEntity */
public class class_1309 extends class_1297 {
	public final Map<String, Integer> effectsApplied = new TreeMap<>();
	public String lastEffectParams = "-";
	public class_1309(String name, class_1937 world, class_2338 pos) { super(name, world, pos); }
	/** addStatusEffect */
	public boolean method_6092(class_1293 e) {
		effectsApplied.merge(e.effect.id, 1, Integer::sum);
		lastEffectParams = e.duration + " ticks, amplifier " + e.amplifier;
		return true;
	}
}
