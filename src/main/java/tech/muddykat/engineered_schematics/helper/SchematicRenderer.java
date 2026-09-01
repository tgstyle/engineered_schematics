package tech.muddykat.engineered_schematics.helper;

import tech.muddykat.engineered_schematics.client.ESShaders;
import tech.muddykat.engineered_schematics.item.ESSchematicSettings;
import tech.muddykat.engineered_schematics.item.SchematicProjection;
import tech.muddykat.engineered_schematics.util.ESLang;
import tech.muddykat.engineered_schematics.util.ESMultiblocks;

import blusunrize.immersiveengineering.api.MultiblockHandler;
import blusunrize.immersiveengineering.common.blocks.TileEntityMultiblockPart;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class SchematicRenderer {
    private static final int COLOR_ERROR = 0xFF0000;
    private static final int COLOR_WARNING = 0xFFFF00;
    private static final int COLOR_SUCCESS = 0x00BF00;
    private static final int COLOR_HIGHLIGHT = 0x44FF44;
    private static final float[] COLOR_HELD = {0.2F, 1.0F, 0.5F};
    private static final float[] COLOR_NORMAL = {0.2F, 0.5F, 1.0F};
    private static final float LINE_ALPHA = 0.3F;
    private static final double GRID_LIFT = 0.02;
    private static final float TRIGGER_LIFT = 0.05F;
    private static final float TRIGGER_SCALE = 0.75F;
    private static final Set<Block> ANY_STATE = new HashSet<>(Arrays.asList(Blocks.HOPPER, Blocks.PISTON));

    private SchematicRenderer() {}

    public static void renderSchematic(ESSchematicSettings settings, EntityPlayer player, World world) {
        MultiblockHandler.IMultiblock multiblock = settings.getMultiblock();
        BlockPos origin = settings.getPos();
        if (multiblock == null || origin == null) { return; }
        SchematicProjection projection = setupProjection(settings, multiblock);
        Vec3i size = projection.getSize();
        Map<BlockPos, Boolean> badStates = new HashMap<>();
        List<Entry> toRender = new ArrayList<>();
        RenderingState state = processBlocks(projection, world, origin, badStates, toRender);
        for (Entry entry : toRender) {
            TileEntity te = world.getTileEntity(entry.info.tPos.add(origin));
            if (te instanceof TileEntityMultiblockPart) {
                renderSchematicGrid(settings, COLOR_SUCCESS);
                return;
            }
        }
        renderResults(state, settings, multiblock, origin, player, size, badStates, toRender, projection);
    }

    private static SchematicProjection setupProjection(ESSchematicSettings settings, MultiblockHandler.IMultiblock multiblock) {
        SchematicProjection projection = new SchematicProjection(multiblock);
        projection.setRotation(settings.getRotation());
        projection.setFlip(settings.isMirrored());
        return projection;
    }

    private static RenderingState processBlocks(SchematicProjection projection, World world, BlockPos origin, Map<BlockPos, Boolean> badStates, List<Entry> toRender) {
        int[] currentLayer = new int[1];
        int[] badBlocks = new int[1];
        int[] goodBlocks = new int[1];
        int[] imperfectionLayer = {-1};
        projection.processAll((layer, info) -> processBlock(layer, info, world, origin, badStates, toRender, currentLayer, badBlocks, goodBlocks, imperfectionLayer));
        return new RenderingState(goodBlocks[0] == projection.getBlockCount(), imperfectionLayer[0] != -1, badStates.containsValue(false), currentLayer[0]);
    }

    private static boolean processBlock(int layer, SchematicProjection.Info info, World world, BlockPos origin, Map<BlockPos, Boolean> badStates, List<Entry> toRender, int[] currentLayer, int[] badBlocks, int[] goodBlocks, int[] imperfectionLayer) {
        if (badBlocks[0] == 0 && layer > currentLayer[0]) { currentLayer[0] = layer; }
        else if (layer != currentLayer[0]) { return true; }
        BlockPos realPos = info.tPos.add(origin);
        IBlockState currentState = world.getBlockState(realPos);
        IBlockState targetState = info.getModifiedState();
        if (isValidBlockForSchematic(targetState, currentState)) {
            toRender.add(new Entry(RenderLayer.PERFECT, info));
            goodBlocks[0]++;
            return false;
        }
        if (!currentState.getBlock().isAir(currentState, world, realPos)) {
            toRender.add(new Entry(RenderLayer.BAD, info));
            badStates.put(info.tPos, targetState.getBlock() == currentState.getBlock());
            imperfectionLayer[0] = layer;
            return false;
        }
        badBlocks[0]++;
        toRender.add(new Entry(RenderLayer.ALL, info));
        return false;
    }

    public static boolean isValidBlockForSchematic(IBlockState expected, IBlockState actual) {
        if (expected == actual) { return true; }
        if (expected.getBlock() == actual.getBlock()) { return ANY_STATE.contains(expected.getBlock()) || expected.getBlock().getMetaFromState(expected) == actual.getBlock().getMetaFromState(actual); }
        return sharesOreName(expected, actual);
    }

    private static boolean sharesOreName(IBlockState expected, IBlockState actual) {
        ItemStack expectedStack = stackFor(expected);
        ItemStack actualStack = stackFor(actual);
        if (expectedStack.isEmpty() || actualStack.isEmpty()) { return false; }
        int[] expectedIds = OreDictionary.getOreIDs(expectedStack);
        if (expectedIds.length == 0) { return false; }
        for (int actualId : OreDictionary.getOreIDs(actualStack)) {
            for (int expectedId : expectedIds) {
                if (actualId == expectedId) { return true; }
            }
        }
        return false;
    }

    private static ItemStack stackFor(IBlockState state) {
        Item item = Item.getItemFromBlock(state.getBlock());
        if (item == Items.AIR) { return ItemStack.EMPTY; }
        return new ItemStack(item, 1, state.getBlock().getMetaFromState(state));
    }

    private static void renderResults(RenderingState state, ESSchematicSettings settings, MultiblockHandler.IMultiblock multiblock, BlockPos origin, EntityPlayer player, Vec3i size, Map<BlockPos, Boolean> badStates, List<Entry> toRender, SchematicProjection projection) {
        SchematicBlockAccess access = new SchematicBlockAccess(projection);
        BlockPos.MutableBlockPos min = new BlockPos.MutableBlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        BlockPos.MutableBlockPos max = new BlockPos.MutableBlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        ItemStack heldStack = player.getHeldItemMainhand();
        IBlockState heldState = heldStack.getItem() instanceof ItemBlock ? ((ItemBlock)heldStack.getItem()).getBlock().getDefaultState() : null;
        GlStateManager.pushMatrix();
        GlStateManager.translate(origin.getX(), origin.getY(), origin.getZ());
        for (Entry entry : toRender) {
            if (entry.layer != RenderLayer.ALL) { continue; }
            if (state.hasWrongBlock) { continue; }
            boolean isHeld = heldState != null && isValidBlockForSchematic(entry.info.getRawState(), heldState);
            renderPhantom(entry.info, access, isHeld ? COLOR_HELD : COLOR_NORMAL);
            if (isHeld) { renderCenteredOutlineBox(entry.info.tPos, COLOR_HIGHLIGHT); }
        }
        for (Entry entry : toRender) {
            if (entry.layer == RenderLayer.BAD) { renderCenteredOutlineBox(entry.info.tPos, Boolean.TRUE.equals(badStates.get(entry.info.tPos)) ? COLOR_WARNING : COLOR_ERROR); }
            else if (entry.layer == RenderLayer.PERFECT) { updatePerfectBounds(min, max, entry.info); }
        }
        int gridLayer = state.currentLayer;
        for (BlockPos bad : badStates.keySet()) {
            gridLayer = Math.min(gridLayer, bad.getY());
        }
        if (!state.perfect) { renderGridForRotation(settings, multiblock, size, state, gridLayer); }
        else {
            renderOutlineBox(min, max);
            renderFootprintText(settings.getRotation(), size, 0xFFFFFF, ESLang.translate("item.engineered_schematics.multiblock_schematic.formed"), 0);
            renderFormationTool(settings, multiblock, projection);
        }
        GlStateManager.popMatrix();
    }

    private static void renderFormationTool(ESSchematicSettings settings, MultiblockHandler.IMultiblock multiblock, SchematicProjection projection) {
        BlockPos trigger = ESMultiblocks.getTriggerOffset(multiblock);
        if (trigger == null) { return; }
        BlockPos cell = projection.toLocal(trigger);
        ItemStack tool = settings.getFormationTool();
        if (tool.isEmpty()) { tool = ESSchematicSettings.defaultFormationTool(); }
        Rotation rotation = settings.getRotation();
        BlockPos outward = new BlockPos(0, 0, 1).rotate(rotation);
        renderCenteredOutlineBox(cell, COLOR_HIGHLIGHT);
        GlStateManager.pushMatrix();
        GlStateManager.translate(cell.getX() + 0.5F + outward.getX() * (0.5F + TRIGGER_LIFT),
                cell.getY() + 0.5F,
                cell.getZ() + 0.5F + outward.getZ() * (0.5F + TRIGGER_LIFT));
        GlStateManager.rotate(90.0F * rotation.ordinal(), 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(-0.5F * TRIGGER_SCALE, -0.5F * TRIGGER_SCALE, 0.0F);
        GlStateManager.scale(TRIGGER_SCALE, TRIGGER_SCALE, TRIGGER_SCALE);
        ESShaders.use(0.0F, 0.67F, 0.0F, 0.0F);
        Minecraft.getMinecraft().getRenderItem().renderItem(tool, ItemCameraTransforms.TransformType.NONE);
        ESShaders.end();
        GlStateManager.popMatrix();
    }

    private static float rotatedX(float x, float z, Rotation rotation) {
        if (rotation == Rotation.CLOCKWISE_90) { return -z; }
        if (rotation == Rotation.CLOCKWISE_180) { return -x; }
        if (rotation == Rotation.COUNTERCLOCKWISE_90) { return z; }
        return x;
    }

    private static float rotatedZ(float x, float z, Rotation rotation) {
        if (rotation == Rotation.CLOCKWISE_90) { return x; }
        if (rotation == Rotation.CLOCKWISE_180) { return -z; }
        if (rotation == Rotation.COUNTERCLOCKWISE_90) { return -x; }
        return z;
    }

    private static void renderGridForRotation(ESSchematicSettings settings, MultiblockHandler.IMultiblock multiblock, Vec3i size, RenderingState state, int y) {
        Rotation rotation = settings.getRotation();
        Vec3i footprint = orientedFootprint(size, rotation);
        GlStateManager.pushMatrix();
        GlStateManager.translate(-Math.floorDiv(footprint.getX(), 2), y, -Math.floorDiv(footprint.getZ(), 2));
        int color = state.hasImperfection ? (state.hasWrongBlock ? COLOR_ERROR : COLOR_WARNING) : 0xFFFFFF;
        String name = state.hasImperfection && state.hasWrongBlock ? ESLang.translate("item.engineered_schematics.multiblock_schematic.error") : ESMultiblocks.getDisplayName(multiblock);
        renderGrid(footprint, color, y, name, rotation);
        GlStateManager.popMatrix();
    }

    private static Vec3i orientedFootprint(Vec3i size, Rotation rotation) {
        boolean alongX = rotation == Rotation.NONE || rotation == Rotation.CLOCKWISE_180;
        return alongX ? size : new Vec3i(size.getZ(), size.getY(), size.getX());
    }

    private static void renderFootprintText(Rotation rotation, Vec3i size, int rgb, String text, int y) {
        Vec3i footprint = orientedFootprint(size, rotation);
        GlStateManager.pushMatrix();
        GlStateManager.translate(-Math.floorDiv(footprint.getX(), 2), y, -Math.floorDiv(footprint.getZ(), 2));
        drawFrontGroundText(footprint, rotation, rgb, text, footprint.getX());
        GlStateManager.popMatrix();
    }

    public static void renderSchematicGrid(ESSchematicSettings settings, int color) {
        MultiblockHandler.IMultiblock multiblock = settings.getMultiblock();
        BlockPos origin = settings.getPos();
        if (multiblock == null || origin == null) { return; }
        Rotation rotation = settings.getRotation();
        Vec3i footprint = orientedFootprint(SchematicProjection.getSize(multiblock), rotation);
        GlStateManager.pushMatrix();
        GlStateManager.translate(origin.getX(), origin.getY(), origin.getZ());
        GlStateManager.translate(-Math.floorDiv(footprint.getX(), 2), 0, -Math.floorDiv(footprint.getZ(), 2));
        renderGrid(footprint, color, 0, ESMultiblocks.getDisplayName(multiblock), rotation);
        GlStateManager.popMatrix();
    }

    private static void renderPhantom(SchematicProjection.Info info, SchematicBlockAccess access, float[] color) {
        IBlockState state = info.getModifiedState().getActualState(access, info.tPos);
        if (state.getRenderType() != EnumBlockRenderType.MODEL) { return; }
        IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state);
        GlStateManager.pushMatrix();
        GlStateManager.translate(info.tPos.getX(), info.tPos.getY(), info.tPos.getZ());
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        GlStateManager.translate(0.5F, 0.5F, 0.5F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        ESShaders.use(0.0F, color[0], color[1], color[2]);
        Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(state, model, 1.0F, 1.0F, 1.0F, 1.0F);
        ESShaders.end();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }

    private static void updatePerfectBounds(BlockPos.MutableBlockPos min, BlockPos.MutableBlockPos max, SchematicProjection.Info info) {
        min.setPos(Math.min(info.tPos.getX(), min.getX()), Math.min(info.tPos.getY(), min.getY()), Math.min(info.tPos.getZ(), min.getZ()));
        max.setPos(Math.max(info.tPos.getX(), max.getX()), Math.max(info.tPos.getY(), max.getY()), Math.max(info.tPos.getZ(), max.getZ()));
    }

    private static void renderCenteredOutlineBox(BlockPos pos, int rgb) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(pos.getX(), pos.getY(), pos.getZ());
        renderBox(0, 0, 0, 1, 1, 1, rgb);
        GlStateManager.popMatrix();
    }

    private static void renderOutlineBox(Vec3i min, Vec3i max) { renderBox(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1, COLOR_SUCCESS); }

    private static void renderGrid(Vec3i size, int rgb, int currentLayer, String name, Rotation rotation) {
        beginLines();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (int z = 0; z <= size.getZ(); z++) {
            line(buffer, 0, GRID_LIFT, z, size.getX(), GRID_LIFT, z, rgb);
        }
        for (int x = 0; x <= size.getX(); x++) {
            line(buffer, x, GRID_LIFT, 0, x, GRID_LIFT, size.getZ(), rgb);
        }
        Tessellator.getInstance().draw();
        endLines();
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, -currentLayer, 0);
        drawFrontGroundText(size, rotation, rgb, name, size.getX());
        GlStateManager.popMatrix();
    }

    private static void renderBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int rgb) {
        double eps = 0.01;
        minX -= eps;
        minY -= eps;
        minZ -= eps;
        maxX += eps;
        maxY += eps;
        maxZ += eps;
        beginLines();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        line(buffer, minX, minY, minZ, maxX, minY, minZ, rgb);
        line(buffer, maxX, minY, minZ, maxX, minY, maxZ, rgb);
        line(buffer, maxX, minY, maxZ, minX, minY, maxZ, rgb);
        line(buffer, minX, minY, maxZ, minX, minY, minZ, rgb);
        line(buffer, minX, maxY, minZ, maxX, maxY, minZ, rgb);
        line(buffer, maxX, maxY, minZ, maxX, maxY, maxZ, rgb);
        line(buffer, maxX, maxY, maxZ, minX, maxY, maxZ, rgb);
        line(buffer, minX, maxY, maxZ, minX, maxY, minZ, rgb);
        line(buffer, minX, minY, minZ, minX, maxY, minZ, rgb);
        line(buffer, maxX, minY, minZ, maxX, maxY, minZ, rgb);
        line(buffer, maxX, minY, maxZ, maxX, maxY, maxZ, rgb);
        line(buffer, minX, minY, maxZ, minX, maxY, maxZ, rgb);
        Tessellator.getInstance().draw();
        endLines();
    }

    private static void line(BufferBuilder buffer, double x1, double y1, double z1, double x2, double y2, double z2, int rgb) {
        float red = (rgb >> 16 & 0xFF) / 255.0F;
        float green = (rgb >> 8 & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        buffer.pos(x1, y1, z1).color(red, green, blue, LINE_ALPHA).endVertex();
        buffer.pos(x2, y2, z2).color(red, green, blue, LINE_ALPHA).endVertex();
    }

    private static void beginLines() {
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.depthMask(false);
        GlStateManager.glLineWidth(2.0F);
    }

    private static void endLines() {
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    private static void drawFrontGroundText(Vec3i size, Rotation rotation, int rgb, String text, int span) {
        if (text.isEmpty()) { return; }
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 0.0125F, 0);
        if (rotation == Rotation.NONE) { GlStateManager.translate(0, 0, size.getZ() + 0.125F); }
        else if (rotation == Rotation.CLOCKWISE_90) { GlStateManager.translate(-0.125F, 0, 0); }
        else if (rotation == Rotation.COUNTERCLOCKWISE_90) { GlStateManager.translate(size.getX() + 0.125F, 0, size.getZ()); }
        else { GlStateManager.translate(size.getX(), 0, -0.125F); }
        GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(90.0F * rotation.ordinal(), 0.0F, 0.0F, 1.0F);
        float scale = 0.1F * span / text.length();
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        font.drawString(text, 0, 0, rgb | 0xFF000000, false);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static final class Entry {
        private final RenderLayer layer;
        private final SchematicProjection.Info info;

        private Entry(RenderLayer layer, SchematicProjection.Info info) {
            this.layer = layer;
            this.info = info;
        }
    }

    private static final class RenderingState {
        private final boolean perfect;
        private final boolean hasImperfection;
        private final boolean hasWrongBlock;
        private final int currentLayer;

        private RenderingState(boolean perfect, boolean hasImperfection, boolean hasWrongBlock, int currentLayer) {
            this.perfect = perfect;
            this.hasImperfection = hasImperfection;
            this.hasWrongBlock = hasWrongBlock;
            this.currentLayer = currentLayer;
        }
    }

    public enum RenderLayer {
        ALL, BAD, PERFECT
    }
}
