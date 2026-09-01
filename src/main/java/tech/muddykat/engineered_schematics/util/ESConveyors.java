package tech.muddykat.engineered_schematics.util;

import blusunrize.immersiveengineering.api.tool.ConveyorHandler;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ESConveyors {
    public static final String DEFAULT_TYPE = "immersiveengineering:conveyor";
    private static final String KEY_TYPE = "conveyorType";

    private ESConveyors() {}

    public static boolean isConveyor(ItemStack stack) { return !stack.isEmpty() && Block.getBlockFromItem(stack.getItem()) == ConveyorHandler.conveyorBlock; }

    public static String typeOf(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey(KEY_TYPE) ? nbt.getString(KEY_TYPE) : DEFAULT_TYPE;
    }

    public static void applySubtype(World world, BlockPos pos, ItemStack stack) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof ConveyorHandler.IConveyorTile)) { return; }
        ConveyorHandler.IConveyorBelt belt = ConveyorHandler.getConveyor(new ResourceLocation(typeOf(stack)), tile);
        if (belt == null) { return; }
        ((ConveyorHandler.IConveyorTile)tile).setConveyorSubtype(belt);
        tile.markDirty();
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }
}
