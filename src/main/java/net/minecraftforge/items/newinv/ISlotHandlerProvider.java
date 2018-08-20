package net.minecraftforge.items.newinv;

public interface ISlotHandlerProvider<T extends ISlotHandler> extends IHandlerProvider<T> {
    /**
     * Adds an {@link ISlotObserver} to be notified of changes in this ISlotHolder.
     * @implNote Notice, that these have to be stored in WeakReference Objects, because it is expected,
     * that f.i. TileEntities may be added as SlotObservers. Notice also, that this requires you as a user of this Method to keep an active reference to this {@link ISlotObserver}.
     * @param observer The {@link ISlotObserver} to be notified of changes
     */
    public void addSlotObserver(ISlotObserver observer);
}
