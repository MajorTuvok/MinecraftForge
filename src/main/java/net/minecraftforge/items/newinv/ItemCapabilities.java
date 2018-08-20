package net.minecraftforge.items.newinv;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;

public class ItemCapabilities {
    @CapabilityInject(IItemHandler.class)
    public static Capability<IItemHandler> ITEM_HANDLER_CAPABILITY = null;
    @CapabilityInject(ISlotHolder.class)
    public static Capability<ISlotHolder> SLOT_HOLDER_CAPABILITY = null;
    @CapabilityInject(IHandlerProviderFactory.class)
    public static Capability<IHandlerProviderFactory<?,?>> HANDLER_PROVIDER_CAPABILITY = null;

    public static void register() {
        CapabilityManager.INSTANCE.register(IItemHandler.class, new IStorage<IItemHandler>() {
            @Nullable
            @Override
            public NBTBase writeNBT(Capability<IItemHandler> capability, IItemHandler instance, EnumFacing side) {
                return instance.serializeNBT();
            }

            @Override
            public void readNBT(Capability<IItemHandler> capability, IItemHandler instance, EnumFacing side, NBTBase nbt) {
                instance.deserializeNBT(nbt);
            }
        }, () -> new ItemSlotHandler(1));

        CapabilityManager.INSTANCE.register(ISlotHolder.class, new IStorage<ISlotHolder>() {
            @Override
            public NBTBase writeNBT(Capability<ISlotHolder> capability, ISlotHolder instance, EnumFacing side) {
                NBTTagList list = new NBTTagList();
                int count = 0;
                for (ItemStack stack: instance) {
                    if (!stack.isEmpty())
                    {
                        NBTTagCompound itemTag = new NBTTagCompound();
                        itemTag.setInteger("Slot", count);
                        stack.writeToNBT(itemTag);
                        list.appendTag(itemTag);
                    }
                    count++;
                }
                return list;
            }

            @Override
            public void readNBT(Capability<ISlotHolder> capability, ISlotHolder instance, EnumFacing side, NBTBase nbt) {
                NBTTagList list = (NBTTagList) nbt;
                for (NBTBase nbtCompound: list) {
                    NBTTagCompound tagCompound = (NBTTagCompound) nbtCompound;
                    int slot = tagCompound.getInteger("Slot");
                    instance.setStack(new ItemStack(tagCompound),slot);
                }
            }
        },() -> new ItemSlotHandler(1));
        CapabilityManager.INSTANCE.register(IHandlerProviderFactory.class, new IStorage<IHandlerProviderFactory>() {
            @Nullable
            @Override
            public NBTBase writeNBT(Capability<IHandlerProviderFactory> capability, IHandlerProviderFactory instance, EnumFacing side) {
                return null;
            }

            @Override
            public void readNBT(Capability<IHandlerProviderFactory> capability, IHandlerProviderFactory instance, EnumFacing side, NBTBase nbt) {

            }
        }, () -> (IHandlerProviderFactory) () -> null);
    }
}
