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

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.fml.common.toposort.TopologicalSort;
import net.minecraftforge.fml.common.toposort.TopologicalSort.DirectedGraph;

import java.util.*;

/**
 * Class which handles sky rendering. Sky render layers can be registered here.
 * */
public class SkyRenderHandler
{
    public static final ResourceLocation SKY_LAYER_START = new ResourceLocation("sky_all");
    public static final ResourceLocation SKY_LAYER_END = new ResourceLocation("sky_end");
    public static final ResourceLocation SKY_LAYER_FOREGROUND = new ResourceLocation("sky_foreground");
    public static final ResourceLocation SKY_LAYER_BACKGROUND = new ResourceLocation("sky_background");
    public static final ResourceLocation SKY_LAYER_CELESTIAL = new ResourceLocation("celestial");
    public static final ResourceLocation SKY_LAYER_PLANETARY = new ResourceLocation("planetary");
    public static final ResourceLocation SKY_LAYER_STARS = new ResourceLocation("stars");
    public static final ResourceLocation SKY_LAYER_SUN = new ResourceLocation("sun");
    public static final ResourceLocation SKY_LAYER_MOON = new ResourceLocation("moon");

    private boolean unfinished;
    private boolean enabled;
    private Runnable update = () -> { this.unfinished = this.enabled = true; };

    private Map<ResourceLocation,SkyLayer> mLayerLookup;
    private List<SkyLayer> mAllLayers;
    private List<SkyLayer> mSortedLayers;

    /**
     * Registers a new SkyLayer with the given id. If one already existed for the given id, it will be replaced.
     * @param id the layer id
     * @return The newly created and registered SkyLayer
     */
    public SkyLayer register(@NotNull ResourceLocation id)
    {
        update.run();
        SkyLayer layer = new SkyLayer(Objects.requireNonNull(id), this.update);
        mAllLayers.remove(layer); //remove existing layers
        mAllLayers.add(layer);
        mLayerLookup.put(id,layer);
        return layer;
    }

    /**
     * Removes the layer with certain layer id.
     * @param id the layer id
     * @return <code>true</code> if corresponding layer existed and removed, <code>false</code> otherwise
     * */
    public boolean remove(ResourceLocation id)
    {
        if (!hasSkyLayer(id))
            return false;
        update.run();
        return mAllLayers.remove(mLayerLookup.remove(id));
    }

    public SkyRenderHandler(WorldProvider provider)
    {

        SkyLayer root = register(SKY_LAYER_START);
        SkyLayer end = register(SKY_LAYER_END);
        SkyLayer skyBg = this.register(SKY_LAYER_BACKGROUND);
        SkyLayer celestial = this.register(SKY_LAYER_CELESTIAL);
        SkyLayer skyFg = this.register(SKY_LAYER_FOREGROUND);

        celestial.addDependency(SKY_LAYER_BACKGROUND);
        celestial.addDependant(SKY_LAYER_FOREGROUND);
        skyBg.setRenderer(new SkyBackRenderer());
        skyFg.setRenderer(new SkyFrontRenderer());


        if(provider.isSurfaceWorld())
        {
            SkyLayer planetary = this.register(SKY_LAYER_PLANETARY);
            planetary.addDependency(SKY_LAYER_CELESTIAL);
            planetary.addDependant(SKY_LAYER_FOREGROUND);
            SkyLayer stars = this.register(SKY_LAYER_STARS);

            stars.setRenderer(new StarRenderer());
            stars.addDependency(SKY_LAYER_CELESTIAL);
            stars.addDependant(SKY_LAYER_PLANETARY); //stars are before 'planets' but after celestial objects start

            SkyLayer sun = this.register(SKY_LAYER_SUN);
            SkyLayer moon = this.register(SKY_LAYER_MOON);

            //sun and moon are independent of each other
            sun.setRenderer(new SunRenderer());
            sun.addDependency(SKY_LAYER_PLANETARY);
            sun.addDependant(SKY_LAYER_FOREGROUND);//wants to be rendered before the foreground
            moon.setRenderer(new MoonRenderer());
            moon.addDependency(SKY_LAYER_PLANETARY);
            moon.addDependant(SKY_LAYER_FOREGROUND);//wants to be rendered before the foreground
        }

        this.enabled = false;
    }

    public boolean hasSkyLayer(ResourceLocation id)
    {
        return mLayerLookup.containsKey(id);
    }

    @Nullable
    public SkyLayer getLayer(ResourceLocation id)
    {
        return mLayerLookup.get(id);
    }

