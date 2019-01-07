package net.minecraftforge.common.tags;

import net.minecraft.advancements.FunctionManager;
import net.minecraft.block.Block;
import net.minecraft.command.FunctionObject;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.network.PacketBuffer;
import net.minecraft.resources.IResourceManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.NetworkTagManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static net.minecraftforge.registries.ForgeRegistries.*;

public final class ForgeTagManager extends NetworkTagManager
{

    public static final ResourceLocation FLUID_TAGS = new ResourceLocation("fluids");
    public static final ResourceLocation FUNCTION_TAGS = new ResourceLocation("functions");
    private static final Logger LOGGER = LogManager.getLogger();

    private static final ForgeTagManager INSTANCE = new ForgeTagManager();
    public static ForgeTagManager getInstance()
    {
        return INSTANCE;
    }

    private static void registerVanillaTags()
    {
        getInstance().overrideTagCollection(GameData.BLOCKS, ForgeTagCollection.fromForgeRegistry(BLOCKS,ForgeTagCollection.getDefaultLoadingLocation(GameData.BLOCKS),"block"),
                (manager -> BlockTags.setCollection(manager.getBlocks())), true, true, true);
        getInstance().overrideTagCollection(GameData.ITEMS, ForgeTagCollection.fromForgeRegistry(ITEMS,ForgeTagCollection.getDefaultLoadingLocation(GameData.ITEMS),"item"),
                (manager -> ItemTags.setCollection(manager.getItems())), true, true, true);
        getInstance().overrideTagCollection(FLUID_TAGS, new ForgeTagCollection<>(Fluid.REGISTRY,ForgeTagCollection.getDefaultLoadingLocation(FLUID_TAGS),"fluid"),
                (manager -> ItemTags.setCollection(manager.getItems())), true, true, true);
    }

    private static void registerForgeTags(final ResourceLocation id, final IForgeRegistry<?> reg, final String name)
    {
        getInstance().registerTagCollection(id, ForgeTagCollection.fromForgeRegistry(reg, ForgeTagCollection.getDefaultLoadingLocation(id), name));
    }

    public static void initForgeTags()
    {
        registerVanillaTags();
        registerForgeTags(GameData.POTIONS, POTIONS, "potion");
        registerForgeTags(GameData.BIOMES, BIOMES, "biome");
        registerForgeTags(GameData.SOUNDEVENTS, SOUND_EVENTS,  "sound event");
        registerForgeTags(GameData.POTIONTYPES, POTION_TYPES, "potion type");
        registerForgeTags(GameData.ENCHANTMENTS, ENCHANTMENTS,  "enchantment");
        registerForgeTags(GameData.PROFESSIONS, VILLAGER_PROFESSIONS,  "villager profession");
        registerForgeTags(GameData.ENTITIES, ENTITIES, "entity");
        registerForgeTags(GameData.TILEENTITIES, TILE_ENTITIES, "tile entity");
    }

    public static ForgeTagCollection<FunctionObject> createFunctionTags(FunctionManager manager)
    {
        return getInstance().overrideTagCollection(FUNCTION_TAGS,
                new ForgeTagCollection<>((p_200107_1_) -> manager.getFunction(p_200107_1_) != null, manager::getFunction, null,null, ForgeTagCollection.getDefaultLoadingLocation(ForgeTagManager.FUNCTION_TAGS), true, "function"),
                null, false, false,true);
    }

    private final Map<ResourceLocation, TagCollectionValueEntry> tagCollections;
    private int generation;

    public int getGeneration()
    {
        return generation;
    }

    public <T> ForgeTagCollection<T> registerTagCollection(ResourceLocation id, ForgeTagCollection<T> tagCollection)
    {
        return registerTagCollection(id, tagCollection, null);
    }

    public <T> ForgeTagCollection<T> registerTagCollection(ResourceLocation id, ForgeTagCollection<T> tagCollection, @Nullable Consumer<ForgeTagManager> reloadCallback)
    {
        return registerTagCollection(id, tagCollection, reloadCallback, true);
    }

    public <T> ForgeTagCollection<T> registerTagCollection(ResourceLocation id, @Nonnull ForgeTagCollection<T> tagCollection, @Nullable Consumer<ForgeTagManager> reloadCallback, boolean performSync)
    {
        return registerTagCollection(id,tagCollection,reloadCallback,performSync,true);
    }

    public <T> ForgeTagCollection<T> registerTagCollection(ResourceLocation id, @Nonnull ForgeTagCollection<T> tagCollection, @Nullable Consumer<ForgeTagManager> reloadCallback, boolean performSync, boolean performReload)
    {
        if (tagCollections.containsKey(id))
            throw new IllegalArgumentException("Cannot register " + id + " TagCollection twice!");
        tagCollections.put(id, new TagCollectionValueEntry(tagCollection, reloadCallback, performSync, performReload,false));
        return tagCollection;
    }

    /*
    public <T> ForgeTagCollection<T> overrideTagCollection(ResourceLocation id, @Nonnull ForgeTagCollection<T> tagCollection, @Nullable Consumer<ForgeTagManager> reloadCallback, boolean performSync, boolean performReload)
    {
        overrideTagCollection(id,tagCollection,reloadCallback,performSync,performReload,false);
        return tagCollection;
    }*/

