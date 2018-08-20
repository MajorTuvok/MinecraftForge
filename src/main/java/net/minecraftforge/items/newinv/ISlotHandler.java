package net.minecraftforge.items.newinv;


import net.minecraft.item.ItemStack;

public interface ISlotHandler extends IItemHandler,ISlotHolder {
    /**
     * Checks whether or not a specific Item, a specific NBT, etc. prevents this Stack from being inserted into the given Slot.
     *
     * @param stack The stack to test for
     * @param slot  The slot to test
     * @return whether or not this type of stack can principally be inserted into the given slot
     */
    @Override
    default boolean isValidStackForSlot(ItemStack stack, int slot) {
        if (!isValidStack(stack))
            return false;
        ItemStack cur = getStack(slot);
        return cur.isEmpty() || ItemStack.areItemsEqual(cur,stack);
    }
}
