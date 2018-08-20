package net.minecraftforge.items.newinv;

import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public interface ISlotHolder extends Iterable<ItemStack>{
    public int getSlotCount();

    @Nonnull
    public ItemStack getStack(int slot);

    /**
     * Checks whether or not a specific Item, a specific NBT, etc. prevents this Stack from being inserted into the given Slot.
     * @param stack The stack to test for
     * @param slot The slot to test
     * @return whether or not this type of stack can principally be inserted into the given slot
     */
    public default boolean isValidStackForSlot(ItemStack stack, int slot) {
        return true;
    }

    /**
     * Checks whether it is possible to insert the given Stack into the given slot, taking {@link #isValidStackForSlot(ItemStack, int)} into account.
     * @param stack The stack to test for
     * @param slot The slot to test
     * @return whether or not this specific stack can be inserted into the given slot. Should also return true if only a part of this stack can be inserted.
     */
    public boolean canInsertStackInSlot(ItemStack stack, int slot);

    /**
     * Try to set the given stack in the given slot.
     * @param stack The stack to set
     * @param slot The slot to set
     * @return Whether or not it was possible to set the given stack in the given slot.
     *  In general this will be the same as calling {@link #canInsertStackInSlot(ItemStack, int)} prior to calling this method.
     */
    public boolean setStack(ItemStack stack, int slot);

    public int getStackLimit(int slot);
}
