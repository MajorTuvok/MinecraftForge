package net.minecraftforge.items.newinv.transactions;

import net.minecraft.util.math.MathHelper;
import net.minecraftforge.items.newinv.IHandlerProvider;
import net.minecraftforge.items.newinv.ISlotHandler;

import javax.annotation.Nonnull;

/**
 * Helper class abstracting away commonly used logic for SlotHandlerTransactions
 */
public class TransactionSlotHelper {
    private Transaction<? extends ISlotHandler> transaction;
    private int lastSlot;

    public TransactionSlotHelper(@Nonnull Transaction<? extends ISlotHandler> transaction) {
        this.transaction = transaction;
        this.lastSlot = 0;
    }

    public boolean isValid() {
        return transaction.isValid();
    }

    public IHandlerProvider<? extends ISlotHandler> getHandlerProvider() {
        return transaction.getHandlerProvider();
    }

    public int getLastSlot() {
        return lastSlot;
    }

    protected void nextSlot(@Nonnull ISlotHandler handler) {
        if (getLastSlot()+1>=handler.getSlotCount())
            setLastSlot(0);
        else
            setLastSlot(getLastSlot()+1);
    }

    protected void nextSlot() {
        ISlotHandler handler = isValid()?getHandlerProvider().get():null;
        if (handler!=null)
            nextSlot(handler);
        else
            setLastSlot(getLastSlot()+1);
    }

    protected void setLastSlot(int lastSlot) {
        this.lastSlot = MathHelper.clamp(lastSlot,
                0,
                isValid()? getHandlerProvider().get().getSlotCount()-1 : Integer.MAX_VALUE);
    }
}