    public void build()
    {
        // Search for actual pairs

        DirectedGraph<SkyLayer> graph = new DirectedGraph<>();
        SkyLayer root = getLayer(SKY_LAYER_START);
        SkyLayer end = getLayer(SKY_LAYER_END);
        graph.addNode(root);
        graph.addEdge(root,end);

        setupNodes(graph);

        setupEdges(graph,root,end);

        mSortedLayers = TopologicalSort.topologicalSort(graph);
        mSortedLayers.removeIf(skyLayer -> skyLayer.getRenderer() == null); //removes not required layers
        // Finished
        this.unfinished = false;
    }

    private void setupNodes(DirectedGraph<SkyLayer> graph)
    {
        for (SkyLayer layer:mAllLayers)
        {
            graph.addNode(layer);
        }
    }

    private void setupEdges(DirectedGraph<SkyLayer> graph, SkyLayer root, SkyLayer end)
    {
        for (SkyLayer layer:mAllLayers)
        {
            Set<ResourceLocation> dependencies = layer.getDependencies();
            Set<ResourceLocation> dependants = layer.getDependendants();
            graph.addEdge(root,layer);
            graph.addEdge(layer,end);

            for (ResourceLocation location: dependencies)
            {
                if (!location.equals(SKY_LAYER_END) && hasSkyLayer(location))
                {
                    SkyLayer depLayer = getLayer(location);
                    if (depLayer!=null) //should always be true
                        graph.addEdge(depLayer,layer);
                }
            }

            for (ResourceLocation location: dependants)
            {
                if (!location.equals(SKY_LAYER_START) && hasSkyLayer(location))
                {
                    SkyLayer depLayer = getLayer(location);
                    if (depLayer!=null) //should always be true
                        graph.addEdge(layer,depLayer);
                }
            }
        }
    }

    public boolean render(float partialTicks, WorldClient world, Minecraft mc)
    {
        if(!this.enabled)
            return false;
        if(this.unfinished)
            this.build();

        for (SkyLayer layer: mSortedLayers)
        {
            IRenderHandler renderHandler = layer.getRenderer();
            if (renderHandler!=null)
                renderHandler.render(partialTicks,world,mc);
        }

        return true;
    }


    private static class SkyBackRenderer extends IRenderHandler
    {
        private final VertexFormat vertexBufferFormat;
        private static final ResourceLocation END_SKY_TEXTURES = new ResourceLocation("textures/environment/end_sky.png");
        private boolean vboEnabled;
        private VertexBuffer skyVBO;
        private int glSkyList;

        private SkyBackRenderer()
        {
            this.vertexBufferFormat = new VertexFormat();
            this.vertexBufferFormat.addElement(new VertexFormatElement(0, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.POSITION, 3));
            this.vboEnabled = OpenGlHelper.useVbo();
            this.generateSky();
        }

        private void generateSky()
        {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();

            if (this.skyVBO != null)
            {
                this.skyVBO.deleteGlBuffers();
            }

            if (this.glSkyList >= 0)
            {
                GLAllocation.deleteDisplayLists(this.glSkyList);
                this.glSkyList = -1;
            }

            if (this.vboEnabled)
            {
                this.skyVBO = new VertexBuffer(this.vertexBufferFormat);
                renderSky(bufferbuilder, 16.0F, false);
                bufferbuilder.finishDrawing();
                bufferbuilder.reset();
                this.skyVBO.bufferData(bufferbuilder.getByteBuffer());
            }
            else
            {
                this.glSkyList = GLAllocation.generateDisplayLists(1);
                GlStateManager.glNewList(this.glSkyList, 4864);
                renderSky(bufferbuilder, 16.0F, false);
                tessellator.draw();
                GlStateManager.glEndList();
            }
        }

