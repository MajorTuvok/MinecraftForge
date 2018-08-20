package net.minecraftforge.items.newinv.transactions;

import net.minecraft.item.ItemStack;
import net.minecraftforge.items.newinv.IItemHandler;
import net.minecraftforge.items.newinv.ISlotHandler;
import net.minecraftforge.items.newinv.ISlotHandlerProvider;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

public class SlotHandlerInsertTransaction extends InsertTransaction<ISlotHandler,ItemStack,ItemStack>{
    private final Predicate<ItemStack> condition;
    private TransactionSlotHelper helper;

    public SlotHandlerInsertTransaction(@Nonnull ISlotHandlerProvider<ISlotHandler> supplier, @Nonnull final Predicate<ItemStack> condition) {
        super(supplier);
        this.condition = condition;
        this.helper = new TransactionSlotHelper(this);
    }

    @Override
    protected ISlotHandlerProvider<ISlotHandler> getHandlerProvider() {
        return (ISlotHandlerProvider<ISlotHandler>) super.getHandlerProvider();
    }

    /**
     * @param input    The input to insert with this Transactions operation. It might be changed by this operation if simulate is false.
     * @param simulate whether or not this Transaction should only be simulated (e.g. if this is true there won't be any changes to the underlying {@link IItemHandler})
     * @return The Result of this Transactions Operation or an empty Result if this Transaction is no longer valid.
     *          The input stack will be returned if it was impossible to perform this Transactions Operation and a part of the input Stack will be returned if it couldn'T fully be inserted
     */
    @Override
    public ItemStack insert(ItemStack input, boolean simulate) {
        if (isValid()) {
            ISlotHandler handler = getHandlerProvider().get();
            ItemStack res = simulate?input.copy():input; //if simulate is true, we should not modify the input stack
            while (findSlotToInsert(handler,input) && res.getCount()>0) {

                int stackLimit = handler.getStackLimit(getLastSlot());
                ItemStack current = handler.getStack(getLastSlot()); //the current stack in the slot really doesn't matter, if handler.canInsertStackInSlot works correctly
                if (stackLimit<current.getCount()+res.getCount()) {
                    current.setCount(stackLimit);
                }
                res.setCount(res.getCount()-current.getCount());

                if (!simulate)
                    handler.setStack(current,getLastSlot());

                if (res.getCount()>0)
                    nextSlot(handler);
            }
            return res.isEmpty()?ItemStack.EMPTY:res;
        }
        return ItemStack.EMPTY;
    }

    protected TransactionSlotHelper getHelper() {
        return helper;
    }

    protected void setHelper(TransactionSlotHelper helper) {
        this.helper = helper;
    }

    public Predicate<ItemStack> getCondition() {
        return condition;
    }

    public int getLastSlot() {
        return helper.getLastSlot();
    }

    protected void setLastSlot(int lastSlot) {
        helper.setLastSlot(lastSlot);
    }

    protected void nextSlot(@Nonnull ISlotHandler handler) {
        helper.nextSlot(handler);
    }

    protected void nextSlot() {
        helper.nextSlot();
    }

    protected boolean findSlotToInsert(@Nonnull ISlotHandler handler, @Nonnull ItemStack stack) {
        int startSlot = getLastSlot();
        if (handler.canInsertStackInSlot(stack,getLastSlot()) && getCondition().test(handler.getStack(getLastSlot())))
            return true;
        nextSlot(handler);
        while (startSlot!=getLastSlot() && !handler.canInsertStackInSlot(stack,getLastSlot()) && !getCondition().test(handler.getStack(getLastSlot()))) {
            nextSlot(handler);
        }
        return startSlot!=getLastSlot();
    }
}
