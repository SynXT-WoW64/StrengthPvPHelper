package com.synxt.strengthpvp.mixin;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Screen.addDrawableChild is declared on Screen, not on TitleScreen.
 * Expose it from its declaring class so TitleScreenMixin can add its button
 * without an invalid @Shadow against the subclass.
 */
@Mixin(Screen.class)
public interface ScreenAddDrawableChildInvoker {
    @Invoker("addDrawableChild")
    <T extends Element & Drawable & Selectable> T strengthpvp$invokeAddDrawableChild(T element);
}
