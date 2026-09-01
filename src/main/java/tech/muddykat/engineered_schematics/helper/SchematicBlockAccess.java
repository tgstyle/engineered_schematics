package tech.muddykat.engineered_schematics.helper;

import tech.muddykat.engineered_schematics.item.SchematicProjection;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class SchematicBlockAccess implements IBlockAccess {
    private static final IBlockState AIR = Blocks.AIR.getDefaultState();
    private final Map<BlockPos, IBlockState> states = new HashMap<>();

    public SchematicBlockAccess(SchematicProjection projection) {
        projection.processAll((layer, info) -> {
            this.states.put(info.tPos, info.getModifiedState());
            return false;
        });
    }

    @Override @Nullable public TileEntity getTileEntity(@Nonnull BlockPos pos) { return null; }

    @Override public int getCombinedLight(@Nonnull BlockPos pos, int lightValue) { return 0xF000F0; }

    @Override @Nonnull public IBlockState getBlockState(@Nonnull BlockPos pos) {
        IBlockState state = this.states.get(pos);
        return state == null ? AIR : state;
    }

    @Override public boolean isAirBlock(@Nonnull BlockPos pos) { return !this.states.containsKey(pos); }

    @Override @Nonnull public Biome getBiome(@Nonnull BlockPos pos) { return Biomes.PLAINS; }

    @Override public int getStrongPower(@Nonnull BlockPos pos, @Nonnull EnumFacing direction) { return 0; }

    @Override @Nonnull public WorldType getWorldType() { return WorldType.DEFAULT; }

    @Override public boolean isSideSolid(@Nonnull BlockPos pos, @Nonnull EnumFacing side, boolean defaultValue) { return getBlockState(pos).isSideSolid(this, pos, side); }
}
