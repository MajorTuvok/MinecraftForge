package net.minecraftforge.items.newinv.transactions;

import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.newinv.ISlotHandler;
import net.minecraftforge.items.newinv.ISlotHandlerProvider;

import javax.annotation.Nonnull;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public class SlotHandlerSimpleMultiExtractTransaction extends ExtractTransaction<ISlotHandler,NonNullList<ItemStack>> {
    private Predicate<ItemStack> condition;
    private ToIntFunction<ItemStack> extractFunction;

    public SlotHandlerSimpleMultiExtractTransaction(@Nonnull ISlotHandlerProvider<ISlotHandler> supplier, final Predicate<ItemStack> condition, final ToIntFunction<ItemStack> extractFunction) {
        super(supplier);
        this.condition = condition;
        this.extractFunction = extractFunction;
    }

    @Override
    protected ISlotHandlerProvider<ISlotHandler> getHandlerProvider() {
        return (ISlotHandlerProvider<ISlotHandler>) super.getHandlerProvider();
    }

    public Predicate<ItemStack> getCondition() {
        return condition;
    }

    public ToIntFunction<ItemStack> getExtractFunction() {
        return extractFunction;
    }

    /**
     * @param simulate whether or not this Transactions Operation should only be simulated
     * @return The Result of this Transactions Operation or an empty Result if this Transaction is no longer valid or the Transaction can no longer be executed.
     */
    @Override
    public NonNullList<ItemStack> extract(boolean simulate) {
        if (isValid()) {
            return getHandlerProvider().get().extractAllIf(getCondition(),getExtractFunction(),simulate);
        }
        return NonNullList.create();
    }
}