        @Override
        public void render(float partialTicks, WorldClient world, Minecraft mc)
        {
            if(this.vboEnabled != OpenGlHelper.useVbo())
            {
                this.vboEnabled = OpenGlHelper.useVbo();
                this.generateSky();
            }

            if (world.provider.getDimensionType().getId() == 1)
            {
                GlStateManager.disableFog();
                GlStateManager.disableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.depthMask(false);
                mc.renderEngine.bindTexture(END_SKY_TEXTURES);
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder bufferbuilder = tessellator.getBuffer();

                for (int k1 = 0; k1 < 6; ++k1)
                {
                    GlStateManager.pushMatrix();

                    if (k1 == 1)
                    {
                        GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                    }

                    if (k1 == 2)
                    {
                        GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                    }

                    if (k1 == 3)
                    {
                        GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
                    }

                    if (k1 == 4)
                    {
                        GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                    }

                    if (k1 == 5)
                    {
                        GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
                    }

                    bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                    bufferbuilder.pos(-100.0D, -100.0D, -100.0D).tex(0.0D, 0.0D).color(40, 40, 40, 255).endVertex();
                    bufferbuilder.pos(-100.0D, -100.0D, 100.0D).tex(0.0D, 16.0D).color(40, 40, 40, 255).endVertex();
                    bufferbuilder.pos(100.0D, -100.0D, 100.0D).tex(16.0D, 16.0D).color(40, 40, 40, 255).endVertex();
                    bufferbuilder.pos(100.0D, -100.0D, -100.0D).tex(16.0D, 0.0D).color(40, 40, 40, 255).endVertex();
                    tessellator.draw();
                    GlStateManager.popMatrix();
                }

                GlStateManager.depthMask(true);
                GlStateManager.enableTexture2D(); // Useless, but kept here
                GlStateManager.enableAlpha();
            }
            else if (world.provider.isSurfaceWorld())
            {
                GlStateManager.disableTexture2D();
                Vec3d vec3d = world.getSkyColor(mc.getRenderViewEntity(), partialTicks);
                float f = (float)vec3d.x;
                float f1 = (float)vec3d.y;
                float f2 = (float)vec3d.z;

                if (mc.gameSettings.anaglyph)
                {
                    float f3 = (f * 30.0F + f1 * 59.0F + f2 * 11.0F) / 100.0F;
                    float f4 = (f * 30.0F + f1 * 70.0F) / 100.0F;
                    float f5 = (f * 30.0F + f2 * 70.0F) / 100.0F;
                    f = f3;
                    f1 = f4;
                    f2 = f5;
                }

                GlStateManager.color(f, f1, f2);
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder bufferbuilder = tessellator.getBuffer();
                GlStateManager.depthMask(false);
                GlStateManager.enableFog();
                GlStateManager.color(f, f1, f2);

                if (this.vboEnabled)
                {
                    this.skyVBO.bindBuffer();
                    GlStateManager.glEnableClientState(32884);
                    GlStateManager.glVertexPointer(3, 5126, 12, 0);
                    this.skyVBO.drawArrays(7);
                    this.skyVBO.unbindBuffer();
                    GlStateManager.glDisableClientState(32884);
                }
                else
                {
                    GlStateManager.callList(this.glSkyList);
                }

                GlStateManager.disableFog();
                GlStateManager.disableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                RenderHelper.disableStandardItemLighting();
                float[] afloat = world.provider.calcSunriseSunsetColors(world.getCelestialAngle(partialTicks), partialTicks);

                if (afloat != null)
                {
                    GlStateManager.disableTexture2D();
                    GlStateManager.shadeModel(7425);
                    GlStateManager.pushMatrix();
                    GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                    GlStateManager.rotate(MathHelper.sin(world.getCelestialAngleRadians(partialTicks)) < 0.0F ? 180.0F : 0.0F, 0.0F, 0.0F, 1.0F);
                    GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                    float f6 = afloat[0];
                    float f7 = afloat[1];
                    float f8 = afloat[2];

                    if (mc.gameSettings.anaglyph)
                    {
                        float f9 = (f6 * 30.0F + f7 * 59.0F + f8 * 11.0F) / 100.0F;
                        float f10 = (f6 * 30.0F + f7 * 70.0F) / 100.0F;
                        float f11 = (f6 * 30.0F + f8 * 70.0F) / 100.0F;
                        f6 = f9;
                        f7 = f10;
                        f8 = f11;
                    }

                    bufferbuilder.begin(6, DefaultVertexFormats.POSITION_COLOR);
                    bufferbuilder.pos(0.0D, 100.0D, 0.0D).color(f6, f7, f8, afloat[3]).endVertex();
                    int l1 = 16;

                    for (int j2 = 0; j2 <= 16; ++j2)
                    {
                        float f21 = (float)j2 * ((float)Math.PI * 2F) / 16.0F;
                        float f12 = MathHelper.sin(f21);
                        float f13 = MathHelper.cos(f21);
                        bufferbuilder.pos((double)(f12 * 120.0F), (double)(f13 * 120.0F), (double)(-f13 * 40.0F * afloat[3])).color(afloat[0], afloat[1], afloat[2], 0.0F).endVertex();
                    }

                    tessellator.draw();
                    GlStateManager.popMatrix();
                    GlStateManager.shadeModel(7424);
                }

                GlStateManager.enableTexture2D();
            }
        }
    }

