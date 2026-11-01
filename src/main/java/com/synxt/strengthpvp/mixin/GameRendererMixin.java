package com.synxt.strengthpvp.mixin;

import com.synxt.strengthpvp.config.ModConfig;
import com.synxt.strengthpvp.feature.FeatureModule;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "bobViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void strengthpvp$disableHurtCamera(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (ModConfig.isEnabled(FeatureModule.VIEW_CLEANER)) ci.cancel();
    }

    @Inject(method = "getBasicProjectionMatrix(D)Lnet/minecraft/util/math/Matrix4f;", at = @At("RETURN"), cancellable = true)
    private void strengthpvp$applyAspectRatio(double fov, CallbackInfoReturnable<Matrix4f> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ModConfig.isEnabled(FeatureModule.ASPECT_RATIO) || client.player == null || client.getWindow() == null) return;
        float current = (float) client.getWindow().getFramebufferWidth() / (float) Math.max(1, client.getWindow().getFramebufferHeight());
        int preset = ModConfig.getOption(FeatureModule.ASPECT_RATIO);
        float target = preset == 1 ? (4.0f / 3.0f) : preset == 2 ? (16.0f / 10.0f) : (16.0f / 9.0f);
        if (current <= 0.0f || target <= 0.0f) return;
        Matrix4f matrix = cir.getReturnValue();
        Matrix4fAccessor accessor = (Matrix4fAccessor) (Object) matrix;
        accessor.strengthpvp$setScaleX(accessor.strengthpvp$getScaleX() * (current / target));
        cir.setReturnValue(matrix);
    }
}
