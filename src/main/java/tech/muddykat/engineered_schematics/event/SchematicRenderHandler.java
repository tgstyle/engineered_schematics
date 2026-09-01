package tech.muddykat.engineered_schematics.event;

import tech.muddykat.engineered_schematics.helper.SchematicRenderer;
import tech.muddykat.engineered_schematics.item.ESSchematicSettings;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SchematicRenderHandler {
    @SubscribeEvent public void renderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        Entity view = mc.getRenderViewEntity();
        if (player == null || view == null) { return; }
        ItemStack mainItem = player.getHeldItemMainhand();
        ItemStack offhandItem = player.getHeldItemOffhand();
        boolean offhandProjects = ESSchematicSettings.hasSettings(offhandItem);
        if (!offhandProjects && !ESSchematicSettings.hasSettings(mainItem)) { return; }
        float partialTicks = event.getPartialTicks();
        double viewX = view.lastTickPosX + (view.posX - view.lastTickPosX) * partialTicks;
        double viewY = view.lastTickPosY + (view.posY - view.lastTickPosY) * partialTicks;
        double viewZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * partialTicks;
        GlStateManager.pushMatrix();
        GlStateManager.translate(-viewX, -viewY, -viewZ);
        if (offhandProjects) {
            for (int i = 0; i <= 9; i++) {
                ItemStack stack = i == 9 ? offhandItem : player.inventory.getStackInSlot(i);
                if (isSchematic(stack) && ESSchematicSettings.hasSettings(stack)) { SchematicRenderer.renderSchematic(new ESSchematicSettings(stack), player, player.world); }
            }
        }
        else {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (isSchematic(stack) && ESSchematicSettings.hasSettings(stack)) { SchematicRenderer.renderSchematicGrid(new ESSchematicSettings(stack), stack == mainItem ? 0xFFFFFF : 0x666666); }
            }
        }
        GlStateManager.popMatrix();
    }

    private static boolean isSchematic(ItemStack stack) { return !stack.isEmpty() && stack.getItem() == ESRegistry.SCHEMATIC_ITEM; }
}