    private static class SunRenderer extends IRenderHandler
    {
        private static final ResourceLocation SUN_TEXTURES = new ResourceLocation("textures/environment/sun.png");

        @Override
        public void render(float partialTicks, WorldClient world, Minecraft mc)
        {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.pushMatrix();
            float f16 = 1.0F - world.getRainStrength(partialTicks);
            GlStateManager.color(1.0F, 1.0F, 1.0F, f16);
            GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(world.getCelestialAngle(partialTicks) * 360.0F, 1.0F, 0.0F, 0.0F);
            float f17 = 30.0F;
            mc.renderEngine.bindTexture(SUN_TEXTURES);
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
            bufferbuilder.pos((double)(-f17), 100.0D, (double)(-f17)).tex(0.0D, 0.0D).endVertex();
            bufferbuilder.pos((double)f17, 100.0D, (double)(-f17)).tex(1.0D, 0.0D).endVertex();
            bufferbuilder.pos((double)f17, 100.0D, (double)f17).tex(1.0D, 1.0D).endVertex();
            bufferbuilder.pos((double)(-f17), 100.0D, (double)f17).tex(0.0D, 1.0D).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
        }
    }

    private static class MoonRenderer extends IRenderHandler
    {
        private static final ResourceLocation MOON_PHASES_TEXTURES = new ResourceLocation("textures/environment/moon_phases.png");

        @Override
        public void render(float partialTicks, WorldClient world, Minecraft mc)
        {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.pushMatrix();
            float f16 = 1.0F - world.getRainStrength(partialTicks);
            GlStateManager.color(1.0F, 1.0F, 1.0F, f16);
            GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(world.getCelestialAngle(partialTicks) * 360.0F, 1.0F, 0.0F, 0.0F);
            float f17 = 20.0F;
            mc.renderEngine.bindTexture(MOON_PHASES_TEXTURES);
            int k1 = world.getMoonPhase();
            int i2 = k1 % 4;
            int k2 = k1 / 4 % 2;
            float f22 = (float)(i2 + 0) / 4.0F;
            float f23 = (float)(k2 + 0) / 2.0F;
            float f24 = (float)(i2 + 1) / 4.0F;
            float f14 = (float)(k2 + 1) / 2.0F;
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
            bufferbuilder.pos((double)(-f17), -100.0D, (double)f17).tex((double)f24, (double)f14).endVertex();
            bufferbuilder.pos((double)f17, -100.0D, (double)f17).tex((double)f22, (double)f14).endVertex();
            bufferbuilder.pos((double)f17, -100.0D, (double)(-f17)).tex((double)f22, (double)f23).endVertex();
            bufferbuilder.pos((double)(-f17), -100.0D, (double)(-f17)).tex((double)f24, (double)f23).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
        }
    }

    private static class StarRenderer extends IRenderHandler
    {
        private final VertexFormat vertexBufferFormat;
        private boolean vboEnabled;
        private VertexBuffer starVBO;
        private int starGLCallList;

        private StarRenderer()
        {
            this.vertexBufferFormat = new VertexFormat();
            this.vertexBufferFormat.addElement(new VertexFormatElement(0, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.POSITION, 3));
            this.vboEnabled = OpenGlHelper.useVbo();
            this.generateStars();
        }

        private void generateStars()
        {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();

            if (this.starVBO != null)
            {
                this.starVBO.deleteGlBuffers();
            }

            if (this.starGLCallList >= 0)
            {
                GLAllocation.deleteDisplayLists(this.starGLCallList);
                this.starGLCallList = -1;
            }

            if (this.vboEnabled)
            {
                this.starVBO = new VertexBuffer(this.vertexBufferFormat);
                this.renderStars(bufferbuilder);
                bufferbuilder.finishDrawing();
                bufferbuilder.reset();
                this.starVBO.bufferData(bufferbuilder.getByteBuffer());
            }
            else
            {
                this.starGLCallList = GLAllocation.generateDisplayLists(1);
                GlStateManager.pushMatrix();
                GlStateManager.glNewList(this.starGLCallList, 4864);
                this.renderStars(bufferbuilder);
                tessellator.draw();
                GlStateManager.glEndList();
                GlStateManager.popMatrix();
            }
        }

