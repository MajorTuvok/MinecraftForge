package net.minecraftforge.items.newinv;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public class ItemSlotHandler implements ISlotHandler{
    private NonNullList<ItemStack> stacks;

    public ItemSlotHandler(NonNullList<ItemStack> stacks) {
        this.stacks = Objects.requireNonNull(stacks,"Cannot construct an "+getClass().getSimpleName()+" without a backing List!");
    }

    public ItemSlotHandler(int size) {
        this(NonNullList.withSize(size,ItemStack.EMPTY));
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<ItemStack> iterator() {
        return getStacks().iterator();
    }

    /**
     * Try to insert (or simulate insertion) the given Stack into this IItemHandler if the given Predicate holds true against any present {@link ItemStack}
     * (Note that this should only be tested if and only if {@link #isValidStack(ItemStack)} returns true and there is enough space for the given stack). <br>
     * This is especially useful for creating insertion filters, which f.e. only insert if there is already one {@link ItemStack} of the inserted type, or other similar mechanisms.
     *
     * @param input     The stack to insert
     * @param predicate
     * @param simulate  Whether or not insertion behaviour should only be simulated.
     *                  If this is true, then nothing will change in this IItemHandler.
     * @return The remaining ItemStack
     */
    @Override
    public ItemStack insertIf(ItemStack input, Predicate<ItemStack> predicate, boolean simulate) {
        ItemStack res = simulate ? input.copy() : input; //if simulate is true, we should not modify the input stack
        int curSlot = findSlotToInsert(predicate, 0, input);
        while (curSlot != -1 && res.getCount() > 0) {

            int stackLimit = getStackLimit(curSlot);
            ItemStack current = getStack(curSlot);
            if (current.isEmpty()) {
                current = res.copy();
                current.setCount(0); //same item as res, but empty
            }
            if (stackLimit < current.getCount() + res.getCount()) {
                current.setCount(stackLimit);
            }
            res.setCount(res.getCount() - current.getCount());

            if (!simulate) {
                setStack(current, curSlot);
            }

            if (res.getCount() > 0) {
                curSlot = findSlotToInsert(predicate,curSlot,input);
            }
        }
        return res.isEmpty() ? ItemStack.EMPTY : res;
    }

    @Override
    public NonNullList<ItemStack> extractAllIf(Predicate<ItemStack> predicate, ToIntFunction<ItemStack> countPerStack, boolean simulate) {
        NonNullList<ItemStack> list = NonNullList.create();
        for (int i=0; i<getSlotCount(); ++i) {
            ItemStack stack = getStack(i);
            if (predicate.test(stack)) {
                ItemStack ext = ItemHelper.extractFromSlot(this,countPerStack,i,simulate);
                list.add(ext);
            }
        }
        return list;
    }

    @Override
    public ItemStack extractIf(Predicate<ItemStack> predicate, ToIntFunction<ItemStack> count, boolean simulate) {
        for (int i=0; i<getSlotCount(); ++i) {
            ItemStack stack = getStack(i);
            if (predicate.test(stack)) {
                return ItemHelper.extractFromSlot(this,count,i,simulate);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean test(Predicate<ItemStack> predicate) {
        for(ItemStack stack : this)
            if (predicate.test(stack))
                return true;
        return false;
    }

    protected NonNullList<ItemStack> getStacks() {
        return stacks;
    }

    @Override
    public int getSlotCount() {
        return getStacks().size();
    }

    @Nonnull
    @Override
    public ItemStack getStack(int slot) {
        validateSlot(slot);
        return getStacks().get(slot);
    }

    /**
     * Checks whether it is possible to insert the given Stack into the given slot, taking {@link #isValidStackForSlot(ItemStack, int)} into account.
     * Basically this not only state's if this type of stack can go into the given slot, but also that it is currently possible to insert it there.
     *
     * @param stack The stack to test for
     * @param slot  The slot to test
     * @return whether or not this specific stack can be inserted into the given slot. Should also return true if only a part of this stack can be inserted.
     */
    @Override
    public boolean canInsertStackInSlot(ItemStack stack, int slot) {
        validateSlot(slot);
        if (isValidStackForSlot(stack,slot)) {
            ItemStack cur = getStack(slot);
            return cur.getCount()<getStackLimit(slot);
        }
        return false;
    }

    /**
     * Try to set the given stack in the given slot.
     *
     * @param stack The stack to set
     * @param slot  The slot to set
     * @return Whether or not it was possible to set the given stack in the given slot.
     * In general this will be the same as calling {@link #canInsertStackInSlot(ItemStack, int)} prior to calling this method.
     */
    @Override
    public boolean setStack(ItemStack stack, int slot) {
        validateSlot(slot);
        if (!canInsertStackInSlot(stack,slot))
            return false;
        ItemStack prev = getStack(slot);
        getStacks().set(slot,stack);
        return true;
    }

    @Override
    public int getStackLimit(int slot) {
        return getStack(slot).getMaxStackSize();
    }

    @Override
    public NBTBase serializeNBT() {
        NBTTagList nbtTagList = new NBTTagList();
        int size = getSlotCount();
        for (int i = 0; i < size; i++)
        {
            ItemStack stack = getStack(i);
            if (!stack.isEmpty())
            {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setInteger("Slot", i);
                stack.writeToNBT(itemTag);
                nbtTagList.appendTag(itemTag);
            }
        }
        return nbtTagList;
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        NBTTagList tagList = (NBTTagList) nbt;
        for (int i = 0; i < tagList.tagCount(); i++)
        {
            NBTTagCompound itemTags = tagList.getCompoundTagAt(i);
            int j = itemTags.getInteger("Slot");

            if (j >= 0 && j < getSlotCount())
            {
                setStack(new ItemStack(itemTags),j);
            }
        }
    }

    protected int findSlotToInsert(Predicate<ItemStack> condition, int startSlot, @Nonnull ItemStack stack) {
        startSlot = MathHelper.clamp(startSlot,0,getStacks().size()-1);
        int curSlot = startSlot;

        if (canInsertStackInSlot(stack,curSlot) && condition.test(getStack(curSlot)))
            return curSlot;

        curSlot = validityIncrement(curSlot);

        while (startSlot!=curSlot && !canInsertStackInSlot(stack,curSlot) && !condition.test(getStack(curSlot))) {
            curSlot = validityIncrement(curSlot);
        }
        return startSlot!=curSlot?curSlot:-1;
    }

    public boolean isValidSlot(int slot) {
        return (slot>=0 && slot<getStacks().size());
    }

    protected void validateSlot(int slot) {
        if (slot<0 || slot>=getStacks().size())
            throw new IndexOutOfBoundsException("Slot "+slot+" is out of "+getClass().getSimpleName()+"'s bounds. Valid range is [0, "+(getStacks().size()-1)+"] !");
    }

    protected int validityIncrement(int slotNum) {
        return isValidSlot(slotNum+1) ? slotNum+1:0;
    }
}
