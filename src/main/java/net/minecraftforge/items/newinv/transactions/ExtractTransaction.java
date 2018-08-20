package net.minecraftforge.items.newinv.transactions;

import net.minecraftforge.items.newinv.IHandlerProvider;
import net.minecraftforge.items.newinv.IItemHandler;

import javax.annotation.Nonnull;

public abstract class ExtractTransaction<H extends IItemHandler,R> extends Transaction<H>{
    public ExtractTransaction(@Nonnull IHandlerProvider<H> supplier) {
        super(supplier);
    }

    /**
     * @param simulate whether or not this Transactions Operation should only be simulated
     * @return The Result of this Transactions Operation or an empty Result if this Transaction is no longer valid or the Transaction can no longer be executed.
     */
    public abstract R extract(boolean simulate);
}