        private void renderStars(BufferBuilder bufferBuilderIn)
        {
            Random random = new Random(10842L);
            bufferBuilderIn.begin(7, DefaultVertexFormats.POSITION);

            for (int i = 0; i < 1500; ++i)
            {
                double d0 = (double)(random.nextFloat() * 2.0F - 1.0F);
                double d1 = (double)(random.nextFloat() * 2.0F - 1.0F);
                double d2 = (double)(random.nextFloat() * 2.0F - 1.0F);
                double d3 = (double)(0.15F + random.nextFloat() * 0.1F);
                double d4 = d0 * d0 + d1 * d1 + d2 * d2;

                if (d4 < 1.0D && d4 > 0.01D)
                {
                    d4 = 1.0D / Math.sqrt(d4);
                    d0 = d0 * d4;
                    d1 = d1 * d4;
                    d2 = d2 * d4;
                    double d5 = d0 * 100.0D;
                    double d6 = d1 * 100.0D;
                    double d7 = d2 * 100.0D;
                    double d8 = Math.atan2(d0, d2);
                    double d9 = Math.sin(d8);
                    double d10 = Math.cos(d8);
                    double d11 = Math.atan2(Math.sqrt(d0 * d0 + d2 * d2), d1);
                    double d12 = Math.sin(d11);
                    double d13 = Math.cos(d11);
                    double d14 = random.nextDouble() * Math.PI * 2.0D;
                    double d15 = Math.sin(d14);
                    double d16 = Math.cos(d14);

                    for (int j = 0; j < 4; ++j)
                    {
                        double d17 = 0.0D;
                        double d18 = (double)((j & 2) - 1) * d3;
                        double d19 = (double)((j + 1 & 2) - 1) * d3;
                        double d20 = 0.0D;
                        double d21 = d18 * d16 - d19 * d15;
                        double d22 = d19 * d16 + d18 * d15;
                        double d23 = d21 * d12 + 0.0D * d13;
                        double d24 = 0.0D * d12 - d21 * d13;
                        double d25 = d24 * d9 - d22 * d10;
                        double d26 = d22 * d9 + d24 * d10;
                        bufferBuilderIn.pos(d5 + d25, d6 + d23, d7 + d26).endVertex();
                    }
                }
            }
        }

        @Override
        public void render(float partialTicks, WorldClient world, Minecraft mc)
        {
            if(this.vboEnabled != OpenGlHelper.useVbo())
            {
                this.vboEnabled = OpenGlHelper.useVbo();
                this.generateStars();
            }

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            GlStateManager.pushMatrix();
            float f16 = 1.0F - world.getRainStrength(partialTicks);
            GlStateManager.color(1.0F, 1.0F, 1.0F, f16);
            GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(world.getCelestialAngle(partialTicks) * 360.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.disableTexture2D();
            float f15 = world.getStarBrightness(partialTicks) * f16;

            if (f15 > 0.0F)
            {
                GlStateManager.color(f15, f15, f15, f15);

                if (this.vboEnabled)
                {
                    this.starVBO.bindBuffer();
                    GlStateManager.glEnableClientState(32884);
                    GlStateManager.glVertexPointer(3, 5126, 12, 0);
                    this.starVBO.drawArrays(7);
                    this.starVBO.unbindBuffer();
                    GlStateManager.glDisableClientState(32884);
                }
                else
                {
                    GlStateManager.callList(this.starGLCallList);
                }
            }
            GlStateManager.popMatrix();
            GlStateManager.enableTexture2D();
        }
    }

    private static class SkyFrontRenderer extends IRenderHandler
    {
        private final VertexFormat vertexBufferFormat;
        private boolean vboEnabled;
        private VertexBuffer sky2VBO;
        private int glSkyList2;

        private SkyFrontRenderer()
        {
            this.vertexBufferFormat = new VertexFormat();
            this.vertexBufferFormat.addElement(new VertexFormatElement(0, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.POSITION, 3));
            this.generateSky2();
            this.vboEnabled = OpenGlHelper.useVbo();
        }

        private void generateSky2()
        {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();

            if (this.sky2VBO != null)
            {
                this.sky2VBO.deleteGlBuffers();
            }

            if (this.glSkyList2 >= 0)
            {
                GLAllocation.deleteDisplayLists(this.glSkyList2);
                this.glSkyList2 = -1;
            }

            if (this.vboEnabled)
            {
                this.sky2VBO = new VertexBuffer(this.vertexBufferFormat);
                renderSky(bufferbuilder, -16.0F, true);
                bufferbuilder.finishDrawing();
                bufferbuilder.reset();
                this.sky2VBO.bufferData(bufferbuilder.getByteBuffer());
            }
            else
            {
                this.glSkyList2 = GLAllocation.generateDisplayLists(1);
                GlStateManager.glNewList(this.glSkyList2, 4864);
                renderSky(bufferbuilder, -16.0F, true);
                tessellator.draw();
                GlStateManager.glEndList();
            }
        }