    private <T> ForgeTagCollection<T> overrideTagCollection(ResourceLocation id, @Nonnull ForgeTagCollection<T> tagCollection, @Nullable Consumer<ForgeTagManager> reloadCallback, boolean performSync, boolean performReload, boolean vanilla)
    {
        tagCollections.put(id, new TagCollectionValueEntry(tagCollection, reloadCallback, performSync, performReload,vanilla));
        return tagCollection;
    }

    private ForgeTagManager()
    {
        super();
        tagCollections = new HashMap<>();
        generation = 0;
    }


    public ForgeTagCollection<?> getTagsForId(ResourceLocation id)
    {
        return tagCollections.get(id).getTags();
    }

    @SuppressWarnings("unchecked")
    public <T> ForgeTagCollection<T> getTagsForIdUnchecked(ResourceLocation id)
    {
        return (ForgeTagCollection<T>) tagCollections.get(id).getTags();
    }

    @Override
    public ForgeTagCollection<Block> getBlocks()
    {
        return getTagsForIdUnchecked(GameData.BLOCKS);
    }

    @Override
    public ForgeTagCollection<Item> getItems()
    {
        return getTagsForIdUnchecked(GameData.ITEMS);
    }

    @Override
    public ForgeTagCollection<Fluid> getFluids()
    {
        return getTagsForIdUnchecked(FLUID_TAGS);
    }

    @Override
    public void clear()
    {
        for (Map.Entry<ResourceLocation, TagCollectionValueEntry> entry : tagCollections.entrySet())
            entry.getValue().clear();
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager)
    {
        for (Map.Entry<ResourceLocation, TagCollectionValueEntry> entry : tagCollections.entrySet())
        {
                entry.getValue().reload(resourceManager, this);
        }
        generation++;
    }

    @Override
    public void write(PacketBuffer buffer)
    {
        this.getBlocks().write(buffer);
        this.getItems().write(buffer);
        this.getFluids().write(buffer);
        //we might wind up sending more data than vanilla needs (and therefore produce excessive Network traffic...)
        //writeForgeTags(buffer);
    }

    private void writeForgeTags(final PacketBuffer buffer)
    {
        tagCollections.entrySet().stream().filter(e -> e.getValue().shouldSync() && !e.getValue().isVanilla()).forEach(e -> {
            buffer.writeResourceLocation(e.getKey());
            e.getValue().write(buffer);
        });
    }

    public void readTags(PacketBuffer buffer)
    {
        this.getBlocks().read(buffer);
        this.getItems().read(buffer);
        this.getFluids().read(buffer);
        //readForgeTags(buffer);
        generation++;
    }

    private void readForgeTags(PacketBuffer buffer)
    {
        while (buffer.isReadable())
        {
            ResourceLocation id = buffer.readResourceLocation();
            //we cannot make any assumptions about the Format an unknown collection might have written into the buffer (custom implementations are allowed...)-> we have to Fail
            //modders who see this: if you want your tags not to be synced: just set the boolean to false!
            ForgeTagCollection<?> tags = Objects.requireNonNull(getTagsForId(id), "Tag List mismatch! Received " + id + " from Server, but it was not registered Client-Side!");
            tags.read(buffer);
        }
    }

    public void onRemoteClientReceivedTags()
    {
        for (Map.Entry<ResourceLocation, TagCollectionValueEntry> entry : tagCollections.entrySet())
        {
            entry.getValue().onReloaded(this);
        }
    }

    private static final class TagCollectionValueEntry
    {
        private final ForgeTagCollection<?> collection;
        private final boolean performSync;
        private final boolean performReload;
        @Nullable
        private final Consumer<ForgeTagManager> reloadCallback;
        private final boolean isVanillaCollection;

        TagCollectionValueEntry(@Nonnull ForgeTagCollection<?> collection, @Nullable Consumer<ForgeTagManager> reloadCallback, boolean performSync, boolean performReload, boolean vanillaCollection)
        {
            this.collection = collection;
            this.reloadCallback = reloadCallback;
            this.performSync = performSync;
            this.performReload = performReload;
            this.isVanillaCollection = vanillaCollection;
        }


        boolean shouldSync()
        {
            return performSync;
        }


        void clear()
        {
            collection.clear();
        }


        ForgeTagCollection<?> getTags()
        {
            return collection;
        }


        void read(PacketBuffer buffer)
        {
            collection.read(buffer);
        }


        void write(PacketBuffer buffer)
        {
            collection.write(buffer);
        }

        void reload(IResourceManager resManager, ForgeTagManager tagManager)
        {
            if (performReload)
            {
                clear();
                collection.reload(resManager);
                onReloaded(tagManager);
            }
        }

        void onReloaded(ForgeTagManager tagManager)
        {
            if (reloadCallback != null) reloadCallback.accept(tagManager);
        }

        boolean isVanilla()
        {
            return isVanillaCollection;
        }
    }
}
