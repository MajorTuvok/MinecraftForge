package net.minecraftforge.items.newinv;

import javax.annotation.Nullable;

public interface IHandlerProviderFactory <H extends IItemHandler, P extends IHandlerProvider<H>>{
    @Nullable
    public P newHandlerProvider();
}
