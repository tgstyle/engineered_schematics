package tech.muddykat.engineered_schematics.event;

import tech.muddykat.engineered_schematics.helper.SchematicRenderer;
import tech.muddykat.engineered_schematics.item.ESSchematicSettings;
import tech.muddykat.engineered_schematics.item.SchematicProjection;
import tech.muddykat.engineered_schematics.registry.ESRegistry;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SchematicPickBlockHandler {
    private static final int MOUSE_MIDDLE = 2;

    @SubscribeEvent public void handlePickSchematicBlock(MouseEvent event) {
        if (event.getButton() != MOUSE_MIDDLE || !event.isButtonstate()) { return; }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != RayTraceResult.Type.BLOCK) { return; }
        ItemStack offhandItem = player.getHeldItemOffhand();
        if (offhandItem.getItem() != ESRegistry.SCHEMATIC_ITEM) { return; }
        for (int i = 0; i <= 9; i++) {
            ItemStack stack = i == 9 ? offhandItem : player.inventory.getStackInSlot(i);
            if (stack.getItem() == ESRegistry.SCHEMATIC_ITEM && handleSchematicPick(stack, player, mc.objectMouseOver)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    public static boolean handleSchematicPick(ItemStack schematic, EntityPlayer player, RayTraceResult hit) {
        ESSchematicSettings settings = new ESSchematicSettings(schematic);
        if (settings.getMultiblock() == null || !settings.isPlaced() || settings.getPos() == null) { return false; }
        World world = player.world;
        BlockPos origin = settings.getPos();
        SchematicProjection projection = new SchematicProjection(settings.getMultiblock());
        projection.setFlip(settings.isMirrored());
        projection.setRotation(settings.getRotation());
        int workingLayer = 0;
        for (int layer = 0; layer < projection.getSize().getY(); layer++) {
            boolean incomplete = projection.process(layer, info -> !SchematicRenderer.isValidBlockForSchematic(info.getModifiedState(), world.getBlockState(info.tPos.add(origin))));
            if (incomplete) {
                workingLayer = layer;
                break;
            }
        }
        BlockPos wanted = hit.getBlockPos().offset(hit.sideHit);
        boolean[] picked = {false};
        projection.process(workingLayer, info -> {
            BlockPos realPos = info.tPos.add(origin);
            if (!realPos.equals(wanted)) { return false; }
            IBlockState state = world.getBlockState(realPos);
            if (!state.getBlock().isAir(state, world, realPos)) { return false; }
            ItemStack stack = stackFor(info.getRawState());
            if (stack.isEmpty()) { return false; }
            pickInto(player, stack);
            picked[0] = true;
            return true;
        });
        return picked[0];
    }

    private static ItemStack stackFor(IBlockState state) {
        Item item = Item.getItemFromBlock(state.getBlock());
        if (item == Items.AIR) { return ItemStack.EMPTY; }
        return new ItemStack(item, 1, state.getBlock().getMetaFromState(state));
    }

    private static void pickInto(EntityPlayer player, ItemStack stack) {
        Minecraft mc = Minecraft.getMinecraft();
        InventoryPlayer inventory = player.inventory;
        if (player.capabilities.isCreativeMode) {
            inventory.setPickedItemStack(stack);
            mc.playerController.sendSlotPacket(player.getHeldItemMainhand(), 36 + inventory.currentItem);
            return;
        }
        int slot = inventory.getSlotFor(stack);
        if (slot == -1) { return; }
        if (InventoryPlayer.isHotbar(slot)) { inventory.currentItem = slot; }
        else { mc.playerController.pickItem(slot); }
    }
}
