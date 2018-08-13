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

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;

public class SkyLayer {
    public final ResourceLocation id;
    private Group group = null;
    private IRenderHandler renderer = null;
    private Runnable update;

    SkyLayer(ResourceLocation idIn, Runnable updateIn)
    {
        this.id = idIn;
        this.update = updateIn;
    }

    /**
     * Creates layer group associated with this layer. <p>
     * This will discard the associated renderer if it exists.
     * @return the resulted group
     * */
    public SkyLayer.Group makeGroup()
    {
        if(this.group == null)
        {
            this.group = new Group();
            if(this.renderer != null)
            {
                this.renderer = null;
                update.run();
            }
        }
        return this.group;
    }

    /**
     * Associates the renderer to a layer which doesn't have a group associated.
     * @throws IllegalStateException if the layer is associated with a layer group
     * */
    public void setRenderer(IRenderHandler rendererIn)
    {
        if(this.group != null)
            throw new IllegalStateException(
                    String.format("Layer %s is associated with a layer group, so it can't have a renderer",
                            this.id));
        update.run();
        this.renderer = rendererIn;
    }

    /**
     * @return gets layer group associated with this layer if it exists, <code>null</code> otherwise
     * */
    public @Nullable Group getGroup()
    {
        return this.group;
    }

    /**
     * @return gets the renderer if it exists, <code>null</code> otherwise
     * */
    public @Nullable IRenderHandler getRenderer()
    {
        return this.renderer;
    }

    public class Group
    {
        public final SkyLayer layer;

        Group()
        {
            this.layer = SkyLayer.this;
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if(o instanceof SkyLayer)
        {
            return this.id.equals(((SkyLayer)o).id);
        }
        else return false;
    }

    @Override
    public int hashCode()
    {
        return this.id.hashCode();
    }
}
