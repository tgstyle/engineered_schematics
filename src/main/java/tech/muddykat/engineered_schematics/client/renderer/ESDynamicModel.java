package tech.muddykat.engineered_schematics.client.renderer;

import tech.muddykat.engineered_schematics.EngineeredSchematics;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SideOnly(Side.CLIENT)
@EventBusSubscriber(value = Side.CLIENT, modid = EngineeredSchematics.MODID)
public class ESDynamicModel {
    private static final List<ESDynamicModel> MODELS = new ArrayList<>();
    private final ResourceLocation location;
    private IModel model;
    private IBakedModel baked;

    public ESDynamicModel(String name) {
        this.location = new ResourceLocation(EngineeredSchematics.MODID, "dynamic/" + name);
        MODELS.add(this);
    }

    @SubscribeEvent public static void stitchTextures(TextureStitchEvent.Pre event) {
        for (ESDynamicModel model : MODELS) {
            model.load();
            if (model.model == null) { continue; }
            for (ResourceLocation texture : model.model.getTextures()) {
                event.getMap().registerSprite(texture);
            }
        }
    }

    @SubscribeEvent public static void bakeModels(ModelBakeEvent event) {
        for (ESDynamicModel model : MODELS) {
            model.load();
            if (model.model != null) { model.baked = model.model.bake(TRSRTransformation.identity(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter()); }
        }
    }

    private void load() {
        if (this.model != null) { return; }
        try {
            this.model = ModelLoaderRegistry.getModel(this.location);
        }
        catch (Exception exception) {
            EngineeredSchematics.LOGGER.error("Could not load the dynamic model {}", this.location, exception);
        }
    }

    public List<BakedQuad> getQuads() {
        if (this.baked == null) { return Collections.emptyList(); }
        return this.baked.getQuads(null, null, 0L);
    }
}
