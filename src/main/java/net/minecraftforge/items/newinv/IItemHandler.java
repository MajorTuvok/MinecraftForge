package net.minecraftforge.items.newinv;

import com.google.common.base.Predicates;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.newinv.ItemHelper.Functions;

import javax.annotation.Nonnull;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public interface IItemHandler extends INBTSerializable<NBTBase> {
    /**
     * Checks whether or not a specific Item, a specific NBT, etc. prevents this Stack from being inserted into this IItemHandler.
     * Ignores f.e. whether there is space for this stack in this IItemHandler or not. Notice that you cannot insert empty stacks are always valid.
     * @param stack The stack to test for
     * @return whether or not this type of stack can principally be inserted
     */
    public default boolean isValidStack(ItemStack stack) {
        return true;
    }

    /**
     * Try to insert (or simulate insertion) the given Stack into this IItemHandler.
     * Should provide similar behaviour as if calling {@link #insertIf(ItemStack, Predicate, boolean)} with an always true predicate.
     * <br>
     * Ideally you can perform some optimizations instead of simply calling it with an always true Predicate
     * @see #insertIf(ItemStack, Predicate, boolean)
     */
    public default ItemStack insert(ItemStack stack, boolean simulate) {
        return insertIf(stack,Predicates.alwaysTrue(),simulate);
    }

    /**
     * Try to insert (or simulate insertion) the given Stack into this IItemHandler if the given Predicate holds true against any present {@link ItemStack}
     * (Note that this should only be tested if and only if {@link #isValidStack(ItemStack)} returns true and there is enough space for the given stack). <br>
     * This is especially useful for creating insertion filters, which f.e. only insert if there is already one {@link ItemStack} of the inserted type, or other similar mechanisms.
     * @param stack The stack to insert
     * @param simulate Whether or not insertion behaviour should only be simulated.
     *                 If this is true, then nothing will change in this IItemHandler.
     * @return The remaining ItemStack
     */
    public ItemStack insertIf(ItemStack stack, Predicate<ItemStack> predicate, boolean simulate);

    /**
     *
     * @param stack The stack to extract
     * @param simulate whether or not this should only be simulated
     * @return the truly extracted ItemStack, which might be smaller than requested or empty
     */
    @Nonnull
    public default ItemStack extract(ItemStack stack, boolean simulate) {
        return extractIf(Functions.equalItemPredicate(stack), ItemHelper.Functions.constantFunction(stack.getCount()),simulate);
    }

    @Nonnull
    public ItemStack extractIf(Predicate<ItemStack> predicate, ToIntFunction<ItemStack> count, boolean simulate);

    @Nonnull
    public NonNullList<ItemStack> extractAllIf(Predicate<ItemStack> predicate, ToIntFunction<ItemStack> countPerStack, boolean simulate);

    public boolean test(Predicate<ItemStack> predicate);
}
