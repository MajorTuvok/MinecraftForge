package net.minecraftforge.items.newinv;

import net.minecraft.item.ItemStack;

public interface ISlotObserver {
    public void onSlotChanged(ItemStack oldStack, ItemStack newStack, int slot);
}
