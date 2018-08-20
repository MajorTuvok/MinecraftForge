package net.minecraftforge.items.newinv;

import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.newinv.transactions.ExtractTransaction;
import net.minecraftforge.items.newinv.transactions.InsertTransaction;
import net.minecraftforge.items.newinv.transactions.Transaction;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public interface IHandlerProvider<T extends IItemHandler> extends Supplier<T> {
    /**
     * Called by transactions if there close Methods were called and they should be removed from any caches this HandlerProvider might have.
     * @param transaction The transaction that was terminated
     */
    public void onTransactionTerminated(Transaction transaction);

    /**
     * Adds an {@link IItemObserver} to be notified of changes in this IItemHandler.
     * @param observer The {@link IItemObserver} to be notified of changes
     */
    public void addItemObserver(IItemObserver observer);

    /**
     * The contract of this Methods is, that whenever it is called, then this {@link IHandlerProvider}'s {@link #get()} Method will return a non-null value, null otherwise.
     * This checks for example for {@link net.minecraft.tileentity.TileEntity}s, that the {@link net.minecraft.world.chunk.Chunk} the {@link net.minecraft.tileentity.TileEntity} is in, is still loaded.
     * @return whether or not, this {@link IHandlerProvider} is still valid and subsequent calls to {@link #get()} will return a non-null value.
     */
    public boolean isValid();

    public InsertTransaction<T, ItemStack,ItemStack> startInsert(Predicate<ItemStack> insertCallback);

    public ExtractTransaction<T,ItemStack> startExtract(Predicate<ItemStack> extractCallback, ToIntFunction<ItemStack> count);

    public ExtractTransaction<T, NonNullList<ItemStack>> startExtractMultiple(Predicate<ItemStack> extractCallback, ToIntFunction<ItemStack> count);
}
