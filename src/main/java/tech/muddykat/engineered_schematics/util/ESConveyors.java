package tech.muddykat.engineered_schematics.util;

import blusunrize.immersiveengineering.api.tool.ConveyorHandler;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class ESConveyors {
    public static final String DEFAULT_TYPE = "immersiveengineering:conveyor";
    private static final String KEY_TYPE = "conveyorType";
    private static final Map<String, EnumFacing> FORMATION_FACINGS = formationFacings();

    private ESConveyors() {}

    private static Map<String, EnumFacing> formationFacings() {
        Map<String, EnumFacing> facings = new HashMap<>();
        facings.put("IE:BottlingMachine", EnumFacing.EAST);
        facings.put("IE:MetalPress", EnumFacing.SOUTH);
        return facings;
    }

    @Nullable public static EnumFacing formationFacing(String uniqueName) { return FORMATION_FACINGS.get(uniqueName); }

    public static boolean isConveyor(ItemStack stack) { return !stack.isEmpty() && Block.getBlockFromItem(stack.getItem()) == ConveyorHandler.conveyorBlock; }

    public static String typeOf(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.hasKey(KEY_TYPE) ? nbt.getString(KEY_TYPE) : DEFAULT_TYPE;
    }

    public static void applySubtype(World world, BlockPos pos, ItemStack stack, @Nullable EnumFacing facing) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof ConveyorHandler.IConveyorTile)) { return; }
        ConveyorHandler.IConveyorBelt belt = ConveyorHandler.getConveyor(new ResourceLocation(typeOf(stack)), tile);
        if (belt == null) { return; }
        ((ConveyorHandler.IConveyorTile)tile).setConveyorSubtype(belt);
        if (facing != null && tile instanceof IEBlockInterfaces.IDirectionalTile) { ((IEBlockInterfaces.IDirectionalTile)tile).setFacing(facing); }
        tile.markDirty();
        IBlockState state = world.getBlockState(pos);
        world.notifyBlockUpdate(pos, state, state, 3);
    }
}
