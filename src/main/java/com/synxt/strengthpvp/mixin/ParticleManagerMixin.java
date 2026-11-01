package com.synxt.strengthpvp.mixin;

import com.synxt.strengthpvp.config.ModConfig;
import com.synxt.strengthpvp.feature.FeatureModule;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Inject(method = "addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;)V",
            at = @At("HEAD"), cancellable = true)
    private void strengthpvp$hideCrystalExplosionEmitter(Entity entity, ParticleEffect effect, CallbackInfo ci) {
        if (ModConfig.isEnabled(FeatureModule.VIEW_CLEANER) && entity instanceof EndCrystalEntity) ci.cancel();
    }
}
