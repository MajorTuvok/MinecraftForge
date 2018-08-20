package net.minecraftforge.items.newinv;

import net.minecraft.item.ItemStack;

public interface IItemObserver {
    public void onInsertStack(ItemStack stack);

    public void onExtractStack(ItemStack stack);
}
