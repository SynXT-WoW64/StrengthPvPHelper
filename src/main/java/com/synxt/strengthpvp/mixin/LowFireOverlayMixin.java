package com.synxt.strengthpvp.mixin;

import com.synxt.strengthpvp.config.ModConfig;
import com.synxt.strengthpvp.feature.FeatureModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lowers the first-person fire overlay while preserving the actual on-fire status. */
@Mixin(InGameOverlayRenderer.class)
public abstract class LowFireOverlayMixin {
    @Inject(method = "renderFireOverlay(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At("HEAD"))
    private static void strengthpvp$pushLowFire(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        if (!ModConfig.isEnabled(FeatureModule.LOW_FIRE)) return;
        matrices.push();
        matrices.scale(1.0f, 0.62f, 1.0f);
        matrices.translate(0.0f, -0.31f, 0.0f);
    }

    @Inject(method = "renderFireOverlay(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/util/math/MatrixStack;)V", at = @At("RETURN"))
    private static void strengthpvp$popLowFire(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        if (ModConfig.isEnabled(FeatureModule.LOW_FIRE)) matrices.pop();
    }
}
