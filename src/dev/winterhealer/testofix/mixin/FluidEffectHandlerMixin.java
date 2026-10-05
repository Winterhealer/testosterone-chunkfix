package dev.winterhealer.testofix.mixin;

import net.minecraft.class_1309;
import net.minecraft.class_1937;
import net.minecraft.class_3218;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Testosterone 2.0.3's fluidEffectHandler loops over every loaded entity each tick and calls
 * World.getFluidState() on its position. For entities in chunks that are loaded but not ticking,
 * that lookup adds a fresh chunk ticket every tick, so those chunks (and their entities) never
 * unload. Skip entities whose chunk isn't entity-ticking; they can't be affected by fluids anyway.
 *
 * Names are 1.20.1 intermediary: class_1309 = LivingEntity, class_3218 = ServerWorld,
 * method_37908 = getWorld, method_24515 = getBlockPos, method_37118 = shouldTickEntity(BlockPos).
 */
@Mixin(targets = "net.mifort.testosterone.events.fluidEffectHandler", remap = false)
public abstract class FluidEffectHandlerMixin {
    @Inject(method = "applyPotionEffect", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private static void testofix$skipNonTickingEntities(class_1309 entity, CallbackInfo ci) {
        class_1937 world = entity.method_37908();
        if (world instanceof class_3218 serverWorld && !serverWorld.method_37118(entity.method_24515())) {
            ci.cancel();
        }
    }
}
