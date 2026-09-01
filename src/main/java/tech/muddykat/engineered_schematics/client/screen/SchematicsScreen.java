package tech.muddykat.engineered_schematics.client.screen;

import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.helper.SchematicBlockAccess;
import tech.muddykat.engineered_schematics.item.SchematicProjection;
import tech.muddykat.engineered_schematics.menu.SchematicsContainerMenu;
import tech.muddykat.engineered_schematics.util.ESLang;
import tech.muddykat.engineered_schematics.util.ESConveyors;
import tech.muddykat.engineered_schematics.util.ESMultiblocks;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.MultiblockHandler;
import blusunrize.immersiveengineering.api.tool.ConveyorHandler;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class SchematicsScreen extends GuiContainer {
    private static final ResourceLocation TEXTURE = EngineeredSchematics.makeTextureLocation("schematic_gui");
    private static final int MAX_DISPLAY_WIDTH = 80;
    private static final int TEXT_COLOR = 0x666666;
    private static final int LIST_LEFT = 9;
    private static final int LIST_TOP = 20;
    private static final int LIST_WIDTH = 110;
    private static final int LIST_HEIGHT = 92;
    private static final int ENTRY_HEIGHT = 21;
    private static final float PREVIEW_DEPTH = 100.0F;
    private static final EnumFacing CONVEYOR_FACING = EnumFacing.EAST;
    private static final Map<String, Map<Integer, EnumFacing>> CONVEYOR_FACINGS = conveyorFacings();
    private static final float FORMED_SCALE = 0.75F;
    private static final float BLOCK_ROTATION = 180.0F;
    private static final Map<String, Float> FORMED_SCALES = formedScales();
    private static final float[] FORMED_SHIFT_DEFAULT = {0.0F, 0.0F};
    private static final Map<String, float[]> FORMED_SHIFTS = formedShifts();
    private static final float[] PREVIEW_SHIFT_DEFAULT = {0.0F, 0.0F};
    private static final Map<String, float[]> PREVIEW_SHIFTS = previewShifts();
    private final SchematicsContainerMenu menu;
    private int scroll;

    public SchematicsScreen(SchematicsContainerMenu menu) {
        super(menu);
        this.menu = menu;
        this.xSize = 230;
        this.ySize = 218;
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.fontRenderer.drawString(ESLang.translate("desc.engineered_schematics.schematic_table"), 14, 9, TEXT_COLOR);
        this.fontRenderer.drawString(this.mc.player.inventory.getDisplayName().getUnformattedText(), 36, 125, TEXT_COLOR);
        this.fontRenderer.drawString(ESLang.translate("engineered_schematics.gui.schematic_table.mirror"), 143, 117, this.menu.isMirrored() ? 0x00AA00 : TEXT_COLOR);
    }

    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        drawSchematicList(mouseX, mouseY);
        List<MultiblockHandler.IMultiblock> multiblocks = this.menu.getAvailableMultiblocks();
        if (multiblocks.isEmpty()) { return; }
        MultiblockHandler.IMultiblock selected = multiblocks.get(this.menu.getSelectedSchematic());
        drawMultiblockName(selected);
        drawMultiblockPreview(selected);
    }

    private void drawSchematicList(int mouseX, int mouseY) {
        List<MultiblockHandler.IMultiblock> multiblocks = this.menu.getAvailableMultiblocks();
        FontRenderer font = this.fontRenderer;
        int left = this.guiLeft + LIST_LEFT;
        int top = this.guiTop + LIST_TOP;
        for (int i = 0; i < multiblocks.size(); i++) {
            int entryTop = top + i * ENTRY_HEIGHT - this.scroll;
            if (!isEntryVisible(entryTop, top)) { continue; }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.mc.getTextureManager().bindTexture(TEXTURE);
            drawTexturedModalRect(left, entryTop, 1, 219, 104, 20);
            boolean hover = mouseX >= left && mouseX < left + LIST_WIDTH && mouseY >= entryTop && mouseY < entryTop + ENTRY_HEIGHT - 1;
            int color = i == this.menu.getSelectedSchematic() ? 0xFFFFFF : (hover ? 0xAAFFAA : TEXT_COLOR);
            String name = ESMultiblocks.getDisplayName(multiblocks.get(i));
            float scale = scaleFor(font, name);
            GlStateManager.pushMatrix();
            GlStateManager.translate(left + 10, entryTop + 6, 0);
            GlStateManager.scale(scale, scale, scale);
            font.drawString(name, 0, 0, color);
            GlStateManager.popMatrix();
        }
    }

    private void drawMultiblockName(MultiblockHandler.IMultiblock multiblock) {
        String name = ESMultiblocks.getDisplayName(multiblock);
        float scale = scaleFor(this.fontRenderer, name);
        GlStateManager.pushMatrix();
        GlStateManager.translate(this.guiLeft + 174.5F, this.guiTop + 12.0F, 0.0F);
        GlStateManager.scale(scale, scale, scale);
        this.fontRenderer.drawString(name, -this.fontRenderer.getStringWidth(name) / 2, 0, TEXT_COLOR);
        GlStateManager.popMatrix();
    }

    private float scaleFor(FontRenderer font, String text) {
        float width = font.getStringWidth(text);
        return width > MAX_DISPLAY_WIDTH ? MAX_DISPLAY_WIDTH / width : 1.0F;
    }

    private static Map<String, Map<Integer, EnumFacing>> conveyorFacings() {
        Map<String, Map<Integer, EnumFacing>> facings = new HashMap<>();
        Map<Integer, EnumFacing> metalPress = new HashMap<>();
        metalPress.put(3, EnumFacing.SOUTH);
        metalPress.put(5, EnumFacing.SOUTH);
        facings.put("IE:MetalPress", metalPress);
        Map<Integer, EnumFacing> assembler = new HashMap<>();
        assembler.put(10, EnumFacing.SOUTH);
        assembler.put(16, EnumFacing.SOUTH);
        facings.put("IE:Assembler", assembler);
        Map<Integer, EnumFacing> autoWorkbench = new HashMap<>();
        autoWorkbench.put(13, EnumFacing.EAST);
        autoWorkbench.put(14, EnumFacing.EAST);
        autoWorkbench.put(15, EnumFacing.EAST);
        autoWorkbench.put(16, EnumFacing.NORTH);
        facings.put("IE:AutoWorkbench", autoWorkbench);
        return facings;
    }

    private static EnumFacing conveyorFacing(MultiblockHandler.IMultiblock multiblock, int index) {
        Map<Integer, EnumFacing> facings = CONVEYOR_FACINGS.get(multiblock.getUniqueName());
        return facings == null ? CONVEYOR_FACING : facings.getOrDefault(index, CONVEYOR_FACING);
    }

    private static boolean isEntryVisible(int entryTop, int top) { return entryTop >= top && entryTop + ENTRY_HEIGHT <= top + LIST_HEIGHT; }

    private static Map<String, Float> formedScales() {
        Map<String, Float> scales = new HashMap<>();
        scales.put("IE:ArcFurnace", 0.75F);
        return scales;
    }

    private static Map<String, float[]> formedShifts() {
        Map<String, float[]> shifts = new HashMap<>();
        shifts.put("IE:BottlingMachine", new float[]{-1.0F, 0.0F});
        return shifts;
    }

    private static Map<String, float[]> previewShifts() {
        Map<String, float[]> shifts = new HashMap<>();
        shifts.put("IE:CokeOven", new float[]{1.0F, 0.0F});
        shifts.put("IE:BlastFurnace", new float[]{1.0F, 0.0F});
        return shifts;
    }

    private void drawMultiblockPreview(MultiblockHandler.IMultiblock multiblock) {
        SchematicProjection projection = new SchematicProjection(multiblock);
        if (projection.getBlockCount() == 0) { return; }
        SchematicBlockAccess access = new SchematicBlockAccess(projection);
        Vec3i size = projection.getSize();
        float maxDimension = Math.max(size.getY(), Math.max(size.getX(), size.getZ()));
        boolean formed = multiblock.canRenderFormedStructure();
        float scale = multiblock.getManualScale() * (formed ? FORMED_SCALE * FORMED_SCALES.getOrDefault(multiblock.getUniqueName(), 1.0F) : 0.65F);
        BlockRendererDispatcher dispatcher = this.mc.getBlockRendererDispatcher();
        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableDepth();
        GlStateManager.translate(this.guiLeft + 174.5F, this.guiTop + 54.5F, PREVIEW_DEPTH + maxDimension);
        GlStateManager.scale(scale, -scale, 1.0F);
        GlStateManager.rotate(25.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        if (formed) {
            float[] shift = FORMED_SHIFTS.getOrDefault(multiblock.getUniqueName(), FORMED_SHIFT_DEFAULT);
            GlStateManager.translate(size.getZ() / -2.0F + shift[0], size.getY() / -2.0F - shift[1], size.getX() / -2.0F);
        }
        else {
            float[] shift = PREVIEW_SHIFTS.getOrDefault(multiblock.getUniqueName(), PREVIEW_SHIFT_DEFAULT);
            GlStateManager.translate(shift[0], -shift[1], 0.0F);
            GlStateManager.rotate(BLOCK_ROTATION, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, size.getY() / -2.0F, 0.0F);
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.shadeModel(Minecraft.isAmbientOcclusionEnabled() ? GL11.GL_SMOOTH : GL11.GL_FLAT);
        this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        if (formed) {
            multiblock.renderFormedStructure();
            GlStateManager.disableDepth();
            GlStateManager.disableRescaleNormal();
            GlStateManager.popMatrix();
            RenderHelper.enableGUIStandardItemLighting();
            return;
        }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int[] cell = {0};
        projection.processAll((layer, info) -> {
            BlockPos pos = info.tPos;
            ItemStack stack = projection.stackFor(info);
            int index = cell[0]++;
            GlStateManager.translate(pos.getX(), pos.getY(), pos.getZ());
            boolean overwritten = ESConveyors.isConveyor(stack) ? ImmersiveEngineering.proxy.drawConveyorInGui(ESConveyors.typeOf(stack), conveyorFacing(multiblock, index)) : multiblock.overwriteBlockRender(stack, index);
            GlStateManager.translate(-pos.getX(), -pos.getY(), -pos.getZ());
            if (overwritten) {
                this.mc.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
                return false;
            }
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
            dispatcher.renderBlock(access.getBlockState(pos), pos, access, buffer);
            tessellator.draw();
            return false;
        });
        GlStateManager.disableDepth();
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
        RenderHelper.enableGUIStandardItemLighting();
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        if (button != 0) { return; }
        if (mouseX >= this.guiLeft + 129 && mouseX < this.guiLeft + 220 && mouseY >= this.guiTop + 112 && mouseY < this.guiTop + 128) {
            sendButton(SchematicsContainerMenu.BUTTON_MIRROR);
            return;
        }
        int left = this.guiLeft + LIST_LEFT;
        int top = this.guiTop + LIST_TOP;
        if (mouseX < left || mouseX >= left + LIST_WIDTH || mouseY < top || mouseY >= top + LIST_HEIGHT) { return; }
        int index = (mouseY - top + this.scroll) / ENTRY_HEIGHT;
        if (index < 0 || index >= this.menu.getAvailableMultiblocks().size()) { return; }
        if (!isEntryVisible(top + index * ENTRY_HEIGHT - this.scroll, top)) { return; }
        sendButton(SchematicsContainerMenu.BUTTON_SELECT + index);
    }

    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) { return; }
        int content = this.menu.getAvailableMultiblocks().size() * ENTRY_HEIGHT;
        int max = Math.max(0, content - LIST_HEIGHT);
        this.scroll = Math.max(0, Math.min(max, this.scroll - Integer.signum(wheel) * ENTRY_HEIGHT));
    }

    private void sendButton(int id) {
        this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, id);
        this.menu.enchantItem(this.mc.player, id);
    }
}
