package harness;
import org.spongepowered.asm.service.IMixinServiceBootstrap;
public class TestBootstrap implements IMixinServiceBootstrap {
	@Override public String getName() { return "Test"; }
	@Override public String getServiceClassName() { return "harness.TestService"; }
	@Override public void bootstrap() { }
}
