package tech.muddykat.engineered_schematics.client;

import tech.muddykat.engineered_schematics.EngineeredSchematics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.ShaderLinkHelper;
import net.minecraft.client.shader.ShaderManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.io.IOException;

@SideOnly(Side.CLIENT)
public class ESShaders {
    private static final String PROGRAM = EngineeredSchematics.MODID + ":schematic";
    private static ShaderManager schematic;
    private static boolean unavailable;

    public static void use(float time, float red, float green, float blue) {
        ShaderManager shader = getSchematicShader();
        if (shader == null) { return; }
        shader.getShaderUniformOrDefault("Time").set(time);
        shader.getShaderUniformOrDefault("ColorTint").set(red, green, blue);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableAlpha();
        GlStateManager.depthFunc(GL11.GL_LESS);
        shader.useShader();
    }

    public static void end() {
        if (schematic == null) { return; }
        schematic.endShader();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
    }

    @Nullable
    @SuppressWarnings("ConstantConditions")
    public static ShaderManager getSchematicShader() {
        if (schematic != null || unavailable) { return schematic; }
        if (!OpenGlHelper.areShadersSupported()) {
            unavailable = true;
            EngineeredSchematics.LOGGER.warn("Shaders are not supported, the schematic projection will not be tinted");
            return null;
        }
        if (ShaderLinkHelper.getStaticShaderLinkHelper() == null) { ShaderLinkHelper.setNewStaticShaderLinkHelper(); }
        try {
            schematic = new ShaderManager(Minecraft.getMinecraft().getResourceManager(), PROGRAM);
            schematic.addSamplerTexture("Sampler0", Minecraft.getMinecraft().getTextureMapBlocks());
        }
        catch (IOException exception) {
            unavailable = true;
            EngineeredSchematics.LOGGER.error("Could not load the schematic shader program " + PROGRAM, exception);
        }
        return schematic;
    }
}
