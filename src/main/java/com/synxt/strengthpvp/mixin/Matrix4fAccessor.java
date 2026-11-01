package com.synxt.strengthpvp.mixin;

import net.minecraft.util.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Matrix4f.class)
public interface Matrix4fAccessor {
    @Accessor("a00") float strengthpvp$getScaleX();
    @Accessor("a00") void strengthpvp$setScaleX(float value);
}
