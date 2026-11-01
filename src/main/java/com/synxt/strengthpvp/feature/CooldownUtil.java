package com.synxt.strengthpvp.feature;

import com.synxt.strengthpvp.mixin.ItemCooldownEntryAccessor;
import com.synxt.strengthpvp.mixin.ItemCooldownManagerAccessor;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;

import java.util.Map;

/** Reads exact vanilla cooldown end ticks through mixin accessors. */
public final class CooldownUtil {
    private CooldownUtil() { }

    public static int getRemainingTicks(ItemCooldownManager manager, Item item, float tickDelta) {
        if (manager == null || item == null) return 0;
        try {
            ItemCooldownManagerAccessor accessor = (ItemCooldownManagerAccessor) manager;
            Map<Item, Object> entries = accessor.strengthpvp$getEntries();
            Object entry = entries.get(item);
            if (entry == null) return 0;
            int endTick = ((ItemCooldownEntryAccessor) entry).strengthpvp$getEndTick();
            double remaining = endTick - (accessor.strengthpvp$getTick() + tickDelta);
            return Math.max(0, (int) Math.ceil(remaining));
        } catch (RuntimeException exception) {
            // Keep the HUD safe if another mod replaces the vanilla cooldown manager.
            return 0;
        }
    }
}
