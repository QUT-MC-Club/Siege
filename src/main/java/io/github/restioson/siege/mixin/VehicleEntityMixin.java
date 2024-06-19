package io.github.restioson.siege.mixin;

import io.github.restioson.siege.duck.SiegeVehicleExt;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VehicleEntity.class)
public class VehicleEntityMixin implements SiegeVehicleExt {
    @Unique
    public boolean siege$inSiegeGame;

    @Inject(method = "killAndDropItem", at = @At("HEAD"), cancellable = true)
    void killAndDropItem(Item selfAsItem, CallbackInfo ci) {
        if (this.siege$inSiegeGame) {
            ci.cancel();
        }
    }

    @Unique
    @Override
    public void siege$setInSiegeGame() {
        this.siege$inSiegeGame = true;
    }
}
