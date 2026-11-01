package com.synxt.strengthpvp.mixin;

import com.synxt.strengthpvp.config.ModConfig;
import com.synxt.strengthpvp.feature.FeatureModule;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Local first-person pose and editable swing curve. No attack packets, reach or hit registration are changed. */
@Mixin(HeldItemRenderer.class)
public abstract class BetterPvpHeldItemMixin {
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void strengthpvp$pushHandPose(AbstractClientPlayerEntity player, float tickDelta, float pitch,
            Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices,
            VertexConsumerProvider vertices, int light, CallbackInfo ci) {
        if (!ModConfig.isEnabled(FeatureModule.BETTER_PVP)) return;

        float strength = ModConfig.getOption(FeatureModule.BETTER_PVP) / 100.0f;
        float phase = strengthpvp$easeProgress(swingProgress);
        float swingArc = (float) Math.sin(Math.PI * phase) * strength;
        float baseX = ModConfig.getOption2(FeatureModule.BETTER_PVP) / 100.0f;
        float baseY = ModConfig.getOption3(FeatureModule.BETTER_PVP) / 100.0f;
        float depth = ModConfig.getOption5(FeatureModule.BETTER_PVP) / 100.0f;
        float restTilt = ModConfig.getOption4(FeatureModule.BETTER_PVP);
        float animatedTilt = ModConfig.getOption10(FeatureModule.BETTER_PVP) * swingArc;
        float animatedShift = (ModConfig.getOption11(FeatureModule.BETTER_PVP) / 100.0f) * swingArc;
        float scale = ModConfig.getOption6(FeatureModule.BETTER_PVP) / 100.0f;

        matrices.push();
        matrices.translate(baseX + animatedShift, baseY, depth);
        if (restTilt + animatedTilt != 0.0f) {
            matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(restTilt + animatedTilt));
        }
        matrices.scale(scale, scale, scale);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void strengthpvp$popHandPose(AbstractClientPlayerEntity player, float tickDelta, float pitch,
            Hand hand, float swingProgress, ItemStack stack, float equipProgress, MatrixStack matrices,
            VertexConsumerProvider vertices, int light, CallbackInfo ci) {
        if (ModConfig.isEnabled(FeatureModule.BETTER_PVP)) matrices.pop();
    }

    @ModifyArg(method = "renderFirstPersonItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applySwingOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V"), index = 2)
    private float strengthpvp$customSwingAnimation(float swingProgress) {
        if (!ModConfig.isEnabled(FeatureModule.BETTER_PVP)) return swingProgress;
        return strengthpvp$easeSwing(swingProgress);
    }

    /** Shared easing curve drives both the item swing and the animated hand pose. */
    private static float strengthpvp$easeSwing(float swingProgress) {
        float progress = strengthpvp$easeProgress(swingProgress);
        float strength = ModConfig.getOption(FeatureModule.BETTER_PVP) / 100.0f;
        return Math.max(0.0f, Math.min(1.0f, progress * strength));
    }

    private static float strengthpvp$easeProgress(float swingProgress) {
        float speed = ModConfig.getOption9(FeatureModule.BETTER_PVP) / 100.0f;
        float rawProgress = Math.max(0.0f, Math.min(1.0f, swingProgress));
        float progress = (float) Math.pow(rawProgress, 1.0f / Math.max(0.01f, speed));
        int preset = ModConfig.getOption7(FeatureModule.BETTER_PVP);
        switch (preset) {
            case 1: // vanilla timing
                break;
            case 2: // smooth sine
                progress = (float) (0.5 - Math.cos(Math.PI * progress) * 0.5);
                break;
            case 3: // quick response, soft landing
                progress = 1.0f - (float) Math.pow(1.0f - progress, 3.0);
                break;
            case 4: // user-controlled symmetric curve
                float power = 1.0f + ModConfig.getOption8(FeatureModule.BETTER_PVP) * 0.65f;
                if (progress < 0.5f) {
                    progress = 0.5f * (float) Math.pow(progress * 2.0f, power);
                } else {
                    progress = 1.0f - 0.5f * (float) Math.pow((1.0f - progress) * 2.0f, power);
                }
                break;
            default: // smoothstep
                progress = progress * progress * (3.0f - 2.0f * progress);
                break;
        }
        return Math.max(0.0f, Math.min(1.0f, progress));
    }
}
