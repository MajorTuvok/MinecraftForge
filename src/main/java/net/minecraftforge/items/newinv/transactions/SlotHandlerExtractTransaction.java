package net.minecraftforge.items.newinv.transactions;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.newinv.ISlotHandler;
import net.minecraftforge.items.newinv.ISlotHandlerProvider;
import net.minecraftforge.items.newinv.ItemHelper;

import javax.annotation.Nonnull;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public class SlotHandlerExtractTransaction extends ExtractTransaction<ISlotHandler,ItemStack> {
    private final Predicate<ItemStack> condition;
    private final ToIntFunction<ItemStack> extractFunction;
    private TransactionSlotHelper helper;

    public SlotHandlerExtractTransaction(@Nonnull ISlotHandlerProvider<ISlotHandler> supplier, @Nonnull final Predicate<ItemStack> condition, final ToIntFunction<ItemStack> extractFunction) {
        super(supplier);
        this.condition = condition;
        this.extractFunction = extractFunction;
        this.helper = new TransactionSlotHelper(this);
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

    public int getLastSlot() {
        return helper.getLastSlot();
    }

    protected void nextSlot(@Nonnull ISlotHandler handler) {
        helper.nextSlot(handler);
    }

    protected void nextSlot() {
        helper.nextSlot();
    }

    protected void setLastSlot(int lastSlot) {
        helper.setLastSlot(lastSlot);
    }

    /**
     * @param simulate whether or not this Transactions Operation should only be simulated
     * @return The Result of this Transactions Operation or an empty Result if this Transaction is no longer valid or the Transaction can no longer be executed.
     */
    @Override
    public ItemStack extract(boolean simulate) {
        ItemStack res = ItemStack.EMPTY;
        if (isValid()) {
            ISlotHandler handler = getHandlerProvider().get();
            if (findSlotToExtract(handler)) {
                res = extractStack(handler,getLastSlot(),simulate);
            }
        }
        return res;
    }

    protected ItemStack extractStack(ISlotHandler handler, int slot, boolean simulate) {
        return ItemHelper.extractFromSlot(handler,getExtractFunction(),slot,simulate);
    }

    protected boolean findSlotToExtract(@Nonnull ISlotHandler handler) {
        int startSlot = getLastSlot();
        if (getCondition().test(handler.getStack(getLastSlot())))
            return true;
        nextSlot(handler);
        while (startSlot!=getLastSlot() && !getCondition().test(handler.getStack(getLastSlot()))) {
            nextSlot(handler);
        }
        return startSlot!=getLastSlot();
    }
}
