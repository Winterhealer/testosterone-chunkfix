package harness;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Plays the role of Knot's class loader: game/mod classes are defined here after passing through Mixin. */
public class GameLoader extends ClassLoader {
	private final List<Object> roots = new ArrayList<>(); // Path (dir) or ZipFile, searched in order
	private final Set<String> loaded = Collections.synchronizedSet(new HashSet<>());
	final Map<String, byte[]> transformed = new LinkedHashMap<>();
	UnaryOperator<byte[]> preTransform = UnaryOperator.identity(); // lets a test tamper with raw target bytes

	public GameLoader(ClassLoader parent, List<Path> paths) throws IOException {
		super("game", parent);
		for (Path p : paths) roots.add(Files.isDirectory(p) ? p : new ZipFile(p.toFile()));
	}

	static boolean isGameClass(String name) {
		return name.startsWith("net.minecraft.") || name.startsWith("net.mifort.") || name.startsWith("com.tterrag.")
				|| name.startsWith("dev.winterhealer.") || name.startsWith("testdriver.");
	}

	byte[] resourceBytes(String path) {
		try {
			for (Object r : roots) {
				if (r instanceof Path dir) {
					Path f = dir.resolve(path);
					if (Files.isRegularFile(f)) return Files.readAllBytes(f);
				} else {
					ZipFile z = (ZipFile) r;
					ZipEntry e = z.getEntry(path);
					if (e != null) try (InputStream in = z.getInputStream(e)) { return in.readAllBytes(); }
				}
			}
		} catch (IOException e) { throw new RuntimeException(e); }
		return null;
	}

	byte[] rawBytes(String className) {
		className = className.replace('/', '.'); // Mixin asks for internal names, Knot accepts both forms
		if (!isGameClass(className)) return null;
		byte[] b = resourceBytes(className.replace('.', '/') + ".class");
		return b == null ? null : preTransform.apply(b);
	}

	boolean isLoaded(String name) { return loaded.contains(name); }

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (!isGameClass(name)) return super.loadClass(name, resolve);
		synchronized (getClassLoadingLock(name)) {
			Class<?> c = findLoadedClass(name);
			if (c == null) {
				if (name.startsWith("dev.winterhealer.testofix.mixin.")) throw new IllegalStateException("Mixin class must never be loaded directly: " + name);
				byte[] b = rawBytes(name);
				if (b == null) throw new ClassNotFoundException(name);
				byte[] t = TestService.transformer.transformClassBytes(name, name, b);
				if (t != b && !Arrays.equals(t, b)) transformed.put(name, t);
				c = defineClass(name, t, 0, t.length);
				loaded.add(name);
			}
			if (resolve) resolveClass(c);
			return c;
		}
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		byte[] b = resourceBytes(name);
		return b != null ? new ByteArrayInputStream(b) : super.getResourceAsStream(name);
	}
}
