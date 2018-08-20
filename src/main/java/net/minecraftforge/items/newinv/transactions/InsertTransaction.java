package net.minecraftforge.items.newinv.transactions;

import net.minecraftforge.items.newinv.IHandlerProvider;
import net.minecraftforge.items.newinv.IItemHandler;

import javax.annotation.Nonnull;

public abstract class InsertTransaction<H extends IItemHandler,R,I> extends Transaction<H> {
    public InsertTransaction(@Nonnull IHandlerProvider<H> supplier) {
        super(supplier);
    }

    /**
     * @param input    The input to insert with this Transactions operation. It might be changed by this operation if simulate is false.
     * @param simulate whether or not this Transaction should only be simulated (e.g. if this is true there won't be any changes to the underlying {@link IItemHandler})
     * @return The Result of this Transactions Operation or an empty Result if this Transaction is no longer valid.
     *          The input stack will be returned if it was impossible to perform this Transactions Operation and a part of the input Stack will be returned if it couldn'T fully be inserted
     */
    public abstract R insert(I input, boolean simulate);
}
