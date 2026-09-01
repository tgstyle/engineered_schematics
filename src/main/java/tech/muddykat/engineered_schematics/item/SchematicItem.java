package tech.muddykat.engineered_schematics.item;

import tech.muddykat.engineered_schematics.EngineeredSchematics;
import tech.muddykat.engineered_schematics.util.ESConveyors;
import tech.muddykat.engineered_schematics.util.ESLang;
import tech.muddykat.engineered_schematics.util.ESMultiblocks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class SchematicItem extends Item {
    public SchematicItem() {
        setRegistryName(EngineeredSchematics.MODID, "multiblock_schematic");
        setTranslationKey(EngineeredSchematics.MODID + ".multiblock_schematic");
        setMaxStackSize(1);
        setCreativeTab(EngineeredSchematics.CREATIVE_TAB);
    }

    public static ESSchematicSettings getSettings(@Nullable ItemStack stack) { return new ESSchematicSettings(stack); }

    @Override @Nonnull public String getItemStackDisplayName(@Nonnull ItemStack stack) {
        String selfKey = getTranslationKey(stack);
        if (ESSchematicSettings.hasSettings(stack)) {
            ESSchematicSettings settings = getSettings(stack);
            if (settings.getMultiblock() != null) {
                String key = selfKey + ".specific" + (settings.isMirrored() ? ".mirrored" : "") + ".name";
                return TextFormatting.AQUA + ESLang.format(key, ESMultiblocks.getDisplayName(settings.getMultiblock()));
            }
        }
        return TextFormatting.AQUA + ESLang.translate(selfKey + ".name");
    }

    @Override @SideOnly(Side.CLIENT) public void addInformation(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<String> tooltip, @Nonnull ITooltipFlag flag) {
        ESSchematicSettings settings = getSettings(stack);
        if (settings.getMultiblock() == null) {
            tooltip.add(ESLang.translate("desc.engineered_schematics.info.schematic.no_multiblock"));
            return;
        }
        Vec3i size = SchematicProjection.getSize(settings.getMultiblock());
        tooltip.add(ESLang.format("desc.engineered_schematics.info.schematic.size", "[" + size.getX() + "x" + size.getY() + "x" + size.getZ() + "]"));
        tooltip.add(ESLang.format("desc.engineered_schematics.info.schematic.tier", TextFormatting.AQUA + settings.getFormationTool().getDisplayName() + TextFormatting.RESET));
        tooltip.add(ESLang.format("desc.engineered_schematics.info.schematic.block_info", TextFormatting.GOLD + GameSettings.getKeyDisplayString(Minecraft.getMinecraft().gameSettings.keyBindSneak.getKeyCode()) + TextFormatting.RESET));
    }

    @Override @Nonnull public EnumActionResult onItemUse(@Nonnull EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumHand hand, @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) { return EnumActionResult.SUCCESS; }
        ItemStack stack = player.getHeldItem(hand);
        ESSchematicSettings settings = getSettings(stack);
        settings.setPos(pos.up());
        settings.setPlaced(true);
        if (player.isSneaking()) {
            settings.setRotation(rotationFor(player.getHorizontalFacing()));
            settings.applyTo(stack);
            player.sendStatusMessage(new TextComponentTranslation("desc.engineered_schematics.info.schematic.rotated"), true);
            player.sendStatusMessage(new TextComponentTranslation("desc.engineered_schematics.info.schematic.moved"), true);
            return EnumActionResult.SUCCESS;
        }
        settings.applyTo(stack);
        return EnumActionResult.SUCCESS;
    }

    @Override @Nonnull public ActionResult<ItemStack> onItemRightClick(@Nonnull World world, @Nonnull EntityPlayer player, @Nonnull EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        ESSchematicSettings settings = getSettings(stack);
        BlockPos pos = settings.getPos();
        if (player.isSneaking() && player.capabilities.isCreativeMode && pos != null && settings.getMultiblock() != null) {
            if (!world.isRemote) {
                SchematicProjection projection = new SchematicProjection(settings.getMultiblock());
                projection.setFlip(settings.isMirrored());
                projection.setRotation(settings.getRotation());
                projection.processAll((layer, info) -> {
                    BlockPos placed = info.tPos.add(pos);
                    world.setBlockState(placed, info.getModifiedState(), 3);
                    ItemStack cell = projection.stackFor(info);
                    if (ESConveyors.isConveyor(cell)) { ESConveyors.applySubtype(world, placed, cell); }
                    return false;
                });
                player.sendStatusMessage(new TextComponentTranslation("desc.engineered_schematics.info.schematic.placed"), true);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }

    private static Rotation rotationFor(EnumFacing facing) {
        if (facing == EnumFacing.NORTH) { return Rotation.NONE; }
        if (facing == EnumFacing.EAST) { return Rotation.CLOCKWISE_90; }
        if (facing == EnumFacing.WEST) { return Rotation.COUNTERCLOCKWISE_90; }
        return Rotation.CLOCKWISE_180;
    }
}
