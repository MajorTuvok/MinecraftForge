/*
 * Minecraft Forge
 * Copyright (c) 2016-2018.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.ResourceLocation;

/**
 * Class which handles sky rendering. Sky render layers can be registered here.
 * TODO Proper sorting system
 * */
public class SkyRenderHandler
{
    private SkyLayer rootLayer;
    private final Map<ResourceLocation, SkyLayer> layerMap = new HashMap<>();
    private final MutableGraph<SkyLayer> layerGraph = GraphBuilder.directed().build();
    private final MutableGraph<SkyLayer> orderGraph = GraphBuilder.directed().build();

    private boolean enabled;
    private final ListMultimap<SkyLayer, SkyLayer> order = ArrayListMultimap.create();

    private SkyLayer register(ResourceLocation id)
    {
        SkyLayer layer = new SkyLayer(id);
        this.rootLayer = layer;
        layerMap.put(id, layer);
        layerGraph.addNode(layer);
        orderGraph.addNode(layer);
        return layer;
    }

    /**
     * Registers certain layer as part of certain layer group.
     * Replaces previous one with an empty layer if there was a layer.
     * @param layerGroup the layer group
     * @param id the layer id
     * @return the registered layer
     * @throws IllegalArgumentException if the specified layer group is invalid / not registered
     * */
    public SkyLayer register(SkyLayer.Group layerGroup, ResourceLocation id)
    {
        if(!layerMap.containsKey(layerGroup.layer.id))
            throw new IllegalArgumentException(String.format("Invalid layer %s", layerGroup.layer.id));

        if(this.rootLayer.id.equals(id))
            throw new IllegalArgumentException(String.format("Can't remove root layer with id %s", id));

        if(layerMap.containsKey(id))
        {
            SkyLayer layer = layerMap.get(id);
            for(SkyLayer subLayer : Graphs.reachableNodes(this.layerGraph, layer))
            {
                SkyLayer parent = layerGraph.predecessors(subLayer).stream().findFirst().get();
                layerMap.remove(subLayer.id);
                layerGraph.removeNode(subLayer);
                orderGraph.removeNode(subLayer);
                order.remove(parent, subLayer);
            }
        }

        SkyLayer layer = new SkyLayer(id);
        layerMap.put(id, layer);
        layerGraph.addNode(layer);
        layerGraph.putEdge(layerGroup.layer, layer);
        orderGraph.addNode(layer);
        order.put(layerGroup.layer, layer);
        return layer;
    }

    /**
     * Require ordering between two layers. <p>
     * Ordering from a parent layer to a child layer will be ignored.
     * @param prior the prior layer which comes before the other
     * @param posterior the posterior layer which comes after the other
     * */
    public void requireOrder(SkyLayer prior, SkyLayer posterior)
    {
        orderGraph.putEdge(prior, posterior);
    }

    /**
     * @return the root sky layer
     * */
    public SkyLayer getRootLayer()
    {
        return this.rootLayer;
    }

    /**
     * @return sky layer registered for specified id
     * */
    public SkyLayer getLayer(ResourceLocation id)
    {
        return layerMap.get(id);
    }

    /**
     * @return the sub-layers in the specified layer group
     * */
    public Set<SkyLayer> getSubLayers(SkyLayer.Group group)
    {
        return layerGraph.successors(group.layer);
    }

    @SuppressWarnings("unused")
    public SkyRenderHandler()
    {
        SkyLayer rootLayer = this.register(new ResourceLocation("sky_all"));

        SkyLayer.Group rootLayerGroup = rootLayer.makeGroup();
        SkyLayer skyBg = this.register(rootLayerGroup, new ResourceLocation("sky_background"));
        SkyLayer celestial = this.register(rootLayerGroup, new ResourceLocation("celestial"));
        SkyLayer skyFg = this.register(rootLayerGroup, new ResourceLocation("sky_foreground"));

        SkyLayer.Group celestialGroup = celestial.makeGroup();
        SkyLayer planetary = this.register(celestialGroup, new ResourceLocation("planetary"));
        SkyLayer stars = this.register(celestialGroup, new ResourceLocation("stars"));

        SkyLayer.Group planetaryGroup = planetary.makeGroup();
        SkyLayer sun = this.register(planetaryGroup, new ResourceLocation("sun"));
        SkyLayer moon = this.register(planetaryGroup, new ResourceLocation("moon"));
    }

    public void build()
    {
        // Search for actual pairs
        Set<SkyLayer> temp = Sets.newHashSet();
        for(EndpointPair<SkyLayer> pair : orderGraph.edges())
        {
            Optional<SkyLayer> current = Optional.of(pair.source());
            temp.clear();
            while(current.isPresent())
            {
                temp.add(current.get());
                current = layerGraph.predecessors(current.get()).stream().findFirst();
            }

            if(temp.contains(pair.target()))
                continue;

            current = Optional.of(pair.target());
            Optional<SkyLayer> parent = layerGraph.predecessors(pair.target()).stream().findFirst();
            while(parent.isPresent())
            {
                if(temp.contains(parent.get()))
                    break;
                current = parent;
                parent = layerGraph.predecessors(current.get()).stream().findFirst();
            }

            // TODO Put parent settings here
        }

        for(SkyLayer group : order.keySet())
        {
            List<SkyLayer> layer = order.get(group);
        }
    }

    public boolean render(float partialTicks, WorldClient world, Minecraft mc)
    {
        if(!this.enabled)
            return false;

        this.render(this.rootLayer, partialTicks, world, mc);
        return true;
    }

    private void render(SkyLayer layer, float partialTicks, WorldClient world, Minecraft mc)
    {
        if(layer.getGroup() != null)
        {
         //   for(SkyLayer subLayer : layer.asGroup().subLayers())
         //       this.render(subLayer, partialTicks, world, mc);
        }
        if(layer.getRenderer() != null)
            layer.getRenderer().render(partialTicks, world, mc);
    }

    /*private static class SkyBackRenderer extends IRenderHandler
    {
        @Override
        public void render(float partialTicks, WorldClient world, Minecraft mc)
        {
            
        }
    }*/
}