        @Override
        public void render(float partialTicks, WorldClient world, Minecraft mc)
        {
            if(this.vboEnabled != OpenGlHelper.useVbo())
            {
                this.vboEnabled = OpenGlHelper.useVbo();
                this.generateSky2();
            }

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            Vec3d vec3d = world.getSkyColor(mc.getRenderViewEntity(), partialTicks);
            float f = (float)vec3d.x;
            float f1 = (float)vec3d.y;
            float f2 = (float)vec3d.z;

            if (mc.gameSettings.anaglyph)
            {
                float f3 = (f * 30.0F + f1 * 59.0F + f2 * 11.0F) / 100.0F;
                float f4 = (f * 30.0F + f1 * 70.0F) / 100.0F;
                float f5 = (f * 30.0F + f2 * 70.0F) / 100.0F;
                f = f3;
                f1 = f4;
                f2 = f5;
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.enableFog();
            GlStateManager.disableTexture2D();
            GlStateManager.color(0.0F, 0.0F, 0.0F);
            double d3 = mc.player.getPositionEyes(partialTicks).y - world.getHorizon();

            if (d3 < 0.0D)
            {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0.0F, 12.0F, 0.0F);

                if (this.vboEnabled)
                {
                    this.sky2VBO.bindBuffer();
                    GlStateManager.glEnableClientState(32884);
                    GlStateManager.glVertexPointer(3, 5126, 12, 0);
                    this.sky2VBO.drawArrays(7);
                    this.sky2VBO.unbindBuffer();
                    GlStateManager.glDisableClientState(32884);
                }
                else
                {
                    GlStateManager.callList(this.glSkyList2);
                }

                GlStateManager.popMatrix();
                float f18 = 1.0F;
                float f19 = -((float)(d3 + 65.0D));
                float f20 = -1.0F;
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
                bufferbuilder.pos(-1.0D, (double)f19, 1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(1.0D, (double)f19, 1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(1.0D, -1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(-1.0D, -1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(-1.0D, -1.0D, -1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(1.0D, -1.0D, -1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(1.0D, (double)f19, -1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(-1.0D, (double)f19, -1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(1.0D, -1.0D, -1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(1.0D, -1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(1.0D, (double)f19, 1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(1.0D, (double)f19, -1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(-1.0D, (double)f19, -1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(-1.0D, (double)f19, 1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(-1.0D, -1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(-1.0D, -1.0D, -1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(-1.0D, -1.0D, -1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(-1.0D, -1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(1.0D, -1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
                bufferbuilder.pos(1.0D, -1.0D, -1.0D).color(0, 0, 0, 255).endVertex();
                tessellator.draw();
            }

            if (world.provider.isSkyColored())
            {
                GlStateManager.color(f * 0.2F + 0.04F, f1 * 0.2F + 0.04F, f2 * 0.6F + 0.1F);
            }
            else
            {
                GlStateManager.color(f, f1, f2);
            }

            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, -((float)(d3 - 16.0D)), 0.0F);
            GlStateManager.callList(this.glSkyList2);
            GlStateManager.popMatrix();
            GlStateManager.enableTexture2D();
            GlStateManager.depthMask(true);
        }
    }

    private static void renderSky(BufferBuilder bufferBuilderIn, float posY, boolean reverseX)
    {
        bufferBuilderIn.begin(7, DefaultVertexFormats.POSITION);

        for (int k = -384; k <= 384; k += 64)
        {
            for (int l = -384; l <= 384; l += 64)
            {
                float f = (float)k;
                float f1 = (float)(k + 64);

                if (reverseX)
                {
                    f1 = (float)k;
                    f = (float)(k + 64);
                }

                bufferBuilderIn.pos((double)f, (double)posY, (double)l).endVertex();
                bufferBuilderIn.pos((double)f1, (double)posY, (double)l).endVertex();
                bufferBuilderIn.pos((double)f1, (double)posY, (double)(l + 64)).endVertex();
                bufferBuilderIn.pos((double)f, (double)posY, (double)(l + 64)).endVertex();
            }
        }
    }
}
