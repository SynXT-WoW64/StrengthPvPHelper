package com.synxt.strengthpvp.mixin;

import com.synxt.strengthpvp.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {
    @Inject(method = "init()V", at = @At("TAIL"))
    private void strengthpvp$addSafeOptimizationToggle(CallbackInfo ci) {
        boolean enabled = ModConfig.isSuperOptimization();
        String label = enabled ? "SynXT: SUPER OPTIMIZATION ON" : "SynXT: SUPER OPTIMIZATION OFF";
        ButtonWidget button = new ButtonWidget(8, 8, 188, 20, new LiteralText(label), clicked -> {
            ModConfig.setSuperOptimization(!ModConfig.isSuperOptimization());
            MinecraftClient.getInstance().setScreen(new TitleScreen());
        });

        ((ScreenAddDrawableChildInvoker) (Object) this).strengthpvp$invokeAddDrawableChild(button);
    }
}
