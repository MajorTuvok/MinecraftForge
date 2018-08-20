package net.minecraftforge.items.newinv;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.items.newinv.transactions.ExtractTransaction;
import net.minecraftforge.items.newinv.transactions.InsertTransaction;
import net.minecraftforge.items.newinv.transactions.Transaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public class ItemHelper {
    public static class Functions {
        public static Predicate<ItemStack> equalItemPredicate(ItemStack predicateStack) {
            return predicateStack::isItemEqual;
        }

        public static ToIntFunction<ItemStack> constantFunction(int num) {
            return stack -> num;
        }

        public static ToIntFunction<ItemStack> stackSizeFunction() {
            return ItemStack::getCount;
        }
    }

    public static class HandlerProviders {

        private static abstract class SimpleHandlerProvider implements IHandlerProvider<IItemHandler> {
            @Override
            public void onTransactionTerminated(Transaction transaction) {

            }

            @Override
            public void addItemObserver(IItemObserver observer) {

            }

            @Override
            public InsertTransaction<IItemHandler, ItemStack, ItemStack> startInsert(final Predicate<ItemStack> insertCallback) {
                return new InsertTransaction<IItemHandler, ItemStack, ItemStack>(this) {
                    @Override
                    public ItemStack insert(ItemStack input, boolean simulate) {
                        if (!isValid())
                            return ItemStack.EMPTY;
                        return getHandlerProvider().get().insertIf(input,insertCallback,simulate);
                    }
                };
            }

            @Override
            public ExtractTransaction<IItemHandler, ItemStack> startExtract(final Predicate<ItemStack> extractCallback, final ToIntFunction<ItemStack> count) {
                return new ExtractTransaction<IItemHandler, ItemStack>(this) {
                    @Override
                    public ItemStack extract(boolean simulate) {
                        if (!isValid())
                            return ItemStack.EMPTY;
                        return getHandlerProvider().get().extractIf(extractCallback,count,simulate);
                    }
                };
            }

            @Override
            public ExtractTransaction<IItemHandler, NonNullList<ItemStack>> startExtractMultiple(final Predicate<ItemStack> extractCallback, final ToIntFunction<ItemStack> count) {
                return new ExtractTransaction<IItemHandler, NonNullList<ItemStack>>(this) {
                    @Override
                    public NonNullList<ItemStack> extract(boolean simulate) {
                        if (!isValid())
                            return NonNullList.create();
                        return getHandlerProvider().get().extractAllIf(extractCallback,count,simulate);
                    }
                };
            }
        }

        private static class SimpleTileEntityHandlerProvider extends SimpleHandlerProvider {
            private World world;
            private BlockPos pos;
            private EnumFacing facing;

            public SimpleTileEntityHandlerProvider(World world, BlockPos pos, @Nullable EnumFacing facing) {
                this.world = world;
                this.pos = pos;
                this.facing = facing;
            }

            @Override
            public boolean isValid() {
                if (world == null || pos == null)
                    return false;
                if (!world.isBlockLoaded(pos) || world.getTileEntity(pos) == null) {
                    world = null;
                    pos = null;
                    return false;
                }
                return true;
            }

            @Override
            @Nullable
            public IItemHandler get() {
                if (world == null || pos == null || !world.isBlockLoaded(pos))
                    return null;
                TileEntity entity = world.getTileEntity(pos);
                if (entity == null)
                    return null;
                assert entity.hasCapability(ItemCapabilities.ITEM_HANDLER_CAPABILITY,facing);
                return entity.getCapability(ItemCapabilities.ITEM_HANDLER_CAPABILITY,facing);
            }
        }
        @Nullable
        public IHandlerProvider<? extends IItemHandler> createHandlerProvider(@Nonnull TileEntity entity, @Nullable EnumFacing facing) {
            IHandlerProviderFactory<?,?> factory = entity.getCapability(ItemCapabilities.HANDLER_PROVIDER_CAPABILITY,facing);
            if (factory!=null) {
                IHandlerProvider<?> provider = factory.newHandlerProvider();
                if (provider!=null)
                    return provider;
            }
            boolean hasItemHandler = entity.hasCapability(ItemCapabilities.ITEM_HANDLER_CAPABILITY,facing);
            if (!hasItemHandler)
                return null;
            BlockPos pos = entity.getPos();
            World access = entity.getWorld();
            return new SimpleTileEntityHandlerProvider(access,pos,facing);
        }
    }

    public static ItemStack extractFromSlot(ISlotHolder handler, ToIntFunction<ItemStack> extractFunction, int slot, boolean simulate) {
        ItemStack res = handler.getStack(slot);
        int extractAmount = MathHelper.clamp(extractFunction.applyAsInt(res),0,res.getCount());
        if (extractAmount>=res.getCount()) {
            if (simulate)
                return res.copy(); //cause the underlying stack isn't changed we need to return a copy

            handler.setStack(ItemStack.EMPTY, slot);
        }
        else {
            ItemStack inHandler = res.copy();
            res.setCount(extractAmount);
            if (simulate)
                return res.copy();

            inHandler.setCount(inHandler.getCount()-extractAmount);
            handler.setStack(inHandler, slot);
        }
        return res; //no need to return a copy, because a new instance was added at this stacks position
    }
}
