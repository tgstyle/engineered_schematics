package tech.muddykat.engineered_schematics.client.renderer;

import tech.muddykat.engineered_schematics.block.entity.SchematicBoardBlockEntity;
import tech.muddykat.engineered_schematics.helper.BorderState;

import blusunrize.immersiveengineering.client.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.util.List;

@SideOnly(Side.CLIENT)
public class CorkboardRenderer extends TileEntitySpecialRenderer<SchematicBoardBlockEntity> {
    public static ESDynamicModel FRAME_EDGE;
    public static ESDynamicModel CORNER;
    public static ESDynamicModel SCHEMATIC;

    @Override public void render(@Nonnull SchematicBoardBlockEntity tile, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (FRAME_EDGE == null) { return; }
        BorderState border = tile.getBorderState();
        BlockPos pos = tile.getPos();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
        GlStateManager.rotate(180 - tile.getFacing().getHorizontalAngle(), 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(-0.5, -0.5, -0.5);
        GlStateManager.pushMatrix();
        if (border.hasLeftEdge()) { renderModel(FRAME_EDGE, pos, 0.0F, 0.0F); }
        if (border.hasRightEdge()) { renderModel(FRAME_EDGE, pos, 0.9375F, 0.0F); }
        if (border.hasBottomLeftCorner()) { renderModel(CORNER, pos, 0.0F, -0.9375F); }
        if (border.hasTopLeftCorner()) { renderModel(CORNER, pos, 0.0F, 0.0F); }
        if (border.hasBottomRightCorner()) { renderModel(CORNER, pos, 0.9375F, -0.9375F); }
        if (border.hasTopRightCorner()) { renderModel(CORNER, pos, 0.9375F, 0.0F); }
        if (border.hasBottomEdge()) { renderModel(FRAME_EDGE, pos, 0.0F, 0.0625F, 180.0F, 0.0F); }
        if (border.hasTopEdge()) { renderModel(FRAME_EDGE, pos, 0.0F, 0.0F, 180.0F, -1.0F); }
        GlStateManager.popMatrix();
        renderPinnedSchematics(tile, border, pos);
        GlStateManager.popMatrix();
    }

    private void renderPinnedSchematics(SchematicBoardBlockEntity tile, BorderState border, BlockPos pos) {
        NonNullList<ItemStack> inventory = tile.getInventory();
        List<Float> randomState = tile.getRandomState();
        GlStateManager.translate(0.09375, -0.125, 0.15);
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).isEmpty()) { continue; }
            float r = -1 + randomState.get(i) * 2;
            float offsetX = -1 + randomState.get(i) * 2;
            float offsetY = -1 + randomState.get(4 + i) * 2;
            if (border.hasRightEdge()) { offsetX -= 0.5F; }
            if (border.hasLeftEdge()) { offsetX += 0.5F; }
            if (border.hasTopEdge()) { offsetY -= 0.5F; }
            if (border.hasBottomEdge()) { offsetY += 0.5F; }
            int column = i % 2;
            int row = i < 2 ? 1 : 0;
            if (!border.hasRightEdge() && !border.hasLeftEdge()) { offsetX += column == 0 ? -0.6F : 0.6F; }
            if (!border.hasTopEdge() && !border.hasBottomEdge()) { offsetY += row == 0 ? -0.6F : 0.6F; }
            GlStateManager.pushMatrix();
            GlStateManager.translate(column * 0.375, row * 0.5F, -i * 0.0025F);
            GlStateManager.translate(0.06F * offsetX, 0.04F * offsetY, 0.0F);
            GlStateManager.rotate(-2 * r, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-6 * r, 0.0F, 0.0F, 1.0F);
            float scale = 1 + r * 0.05F;
            GlStateManager.scale(scale, scale, scale);
            renderModel(SCHEMATIC, pos, 0.0F, 0.0F);
            GlStateManager.popMatrix();
        }
    }

    private void renderModel(ESDynamicModel model, BlockPos pos, float x, float y) { renderModel(model, pos, x, y, 0.0F, 0.0F); }

    private void renderModel(ESDynamicModel model, BlockPos pos, float x, float y, float rotateZ, float afterX) {
        List<BakedQuad> quads = model.getQuads();
        if (quads.isEmpty()) { return; }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        if (rotateZ != 0.0F) { GlStateManager.rotate(rotateZ, 0.0F, 0.0F, 1.0F); }
        GlStateManager.translate(afterX, 0.0F, 0.0F);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        ClientUtils.renderModelTESRFancy(quads, buffer, getWorld(), pos, false);
        tessellator.draw();
        GlStateManager.disableBlend();
        RenderHelper.enableStandardItemLighting();
        GlStateManager.popMatrix();
    }
}
