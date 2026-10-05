package harness;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;

/**
 * Usage: Main <nofix|fix|broken> <compat> <gameClassesDir> <testosteroneJar> <fixJar> <outDir>
 *  nofix  - Testosterone exactly as shipped (reproduces the bug)
 *  fix    - with the fix jar's mixin config, decorated the way Fabric Loader decorates it
 *  broken - fix applied to a Testosterone build whose applyPotionEffect was renamed (must fail loudly)
 */
public class Main {
	public static void main(String[] args) throws Throwable {
		String mode = args[0];
		int compat = Integer.parseInt(args[1]);
		Path game = Path.of(args[2]), testoJar = Path.of(args[3]), fixJar = Path.of(args[4]), out = Path.of(args[5]);

		List<Path> cp = new ArrayList<>(List.of(game));
		if (!mode.equals("nofix")) cp.add(fixJar);
		cp.add(testoJar);
		GameLoader loader = new GameLoader(Main.class.getClassLoader(), cp);
		TestService.loader = loader;
		if (mode.equals("broken")) loader.preTransform = Main::renameApplyPotionEffect;

		// Same sequence as FabricMixinBootstrap.init + FabricLauncherBase.finishMixinBootstrapping
		System.setProperty("mixin.bootstrapService", TestBootstrap.class.getName());
		System.setProperty("mixin.service", TestService.class.getName());
		MixinBootstrap.init();
		if (!mode.equals("nofix")) {
			Mixins.addConfiguration("testofix.mixins.json");
			for (Config c : Mixins.getConfigs()) {
				c.getConfig().decorate(FabricUtil.KEY_MOD_ID, "testosterone_chunkfix");
				c.getConfig().decorate(FabricUtil.KEY_COMPATIBILITY, compat);
				System.out.println("[harness] mixin config " + c.getName() + " registered, compat=" + compat);
			}
		}
		Method gotoPhase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
		gotoPhase.setAccessible(true);
		gotoPhase.invoke(null, MixinEnvironment.Phase.INIT);
		gotoPhase.invoke(null, MixinEnvironment.Phase.DEFAULT);

		String target = "net.mifort.testosterone.events.fluidEffectHandler";
		try {
			loader.loadClass(target); // like Testosterone's main entrypoint calling fluidEffectHandler.register()
		} catch (Throwable t) {
			System.out.println("[harness] LOADING " + target + " FAILED:");
			for (Throwable c = t; c != null; c = c.getCause()) System.out.println("    " + c.getClass().getName() + ": " + c.getMessage());
			return;
		}
		byte[] t = loader.transformed.get(target);
		System.out.println("[harness] " + target + " transformed by mixin: " + (t != null));
		Files.createDirectories(out);
		if (t != null) Files.write(out.resolve("fluidEffectHandler.class"), t);

		Class<?> scenario = loader.loadClass("testdriver.Scenario");
		System.out.print(scenario.getMethod("run").invoke(null));
	}

	/** Simulates a future Testosterone version where the injection target no longer exists. */
	static byte[] renameApplyPotionEffect(byte[] bytes) {
		ClassNode cn = new ClassNode();
		new ClassReader(bytes).accept(cn, 0);
		if (!cn.name.equals("net/mifort/testosterone/events/fluidEffectHandler")) return bytes;
		for (MethodNode m : cn.methods) {
			if (m.name.equals("applyPotionEffect")) m.name = "applyFluidEffects";
			for (AbstractInsnNode insn : m.instructions) {
				if (insn instanceof MethodInsnNode mi && mi.owner.equals(cn.name) && mi.name.equals("applyPotionEffect")) mi.name = "applyFluidEffects";
			}
		}
		ClassWriter cw = new ClassWriter(0);
		cn.accept(cw);
		return cw.toByteArray();
	}
}
