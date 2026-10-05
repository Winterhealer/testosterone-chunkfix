package harness;
import java.util.HashMap;
import java.util.Map;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;
public class TestPropertyService implements IGlobalPropertyService {
	private static final class Key implements IPropertyKey {
		final String name; Key(String name) { this.name = name; }
		@Override public String toString() { return name; }
	}
	private final Map<String, Key> keys = new HashMap<>();
	private final Map<IPropertyKey, Object> props = new HashMap<>();
	@Override public IPropertyKey resolveKey(String name) { return keys.computeIfAbsent(name, Key::new); }
	@SuppressWarnings("unchecked") @Override public <T> T getProperty(IPropertyKey key) { return (T) props.get(key); }
	@Override public void setProperty(IPropertyKey key, Object value) { props.put(key, value); }
	@SuppressWarnings("unchecked") @Override public <T> T getProperty(IPropertyKey key, T def) { return props.containsKey(key) ? (T) props.get(key) : def; }
	@Override public String getPropertyString(IPropertyKey key, String def) { Object v = props.get(key); return v != null ? v.toString() : def; }
}
