package harness;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterConsole;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.util.ReEntranceLock;

/** Mirrors Fabric Loader's MixinServiceKnot, with Knot's class loader swapped for GameLoader. */
public class TestService implements IMixinService, IClassProvider, IClassBytecodeProvider, ITransformerProvider, IClassTracker {
	static IMixinTransformer transformer;
	static GameLoader loader;
	private final ReEntranceLock lock = new ReEntranceLock(1);

	byte[] getClassBytes(String name) throws ClassNotFoundException, IOException {
		byte[] b = loader.rawBytes(name);
		if (b == null) {
			try (InputStream in = ClassLoader.getSystemResourceAsStream(name.replace('.', '/') + ".class")) {
				if (in != null) b = in.readAllBytes();
			}
		}
		if (b == null) throw new ClassNotFoundException(name);
		return b;
	}

	@Override public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException { return getClassNode(name, true); }
	@Override public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException { return getClassNode(name, runTransformers, 0); }
	@Override public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags) throws ClassNotFoundException, IOException {
		ClassReader reader = new ClassReader(getClassBytes(name));
		ClassNode node = new ClassNode();
		reader.accept(node, readerFlags);
		return node;
	}
	@Override public URL[] getClassPath() { return new URL[0]; }
	@Override public Class<?> findClass(String name) throws ClassNotFoundException { return loader.loadClass(name); }
	@Override public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException { return Class.forName(name, initialize, loader); }
	@Override public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException { return Class.forName(name, initialize, TestService.class.getClassLoader()); }
	@Override public String getName() { return "Test/Knot-like"; }
	@Override public boolean isValid() { return true; }
	@Override public void prepare() { }
	@Override public MixinEnvironment.Phase getInitialPhase() { return MixinEnvironment.Phase.PREINIT; }
	@Override public void offer(IMixinInternal internal) {
		if (internal instanceof IMixinTransformerFactory) transformer = ((IMixinTransformerFactory) internal).createTransformer();
	}
	@Override public void init() { }
	@Override public void beginPhase() { }
	@Override public void checkEnv(Object bootSource) { }
	@Override public ReEntranceLock getReEntranceLock() { return lock; }
	@Override public IClassProvider getClassProvider() { return this; }
	@Override public IClassBytecodeProvider getBytecodeProvider() { return this; }
	@Override public ITransformerProvider getTransformerProvider() { return this; }
	@Override public IClassTracker getClassTracker() { return this; }
	@Override public IMixinAuditTrail getAuditTrail() { return null; }
	@Override public IFeatureValidator getFeatureValidator() { return IFeatureValidator.ALLOW_ALL; }
	@Override public IAdviceProvider getAdviceProvider() { return IAdviceProvider.GENERIC; }
	@Override public Collection<String> getPlatformAgents() { return Collections.singletonList("org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault"); }
	@Override public IContainerHandle getPrimaryContainer() { return new ContainerHandleVirtual("test"); }
	@Override public Collection<IContainerHandle> getMixinContainers() { return Collections.emptyList(); }
	@Override public InputStream getResourceAsStream(String name) { return loader.getResourceAsStream(name); }
	@Override public void registerInvalidClass(String className) { }
	@Override public boolean isClassLoaded(String className) { return loader.isLoaded(className); }
	@Override public String getClassRestrictions(String className) { return ""; }
	@Override public Collection<ITransformer> getTransformers() { return Collections.emptyList(); }
	@Override public Collection<ITransformer> getDelegatedTransformers() { return Collections.emptyList(); }
	@Override public void addTransformerExclusion(String name) { }
	@Override public String getSideName() { return "SERVER"; }
	@Override public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() { return MixinEnvironment.CompatibilityLevel.JAVA_8; }
	@Override public MixinEnvironment.CompatibilityLevel getMaxCompatibilityLevel() { return MixinEnvironment.CompatibilityLevel.JAVA_25; }
	@Override public ILogger getLogger(String name) { return new LoggerAdapterConsole(name); }
}
