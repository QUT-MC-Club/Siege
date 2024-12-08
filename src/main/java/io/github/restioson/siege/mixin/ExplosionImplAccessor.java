package io.github.restioson.siege.mixin;

import net.minecraft.world.explosion.ExplosionBehavior;
import net.minecraft.world.explosion.ExplosionImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ExplosionImpl.class)
public interface ExplosionImplAccessor {
    @Accessor
    ExplosionBehavior getBehavior();
}
