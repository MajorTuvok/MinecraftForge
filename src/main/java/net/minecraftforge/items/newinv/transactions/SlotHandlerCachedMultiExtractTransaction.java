package net.minecraftforge.items.newinv.transactions;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.newinv.ISlotHandler;
import net.minecraftforge.items.newinv.ISlotHandlerProvider;
import net.minecraftforge.items.newinv.ISlotObserver;
import net.minecraftforge.items.newinv.ItemHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public class SlotHandlerCachedMultiExtractTransaction extends SlotHandlerSimpleMultiExtractTransaction implements ISlotObserver {
    @Nullable
    private IntList slotCache;
    private int maxCachedExtractions;
    private int cachedExtractions;

    public SlotHandlerCachedMultiExtractTransaction(@Nonnull ISlotHandlerProvider<ISlotHandler> supplier, Predicate<ItemStack> condition, ToIntFunction<ItemStack> extractFunction) {
        super(supplier, condition, extractFunction);
        this.slotCache = null;
        this.maxCachedExtractions = 4;
        this.cachedExtractions = 0;
    }

    @Nullable
    protected IntList getSlotCache() {
        return slotCache;
    }

    protected void setSlotCache(@Nullable IntList slotCache) {
        this.slotCache = slotCache;
    }

    public int getMaxCachedExtractions() {
        return maxCachedExtractions;
    }

    public void setMaxCachedExtractions(int maxCachedExtractions) {
        this.maxCachedExtractions = maxCachedExtractions;
    }

    public int getPerformedCachedExtractions() {
        return cachedExtractions;
    }

    public void setPerformedCachedExtractions(int cachedExtractions) {
        this.cachedExtractions = cachedExtractions;
    }

    public void incrementPerformedCachedExtractions() {
        ++this.cachedExtractions;
    }

    public void updateCache() {
        setPerformedCachedExtractions(0);
        if (createCache() && isValid()) {
            assert getSlotCache() != null;
            ISlotHandler handler = getHandlerProvider().get();
            getSlotCache().clear();
            for (int i = 0; i < handler.getSlotCount(); ++i) {
                if (getCondition().test(handler.getStack(i))) {
                    getSlotCache().add(i);
                }
            }
        }
    }

    public void updateCache(int changedSlot) {
        if (changedSlot < 0) {
            updateCache();
        } else if (createCache() && isValid()) {
            assert getSlotCache() != null;
            ISlotHandler handler = getHandlerProvider().get();
            boolean res = getCondition().test(handler.getStack(changedSlot));
            boolean contains = getSlotCache().contains(changedSlot);
            if (res && !contains) {
                getSlotCache().add(changedSlot);
            } else if (!res && contains) {
                getSlotCache().rem(changedSlot);
            }
        }
    }

    /**
     * @param simulate whether or not this Transactions Operation should only be simulated
     * @return The Result of this Transactions Operation or an empty Result if this Transaction is no longer valid or the Transaction can no longer be executed.
     */
    @Override
    public NonNullList<ItemStack> extract(boolean simulate) {
        if (isValid()) {
            NonNullList<ItemStack> res = NonNullList.create();
            if (getSlotCache() != null && getPerformedCachedExtractions() < getMaxCachedExtractions()) {
                incrementPerformedCachedExtractions();
                extractCachedStacks(res, simulate);
            } else { //TODO combine in one Loop
                updateCache();
                extractCachedStacks(res, simulate);
            }
            return res;
        }
        return NonNullList.create();
    }

    @Override
    public void onSlotChanged(ItemStack oldStack, ItemStack newStack, int slot) {
        updateCache(slot);
    }

    protected void extractCachedStacks(NonNullList<ItemStack> extractTo, boolean simulate) {
        if (isValid() && getSlotCache() != null) {
            ISlotHandler handler = getHandlerProvider().get();
            for (int i = 0; i < getSlotCache().size(); ++i) {
                int prevSize = getSlotCache().size();
                int slot = getSlotCache().getInt(i);
                ItemStack ext = extractStack(handler, slot, simulate);
                if (!ext.isEmpty()) {
                    extractTo.add(ext);
                }
                if (prevSize != getSlotCache().size()) { // The Callback Method (onSlotChanged) determined, that this slot should no longer exist in the slot Cache
                    --i;
                }
            }
        }
    }

    protected ItemStack extractStack(ISlotHandler handler, int slot, boolean simulate) {
        return ItemHelper.extractFromSlot(handler, getExtractFunction(), slot, simulate);
    }

    private boolean createCache() {
        if (isValid()) {
            if (slotCache == null) {
                slotCache = new IntArrayList();
            }
            return true;
        } else {
            slotCache = null;
            return false;
        }
    }
}
