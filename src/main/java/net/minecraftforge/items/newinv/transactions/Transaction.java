package net.minecraftforge.items.newinv.transactions;

import net.minecraftforge.items.newinv.IHandlerProvider;
import net.minecraftforge.items.newinv.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public abstract class Transaction<H extends IItemHandler> implements AutoCloseable {
    @Nullable
    private IHandlerProvider<H> mHandlerProvider;

    public Transaction(@Nonnull IHandlerProvider<H> supplier) {
        mHandlerProvider = Objects.requireNonNull(supplier);
    }

    @Nullable
    protected IHandlerProvider<H> getHandlerProvider() {
        return mHandlerProvider;
    }

    /**
     *
     * @return whether or not Operations may still be performed on this Transaction
     */
    public boolean isValid() {
        return getHandlerProvider()!=null && getHandlerProvider().isValid();
    }

    @Override
    public void close() {
        if (getHandlerProvider()!=null) {
            getHandlerProvider().onTransactionTerminated(this);
            mHandlerProvider = null;
        }
    }
}
